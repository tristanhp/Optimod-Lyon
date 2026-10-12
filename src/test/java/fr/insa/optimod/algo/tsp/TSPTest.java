package fr.insa.optimod.algo.tsp;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TSPTest {
    /** Le message de démarrage mentionne le nom de l'application. */
    @Test
    public void testTSP() {
        // TODO : FAIRE UN TEST MEILLEUR
        TSP1 test = new TSP1();
        test.chercheSolution(10000, new GrapheComplet(10)); // Graphe à 10 sommet et temps limite =1s
        System.out.println("Cout graphe : " + Integer.toString(test.getCoutSolution()));
        System.out.println("Duree TPS1 : " + Long.toString(test.getDureeCalcul()) + "ms");
        /*
         * System.out.println("Graphe : [\n");
         * GrapheComplet g = new GrapheComplet(3);
         * for (int i = 0; i < 10; ++i) {
         * for (int j = 0; j < 10; ++j) {
         * System.out.print(Integer.toString(g.getCout(i, j)) + ", ");
         * }
         * System.out.println("");
         * }
         */
        assertTrue(test.getCoutSolution() == 134); // VERIFIER QUE C'EST LA BONNE REPONSE
    }

    /** Le 4e indice de la meilleure solution */
    @Test
    public void testGetSolution() {
        // TODO : SUPPRIMER
        TSP1 test = new TSP1();
        test.chercheSolution(10000, new GrapheComplet(10)); // Graphe à 10 sommet et temps limite =1s
        System.out.println("Solution a l'indice 4 : " + Integer.toString(test.getSolution(4)));
        assertTrue(test.getSolution(4) == 4);
    }

    /** IGNORE. */
    @Test
    public void testBound() {
        // TODO : SUPPRIMER
        RunTSP test = new RunTSP();
        assertTrue(0 == 0);
    }
}
