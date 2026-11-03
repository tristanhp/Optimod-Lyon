package fr.insa.optimod.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.LocalTime;

import org.junit.Test;

/** Vérifie les plages horaires annoncées des arrêts. */
public class TourneeTest {

    /** Les bornes sont inclusives et restent fixes lorsque l'arrivée change. */
    @Test
    public void plageUneHeureEtDepassement() {
        Noeud noeud = new Noeud(1, 0, 0);
        Tournee.Arret arret = new Tournee.Arret(noeud,
                LocalTime.of(9, 47), LocalTime.of(9, 52));
        assertEquals(LocalTime.of(8, 47), arret.debutPlage());
        assertEquals(LocalTime.of(10, 47), arret.finPlage());
        assertFalse(arret.estHorsPlage());
        assertFalse(new Tournee.Arret(noeud, LocalTime.of(10, 47),
                LocalTime.of(10, 52), arret.debutPlage(), arret.finPlage())
                        .estHorsPlage());
        assertTrue(new Tournee.Arret(noeud, LocalTime.of(10, 48),
                LocalTime.of(10, 53), arret.debutPlage(), arret.finPlage())
                        .estHorsPlage());
    }

    /** Une plage autour de minuit conserve son sens sur les deux jours. */
    @Test
    public void plageAutourDeMinuit() {
        Noeud noeud = new Noeud(1, 0, 0);
        Tournee.Arret arret = new Tournee.Arret(noeud,
                LocalTime.of(0, 30), LocalTime.of(0, 35));
        assertFalse(arret.estHorsPlage());
        assertFalse(new Tournee.Arret(noeud, LocalTime.of(23, 45),
                LocalTime.of(23, 50), arret.debutPlage(), arret.finPlage())
                        .estHorsPlage());
        assertTrue(new Tournee.Arret(noeud, LocalTime.of(12, 0),
                LocalTime.of(12, 5), arret.debutPlage(), arret.finPlage())
                        .estHorsPlage());
    }
}
