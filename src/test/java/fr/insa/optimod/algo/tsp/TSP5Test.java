package fr.insa.optimod.algo.tsp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

import fr.insa.optimod.algo.tsp.TSP.ResultatCalculTSP;

public class TSP5Test {
    /** Nombre de sommets : l'entrepôt et 25 couples collecte-livraison. */
    private static final int NB_SOMMETS_TEST_TOURNEE_NON_OPTIMALE = 51;
    /**
     * Limite de temps en millisecondes : trop courte pour examiner toutes les
     * solutions de 51 sommets, assez longue pour en trouver une.
     */
    private static final int LIMITE_MS = 100;

    private static final int nbSommets = 10;
    private static final int COUT_MAX = 40;
    private static final int COUT_MIN = 10;
    private static GrapheDepandance grapheDepandance;
    private static TSP5 tsp5;

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

        tsp5 = new TSP5();
        tsp5.chercherSolution(10000, grapheDepandance); // Graphe à 10 sommet et temps limite =1s
    }

    /** TSP5 renvoie le cout de la meilleur solution. */
    @Test
    public void testTSP5() {
        // Test TSP5 sur un graphe complet à 10 sommet et vérifie le resultat
        int somme = 0;
        for (int i = 1; i < nbSommets; i++) {
            somme += grapheDepandance.obtenirCoutArc(tsp5.obtenirSommetSolution(i - 1), tsp5.obtenirSommetSolution(i));
        }
        somme += grapheDepandance.obtenirCoutArc(tsp5.obtenirSommetSolution(nbSommets - 1), 0);
        assertEquals(somme, tsp5.obtenirCoutSolution());
    }

    /** Test de la fonction bound de TSP5 */
    @Test
    public void testBound() {
        // Verification que la borne inferieur calcule est la bonne
        assertEquals(131, tsp5.calculerBorneInferieure(0, Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9)));
        assertEquals(99, tsp5.calculerBorneInferieure(5, Arrays.asList(2, 4, 3, 7, 8, 9)));
    }

    /** Test de la fonction croisementArc de TSP5 */
    @Test
    public void testCroisementArc() {
        // TSP5 n'utilise pas la fonction croisementArc, elle renvoie toujours false
        assertEquals(false, tsp5.croisementArc(null, null, null, null));
    }

    /** Test de la fonction trierParDistance de TSP5 */
    @Test
    public void testTrierParDistance1() {
        // Verification que la liste renvoye est trier par distance croissante du sommet
        // courant

        // Test 1
        ArrayList<Integer> list = new ArrayList<Integer>(Arrays.asList(1, 2, 4, 5, 6, 7, 8, 9));
        Integer sommetDepart = 0;
        Iterator<Integer> iterator = tsp5.creerIterateurSuccesseurs(sommetDepart, list, grapheDepandance);
        if (!iterator.hasNext())
            return;
        Integer precedent = iterator.next();
        while (iterator.hasNext()) {
            Integer suivant = iterator.next();
            assertTrue(grapheDepandance.obtenirCoutArc(sommetDepart, precedent) <= grapheDepandance.obtenirCoutArc(
                    sommetDepart,
                    suivant));
            precedent = suivant;
        }
    }

    @Test
    public void testTrierParDistance2() {
        ArrayList<Integer> list = new ArrayList<Integer>(Arrays.asList(1, 2, 4, 5, 6, 7, 8, 9));
        Integer sommetDepart = 3;
        Iterator<Integer> iterator = tsp5.creerIterateurSuccesseurs(sommetDepart, list, grapheDepandance);
        if (!iterator.hasNext())
            return;
        Integer precedent = iterator.next();
        while (iterator.hasNext()) {
            Integer suivant = iterator.next();
            assertTrue(grapheDepandance.obtenirCoutArc(sommetDepart, precedent) <= grapheDepandance.obtenirCoutArc(
                    sommetDepart,
                    suivant));
            precedent = suivant;
        }
    }

    /** Graphe de coûts tirés au hasard (graine fixe), collecte avant livraison. */
    private static GrapheDepandance graphe() {
        Random hasard = new Random(7);
        int[][] cout = new int[NB_SOMMETS_TEST_TOURNEE_NON_OPTIMALE][NB_SOMMETS_TEST_TOURNEE_NON_OPTIMALE];
        for (int i = 0; i < NB_SOMMETS_TEST_TOURNEE_NON_OPTIMALE; i++) {
            for (int j = 0; j < NB_SOMMETS_TEST_TOURNEE_NON_OPTIMALE; j++) {
                cout[i][j] = i == j ? -1 : 10 + hasard.nextInt(1000);
            }
        }
        HashMap<Integer, Integer> collecteAvantLivraison = new HashMap<>();
        for (int livraison = 2; livraison < NB_SOMMETS_TEST_TOURNEE_NON_OPTIMALE; livraison += 2) {
            collecteAvantLivraison.put(livraison, livraison - 1);
        }
        return new GrapheDepandance(NB_SOMMETS_TEST_TOURNEE_NON_OPTIMALE, cout, collecteAvantLivraison);
    }

    /**
     * Avec une limite très courte, la recherche est coupée : le résultat n'est
     * pas {@code RESULTAT_OK} (solution non prouvée optimale), mais la
     * meilleure tournée trouvée est complète et respecte l'ordre
     * collecte-livraison.
     */
    @Test
    public void limiteTresCourteRenvoieUneTourneeValideNonProuvee() {
        GrapheDepandance graphe = graphe();
        TSP5 tsp = new TSP5();
        ResultatCalculTSP resultat = tsp.chercherSolution(LIMITE_MS, graphe);
        assertEquals(ResultatCalculTSP.CALCUL_TROP_LONG, resultat);

        Set<Integer> vus = new HashSet<>();
        for (int rang = 0; rang < NB_SOMMETS_TEST_TOURNEE_NON_OPTIMALE; rang++) {
            Integer sommet = tsp.obtenirSommetSolution(rang);
            assertNotNull(sommet);
            int dependance = graphe.getDepandance(sommet);
            assertTrue("livraison avant collecte",
                    dependance == -1 || vus.contains(dependance));
            assertTrue("un sommet est visité plusieurs fois", vus.add(sommet));
        }
        assertEquals(NB_SOMMETS_TEST_TOURNEE_NON_OPTIMALE, vus.size());
    }
}
