package fr.insa.optimod;

import static org.junit.Assert.assertEquals;
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
        assertTrue(Main.banner().contains("Optimod'Lyon"));
    }

    /** Le démarrage affiche le message de démarrage. */
    @Test
    public void mainPrintsBanner() {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
        try {
            Main.main(new String[0]);
        } finally {
            System.setOut(originalOut);
        }
        assertEquals(Main.banner(),
                captured.toString(StandardCharsets.UTF_8).trim());
    }
}
