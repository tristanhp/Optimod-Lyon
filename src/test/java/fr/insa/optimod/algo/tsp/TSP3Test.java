package fr.insa.optimod.algo.tsp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;

public class TSP3Test {
    private final int nbSommets = 10;
    private static final int COUT_MAX = 40;
    private static final int COUT_MIN = 10;
    GrapheDepandance grapheDepandance;
    TSP3 tsp3;

    @Before
    public void initialisation() {
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
        depandance.put(5, 0);
        depandance.put(6, 1);
        depandance.put(7, 2);
        depandance.put(8, 3);
        depandance.put(9, 4);

        grapheDepandance = new GrapheDepandance(nbSommets, cout, depandance);

        tsp3 = new TSP3();
        tsp3.chercheSolution(10000, grapheDepandance); // Graphe à 10 sommet et temps limite =1s
    }

    /** TSP3 renvoie le cout de la meilleur solution. */
    @Test
    public void testTSP3() {
        // Test TSP3 sur un graphe complet à 10 sommet et vérifie le resultat
        // System.out.println("Cout graphe : " +
        // Integer.toString(tsp3.getCoutSolution()));
        System.out.println("Duree TPS1 : " + Long.toString(tsp3.getDureeCalcul()) + "ms");
        assertEquals(145, tsp3.getCoutSolution()); // VERIFIER QUE C'EST LA BONNE REPONSE
    }

    /** Le 4e indice de la meilleure solution */
    @Test
    public void testGetSolution() {
        // Test TSP3 sur un graphe complet à 10 sommet et vérifie l'indice du sommet n°4
        // System.out.println("Solution a l'indice 4 : " +
        // Integer.toString(tsp3.getSolution(4)));
        assertEquals(tsp3.getSolution(4), Integer.valueOf(9));
    }

    /** Test de la fonction bound de TSP3 */
    @Test
    public void testBound() {
        // Verification que la borne inferieur calcule est la bonne
        assertEquals(125, tsp3.bound(0, Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9)));
        assertEquals(93, tsp3.bound(3, Arrays.asList(2, 4, 5, 7, 8, 9)));
    }

    /** Test de la fonction croisementArc de TSP3 */
    @Test
    public void testCroisementArc() {
        // TSP3 n'utilise pas la fonction croisementArc, elle renvoie toujours false
        assertEquals(false, tsp3.croisementArc(null, null, null, null));
    }

    /** Test de la fonction trierParDistance de TSP3 */
    @Test
    public void testTrierParDistance1() {
        // Verification que la liste renvoye est trier par distance croissante du sommet
        // courant

        // Test 1
        Collection<Integer> list = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9);
        Integer sommetDepart = 0;
        Iterator<Integer> iterator = tsp3.trieParDistance(sommetDepart, list, grapheDepandance).iterator();
        if (!iterator.hasNext())
            return;
        Integer precedent = iterator.next();
        while (iterator.hasNext()) {
            Integer suivant = iterator.next();
            assertTrue(grapheDepandance.getCout(sommetDepart, precedent) <= grapheDepandance.getCout(sommetDepart,
                    suivant));
            precedent = suivant;
        }
    }

    @Test
    public void testTrierParDistance2() {
        Collection<Integer> list = Arrays.asList(1, 4, 5, 6, 7, 9);
        Integer sommetDepart = 3;
        Iterator<Integer> iterator = tsp3.trieParDistance(sommetDepart, list, grapheDepandance).iterator();
        if (!iterator.hasNext())
            return;
        Integer precedent = iterator.next();
        while (iterator.hasNext()) {
            Integer suivant = iterator.next();
            assertTrue(grapheDepandance.getCout(sommetDepart, precedent) <= grapheDepandance.getCout(sommetDepart,
                    suivant));
            precedent = suivant;
        }
    }
}
