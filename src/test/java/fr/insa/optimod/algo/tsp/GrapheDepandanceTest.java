package fr.insa.optimod.algo.tsp;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;

public class GrapheDepandanceTest {
    private final int nbSommets = 10;
    private static final int COUT_MAX = 40;
    private static final int COUT_MIN = 10;
    GrapheDepandance grapheDepandance;

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
    }

    /** Un graphe a bien le bon nombre de sommet. */
    @Test
    public void testNombreSommetGrapheDepandance() {
        assertTrue(grapheDepandance.obtenirNombreSommets() == nbSommets);
    }

    /** Un graphe a 10 sommet a le bon coup sur une arret avec la seed 1. */
    @Test
    public void testGetCoutGrapheDepandance() {
        assertTrue(grapheDepandance.obtenirCoutArc(3, 7) == 37);
    }

    /** Un arc est entre deux sommets différents et pas 2 fois le meme sommet. */
    @Test
    public void testEstArcGrapheDepandance() {
        // Un arc
        assertTrue(grapheDepandance.estArc(3, 7));
        // Pas un arc
        assertFalse(grapheDepandance.estArc(5, 5));
    }

    /** Les depandances sont conserve dans le graphe */
    @Test
    public void testSommetDepandant() {
        // Indice 0 à 4 : pas de depandance
        assertTrue(grapheDepandance.getDepandance(3) == -1);
        // Indice 5 à 9 : une depandance
        assertTrue(grapheDepandance.getDepandance(7) == 2);
    }
}
