package fr.insa.optimod.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;

import org.junit.Test;

import fr.insa.optimod.model.Noeud;
import fr.insa.optimod.model.ProgrammeLivraisons;
import fr.insa.optimod.model.ProgrammeLivraisons.Demande;
import fr.insa.optimod.model.Tournee;
import fr.insa.optimod.model.Tournee.TypeArret;

/**
 * Tests de {@link CommandeChangerNombreLivreurs}.
 */
public class CommandeChangerNombreLivreursTest {

    private static final LocalTime DEPART = LocalTime.of(8, 0);

    private static Noeud noeud(long identifiant) {
        return new Noeud(identifiant, 0, 0);
    }

    private final ProgrammeLivraisons programme = new ProgrammeLivraisons(
            noeud(0), DEPART,
            List.of(new Demande(noeud(1), noeud(2), 60, 60)));

    /** Tournée d'un livreur sans demande : le seul départ de l'entrepôt. */
    private static Tournee tourneeVide() {
        return new Tournee(List.of(new Tournee.Arret(noeud(0), DEPART, DEPART,
                TypeArret.ENTREPOT, null)), List.of());
    }

    /** Une tournée par livreur, de 1 à ce nombre, toutes vides. */
    private static Map<Integer, Tournee> tourneesVides(int nombre) {
        Map<Integer, Tournee> tournees = new HashMap<>();
        for (int livreur = 1; livreur <= nombre; livreur++) {
            tournees.put(livreur, tourneeVide());
        }
        return tournees;
    }

    /** Numéros des livreurs qui ont une tournée, dans l'ordre. */
    private List<Integer> livreursAvecTournee() {
        return new ArrayList<>(programme.obtenirTournees().keySet());
    }

    /** Avant le calcul, régler le nombre ne touche à aucune tournée. */
    @Test
    public void executerChangeLeNombreSansTourneeAvantLeCalcul() {
        new CommandeChangerNombreLivreurs(programme, 3).executer();
        assertEquals(3, programme.obtenirNombreLivreurs());
        assertTrue(programme.obtenirTournees().isEmpty());
    }

    /** Annuler remet le nombre de livreurs d'avant. */
    @Test
    public void annulerRemetLeNombreDAvant() {
        Commande commande = new CommandeChangerNombreLivreurs(programme, 3);
        commande.executer();
        commande.annuler();
        assertEquals(1, programme.obtenirNombreLivreurs());
        assertTrue(programme.obtenirTournees().isEmpty());
    }

    /**
     * Après le calcul, un livreur ajouté a une tournée vide et les autres
     * tournées sont les mêmes objets.
     */
    @Test
    public void executerAjouteUneTourneeVideEtGardeLesAutres() {
        Map<Integer, Tournee> avant = tourneesVides(1);
        programme.definirTournees(avant);
        new CommandeChangerNombreLivreurs(programme, 3).executer();
        assertEquals(3, programme.obtenirNombreLivreurs());
        assertEquals(List.of(1, 2, 3), livreursAvecTournee());
        assertSame(avant.get(1), programme.obtenirTournees().get(1));
        assertEquals(1, programme.obtenirTournees().get(3).arrets().size());
    }

    /** Annuler remet exactement les tournées d'avant (les mêmes objets). */
    @Test
    public void annulerRemetExactementLesTourneesDAvant() {
        Map<Integer, Tournee> avant = tourneesVides(1);
        programme.definirTournees(avant);
        Commande commande = new CommandeChangerNombreLivreurs(programme, 3);
        commande.executer();
        commande.annuler();
        assertEquals(1, programme.obtenirNombreLivreurs());
        assertEquals(List.of(1), livreursAvecTournee());
        assertSame(avant.get(1), programme.obtenirTournees().get(1));
    }

    /** Rétablir remet les tournées d'après, les mêmes, sans aucun calcul. */
    @Test
    public void retablirRemetLesTourneesDApres() {
        programme.definirTournees(tourneesVides(1));
        ListeDeCommandes historique = new ListeDeCommandes();
        historique.executer(new CommandeChangerNombreLivreurs(programme, 2));
        Map<Integer, Tournee> apres = programme.obtenirTournees();
        historique.annuler();
        historique.refaire();
        assertEquals(2, programme.obtenirNombreLivreurs());
        assertEquals(List.of(1, 2), livreursAvecTournee());
        assertSame(apres.get(1), programme.obtenirTournees().get(1));
        assertSame(apres.get(2), programme.obtenirTournees().get(2));
    }

    /** Retirer un livreur sans demande, puis annuler, rend sa tournée. */
    @Test
    public void annulerUnRetraitRendLaTourneeDuLivreur() {
        programme.definirNombreLivreurs(2);
        Map<Integer, Tournee> avant = tourneesVides(2);
        programme.definirTournees(avant);
        Commande commande = new CommandeChangerNombreLivreurs(programme, 1);
        commande.executer();
        assertEquals(List.of(1), livreursAvecTournee());
        commande.annuler();
        assertEquals(2, programme.obtenirNombreLivreurs());
        assertSame(avant.get(1), programme.obtenirTournees().get(1));
        assertSame(avant.get(2), programme.obtenirTournees().get(2));
    }

    /**
     * Les tournées calculées après coup (« Calculer ») remplacent celles que la
     * commande remet : annuler puis rétablir ne ramène pas les anciennes.
     */
    @Test
    public void tourneesMemoriseesApresUnCalculRemplacentCellesDeLaCommande() {
        programme.definirTournees(tourneesVides(1));
        Commande commande = new CommandeChangerNombreLivreurs(programme, 2);
        commande.executer();
        Map<Integer, Tournee> recalculees = tourneesVides(2);
        commande.memoriserTourneesApres(recalculees);
        commande.annuler();
        commande.executer();
        assertSame(recalculees.get(1), programme.obtenirTournees().get(1));
        assertSame(recalculees.get(2), programme.obtenirTournees().get(2));
        Map<Integer, Tournee> autresAvant = tourneesVides(1);
        commande.memoriserTourneesAvant(autresAvant);
        commande.annuler();
        assertSame(autresAvant.get(1), programme.obtenirTournees().get(1));
    }

    /** Ajouter un livreur est confirmé en le nommant. */
    @Test
    public void confirmationDUnLivreurAjoute() {
        Commande commande = new CommandeChangerNombreLivreurs(programme, 2);
        commande.executer();
        assertEquals("Livreur 2 ajouté : il n'a encore aucune demande.",
                commande.confirmation());
    }

    /** Ajouter plusieurs livreurs d'un coup donne le nouveau total. */
    @Test
    public void confirmationDePlusieursLivreursAjoutes() {
        Commande commande = new CommandeChangerNombreLivreurs(programme, 4);
        commande.executer();
        assertEquals("4 livreurs : les nouveaux n'ont encore aucune demande.",
                commande.confirmation());
    }

    /** Retirer un livreur dit combien il en reste (pluriel ou singulier). */
    @Test
    public void confirmationDUnRetrait() {
        programme.definirNombreLivreurs(3);
        Commande versDeux = new CommandeChangerNombreLivreurs(programme, 2);
        versDeux.executer();
        assertEquals("Il reste 2 livreurs.", versDeux.confirmation());
        Commande versUn = new CommandeChangerNombreLivreurs(programme, 1);
        versUn.executer();
        assertEquals("Il reste 1 livreur.", versUn.confirmation());
    }

    /** La description nomme le réglage (messages « Annulé » et « Rétabli »). */
    @Test
    public void descriptionNommeLeNouveauNombre() {
        Commande commande = new CommandeChangerNombreLivreurs(programme, 3);
        commande.executer();
        assertEquals("réglage du nombre de livreurs à 3",
                commande.description());
    }

    /** Rétablir ne prévient les observateurs qu'une fois, sans état mixte. */
    @Test
    public void retablirNeNotifieQuUneFoisAvecDesTourneesCoherentes() {
        programme.definirTournees(tourneesVides(1));
        Commande commande = new CommandeChangerNombreLivreurs(programme, 2);
        commande.executer();
        Map<Integer, Tournee> apres = programme.obtenirTournees();
        commande.annuler();
        AtomicInteger notifications = new AtomicInteger();
        programme.ajouterObservateur(source -> {
            notifications.incrementAndGet();
            assertEquals(2, programme.obtenirNombreLivreurs());
            assertSame(apres.get(2), programme.obtenirTournees().get(2));
        });
        commande.executer();
        assertEquals(1, notifications.get());
    }

    /** Annuler ne prévient les observateurs qu'une fois. */
    @Test
    public void annulerNeNotifieQuUneFois() {
        programme.definirTournees(tourneesVides(1));
        Commande commande = new CommandeChangerNombreLivreurs(programme, 2);
        commande.executer();
        AtomicInteger notifications = new AtomicInteger();
        programme.ajouterObservateur(source -> notifications.incrementAndGet());
        commande.annuler();
        assertEquals(1, notifications.get());
    }

    /** Le nombre « d'avant » est celui de l'exécution, pas de la création. */
    @Test
    public void annulerRemetLeNombreVuALExecution() {
        Commande commande = new CommandeChangerNombreLivreurs(programme, 3);
        programme.definirNombreLivreurs(2);
        commande.executer();
        commande.annuler();
        assertEquals(2, programme.obtenirNombreLivreurs());
    }
}
