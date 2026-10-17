package fr.insa.optimod.algo;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import fr.insa.optimod.model.Plan;

public class AlgoTest {
    /*
     * private static Plan lire(String xml) throws IOException, SAXException {
     * return new Plan(new
     * ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
     * }
     */
    Plan plan;

    @Before
    public void initialisation() throws IOException, SAXException {
        // C'est normal qu'il y ait qu'un seul 's' à "resources" ?
        plan = new Plan("src/test/resources/xml/petitPlan.xml");
    }

    @Test
    public void testTSP1DemandePetit1() {
        assertTrue(false);
    }

    @Test
    public void testTSP1DemandePetit2() {
        assertTrue(false);
    }

}
