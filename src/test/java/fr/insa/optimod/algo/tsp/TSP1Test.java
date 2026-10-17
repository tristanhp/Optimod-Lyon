package fr.insa.optimod.algo.tsp;

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class TSP1Test {
    TSP1 tsp1;

    @Before
    public void initialisation() {
        // Executé avant chaque test
        tsp1 = new TSP1();
        tsp1.chercheSolution(10000, new GrapheComplet(10)); // Graphe à 10 sommet et temps limite =1s
    }

    /** TSP1 renvoie le cout de la meilleur solution. */
    @Test
    public void testTSP1() {
        // Test TSP1 sur un graphe complet à 10 sommet et vérifie le resultat
        // System.out.println("Cout graphe : " +
        // Integer.toString(tsp1.getCoutSolution()));
        System.out.println("Duree TPS1 : " + Long.toString(tsp1.getDureeCalcul()) + "ms");
        assertTrue(tsp1.getCoutSolution() == 134); // VERIFIER QUE C'EST LA BONNE REPONSE
    }

    /** Le 4e indice de la meilleure solution */
    @Test
    public void testGetSolution() {
        // Test TSP1 sur un graphe complet à 10 sommet et vérifie l'indice du sommet n°4
        // System.out.println("Solution a l'indice 4 : " +
        // Integer.toString(tsp1.getSolution(4)));
        assertTrue(tsp1.getSolution(4) == 4);
    }

    /** IGNORE. */
    @Test
    public void testBound() {
        // TODO : Faire
        assertTrue(0 == 0);
    }
}
