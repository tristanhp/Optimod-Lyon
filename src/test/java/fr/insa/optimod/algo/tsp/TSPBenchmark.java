package fr.insa.optimod.algo.tsp;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Iterator;

import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

import fr.insa.optimod.model.Plan;
import fr.insa.optimod.model.ProgrammeLivraisons;
import fr.insa.optimod.model.Tournee;
import fr.insa.optimod.model.Tournee.Arret;

public class TSPBenchmark {

    private Plan plan;
    private ProgrammeLivraisons livraisons;

    @BeforeClass
    public static void ceciEstUneClasseDeTestDeBenchmarkEtNeSertPasAVerifierDeValeur() {
        assertTrue(true);
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

        // Calcul TSP
        long tpsDebut = System.currentTimeMillis();
        plan.calculerTournee(livraisons);
        long tpsFin = System.currentTimeMillis();
        // Fin Calcul

        System.out.println("TSP5 : " + (tpsFin - tpsDebut) + "ms (GrandPlan.xml demandeGrand9.xml)");
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

        // Calcul TSP
        long tpsDebut = System.currentTimeMillis();
        Tournee t = plan.calculerTournee(livraisons);
        long tpsFin = System.currentTimeMillis();
        // Fin Calcul

        Iterator<Arret> it = t.arrets().iterator();
        while (it.hasNext()) {
            Arret i = it.next();
            System.out.print(i.noeud().identifiant() + " ");
        }
        System.out.println();
        System.out.println(
                "4150019167 26155372 26317393 25610684 21717915 60755991 21992645 1400900990 1036842078 208769083 55444215 4150019167");
        System.out.println("TSP5 : " + (tpsFin - tpsDebut) + "ms (GrandPlan.xml demandeMoyen5.xml)");
    }

}
