package fr.insa.optimod.algo.tsp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import org.junit.BeforeClass;
import org.junit.Test;

/** Tests du voyageur de commerce {@link TSP3}. */
public class TSP3Test {
    private static final int NB_SOMMETS = 10;
    private static final int COUT_MAX = 40;
    private static final int COUT_MIN = 10;
    private static GrapheDepandance grapheDepandance;
    private static TSP3 tsp3;

    /** Prépare le graphe et le voyageur de commerce utilisés par les tests. */
    @BeforeClass
    public static void initialisation() {
        // Executé avant chaque test
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

        tsp3 = new TSP3();
        // Graphe à 10 sommet et temps limite =1s
        tsp3.chercherSolution(10000, grapheDepandance);
    }

    /** TSP3 renvoie le cout de la meilleur solution. */
    @Test
    public void testTSP3() {
        // Test TSP3 sur un graphe complet à 10 sommet et vérifie le resultat
        int somme = 0;
        for (int i = 1; i < NB_SOMMETS; i++) {
            somme += grapheDepandance.obtenirCoutArc(tsp3.obtenirSommetSolution(
                    i - 1), tsp3.obtenirSommetSolution(i));
        }
        somme += grapheDepandance.obtenirCoutArc(tsp3.obtenirSommetSolution(
                NB_SOMMETS - 1), 0);
        assertEquals(somme, tsp3.obtenirCoutSolution());
    }

    /** Test de la fonction bound de TSP3 */
    @Test
    public void testBound() {
        // Verification que la borne inferieur calcule est la bonne
        assertEquals(125, tsp3.calculerBorneInferieure(0, Arrays.asList(1, 2, 3,
                4, 5, 6, 7, 8, 9)));
        assertEquals(93, tsp3.calculerBorneInferieure(3, Arrays.asList(2, 4, 5,
                7, 8, 9)));
    }

    /** Test de la fonction croisementArc de TSP3 */
    @Test
    public void testCroisementArc() {
        // TSP3 n'utilise pas la fonction croisementArc, elle renvoie toujours
        // false
        assertEquals(false, tsp3.croisementArc(null, null, null, null));
    }

    /** Test de la fonction trierParDistance de TSP3 */
    @Test
    public void testTrierParDistance1() {
        // Verification que la liste renvoye est trier par distance croissante
        // du sommet
        // courant

        // Test 1
        ArrayList<Integer> list = new ArrayList<Integer>(Arrays.asList(1, 2, 4,
                5, 6, 7, 8, 9));
        Integer sommetDepart = 0;
        Iterator<Integer> iterator =
                tsp3.creerIterateurSuccesseurs(sommetDepart, list,
                grapheDepandance);
        if (!iterator.hasNext()) {
            return;
        }
        Integer precedent = iterator.next();
        while (iterator.hasNext()) {
            Integer suivant = iterator.next();
            System.out.println(
                    grapheDepandance.obtenirCoutArc(sommetDepart, precedent)
                    + " " + grapheDepandance.obtenirCoutArc(
                            sommetDepart, suivant));
            assertTrue(grapheDepandance.obtenirCoutArc(sommetDepart, precedent)
                    <= grapheDepandance.obtenirCoutArc(
                    sommetDepart,
                    suivant));
            precedent = suivant;
        }
    }

    /**
     * Crée l'itérateur des successeurs trié par distance depuis le sommet 3.
     */
    @Test
    public void testTrierParDistance2() {
        ArrayList<Integer> list = new ArrayList<Integer>(Arrays.asList(1, 2, 4,
                5, 6, 7, 8, 9));
        Integer sommetDepart = 3;
        Iterator<Integer> iterator =
                tsp3.creerIterateurSuccesseurs(sommetDepart, list,
                grapheDepandance);
        if (!iterator.hasNext()) {
            return;
        }
        Integer precedent = iterator.next();
        while (iterator.hasNext()) {
            Integer suivant = iterator.next();
            assertTrue(grapheDepandance.obtenirCoutArc(sommetDepart, precedent)
                    <= grapheDepandance.obtenirCoutArc(
                    sommetDepart,
                    suivant));
            precedent = suivant;
        }
    }
}
