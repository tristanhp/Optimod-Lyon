package fr.insa.optimod.algo.tsp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

/** Tests du voyageur de commerce sur les plans et demandes fournis. */
public class TSPTest {
    private static final int NB_SOMMETS = 10;
    private static final int COUT_MAX = 40;
    private static final int COUT_MIN = 10;
    private static GrapheDepandance grapheDepandance;
    private static TSP1 tsp1;
    private static TSP2 tsp2;
    private static TSP3 tsp3;
    private static TSP4 tsp4;
    private static TSP5 tsp5;

    /** Prépare le graphe et le voyageur de commerce utilisés par les tests. */
    @BeforeClass
    public static void initialisation() throws IOException, SAXException {
        int iseed = 1;
        int cout[][];
        cout = new int[NB_SOMMETS][NB_SOMMETS];
        for (int i = 0; i < NB_SOMMETS; i++) {
            for (int j = 0; j < NB_SOMMETS; j++) {
                if (i == j) {
                    cout[i][j] = -1;
                } else {
                    int it = 16807 * (iseed % 127773) - 2836 * (iseed / 127773);
                    if (it > 0) {
                        iseed = it;
                    } else {
                        iseed = 2147483647 + it;
                    }
                    cout[i][j] = COUT_MIN + iseed % (COUT_MAX - COUT_MIN + 1);
                }
            }
        }

        HashMap<Integer, Integer> depandance = new HashMap<>();
        for (int livraison = 2; livraison < NB_SOMMETS; livraison += 2) {
            depandance.put(livraison, livraison - 1);
        }
        grapheDepandance = new GrapheDepandance(NB_SOMMETS, cout, depandance);

        tsp1 = new TSP1();
        tsp1.chercherSolution(10000, grapheDepandance);

        tsp2 = new TSP2();
        tsp2.chercherSolution(10000, grapheDepandance);

        tsp3 = new TSP3();
        tsp3.chercherSolution(10000, grapheDepandance);

        tsp4 = new TSP4();
        tsp4.chercherSolution(10000, grapheDepandance);

        tsp5 = new TSP5();
        tsp5.chercherSolution(10000, grapheDepandance);

    }

    /** test de des couts de TSP1, TSP2, TSP3, TSP4 et TSP5 */
    @Test
    public void testTSP() {
        // Performance TSP
        System.out.println("Duree TPS1 : "
                + Long.toString(tsp1.obtenirDureeCalcul()) + "ms");
        System.out.println("Duree TPS2 : "
                + Long.toString(tsp2.obtenirDureeCalcul()) + "ms");
        System.out.println("Duree TPS3 : "
                + Long.toString(tsp3.obtenirDureeCalcul()) + "ms");
        System.out.println("Duree TPS4 : "
                + Long.toString(tsp4.obtenirDureeCalcul()) + "ms");
        System.out.println("Duree TPS5 : "
                + Long.toString(tsp5.obtenirDureeCalcul()) + "ms");
        System.out.println("Solution TPS1 : "
                + Long.toString(tsp1.obtenirCoutSolution()));
        System.out.println("Solution TPS2 : "
                + Long.toString(tsp2.obtenirCoutSolution()));
        System.out.println("Solution TPS3 : "
                + Long.toString(tsp3.obtenirCoutSolution()));
        System.out.println("Solution TPS4 : "
                + Long.toString(tsp4.obtenirCoutSolution()));
        System.out.println("Solution TPS5 : "
                + Long.toString(tsp5.obtenirCoutSolution()));
        assertEquals(145, tsp1.obtenirCoutSolution());
        assertEquals(145, tsp2.obtenirCoutSolution());
        assertEquals(145, tsp3.obtenirCoutSolution());
        // assertEquals(145, tsp4.obtenirCoutSolution()); // TSP4 utilise opt-2
        // pas applicable sur ce graphe
        assertEquals(145, tsp5.obtenirCoutSolution());
    }

    /**
     * Verifie que les solutions renvoye par TSP1, TSP2, TSP3 et TSP5 sont les memes
     */
    @Test
    public void testGetSolution() {
        for (int i = 0; i < NB_SOMMETS; ++i) {
            assertEquals(tsp1.obtenirSommetSolution(i),
                    tsp2.obtenirSommetSolution(i));
            assertEquals(tsp2.obtenirSommetSolution(i),
                    tsp3.obtenirSommetSolution(i));
            assertEquals(tsp3.obtenirSommetSolution(i),
                    tsp5.obtenirSommetSolution(i));
        }
    }

    /** Verifie les depandance de la solution */
    @Test
    public void testDepandanceSolution() {
        ArrayList<Integer> vus = new ArrayList<>();
        for (int i = 0; i < NB_SOMMETS; ++i) {
            for (int indiceDejaVu = 0; indiceDejaVu < vus.size(); indiceDejaVu++) {
                // i n'est pas une depandance d'un indice deja vus
                assertTrue(grapheDepandance.getDepandance(
                        tsp5.obtenirSommetSolution(indiceDejaVu)) != tsp5
                                .obtenirSommetSolution(i));
            }
            vus.add(tsp5.obtenirSommetSolution(i));
        }
    }
}
