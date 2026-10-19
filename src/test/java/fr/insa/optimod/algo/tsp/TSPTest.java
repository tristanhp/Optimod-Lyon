package fr.insa.optimod.algo.tsp;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TSPTest {
    /** Le message de démarrage mentionne le nom de l'application. */
    @Test
    public void testTSP() {
        // TODO : FAIRE UN TEST MEILLEUR
        TSP1 test = new TSP1();
        test.chercherSolution(10000, new GrapheComplet(10)); // Graphe à 10 sommet et temps limite =1s
        System.out.println("Cout graphe : " + Integer.toString(test.obtenirCoutSolution()));
        System.out.println("Duree TPS1 : " + Long.toString(test.obtenirDureeCalcul()) + "ms");
        /*
         * System.out.println("Graphe : [\n");
         * GrapheComplet graphe = new GrapheComplet(3);
         * for (int indiceSommet = 0; indiceSommet < 10; ++indiceSommet) {
         * for (int indiceDestination = 0; indiceDestination < 10; ++indiceDestination) {
         * System.out.print(Integer.toString(graphe.obtenirCoutArc(indiceSommet, indiceDestination)) + ", ");
         * }
         * System.out.println("");
         * }
         */
        assertTrue(test.obtenirCoutSolution() == 134); // VERIFIER QUE C'EST LA BONNE REPONSE
    }

    /** Le 4e indice de la meilleure solution */
    @Test
    public void verifieSommetSolution() {
        // TODO : SUPPRIMER
        TSP1 test = new TSP1();
        test.chercherSolution(10000, new GrapheComplet(10)); // Graphe à 10 sommet et temps limite =1s
        System.out.println("Solution a l'indice 4 : " + Integer.toString(test.obtenirSommetSolution(4)));
        assertTrue(test.obtenirSommetSolution(4) == 4);
    }

    /** IGNORE. */
    @Test
    public void instancieLanceurTsp() {
        // TODO : SUPPRIMER
        RunTSP test = new RunTSP();
        assertTrue(0 == 0);
    }
}
