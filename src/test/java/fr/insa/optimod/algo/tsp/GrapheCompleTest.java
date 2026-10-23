package fr.insa.optimod.algo.tsp;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class GrapheCompleTest {
    GrapheComplet grapheComplet;

    @Before
    public void initialisation() {
        grapheComplet = new GrapheComplet(10);
    }

    /** Un graphe a bien le bon nombre de sommet. */
    @Test
    public void testNombreSommetGrapheComplet() {
        assertTrue(grapheComplet.obtenirNombreSommets() == 10);
    }

    /** Un graphe a 10 sommet a le bon coup sur une arret avec la seed 1. */
    @Test
    public void testGetCoutGrapheComplet() {
        assertTrue(grapheComplet.obtenirCoutArc(3, 7) == 37);
    }

    /** Un arc est entre deux sommets différents et pas 2 fois le meme sommet. */
    @Test
    public void testEstArcGrapheComplet() {
        // Un arc
        assertTrue(grapheComplet.estArc(3, 7));
        // Pas un arc
        assertFalse(grapheComplet.estArc(5, 5));
    }
}
