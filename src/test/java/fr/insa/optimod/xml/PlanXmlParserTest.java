package fr.insa.optimod.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import fr.insa.optimod.model.Plan;

public class PlanXmlParserTest {

    @Rule
    public TemporaryFolder dossier = new TemporaryFolder();

    private static Plan lire(String xml) throws XmlInvalideException {
        return PlanXmlParser.lire(new ByteArrayInputStream(
                xml.getBytes(StandardCharsets.UTF_8)));
    }

    private static void assertInvalide(String xml, String extraitMessage) {
        try {
            lire(xml);
        } catch (XmlInvalideException e) {
            assertTrue(e.getMessage(), e.getMessage().contains(extraitMessage));
            return;
        }
        throw new AssertionError("XML accepté à tort : " + xml);
    }

    private static final String DEUX_NOEUDS =
            "<noeud id=\"1\" latitude=\"45.75\" longitude=\"4.85\"/>"
            + "<noeud id=\"2\" latitude=\"45.76\" longitude=\"4.86\"/>";

    @Test
    public void litIntersectionsEtTroncons() throws Exception {
        Plan plan = lire("<reseau>" + DEUX_NOEUDS
                + "<troncon origine=\"1\" destination=\"2\" nomRue=\"Rue A\""
                + " longueur=\"12.5\"/></reseau>");
        assertEquals(2, plan.getIntersections().size());
        assertEquals(1, plan.getTroncons().size());
        assertEquals("Rue A", plan.getTroncons().get(0).nomRue());
        assertEquals(12.5, plan.getTroncons().get(0).longueur(), 1e-9);
        assertFalse(plan.getEntrepot().isPresent());
    }

    @Test
    public void litLEntrepot() throws Exception {
        Plan plan = lire("<reseau>" + DEUX_NOEUDS
                + "<entrepot adresse=\"2\"/></reseau>");
        assertEquals(2, plan.getEntrepot().orElseThrow().id());
    }

    @Test
    public void litLesTroisPlansFournis() throws Exception {
        assertPlanFourni("/data/petitPlan.xml", 308, 616);
        assertPlanFourni("/data/moyenPlan.xml", 1448, 3097);
        assertPlanFourni("/data/grandPlan.xml", 3736, 7811);
    }

    private void assertPlanFourni(String ressource, int noeuds, int troncons)
            throws Exception {
        try (InputStream flux = getClass().getResourceAsStream(ressource)) {
            Plan plan = PlanXmlParser.lire(flux);
            assertEquals(noeuds, plan.getIntersections().size());
            assertEquals(troncons, plan.getTroncons().size());
        }
    }

    @Test
    public void litUnFichier() throws Exception {
        File fichier = dossier.newFile("plan.xml");
        java.nio.file.Files.writeString(fichier.toPath(),
                "<reseau>" + DEUX_NOEUDS + "</reseau>");
        assertEquals(2, PlanXmlParser.lire(fichier).getIntersections().size());
    }

    @Test
    public void refuseUnFichierInexistant() throws IOException {
        try {
            PlanXmlParser.lire(new File(dossier.getRoot(), "absent.xml"));
        } catch (XmlInvalideException e) {
            assertTrue(e.getMessage().contains("absent.xml"));
            return;
        }
        throw new AssertionError("Fichier absent accepté");
    }

    @Test
    public void refuseUnXmlMalForme() {
        assertInvalide("<reseau><noeud></reseau>", "bien formé");
    }

    @Test
    public void refuseUneMauvaiseRacine() {
        assertInvalide("<demandeDeLivraisons/>", "<reseau>");
    }

    @Test
    public void refuseUnPlanSansIntersection() {
        assertInvalide("<reseau/>", "aucune intersection");
    }

    @Test
    public void refuseUnAttributManquant() {
        assertInvalide("<reseau><noeud id=\"1\" latitude=\"45\"/></reseau>",
                "« longitude » manquant");
    }

    @Test
    public void refuseUnNombreIllisible() {
        assertInvalide("<reseau><noeud id=\"x\" latitude=\"45\""
                + " longitude=\"4\"/></reseau>", "doit être un entier");
        assertInvalide("<reseau><noeud id=\"1\" latitude=\"abc\""
                + " longitude=\"4\"/></reseau>", "doit être un nombre");
        assertInvalide("<reseau><noeud id=\"1\" latitude=\"NaN\""
                + " longitude=\"4\"/></reseau>", "doit être un nombre");
    }

    @Test
    public void refuseDesCoordonneesImpossibles() {
        assertInvalide("<reseau><noeud id=\"1\" latitude=\"91\""
                + " longitude=\"4\"/></reseau>", "latitude");
        assertInvalide("<reseau><noeud id=\"1\" latitude=\"45\""
                + " longitude=\"181\"/></reseau>", "longitude");
    }

    @Test
    public void refuseUnIdentifiantEnDouble() {
        assertInvalide("<reseau>" + DEUX_NOEUDS
                + "<noeud id=\"1\" latitude=\"45\" longitude=\"4\"/></reseau>",
                "plusieurs fois");
    }

    @Test
    public void refuseUnTronconVersUneIntersectionInconnue() {
        assertInvalide("<reseau>" + DEUX_NOEUDS
                + "<troncon origine=\"1\" destination=\"9\" nomRue=\"R\""
                + " longueur=\"1\"/></reseau>", "aucune intersection");
    }

    @Test
    public void refuseUneLongueurNegative() {
        assertInvalide("<reseau>" + DEUX_NOEUDS
                + "<troncon origine=\"1\" destination=\"2\" nomRue=\"R\""
                + " longueur=\"-1\"/></reseau>", "négative");
    }

    @Test
    public void refuseUnEntrepotInconnuOuMultiple() {
        assertInvalide("<reseau>" + DEUX_NOEUDS + "<entrepot adresse=\"7\"/>"
                + "</reseau>", "aucune intersection");
        assertInvalide("<reseau>" + DEUX_NOEUDS + "<entrepot adresse=\"1\"/>"
                + "<entrepot adresse=\"2\"/></reseau>", "plusieurs entrepôts");
    }

    @Test
    public void refuseUneDeclarationDeType() {
        assertInvalide("<!DOCTYPE reseau [<!ENTITY x \"y\">]><reseau/>",
                "bien formé");
    }
}
