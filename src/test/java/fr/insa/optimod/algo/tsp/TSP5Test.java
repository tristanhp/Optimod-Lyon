package fr.insa.optimod.algo.tsp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.junit.Test;

import fr.insa.optimod.algo.tsp.TSP.ResultatCalculTSP;

/** Tests du voyageur de commerce {@link TSP5} sous une limite de temps. */
public class TSP5Test {

    /** Nombre de sommets : l'entrepôt et huit couples collecte-livraison. */
    private static final int NB_SOMMETS = 17;

    /** Graphe de coûts tirés au hasard (graine fixe), collecte avant livraison. */
    private static GrapheDepandance graphe() {
        Random hasard = new Random(7);
        int[][] cout = new int[NB_SOMMETS][NB_SOMMETS];
        for (int i = 0; i < NB_SOMMETS; i++) {
            for (int j = 0; j < NB_SOMMETS; j++) {
                cout[i][j] = i == j ? -1 : 10 + hasard.nextInt(1000);
            }
        }
        HashMap<Integer, Integer> collecteAvantLivraison = new HashMap<>();
        for (int livraison = 2; livraison < NB_SOMMETS; livraison += 2) {
            collecteAvantLivraison.put(livraison, livraison - 1);
        }
        return new GrapheDepandance(NB_SOMMETS, cout, collecteAvantLivraison);
    }

    /**
     * Avec une limite très courte, la recherche est coupée : le résultat n'est
     * pas {@code RESULTAT_OK} (solution non prouvée optimale), mais la
     * meilleure tournée trouvée est complète et respecte l'ordre
     * collecte-livraison.
     */
    @Test
    public void limiteTresCourteRenvoieUneTourneeValideNonProuvee() {
        TSP5 tsp = new TSP5();
        ResultatCalculTSP resultat = tsp.chercherSolution(1, graphe());
        assertEquals(ResultatCalculTSP.CALCUL_TROP_LONG, resultat);

        Set<Integer> vus = new HashSet<>();
        for (int rang = 0; rang < NB_SOMMETS; rang++) {
            Integer sommet = tsp.obtenirSommetSolution(rang);
            assertNotNull(sommet);
            if (sommet != 0 && sommet % 2 == 0) {
                assertTrue("collecte avant livraison",
                        vus.contains(sommet - 1));
            }
            assertTrue("sommet visité une seule fois", vus.add(sommet));
        }
        assertEquals(NB_SOMMETS, vus.size());
    }
}
