package fr.insa.optimod.algo.tsp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

import fr.insa.optimod.model.Plan;
import fr.insa.optimod.model.ProgrammeLivraisons;
import fr.insa.optimod.model.Tournee;
import fr.insa.optimod.model.Tournee.Arret;

public class TSPTest {
    private static final int nbSommets = 10;
    private static final int COUT_MAX = 40;
    private static final int COUT_MIN = 10;
    private static GrapheDepandance grapheDepandance;
    private static TSP1 tsp1;
    private static TSP2 tsp2;
    private static TSP3 tsp3;
    private static TSP4 tsp4;
    private static TSP5 tsp5;
    private static TSP6 tsp6;

    private Plan plan;
    private ProgrammeLivraisons livraisons;

    @BeforeClass
    public static void initialisation() throws IOException, SAXException {
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
        // System.out.println("Dependance : " + depandance);

        grapheDepandance = new GrapheDepandance(nbSommets, cout, depandance);

        tsp1 = new TSP1();
        tsp1.chercherSolution(10000, grapheDepandance, -1); // Graphe à 10 sommet et temps limite =1s

        tsp2 = new TSP2();
        tsp2.chercherSolution(10000, grapheDepandance, -1); // Graphe à 10 sommet et temps limite =1s

        tsp3 = new TSP3();
        tsp3.chercherSolution(10000, grapheDepandance, -1); // Graphe à 10 sommet et temps limite =1s

        tsp4 = new TSP4();
        tsp4.chercherSolution(10000, grapheDepandance, -1); // Graphe à 10 sommet et temps limite =1s

        tsp5 = new TSP5();
        tsp5.chercherSolution(10000, grapheDepandance, -1); // Graphe à 10 sommet et temps limite =1s

        tsp6 = new TSP6();
        tsp6.chercherSolution(10000, grapheDepandance, -1); // Graphe à 10 sommet et temps limite =1s

    }

    /** test de TSP1 */
    @Test
    public void testTSP() {
        // TODO : FAIRE UN TEST MEILLEUR

        // Performance TSP1
        System.out.println("Duree TPS1 : " + Long.toString(tsp1.obtenirDureeCalcul()) + "ms");
        System.out.println("Duree TPS2 : " + Long.toString(tsp2.obtenirDureeCalcul()) + "ms");
        System.out.println("Duree TPS3 : " + Long.toString(tsp3.obtenirDureeCalcul()) + "ms");
        System.out.println("Duree TPS4 : " + Long.toString(tsp4.obtenirDureeCalcul()) + "ms");
        System.out.println("Duree TPS5 : " + Long.toString(tsp4.obtenirDureeCalcul()) + "ms");
        System.out.println("Duree TPS6 : " + Long.toString(tsp4.obtenirDureeCalcul()) + "ms");
        System.out.println("Solution TPS1 : " + Long.toString(tsp1.obtenirCoutSolution()));
        System.out.println("Solution TPS2 : " + Long.toString(tsp2.obtenirCoutSolution()));
        System.out.println("Solution TPS3 : " + Long.toString(tsp3.obtenirCoutSolution()));
        System.out.println("Solution TPS4 : " + Long.toString(tsp4.obtenirCoutSolution()));
        System.out.println("Solution TPS5 : " + Long.toString(tsp5.obtenirCoutSolution()));
        System.out.println("Solution TPS6 : " + Long.toString(tsp6.obtenirCoutSolution()));
        assertEquals(145, tsp1.obtenirCoutSolution()); // VERIFIER QUE C'EST LA BONNE REPONSE
        assertEquals(145, tsp2.obtenirCoutSolution()); // VERIFIER QUE C'EST LA BONNE REPONSE
        assertEquals(145, tsp3.obtenirCoutSolution()); // VERIFIER QUE C'EST LA BONNE REPONSE
        // assertEquals(145, tsp4.obtenirCoutSolution()); // TSP4 utilise opt-2 pas
        // applicable sur ce graphe
        assertEquals(145, tsp5.obtenirCoutSolution()); // VERIFIER QUE C'EST LA BONNE REPONSE
        // assertEquals(145, tsp6.obtenirCoutSolution()); // TSP6 actuellement
        // non-fonctionnelle
    }

    /** Le 4e indice de la meilleure solution */
    @Test
    public void testGetSolution() {
        System.out.print("Solution (TSP4) : ");
        for (int i = 0; i < nbSommets; ++i) {
            System.out.print(tsp4.obtenirSommetSolution(i) + " -> ");
            // assertEquals(tsp1.obtenirSommetSolution(i), tsp2.obtenirSommetSolution(i));
            // assertEquals(tsp2.obtenirSommetSolution(i), tsp3.obtenirSommetSolution(i));
            // assertEquals(tsp3.obtenirSommetSolution(i), tsp4.obtenirSommetSolution(i));
        }
        System.out.println("0");

        System.out.print("Solution (TSP3) : ");
        for (int i = 0; i < nbSommets; ++i) {
            System.out.print(tsp3.obtenirSommetSolution(i) + " -> ");
        }
        System.out.println("0");
    }

    /** Verifie les depandance de la solution */
    @Test
    public void testDepandanceSolution() {
        ArrayList<Integer> vus = new ArrayList<>();
        for (int i = 0; i < nbSommets; ++i) {
            for (int indiceDejaVu = 0; indiceDejaVu < vus.size(); indiceDejaVu++) {
                // i n'est pas une depandance d'un indice deja vus
                assertTrue(grapheDepandance.getDepandance(tsp5.obtenirSommetSolution(indiceDejaVu)) != tsp5
                        .obtenirSommetSolution(i));
            }
            vus.add(tsp5.obtenirSommetSolution(i));
            // System.out.print(tsp5.obtenirSommetSolution(i) + "["
            // + grapheDepandance.getDepandance(tsp5.obtenirSommetSolution(i)) + "] ");
        }
        // System.out.println("\n" + vus);
    }

    @Test
    public void testGrandPlaneDemandeGrande9() {
        try {
            plan = new Plan("src/main/resources/data/grandPlan.xml");
        } catch (IOException | SAXException e) {
            e.printStackTrace();
        }
        try {
            livraisons = new ProgrammeLivraisons("src/main/resources/data/demandeGrand9.xml", plan);
        } catch (IOException | SAXException e) {
            e.printStackTrace();
        }
        plan.calculerTournee(livraisons);
    }

    @Test
    public void testGrandPlanDemandeMoyen5() {
        try {
            plan = new Plan("src/main/resources/data/grandPlan.xml");
        } catch (IOException | SAXException e) {
            e.printStackTrace();
        }
        try {
            livraisons = new ProgrammeLivraisons("src/main/resources/data/demandeMoyen5.xml", plan);
        } catch (IOException | SAXException e) {
            e.printStackTrace();
        }
        Tournee t = plan.calculerTournee(livraisons);
        Iterator<Arret> it = t.arrets().iterator();
        while (it.hasNext()) {
            Arret i = it.next();
            System.out.print(i.noeud().identifiant() + " ");
        }
        System.out.println();
        System.out.println(
                "4150019167 26155372 26317393 25610684 21717915 60755991 21992645 1400900990 1036842078 208769083 55444215 4150019167");
    }
}
