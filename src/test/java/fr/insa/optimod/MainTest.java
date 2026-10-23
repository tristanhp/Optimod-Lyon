package fr.insa.optimod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

/**
 * Tests de {@link Main}.
 */
public class MainTest {

    /** Le message de démarrage mentionne le nom de l'application. */
    @Test
    public void bannerMentionsApplicationName() {
        assertTrue(Main.obtenirMessageDemarrage().contains("Optimod'Lyon"));
    }

    /** Sans option, le jar ouvre l'interface graphique. */
    @Test
    public void launchesGuiByDefault() {
        assertTrue(Main.shouldLaunchGui(new String[0]));
    }

    /** Avec --version, le jar n'ouvre pas l'interface graphique. */
    @Test
    public void versionOptionDoesNotLaunchGui() {
        assertFalse(Main.shouldLaunchGui(new String[] {Main.VERSION_OPTION}));
    }

    /** Avec --version, le démarrage affiche seulement le message. */
    @Test
    public void versionOptionPrintsBanner() {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
        try {
            Main.main(new String[] {Main.VERSION_OPTION});
        } finally {
            System.setOut(originalOut);
        }
        assertEquals(Main.obtenirMessageDemarrage(),
                captured.toString(StandardCharsets.UTF_8).trim());
    }
}
