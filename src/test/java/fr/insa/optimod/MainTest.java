package fr.insa.optimod;

import static org.junit.Assert.assertTrue;

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
}
