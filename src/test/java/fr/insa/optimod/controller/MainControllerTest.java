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

public class MainControllerTest {

    /** Écran factice qui enregistre ce que le contrôleur lui demande. */
    private static final class FakeScreen implements MapScreen {
        private Runnable onImport;
        private File chosen;
        private final List<String> events = new ArrayList<>();
        private Plan shown;

        @Override
        public void setOnImport(Runnable action) {
            onImport = action;
        }

        @Override
        public Optional<File> chooseXmlFile() {
            return Optional.ofNullable(chosen);
        }

        @Override
        public void showPlan(Plan plan) {
            shown = plan;
            events.add("plan");
        }

        @Override
        public void showInvalidXml(String detail) {
            events.add("invalide");
        }

        @Override
        public void reset() {
            shown = null;
            events.add("reset");
        }
    }

    @Rule
    public TemporaryFolder dossier = new TemporaryFolder();

    private FakeScreen screen;
    private MainController controller;

    @Before
    public void creer() {
        screen = new FakeScreen();
        controller = new MainController(screen);
    }

    private File ecrire(String contenu) throws Exception {
        File f = dossier.newFile();
        Files.writeString(f.toPath(), contenu);
        return f;
    }

    private static final String PLAN_VALIDE = "<reseau>"
            + "<noeud id=\"1\" latitude=\"45.75\" longitude=\"4.85\"/>"
            + "</reseau>";

    @Test
    public void brancheLeBoutonImporter() {
        assertNotNull(screen.onImport);
    }

    @Test
    public void afficheLePlanImporte() throws Exception {
        screen.chosen = ecrire(PLAN_VALIDE);
        screen.onImport.run();
        assertEquals(List.of("plan"), screen.events);
        assertEquals(1, screen.shown.getIntersections().size());
        assertEquals(screen.shown, controller.getPlan());
    }

    @Test
    public void xmlInvalideOuvreLaFenetreEtVideLaPage() throws Exception {
        screen.chosen = ecrire(PLAN_VALIDE);
        screen.onImport.run();
        screen.chosen = ecrire("<reseau>");
        screen.onImport.run();
        assertEquals(List.of("plan", "invalide", "reset"), screen.events);
        assertNull(screen.shown);
        assertNull(controller.getPlan());
    }

    @Test
    public void annulerNeChangeRien() throws Exception {
        screen.chosen = ecrire(PLAN_VALIDE);
        screen.onImport.run();
        screen.chosen = null;
        screen.onImport.run();
        assertEquals(List.of("plan"), screen.events);
        assertNotNull(controller.getPlan());
    }

    @Test
    public void pasDePlanAuDepart() {
        assertNull(controller.getPlan());
        assertTrue(screen.events.isEmpty());
    }
}
