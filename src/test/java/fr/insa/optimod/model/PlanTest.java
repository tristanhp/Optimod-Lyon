package fr.insa.optimod.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

import org.junit.Test;
import org.xml.sax.SAXException;

/**
 * Tests de {@link Plan}.
 */
public class PlanTest {

    private static final String PETIT_PLAN = """
            <reseau>
            <noeud id="1" latitude="45.75" longitude="4.85"/>
            <noeud id="2129259178" latitude="45.76" longitude="4.87"/>
            <noeud id="3" latitude="45.77" longitude="4.88"/>
            <troncon destination="2129259178" longueur="69.9" nomRue="Rue Danton" origine="1"/>
            <troncon destination="3" longueur="12.5" nomRue="" origine="2129259178"/>
            <troncon destination="1" longueur="30.0" nomRue="Rue Garibaldi" origine="2129259178"/>
            </reseau>
            """;

    private static Plan lire(String xml) throws IOException, SAXException {
        return new Plan(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    /** Les noeuds sont lus avec leur latitude et leur longitude dans le bon ordre. */
    @Test
    public void noeudsLus() throws Exception {
        Plan plan = lire(PETIT_PLAN);
        assertEquals(3, plan.getNoeuds().size());
        Noeud noeud = plan.getNoeud(1);
        assertEquals(45.75, noeud.getLatitude(), 1e-9);
        assertEquals(4.85, noeud.getLongitude(), 1e-9);
    }

    /** Les ids qui dépassent un int sont acceptés. */
    @Test
    public void grandIdAccepte() throws Exception {
        assertNotNull(lire(PETIT_PLAN).getNoeud(2129259178L));
    }

    /** Les tronçons relient les bons noeuds. */
    @Test
    public void tronconsLus() throws Exception {
        Plan plan = lire(PETIT_PLAN);
        assertEquals(3, plan.getTroncons().size());
        Troncon troncon = plan.getTroncons().get(0);
        assertEquals(1, troncon.getNoeudOrigine().getId());
        assertEquals(2129259178L, troncon.getNoeudDestination().getId());
        assertEquals(69.9, troncon.getLongueur(), 1e-9);
        assertEquals("Rue Danton", troncon.getNomRue());
    }

    /** Les tronçons sortants d'un noeud sont indexés. */
    @Test
    public void tronconsSortants() throws Exception {
        Plan plan = lire(PETIT_PLAN);
        assertEquals(2, plan.getTronconsSortants(plan.getNoeud(2129259178L)).size());
        assertTrue(plan.getTronconsSortants(plan.getNoeud(3)).isEmpty());
        assertTrue(plan.getTronconsSortants(new Noeud(99, 0, 0)).isEmpty());
    }

    /** Un id inconnu ne renvoie pas de noeud. */
    @Test
    public void noeudInconnu() throws Exception {
        assertNull(lire(PETIT_PLAN).getNoeud(42));
    }

    /** Un tronçon vers un noeud absent est refusé. */
    @Test(expected = SAXException.class)
    public void tronconVersNoeudInconnuRefuse() throws Exception {
        lire("<reseau><noeud id=\"1\" latitude=\"0\" longitude=\"0\"/>"
                + "<troncon destination=\"2\" longueur=\"1\" nomRue=\"\" origine=\"1\"/></reseau>");
    }

    /** Un XML mal formé est refusé. */
    @Test(expected = SAXException.class)
    public void xmlMalFormeRefuse() throws Exception {
        lire("<reseau>");
    }

    /** Le petit plan fourni se lit en entier. */
    @Test
    public void petitPlanFourni() throws Exception {
        try (InputStream xml = getClass().getResourceAsStream("/xml/petitPlan.xml")) {
            Plan plan = new Plan(xml);
            assertFalse(plan.getNoeuds().isEmpty());
            assertFalse(plan.getTroncons().isEmpty());
        }
    }

    /** Un plan se lit aussi depuis un chemin de fichier. */
    @Test
    public void lectureDepuisFichier() throws Exception {
        File fichier = File.createTempFile("plan", ".xml");
        fichier.deleteOnExit();
        Files.writeString(fichier.toPath(), PETIT_PLAN);
        assertEquals(3, new Plan(fichier.getPath()).getNoeuds().size());
    }
}
