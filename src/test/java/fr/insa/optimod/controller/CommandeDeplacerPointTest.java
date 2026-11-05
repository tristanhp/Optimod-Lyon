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
 * Tests de {@link CommandeDeplacerPoint}.
 */
public class CommandeDeplacerPointTest {

    private static Noeud noeud(long identifiant) {
        return new Noeud(identifiant, 0, 0);
    }

    private static final LocalTime DEPART = LocalTime.of(8, 0);

    private final Demande avant = new Demande(noeud(1), noeud(2), 60, 60);

    /** La même demande, avec son point de collecte déplacé. */
    private final Demande apres = new Demande(noeud(5), noeud(2), 60, 60);

    private final ProgrammeLivraisons programme = new ProgrammeLivraisons(
            noeud(0), DEPART, List.of(avant));

    /** Arrêt de l'entrepôt, au départ ou au retour. */
    private static Tournee.Arret entrepot(LocalTime heure) {
        return new Tournee.Arret(noeud(0), heure, heure, TypeArret.ENTREPOT,
                null);
    }

    /** Tournée du seul livreur, qui sert cette demande. */
    private static Tournee tourneeDe(Demande demande, LocalTime retour) {
        return new Tournee(List.of(entrepot(DEPART),
                new Tournee.Arret(demande.collecte(), DEPART, DEPART,
                        TypeArret.COLLECTE, demande),
                new Tournee.Arret(demande.livraison(), DEPART, DEPART,
                        TypeArret.LIVRAISON, demande),
                entrepot(retour)), List.of());
    }

    private final Map<Integer, Tournee> tourneesAvant = Map.of(1,
            tourneeDe(avant, LocalTime.of(9, 0)));

    private final Map<Integer, Tournee> tourneesApres = Map.of(1,
            tourneeDe(apres, LocalTime.of(9, 30)));

    private CommandeDeplacerPoint commande() {
        return new CommandeDeplacerPoint(programme, 0, apres, tourneesAvant,
                tourneesApres);
    }

    /** Exécuter remplace la demande et range les tournées d'après. */
    @Test
    public void executerRemplaceLaDemandeEtRangeLesTournees() {
        programme.definirTournees(tourneesAvant);
        commande().executer();
        assertEquals(List.of(apres), programme.obtenirDemandes());
        assertSame(tourneesApres.get(1), programme.obtenirTournees().get(1));
    }

    /** Annuler remet la demande et les tournées d'avant. */
    @Test
    public void annulerRemetLaDemandeEtLesTourneesDAvant() {
        programme.definirTournees(tourneesAvant);
        Commande commande = commande();
        commande.executer();
        commande.annuler();
        assertEquals(List.of(avant), programme.obtenirDemandes());
        assertSame(tourneesAvant.get(1), programme.obtenirTournees().get(1));
    }

    /**
     * Un observateur ne voit jamais d'état intermédiaire : à chaque
     * notification, les arrêts de la tournée sont ceux de la demande du
     * programme. Exécuter, annuler et rétablir notifient chacun une seule
     * fois.
     */
    @Test
    public void lesObservateursNeVoientAucunEtatIntermediaire() {
        programme.definirTournees(tourneesAvant);
        List<String> incoherences = new ArrayList<>();
        int[] notifications = { 0 };
        programme.ajouterObservateur(source -> {
            notifications[0]++;
            Demande courante = programme.obtenirDemandes().get(0);
            boolean presente = programme.obtenirTournees().get(1).arrets()
                    .stream()
                    .anyMatch(arret -> courante.equals(arret.demande()));
            if (!presente) {
                incoherences.add("notification " + notifications[0]);
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
}
