package fr.insa.optimod.model;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;


/**
 * Plans partagés par les tests des algorithmes.
 */
final class PlansPourTests {

    /**
     * Petit plan écrit à la main (longueurs en mètres) :
     * 1 vers 2 : 500 en direct, 249.6 par le détour 1, 3, 2 ;
     * 2 vers 1 : 300, seul tronçon sortant de 2 ;
     * 3 vers 1 : pas de tronçon direct, 449.6 par 2 ;
     * 4 : noeud isolé.
     */
    static final String PETIT_RESEAU = "<reseau>"
            + "<noeud id=\"1\" latitude=\"45.750\" longitude=\"4.850\"/>"
            + "<noeud id=\"2\" latitude=\"45.751\" longitude=\"4.850\"/>"
            + "<noeud id=\"3\" latitude=\"45.7505\" longitude=\"4.851\"/>"
            + "<noeud id=\"4\" latitude=\"45.760\" longitude=\"4.860\"/>"
            + troncon(1, 2, "500") + troncon(1, 3, "100")
            + troncon(3, 2, "149.6") + troncon(2, 1, "300")
            + "</reseau>";

    private PlansPourTests() {
    }

    /**
     * Construit un tronçon XML.
     *
     * @param origine id du noeud de départ
     * @param destination id du noeud d'arrivée
     * @param longueur longueur en mètres
     * @return l'élément XML du tronçon
     */
    private static String troncon(long origine, long destination,
            String longueur) {
        return "<troncon origine=\"" + origine + "\" destination=\""
                + destination + "\" longueur=\"" + longueur
                + "\" nomRue=\"\"/>";
    }

    /**
     * Lit le petit plan écrit à la main.
     *
     * @return le plan
     * @throws Exception si le XML est invalide
     */
    static Plan petitReseau() throws Exception {
        return new Plan(new ByteArrayInputStream(
                PETIT_RESEAU.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Lit le plan {@code /xml/petitPlan.xml} des ressources de test.
     *
     * @return le plan
     * @throws Exception si le fichier est absent ou invalide
     */
    static Plan petitPlan() throws Exception {
        try (InputStream xml = PlansPourTests.class
                .getResourceAsStream("/data/petitPlan.xml")) {
            return new Plan(xml);
        }
    }
}
