package fr.insa.optimod.algo.tsp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;

public class TSPTest {
    private final int nbSommets = 10;
    private static final int COUT_MAX = 40;
    private static final int COUT_MIN = 10;
    GrapheDepandance grapheDepandance;
    TSP1 tsp1;
    TSP2 tsp2;
    TSP3 tsp3;

    @Before
    public void initialisation() {
        int iseed = 1;
        int cout[][];
        cout = new int[nbSommets][nbSommets];
        for (int i = 0; i < nbSommets; i++) {
            for (int j = 0; j < nbSommets; j++) {
                if (i == j)
                    cout[i][j] = -1;
                else {
                    int it = 16807 * (iseed % 127773) - 2836 * (iseed / 127773);
                    if (it > 0)
                        iseed = it;
                    else
                        iseed = 2147483647 + it;
                    cout[i][j] = COUT_MIN + iseed % (COUT_MAX - COUT_MIN + 1);
                }
            }
        }
        HashMap<Integer, Integer> depandance = new HashMap<>();
        depandance.put(5, 0);
        depandance.put(6, 1);
        depandance.put(7, 2);
        depandance.put(8, 3);
        depandance.put(9, 4);

        grapheDepandance = new GrapheDepandance(nbSommets, cout, depandance);

        tsp1 = new TSP1();
        tsp1.chercherSolution(10000, grapheDepandance); // Graphe à 10 sommet et temps limite =1s

        tsp2 = new TSP2();
        tsp2.chercherSolution(10000, grapheDepandance); // Graphe à 10 sommet et temps limite =1s

        tsp3 = new TSP3();
        tsp3.chercherSolution(10000, grapheDepandance); // Graphe à 10 sommet et temps limite =1s

    }

    /** test de TSP1 */
    @Test
    public void testTSP() {
        // TODO : FAIRE UN TEST MEILLEUR

        /*
         * System.out.println("Cout graphe : " +
         * Integer.toString(tsp1.obtenirCoutSolution()));
         * System.out.println("Duree TPS1 : " + Long.toString(tsp1.obtenirDureeCalcul()) +
         * "ms");
         *
         * System.out.println("Graphe : [\n");
         * for (int i = 0; i < 10; ++i) {
         * for (int j = 0; j < 10; ++j) {
         * System.out.print(Integer.toString(grapheDepandance.obtenirCoutArc(i, j)) + ", ");
         * }
         * System.out.println("");
         * }
         */

        // Performance TSP1
        System.out.println("Duree TPS1 : " + Long.toString(tsp1.obtenirDureeCalcul()) + "ms");
        System.out.println("Duree TPS2 : " + Long.toString(tsp2.obtenirDureeCalcul()) + "ms");
        System.out.println("Duree TPS3 : " + Long.toString(tsp3.obtenirDureeCalcul()) + "ms");
        System.out.println("Solution TPS1 : " + Long.toString(tsp1.obtenirCoutSolution()));
        System.out.println("Solution TPS2 : " + Long.toString(tsp2.obtenirCoutSolution()));
        System.out.println("Solution TPS3 : " + Long.toString(tsp3.obtenirCoutSolution()));
        assertEquals(145, tsp1.obtenirCoutSolution()); // VERIFIER QUE C'EST LA BONNE REPONSE
        assertEquals(145, tsp2.obtenirCoutSolution()); // VERIFIER QUE C'EST LA BONNE REPONSE
        assertEquals(145, tsp3.obtenirCoutSolution()); // VERIFIER QUE C'EST LA BONNE REPONSE
    }

    /** Le 4e indice de la meilleure solution */
    @Test
    public void testGetSolution() {
        assertEquals(tsp1.obtenirSommetSolution(4), Integer.valueOf(9));
    }

    /** Verifie les depandance de la solution */
    @Test
    public void testDepandanceSolution() {
        ArrayList<Integer> vus = new ArrayList<>();
        for (int i = 0; i < nbSommets; ++i) {
            for (int indiceDejaVu = 0; indiceDejaVu < vus.size(); indiceDejaVu++) {
                // i n'est pas une depandance d'un indice deja vus
                assertTrue(grapheDepandance.getDepandance(indiceDejaVu) != tsp1.obtenirSommetSolution(i));
            }
            vus.add(tsp1.obtenirSommetSolution(i));
            // System.out.print(tsp1.obtenirSommetSolution(i) + " ");
        }
        // System.out.println();
    }
}
