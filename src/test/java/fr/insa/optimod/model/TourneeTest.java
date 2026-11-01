package fr.insa.optimod.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalTime;
import java.util.List;

import org.junit.Test;

import fr.insa.optimod.model.ProgrammeLivraisons.Demande;
import fr.insa.optimod.model.Tournee.Arret;
import fr.insa.optimod.model.Tournee.TypeArret;

/**
 * Tests de {@link Tournee}.
 */
public class TourneeTest {

    private static final Noeud ENTREPOT = new Noeud(1, 4.85, 45.75);
    private static final Noeud A = new Noeud(2, 4.86, 45.76);
    private static final Noeud B = new Noeud(3, 4.87, 45.77);
    private static final Demande DEMANDE = new Demande(A, B, 60, 60);

    /** Tournée entrepôt, collecte, livraison, entrepôt, retour à l'heure. */
    private static Tournee tourneeRevenantA(LocalTime retour) {
        LocalTime depart = LocalTime.of(8, 0);
        return new Tournee(List.of(
                new Arret(ENTREPOT, depart, depart, TypeArret.ENTREPOT, null),
                new Arret(A, depart, depart, TypeArret.COLLECTE, DEMANDE),
                new Arret(B, depart, depart, TypeArret.LIVRAISON, DEMANDE),
                new Arret(ENTREPOT, retour, retour, TypeArret.ENTREPOT,
                        null)), List.of());
    }

    /** Une tournée vide part de l'entrepôt et n'a aucun chemin. */
    @Test
    public void tourneeVide() {
        Tournee tournee = Tournee.vide(ENTREPOT, LocalTime.of(8, 0));
        assertEquals(1, tournee.arrets().size());
        Arret depart = tournee.arrets().get(0);
        assertEquals(ENTREPOT, depart.noeud());
        assertEquals(TypeArret.ENTREPOT, depart.type());
        assertNull(depart.demande());
        assertTrue(tournee.chemins().isEmpty());
        assertEquals(LocalTime.of(8, 0), tournee.obtenirHeureRetour());
        assertTrue(tournee.estPossible());
    }

    /** Un retour à 13 h pile est encore possible. */
    @Test
    public void retourA13hPossible() {
        assertTrue(tourneeRevenantA(LocalTime.of(13, 0)).estPossible());
    }

    /** Un retour après 13 h rend la tournée impossible. */
    @Test
    public void retourApres13hImpossible() {
        assertFalse(tourneeRevenantA(LocalTime.of(13, 0, 1)).estPossible());
    }

    /** L'entrepôt n'a pas de demande, une collecte ou une livraison si. */
    @Test(expected = IllegalArgumentException.class)
    public void entrepotAvecDemandeRefuse() {
        new Arret(ENTREPOT, LocalTime.of(8, 0), LocalTime.of(8, 0),
                TypeArret.ENTREPOT, DEMANDE);
    }

    /** Une collecte sans demande est refusée. */
    @Test(expected = IllegalArgumentException.class)
    public void collecteSansDemandeRefusee() {
        new Arret(A, LocalTime.of(8, 0), LocalTime.of(8, 0),
                TypeArret.COLLECTE, null);
    }
}
