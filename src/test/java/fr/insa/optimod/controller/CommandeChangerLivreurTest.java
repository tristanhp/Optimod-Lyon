package fr.insa.optimod.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import fr.insa.optimod.model.Noeud;
import fr.insa.optimod.model.ProgrammeLivraisons;
import fr.insa.optimod.model.ProgrammeLivraisons.Demande;
import fr.insa.optimod.model.Tournee;
import fr.insa.optimod.model.Tournee.TypeArret;

/**
 * Tests de {@link CommandeChangerLivreur}.
 */
public class CommandeChangerLivreurTest {

    private static Noeud noeud(long identifiant) {
        return new Noeud(identifiant, 0, 0);
    }

    private static final LocalTime DEPART = LocalTime.of(8, 0);

    private final Demande premiere = new Demande(noeud(1), noeud(2), 60, 60);
    private final Demande seconde = new Demande(noeud(3), noeud(4), 60, 60);
    private final ProgrammeLivraisons programme = new ProgrammeLivraisons(
            noeud(0), DEPART, List.of(premiere, seconde));

    {
        programme.definirNombreLivreurs(2);
    }

    /** Arrêt de l'entrepôt, au départ ou au retour. */
    private static Tournee.Arret entrepot(LocalTime heure) {
        return new Tournee.Arret(noeud(0), heure, heure, TypeArret.ENTREPOT,
                null);
    }

    /** Tournée d'un livreur sans demande : le seul départ de l'entrepôt. */
    private static Tournee tourneeVide() {
        return new Tournee(List.of(entrepot(DEPART)), List.of(), true);
    }

    /** Tournée d'un livreur qui sert cette demande et rentre à l'heure dite. */
    private static Tournee tourneeDe(Demande demande, LocalTime retour) {
        return new Tournee(List.of(entrepot(DEPART),
                new Tournee.Arret(demande.collecte(), DEPART, DEPART,
                        TypeArret.COLLECTE, demande),
                new Tournee.Arret(demande.livraison(), DEPART, DEPART,
                        TypeArret.LIVRAISON, demande),
                entrepot(retour)), List.of(), true);
    }

    /** La seconde demande, confiée au livreur 2. */
    private final Demande secondeChezLeLivreur2 = seconde.avecLivreur(2);

    /** Livreur 1 avec les deux demandes, livreur 2 sans demande. */
    private final Map<Integer, Tournee> avant = Map.of(
            1, tourneeDe(seconde, LocalTime.of(9, 0)),
            2, tourneeVide());

    /** Livreur 1 sans la seconde demande, livreur 2 avec. */
    private final Map<Integer, Tournee> apres = Map.of(
            1, tourneeVide(),
            2, tourneeDe(secondeChezLeLivreur2, LocalTime.of(8, 30)));

    private CommandeChangerLivreur commande() {
        return new CommandeChangerLivreur(programme, 1, secondeChezLeLivreur2,
                avant, apres);
    }

    /**
     * Exécuter confie la demande à l'autre livreur sans changer son rang dans
     * le programme, et range les tournées d'après.
     */
    @Test
    public void executerChangeLeLivreurSansChangerLeRang() {
        programme.definirTournees(avant);
        commande().executer();
        assertEquals(List.of(premiere, secondeChezLeLivreur2),
                programme.obtenirDemandes());
        assertEquals(apres, programme.obtenirTournees());
    }

    /**
     * Annuler remet exactement la demande d'avant et les tournées d'avant (les
     * mêmes objets).
     */
    @Test
    public void annulerRemetExactementDemandesEtTournees() {
        programme.definirTournees(avant);
        Commande commande = commande();
        commande.executer();
        commande.annuler();
        assertEquals(List.of(premiere, seconde), programme.obtenirDemandes());
        assertSame(avant.get(1), programme.obtenirTournees().get(1));
        assertSame(avant.get(2), programme.obtenirTournees().get(2));
    }

    /**
     * Un observateur ne voit jamais d'état intermédiaire : à chaque
     * notification, la seconde demande est dans la tournée de son livreur.
     * Exécuter, annuler et rétablir notifient chacun une seule fois.
     */
    @Test
    public void lesObservateursNeVoientAucunEtatIntermediaire() {
        programme.definirTournees(avant);
        List<String> incoherences = new ArrayList<>();
        int[] notifications = { 0 };
        programme.ajouterObservateur(source -> {
            notifications[0]++;
            Demande courante = programme.obtenirDemandes().get(1);
            boolean presente = programme.obtenirTournees()
                    .get(courante.livreur()).arrets().stream()
                    .anyMatch(arret -> courante.equals(arret.demande()));
            if (!presente) {
                incoherences.add("livreur " + courante.livreur());
            }
        });
        ListeDeCommandes historique = new ListeDeCommandes();
        historique.executer(commande());
        assertEquals(1, notifications[0]);
        historique.annuler();
        assertEquals(2, notifications[0]);
        historique.refaire();
        assertEquals(3, notifications[0]);
        assertEquals(List.of(), incoherences);
    }

    /** Rétablir range de nouveau les tournées d'après, sans aucun calcul. */
    @Test
    public void retablirRangeDeNouveauLesTourneesDApres() {
        programme.definirTournees(avant);
        ListeDeCommandes historique = new ListeDeCommandes();
        historique.executer(commande());
        historique.annuler();
        historique.refaire();
        assertEquals(List.of(premiere, secondeChezLeLivreur2),
                programme.obtenirDemandes());
        assertSame(apres.get(1), programme.obtenirTournees().get(1));
        assertSame(apres.get(2), programme.obtenirTournees().get(2));
    }

    /**
     * Les tournées calculées après coup (« Calculer ») remplacent celles que la
     * commande remet : annuler puis rétablir ne ramène pas les anciennes.
     */
    @Test
    public void tourneesMemoriseesApresUnCalculRemplacentCellesDeLaCommande() {
        programme.definirTournees(avant);
        Commande commande = commande();
        commande.executer();
        Map<Integer, Tournee> recalculees = Map.of(
                1, tourneeVide(),
                2, tourneeDe(secondeChezLeLivreur2, LocalTime.of(8, 20)));
        commande.memoriserTourneesApres(recalculees);
        commande.annuler();
        commande.executer();
        assertEquals(recalculees, programme.obtenirTournees());
        Map<Integer, Tournee> autresAvant = Map.of(
                1, tourneeDe(seconde, LocalTime.of(8, 50)),
                2, tourneeVide());
        commande.memoriserTourneesAvant(autresAvant);
        commande.annuler();
        assertEquals(autresAvant, programme.obtenirTournees());
    }
}
