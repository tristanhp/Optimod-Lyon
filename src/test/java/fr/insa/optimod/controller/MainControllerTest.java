package fr.insa.optimod.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import fr.insa.optimod.algo.CalculateurTournee;
import fr.insa.optimod.model.Demande;
import fr.insa.optimod.model.Entrepot;
import fr.insa.optimod.model.Etape;
import fr.insa.optimod.model.Livraison;
import fr.insa.optimod.model.Passage;
import fr.insa.optimod.model.Plan;

public class MainControllerTest {

    /** Écran factice qui enregistre ce que le contrôleur lui demande. */
    private static final class FakeScreen implements MapScreen {
        private Runnable actionImportLivraison;

        @Override
        public void definirActionImportLivraison(Runnable action) {
            actionImportLivraison = action;
        }

        @Override
        public Optional<File> choisirFichierLivraison() {
            return Optional.ofNullable(chosen);
        }

        @Override
        public void afficherLivraisonChargee(Livraison livraison) {
            events.add("livraison chargée");
        }

        private Runnable onImport;
        private Runnable onCalculate;
        private File chosen;
        private final List<String> events = new ArrayList<>();
        private Plan shown;
        private boolean calculateEnabled;
        private boolean waiting;
        private List<Passage> tour;
        private String calculationError;

        @Override
        public void definirActionImportPlan(Runnable action) {
            onImport = action;
        }

        @Override
        public void setOnCalculate(Runnable action) {
            onCalculate = action;
        }

        @Override
        public void setCalculateEnabled(boolean enabled) {
            calculateEnabled = enabled;
        }

        @Override
        public void showWaiting(boolean isWaiting) {
            waiting = isWaiting;
            events.add("attente:" + isWaiting);
        }

        @Override
        public Optional<File> choisirFichierPlan() {
            return Optional.ofNullable(chosen);
        }

        @Override
        public void afficherPlan(Plan plan) {
            shown = plan;
            events.add("plan");
        }

        @Override
        public void showTour(List<Passage> passages) {
            tour = passages;
            events.add("tournee");
        }

        @Override
        public void showCalculationError(String detail) {
            calculationError = detail;
            events.add("erreur");
        }

        @Override
        public void afficherErreurXml(String detail) {
            events.add("invalide");
        }

        @Override
        public void reinitialiser() {
            shown = null;
            events.add("reinitialiser");
        }
    }

    @Rule
    public TemporaryFolder dossier = new TemporaryFolder();

    private static final String PLAN_VALIDE = "<reseau>"
            + "<noeud id=\"1\" latitude=\"45.75\" longitude=\"4.85\"/>"
            + "</reseau>";

    private static final Livraison LOT = new Livraison(
            List.of(new Demande(1, 2, 60, 60)), new Entrepot(1, "8:0:0"));

    private static final List<Passage> TOURNEE = List.of(new Passage(
            new Etape(1, 0), LocalTime.of(8, 0), LocalTime.of(8, 0)));

    private FakeScreen screen;
    private MainController controller;
    private final Queue<Runnable> arrierePlan = new ArrayDeque<>();
    private Plan planRecu;
    private Livraison lotRecu;
    private RuntimeException erreurDeCalcul;

    @Before
    public void creer() {
        screen = new FakeScreen();
        CalculateurTournee calcul = (plan, livraison) -> {
            planRecu = plan;
            lotRecu = livraison;
            if (erreurDeCalcul != null) {
                throw erreurDeCalcul;
            }
            return TOURNEE;
        };
        controller = new MainController(screen, calcul, arrierePlan::add,
                Runnable::run);
    }

    private File ecrire(String contenu) throws Exception {
        File f = dossier.newFile();
        Files.writeString(f.toPath(), contenu);
        return f;
    }

    private void importerPlan() throws Exception {
        screen.chosen = ecrire(PLAN_VALIDE);
        screen.onImport.run();
    }

    private void executerArrierePlan() {
        while (!arrierePlan.isEmpty()) {
            arrierePlan.remove().run();
        }
    }

    @Test
    public void brancheLesBoutons() {
        assertNotNull(screen.onImport);
        assertNotNull(screen.onCalculate);
    }

    @Test
    public void afficheLePlanImporte() throws Exception {
        importerPlan();
        assertEquals(List.of("plan"), screen.events);
        assertEquals(1, screen.shown.obtenirNoeuds().size());
        assertEquals(screen.shown, controller.obtenirPlan());
    }

    @Test
    public void xmlInvalideOuvreLaFenetreEtVideLaPage() throws Exception {
        importerPlan();
        screen.chosen = ecrire("<reseau>");
        screen.onImport.run();
        assertEquals(List.of("plan", "invalide", "reinitialiser"), screen.events);
        assertNull(screen.shown);
        assertNull(controller.obtenirPlan());
    }

    @Test
    public void annulerNeChangeRien() throws Exception {
        importerPlan();
        screen.chosen = null;
        screen.onImport.run();
        assertEquals(List.of("plan"), screen.events);
        assertNotNull(controller.obtenirPlan());
    }

    @Test
    public void pasDePlanAuDepart() {
        assertNull(controller.obtenirPlan());
        assertTrue(screen.events.isEmpty());
    }

    @Test
    public void calculerDesactiveSansLotDeRequetes() throws Exception {
        assertFalse(screen.calculateEnabled);
        importerPlan();
        assertFalse(screen.calculateEnabled);
    }

    @Test
    public void calculerActiveAvecUnPlanEtUnLot() throws Exception {
        importerPlan();
        controller.setLivraison(LOT);
        assertTrue(screen.calculateEnabled);
    }

    @Test
    public void importLivraisonActiveLeCalcul() throws Exception {
        screen.chosen = new File(getClass().getResource(
                "/data/petitPlan.xml").toURI());
        screen.onImport.run();
        screen.chosen = new File(getClass().getResource(
                "/data/demandePetit1.xml").toURI());
        screen.actionImportLivraison.run();
        assertNotNull(controller.obtenirLivraison());
        assertTrue(screen.calculateEnabled);
        screen.onCalculate.run();
        executerArrierePlan();
        assertSame(controller.obtenirLivraison(), lotRecu);
        assertSame(TOURNEE, screen.tour);
    }

    @Test
    public void resultatIgnoreSiLaLivraisonChangePendantLeCalcul()
            throws Exception {
        importerPlan();
        controller.setLivraison(LOT);
        screen.onCalculate.run();
        controller.setLivraison(new Livraison(List.of(),
                new Entrepot(1, "9:0:0")));
        executerArrierePlan();
        assertNull(screen.tour);
        assertFalse(screen.waiting);
        assertTrue(screen.calculateEnabled);
    }

    @Test
    public void calculerDesactiveSansPlan() {
        controller.setLivraison(LOT);
        assertFalse(screen.calculateEnabled);
    }

    @Test
    public void unNouveauPlanOublieLeLot() throws Exception {
        importerPlan();
        controller.setLivraison(LOT);
        importerPlan();
        assertFalse(screen.calculateEnabled);
    }

    @Test
    public void cliquerSansPreconditionNeLanceRien() throws Exception {
        screen.onCalculate.run();
        importerPlan();
        screen.onCalculate.run();
        assertTrue(arrierePlan.isEmpty());
        assertFalse(screen.waiting);
    }

    @Test
    public void calculAfficheLaTournee() throws Exception {
        importerPlan();
        controller.setLivraison(LOT);
        screen.events.clear();
        screen.onCalculate.run();
        executerArrierePlan();
        assertEquals(List.of("attente:true", "attente:false", "tournee"),
                screen.events);
        assertSame(TOURNEE, screen.tour);
        assertSame(controller.obtenirPlan(), planRecu);
        assertSame(LOT, lotRecu);
        assertTrue(screen.calculateEnabled);
    }

    @Test
    public void etatDAttentePendantLeCalcul() throws Exception {
        importerPlan();
        controller.setLivraison(LOT);
        screen.onCalculate.run();
        assertTrue(screen.waiting);
        assertFalse(screen.calculateEnabled);
        assertNull(screen.tour);
        executerArrierePlan();
        assertFalse(screen.waiting);
    }

    @Test
    public void unSecondClicPendantLeCalculEstIgnore() throws Exception {
        importerPlan();
        controller.setLivraison(LOT);
        screen.onCalculate.run();
        screen.onCalculate.run();
        assertEquals(1, arrierePlan.size());
    }

    @Test
    public void erreurDeCalculAfficheLErreur() throws Exception {
        erreurDeCalcul = new IllegalStateException("Tournée impossible");
        importerPlan();
        controller.setLivraison(LOT);
        screen.events.clear();
        screen.onCalculate.run();
        executerArrierePlan();
        assertEquals(List.of("attente:true", "attente:false", "erreur"),
                screen.events);
        assertEquals("Tournée impossible", screen.calculationError);
        assertNull(screen.tour);
        assertTrue(screen.calculateEnabled);
    }

    @Test
    public void resultatIgnoreSiLePlanChangePendantLeCalcul()
            throws Exception {
        importerPlan();
        controller.setLivraison(LOT);
        screen.onCalculate.run();
        importerPlan();
        executerArrierePlan();
        assertNull(screen.tour);
        assertFalse(screen.waiting);
    }
}
