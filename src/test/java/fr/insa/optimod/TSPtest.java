package fr.insa.optimod;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import fr.insa.optimod.algo.tsp.GrapheComplet;
import fr.insa.optimod.algo.tsp.TSP1;

public class TSPtest {
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
}
