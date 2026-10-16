package fr.insa.optimod.algo.tsp;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GrapheCompleTest {

    /** Un graphe a bien le bon nombre de sommet. */
    @Test
    public void testNombreSommetGrapheComplet() {
        GrapheComplet grapheComplet = new GrapheComplet(10);
        System.out.println("Nombre de sommet : " + Integer.toString(grapheComplet.getNbSommets()));
        assertTrue(grapheComplet.getNbSommets() == 10);
    }

    /** Un graphe a 10 sommet a le bon coup sur une arret avec la seed 1. */
    @Test
    public void testGetCoutGrapheComplet() {
        GrapheComplet grapheComplet = new GrapheComplet(10);
        System.out.println("Cout de l'arrete (i=3, j=7) : " + Integer.toString(grapheComplet.getCout(3, 7)));
        assertTrue(grapheComplet.getCout(3, 7) == 37);
    }

    /** Un arc est entre deux sommets différents et pas 2 fois le meme sommet. */
    @Test
    public void testEstArcGrapheComplet() {
        GrapheComplet grapheComplet = new GrapheComplet(10);
        // Un arc
        System.out.println("Est arc (i=3, j=7) : " + Boolean.toString(grapheComplet.estArc(3, 7)));
        assertTrue(grapheComplet.estArc(3, 7));
        // Pas un arc
        System.out.println("Est arc (i=5, j=5) : " + Boolean.toString(grapheComplet.estArc(5, 5)));
        assertFalse(grapheComplet.estArc(5, 5));
    }
}
