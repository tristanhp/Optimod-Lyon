package fr.insa.optimod.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import fr.insa.optimod.model.Plan;

public class MainControllerImportTest {

    /** Écran factice qui enregistre ce que le contrôleur lui demande. */
    private static final class FakeScreen implements MapScreen {
        @Override
        public void setOnCalculate(Runnable action) { }
        @Override
        public void setCalculateEnabled(boolean enabled) { }
        @Override
        public void showWaiting(boolean waiting) { }
        @Override
        public void showTour(List<fr.insa.optimod.model.Passage> passages) { }
        @Override
        public void showCalculationError(String detail) { }

        private Runnable actionImportPlan;
        private Runnable actionImportLivraison;
        private File fichierChoisi;
        private final List<String> evenements = new ArrayList<>();
        private Plan planAffiche;

        @Override
        public void definirActionImportPlan(Runnable action) {
            actionImportPlan = action;
        }

        @Override
        public void definirActionImportLivraison(Runnable action) {
            actionImportLivraison = action;
        }

        @Override
        public Optional<File> choisirFichierLivraison() {
            return Optional.ofNullable(fichierChoisi);
        }

        @Override
        public void afficherLivraisonChargee() {
            evenements.add("livraison chargée");
        }

        @Override
        public Optional<File> choisirFichierPlan() {
            return Optional.ofNullable(fichierChoisi);
        }

        @Override
        public void afficherPlan(Plan plan) {
            planAffiche = plan;
            evenements.add("plan");
        }

        @Override
        public void afficherErreurXml(String detail) {
            evenements.add("invalide");
        }

        @Override
        public void reinitialiser() {
            planAffiche = null;
            evenements.add("reinitialiser");
        }
    }

    @Rule
    public TemporaryFolder dossier = new TemporaryFolder();

    private FakeScreen ecran;
    private MainController controleur;

    @Before
    public void creer() {
        ecran = new FakeScreen();
        controleur = new MainController(ecran);
    }

    private File ecrire(String contenu) throws Exception {
        File fichier = dossier.newFile();
        Files.writeString(fichier.toPath(), contenu);
        return fichier;
    }

    private static final String PLAN_VALIDE = "<reseau>"
            + "<noeud id=\"1\" latitude=\"45.75\" longitude=\"4.85\"/>"
            + "</reseau>";

    @Test
    public void brancheLeBoutonImporter() {
        assertNotNull(ecran.actionImportPlan);
        assertNotNull(ecran.actionImportLivraison);
    }

    @Test
    public void afficheLePlanImporte() throws Exception {
        ecran.fichierChoisi = ecrire(PLAN_VALIDE);
        ecran.actionImportPlan.run();
        assertEquals(List.of("plan"), ecran.evenements);
        assertEquals(1, ecran.planAffiche.obtenirNoeuds().size());
        assertEquals(ecran.planAffiche, controleur.obtenirPlan());
    }

    @Test
    public void xmlInvalideOuvreLaFenetreEtVideLaPage() throws Exception {
        ecran.fichierChoisi = ecrire(PLAN_VALIDE);
        ecran.actionImportPlan.run();
        ecran.fichierChoisi = ecrire("<reseau>");
        ecran.actionImportPlan.run();
        assertEquals(List.of("plan", "invalide", "reinitialiser"), ecran.evenements);
        assertNull(ecran.planAffiche);
        assertNull(controleur.obtenirPlan());
    }

    @Test
    public void annulerNeChangeRien() throws Exception {
        ecran.fichierChoisi = ecrire(PLAN_VALIDE);
        ecran.actionImportPlan.run();
        ecran.fichierChoisi = null;
        ecran.actionImportPlan.run();
        assertEquals(List.of("plan"), ecran.evenements);
        assertNotNull(controleur.obtenirPlan());
    }

    @Test
    public void pasDePlanAuDepart() {
        assertNull(controleur.obtenirPlan());
        assertTrue(ecran.evenements.isEmpty());
    }

    private void chargerLivraison() throws Exception {
        ecran.fichierChoisi = ecrire(PLAN_VALIDE);
        ecran.actionImportPlan.run();
        ecran.fichierChoisi = new File(getClass().getResource(
                "/data/demandePetit1.xml").toURI());
        ecran.actionImportLivraison.run();
    }

    @Test
    public void importeLesDemandesEtConfirmeLeChargement() throws Exception {
        chargerLivraison();
        assertEquals(List.of("plan", "livraison chargée"), ecran.evenements);
        assertEquals(1, controleur.obtenirLivraison().obtenirDemandes().size());
        assertEquals("8:0:0", controleur.obtenirLivraison().obtenirHeureDepart());
        assertEquals(ecran.planAffiche, controleur.obtenirPlan());
    }

    @Test
    public void livraisonInvalideConserveLePlanEtLesDemandes()
            throws Exception {
        chargerLivraison();
        var precedente = controleur.obtenirLivraison();
        ecran.fichierChoisi = ecrire(PLAN_VALIDE);
        ecran.actionImportLivraison.run();
        assertEquals(List.of("plan", "livraison chargée", "invalide"),
                ecran.evenements);
        assertEquals(precedente, controleur.obtenirLivraison());
        assertNotNull(controleur.obtenirPlan());
        assertEquals(ecran.planAffiche, controleur.obtenirPlan());
    }

    @Test
    public void annulerImportLivraisonConserveLesDemandes() throws Exception {
        chargerLivraison();
        var precedente = controleur.obtenirLivraison();
        ecran.fichierChoisi = null;
        ecran.actionImportLivraison.run();
        assertEquals(precedente, controleur.obtenirLivraison());
        assertEquals(List.of("plan", "livraison chargée"), ecran.evenements);
    }

    @Test
    public void livraisonSansPlanIgnoree() {
        ecran.actionImportLivraison.run();
        assertNull(controleur.obtenirLivraison());
        assertTrue(ecran.evenements.isEmpty());
    }

    @Test
    public void nouveauPlanOublieLaLivraisonPrecedente() throws Exception {
        chargerLivraison();
        ecran.fichierChoisi = ecrire(PLAN_VALIDE);
        ecran.actionImportPlan.run();
        assertNull(controleur.obtenirLivraison());
        assertEquals(List.of("plan", "livraison chargée", "plan"),
                ecran.evenements);
    }

    @Test
    public void planInvalideOublieLaLivraisonPrecedente() throws Exception {
        chargerLivraison();
        ecran.fichierChoisi = ecrire("<reseau>");
        ecran.actionImportPlan.run();
        assertNull(controleur.obtenirLivraison());
        assertNull(controleur.obtenirPlan());
        assertEquals(List.of("plan", "livraison chargée", "invalide",
                "reinitialiser"), ecran.evenements);
    }
}
