package fr.insa.optimod.algo.tsp;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

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
    public void testGrandPlanDemandeGrande9() {
        recuperePlanLivraison("src/main/resources/data/grandPlan.xml", "src/main/resources/data/demandeGrand9.xml");
        System.out.println("TSP5 : " + tempsCalCulTournee() + "ms (grandPlan.xml demandeGrand9.xml)");
    }

    @Test
    public void testGrandPlanDemandeGrande7() {
        recuperePlanLivraison("src/main/resources/data/grandPlan.xml", "src/main/resources/data/demandeGrand7.xml");
        System.out.println("TSP5 : " + tempsCalCulTournee() + "ms (grandPlan.xml demandeGrand7.xml)");
    }

    @Test
    public void testGrandPlanDemandeMoyen5() {
        recuperePlanLivraison("src/main/resources/data/grandPlan.xml", "src/main/resources/data/demandeMoyen5.xml");
        System.out.println("TSP5 : " + tempsCalCulTournee() + "ms (grandPlan.xml demandeMoyen5.xml)");
    }

    @Test
    public void testMoyenPlanDemandeMoyen3() {
        recuperePlanLivraison("src/main/resources/data/moyenPlan.xml", "src/main/resources/data/demandeMoyen3.xml");
        System.out.println("TSP5 : " + tempsCalCulTournee() + "ms (moyenPlan.xml demandeMoyen3.xml)");
    }

    @Test
    public void testPetitPlanDemandePetit2() {
        recuperePlanLivraison("src/main/resources/data/grandPlan.xml", "src/main/resources/data/demandePetit2.xml");
        System.out.println("TSP5 : " + tempsCalCulTournee() + "ms (grandPlan.xml demandePetit2.xml)");
    }

    @Test
    public void testPetitPlanDemandePetit1() {
        recuperePlanLivraison("src/main/resources/data/petitPlan.xml", "src/main/resources/data/demandePetit1.xml");
        System.out.println("TSP5 : " + tempsCalCulTournee() + "ms (petitPlan.xml demandePetit1.xml)");
    }

    @Test
    public void testGrandPlanDemandeMoyen5AvecAffichageResultat() {
        recuperePlanLivraison("src/main/resources/data/grandPlan.xml", "src/main/resources/data/demandeMoyen5.xml");

        // Calcul TSP
        long tpsDebut = System.currentTimeMillis();
        Map<Integer, Tournee> t = plan.calculerTournees(livraisons);
        long tpsFin = System.currentTimeMillis();
        // Fin Calcul
        Iterator<Arret> it = t.get(1).arrets().iterator();
        while (it.hasNext()) {
            Arret i = it.next();
            System.out.print(i.noeud().identifiant() + " ");
        }
        System.out.println();
        // Resultat théorique
        System.out.println(
                "4150019167 26155372 26317393 25610684 21717915 60755991 21992645 1400900990 1036842078 208769083 55444215 4150019167");
        System.out.println("TSP5 : " + (tpsFin - tpsDebut) + "ms (GrandPlan.xml demandeMoyen5.xml)");
    }

    private void recuperePlanLivraison(String filePlan, String fileLivraison) {
        try {
            plan = new Plan(filePlan);
        } catch (IOException | SAXException e) {
            e.printStackTrace();
        }
        try {
            livraisons = new ProgrammeLivraisons(fileLivraison, plan);
        } catch (IOException | SAXException e) {
            e.printStackTrace();
        }
    }

    private long tempsCalCulTournee() {
        // Calcul TSP
        long tpsDebut = System.currentTimeMillis();
        Map<Integer, Tournee> t = plan.calculerTournees(livraisons);
        long tpsFin = System.currentTimeMillis();
        // Fin Calcul
        return tpsFin - tpsDebut;
    }
}
