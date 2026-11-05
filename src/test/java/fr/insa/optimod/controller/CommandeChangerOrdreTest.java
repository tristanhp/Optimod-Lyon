package fr.insa.optimod.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import fr.insa.optimod.model.Noeud;
import fr.insa.optimod.model.ProgrammeLivraisons;
import fr.insa.optimod.model.ProgrammeLivraisons.Demande;
import fr.insa.optimod.model.Tournee;
import fr.insa.optimod.model.Tournee.TypeArret;

/**
 * Tests de {@link CommandeChangerOrdre}.
 */
public class CommandeChangerOrdreTest {

    private static Noeud noeud(long identifiant) {
        return new Noeud(identifiant, 0, 0);
    }

    private final Demande demande = new Demande(noeud(1), noeud(2), 60, 60);
    private final ProgrammeLivraisons programme = new ProgrammeLivraisons(
            noeud(0), LocalTime.of(8, 0), List.of(demande));

    /** Tournée d'un livreur qui revient à l'entrepôt à l'heure dite. */
    private Tournee tourneeRevenantA(LocalTime retour) {
        LocalTime depart = LocalTime.of(8, 0);
        return new Tournee(List.of(
                new Tournee.Arret(noeud(0), depart, depart, TypeArret.ENTREPOT,
                        null),
                new Tournee.Arret(noeud(1), depart, depart, TypeArret.COLLECTE,
                        demande),
                new Tournee.Arret(noeud(2), depart, depart,
                        TypeArret.LIVRAISON, demande),
                new Tournee.Arret(noeud(0), retour, retour,
                        TypeArret.ENTREPOT, null)),
                List.of());
    }

    private final Map<Integer, Tournee> avant = Map.of(1,
            tourneeRevenantA(LocalTime.of(9, 0)));
    private final Map<Integer, Tournee> apres = Map.of(1,
            tourneeRevenantA(LocalTime.of(9, 30)));

    /** Exécuter range les tournées d'après ; annuler remet celles d'avant. */
    @Test
    public void executerPuisAnnulerRemetLOrdrePrecedent() {
        programme.definirTournees(avant);
        Commande commande = new CommandeChangerOrdre(programme, 1, avant,
                apres);
        commande.executer();
        assertEquals(apres, programme.obtenirTournees());
        commande.annuler();
        assertEquals(avant, programme.obtenirTournees());
    }

    /** Une commande annulée peut être rétablie, sans aucun calcul. */
    @Test
    public void rejouerApresAnnulationRangeDeNouveau() {
        programme.definirTournees(avant);
        ListeDeCommandes historique = new ListeDeCommandes();
        historique.executer(new CommandeChangerOrdre(programme, 1, avant,
                apres));
        historique.annuler();
        assertSame(avant.get(1), programme.obtenirTournees().get(1));
        historique.refaire();
        assertSame(apres.get(1), programme.obtenirTournees().get(1));
    }

    /**
     * Les demandes ne changent pas : seule la tournée du livreur est remplacée.
     */
    @Test
    public void lesDemandesNeChangentPas() {
        programme.definirTournees(avant);
        new CommandeChangerOrdre(programme, 1, avant, apres).executer();
        assertEquals(List.of(demande), programme.obtenirDemandes());
    }

    /**
     * Les tournées calculées après coup (« Calculer ») remplacent celles que la
     * commande remet : annuler puis rétablir ne ramène pas les anciennes.
     */
    @Test
    public void tourneesMemoriseesApresUnCalculRemplacentCellesDeLaCommande() {
        programme.definirTournees(avant);
        Commande commande = new CommandeChangerOrdre(programme, 1, avant,
                apres);
        commande.executer();
        Map<Integer, Tournee> recalculees = Map.of(1,
                tourneeRevenantA(LocalTime.of(8, 45)));
        commande.memoriserTourneesApres(recalculees);
        commande.annuler();
        commande.executer();
        assertEquals(recalculees, programme.obtenirTournees());
        Map<Integer, Tournee> autresAvant = Map.of(1,
                tourneeRevenantA(LocalTime.of(8, 50)));
        commande.memoriserTourneesAvant(autresAvant);
        commande.annuler();
        assertEquals(autresAvant, programme.obtenirTournees());
    }
}
