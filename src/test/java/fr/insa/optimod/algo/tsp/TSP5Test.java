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

    /** Nombre de sommets : l'entrepôt et 25 couples collecte-livraison. */
    private static final int NB_SOMMETS = 51;

    /**
     * Limite de temps en millisecondes : trop courte pour examiner toutes les
     * solutions de 51 sommets, assez longue pour en trouver une.
     */
    private static final int LIMITE_MS = 100;

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
        GrapheDepandance graphe = graphe();
        TSP5 tsp = new TSP5();
        ResultatCalculTSP resultat = tsp.chercherSolution(LIMITE_MS, graphe);
        assertEquals(ResultatCalculTSP.CALCUL_TROP_LONG, resultat);

        Set<Integer> vus = new HashSet<>();
        for (int rang = 0; rang < NB_SOMMETS; rang++) {
            Integer sommet = tsp.obtenirSommetSolution(rang);
            assertNotNull(sommet);
            int dependance = graphe.getDepandance(sommet);
            assertTrue("livraison avant collecte",
                    dependance == -1 || vus.contains(dependance));
            assertTrue("un sommet est visité plusieurs fois", vus.add(sommet));
        }
        assertEquals(NB_SOMMETS, vus.size());
    }

    /**
     * La durée annoncée est mesurée par une horloge monotone, en
     * millisecondes : elle atteint la limite et ne dépasse pas la durée
     * réellement écoulée.
     */
    @Test
    public void dureeDeCalculMesureeParHorlogeMonotone() {
        TSP5 tsp = new TSP5();
        long debut = System.nanoTime();
        tsp.chercherSolution(LIMITE_MS, graphe());
        long ecoule = (System.nanoTime() - debut) / 1_000_000;
        long annonce = tsp.obtenirDureeCalcul();
        assertTrue("durée annoncée " + annonce + " ms", annonce >= LIMITE_MS);
        assertTrue("durée annoncée " + annonce + " ms > " + ecoule + " ms",
                annonce <= ecoule);
    }
}
