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
    public void messageDemarrageContientNomApplication() {
        assertTrue(Main.obtenirMessageDemarrage().contains("Optimod'Lyon"));
    }

    /** Le démarrage affiche le message de démarrage. */
    @Test
    public void demarrageAfficheMessage() {
        PrintStream sortieStandardInitiale = System.out;
        ByteArrayOutputStream sortieCapturee = new ByteArrayOutputStream();
        System.setOut(new PrintStream(sortieCapturee, true, StandardCharsets.UTF_8));
        try {
            Main.main(new String[0]);
        } finally {
            System.setOut(sortieStandardInitiale);
        }
        assertEquals(Main.obtenirMessageDemarrage(),
                sortieCapturee.toString(StandardCharsets.UTF_8).trim());
    }
}
