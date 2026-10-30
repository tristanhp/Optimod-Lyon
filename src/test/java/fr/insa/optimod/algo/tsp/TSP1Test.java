package fr.insa.optimod.algo.tsp;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;

import org.junit.BeforeClass;
import org.junit.Test;

public class TSP1Test {
    private static final int nbSommets = 10;
    private static final int COUT_MAX = 40;
    private static final int COUT_MIN = 10;
    private static GrapheDepandance grapheDepandance;
    private static TSP1 tsp1;

    @BeforeClass
    public static void initialisation() {
        // Executé avant chaque test
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
        for (int livraison = 2; livraison < nbSommets; livraison += 2) {
            depandance.put(livraison, livraison - 1);
        }

        grapheDepandance = new GrapheDepandance(nbSommets, cout, depandance);

        tsp1 = new TSP1();
        tsp1.chercherSolution(10000, grapheDepandance, -1); // Graphe à 10 sommet et temps limite =10s
    }

    /** TSP1 renvoie le cout de la meilleur solution. */
    @Test
    public void testTSP1() {
        // Test TSP1 sur un graphe complet à 10 sommet et vérifie le resultat
        int somme = 0;
        for (int i = 1; i < nbSommets; i++) {
            somme += grapheDepandance.obtenirCoutArc(tsp1.obtenirSommetSolution(i - 1), tsp1.obtenirSommetSolution(i));
        }
        somme += grapheDepandance.obtenirCoutArc(tsp1.obtenirSommetSolution(nbSommets - 1), 0);
        assertEquals(somme, tsp1.obtenirCoutSolution());
    }

    /** Test de la fonction bound de TSP1 */
    @Test
    public void testBound() {
        // TSP1 n'utilise pas la fonction Bound, elle renvoie toujours 0
        assertEquals(0, tsp1.calculerBorneInferieure(null, null));
    }

    /** Test de la fonction croisementArc de TSP1 */
    @Test
    public void testCroisementArc() {
        // TSP1 n'utilise pas la fonction croisementArc, elle renvoie toujours false
        assertEquals(false, tsp1.croisementArc(null, null, null, null));
    }
}
