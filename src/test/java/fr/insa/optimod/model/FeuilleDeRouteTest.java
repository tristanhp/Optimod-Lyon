package fr.insa.optimod.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.List;

import org.junit.Test;

import fr.insa.optimod.model.Tournee.Arret;
import fr.insa.optimod.model.Tournee.TypeArret;

/** Tests de {@link FeuilleDeRoute}. */
public class FeuilleDeRouteTest {

    private static final String RESEAU = "<reseau>"
            + "<noeud id=\"1\" latitude=\"45.75\" longitude=\"4.85\"/>"
            + "<noeud id=\"2\" latitude=\"45.76\" longitude=\"4.86\"/>"
            + "<noeud id=\"3\" latitude=\"45.77\" longitude=\"4.87\"/>"
            + "<troncon origine=\"1\" destination=\"2\" longueur=\"100\""
            + " nomRue=\"Rue A &amp; B\"/>"
            + "<troncon origine=\"2\" destination=\"3\" longueur=\"50\""
            + " nomRue=\"Rue A &amp; B\"/>"
            + "<troncon origine=\"3\" destination=\"1\" longueur=\"70\""
            + " nomRue=\"\"/></reseau>";

    private static Plan plan() throws Exception {
        return new Plan(new ByteArrayInputStream(
                RESEAU.getBytes(StandardCharsets.UTF_8)));
    }

    private static Tournee tournee(Plan plan, long secondesSurPlace) {
        Noeud n1 = plan.obtenirNoeud(1);
        Noeud n2 = plan.obtenirNoeud(2);
        Noeud n3 = plan.obtenirNoeud(3);
        LocalTime huit = LocalTime.of(8, 0);
        LocalTime arrivee = LocalTime.of(8, 5);
        Arret depart = new Arret(n1, huit, huit, TypeArret.ENTREPOT, null);
        Arret retour = new Arret(n1, LocalTime.of(9, 0), LocalTime.of(9, 0),
                TypeArret.ENTREPOT, null);
        Arret milieu = new Arret(n3, arrivee,
                arrivee.plusSeconds(secondesSurPlace), TypeArret.ENTREPOT,
                null);
        return new Tournee(List.of(depart, milieu, retour), List.of(
                new Chemin(150, List.of(
                        new Troncon(n1, n2, 100, "Rue A & B"),
                        new Troncon(n2, n3, 50, "Rue A & B"))),
                new Chemin(70, List.of(new Troncon(n3, n1, 70, "")))), true);
    }

    /** Le nom du fichier suit le numéro du livreur. */
    @Test
    public void nomDuFichierContientLeLivreur() {
        assertEquals("feuille-de-route-livreur-3.html",
                FeuilleDeRoute.nomFichier(3));
    }

    /** Les noms de rue sont échappés dans le HTML. */
    @Test
    public void nomsDeRueEchappes() throws Exception {
        Plan plan = plan();
        String html = FeuilleDeRoute.generer(plan, 1, tournee(plan, 0));
        assertTrue(html, html.contains("Rue A &amp; B"));
        assertFalse(html, html.contains("Rue A & B"));
    }

    /** La page déclare son encodage et un style d'impression. */
    @Test
    public void encodageEtStyleDImpression() throws Exception {
        Plan plan = plan();
        String html = FeuilleDeRoute.generer(plan, 1, tournee(plan, 0));
        assertTrue(html.contains("<meta charset=\"UTF-8\">"));
        assertTrue(html.contains("@media print"));
    }

    /** L'en-tête donne le livreur, l'heure de départ et l'heure de retour. */
    @Test
    public void enTeteDuLivreur() throws Exception {
        Plan plan = plan();
        String html = FeuilleDeRoute.generer(plan, 2, tournee(plan, 0));
        assertTrue(html.contains("Livreur n°2"));
        assertTrue(html.contains("départ prévue de l'entrepôt : 08:00"));
        assertTrue(html.contains("retour prévue à l'entrepôt : 09:00"));
    }

    /** Les durées sont écrites avec des espaces. */
    @Test
    public void dureeSurPlaceLisible() throws Exception {
        Plan plan = plan();
        assertTrue(FeuilleDeRoute.generer(plan, 1, tournee(plan, 90))
                .contains("1 min 30 s"));
        assertTrue(FeuilleDeRoute.generer(plan, 1, tournee(plan, 240))
                .contains("4 min<"));
    }

    /** Les rues consécutives identiques sont regroupées, longueurs ajoutées. */
    @Test
    public void itineraireRegroupeLesRuesConsecutives() throws Exception {
        Plan plan = plan();
        String html = FeuilleDeRoute.generer(plan, 1, tournee(plan, 0));
        assertTrue(html, html.contains(
                "Rue A &amp; B (150 m)</td>"));
        assertTrue(html, html.contains(
                "rue sans nom (70 m)</td>"));
    }

    /** Les arrêts sont dans l'ordre de la tournée. */
    @Test
    public void arretsDansLOrdre() throws Exception {
        Plan plan = plan();
        String html = FeuilleDeRoute.generer(plan, 1, tournee(plan, 0));
        int depart = html.indexOf("08:00</td>");
        int milieu = html.indexOf("08:05</td>");
        int retour = html.indexOf("09:00</td>");
        assertTrue(depart > 0 && depart < milieu && milieu < retour);
    }

    /** Un nœud sans rue nommée est affiché par ses coordonnées. */
    @Test
    public void adresseSansNomAfficheLesCoordonnees() throws Exception {
        Plan plan = new Plan(new ByteArrayInputStream(
                ("<reseau><noeud id=\"1\" latitude=\"45.75\" "
                        + "longitude=\"4.85\"/></reseau>")
                        .getBytes(StandardCharsets.UTF_8)));
        Noeud n1 = plan.obtenirNoeud(1);
        LocalTime huit = LocalTime.of(8, 0);
        Tournee seule = new Tournee(List.of(
                new Arret(n1, huit, huit, TypeArret.ENTREPOT, null)),
                List.of(), true);
        assertTrue(FeuilleDeRoute.generer(plan, 1, seule)
                .contains("45.750000, 4.850000"));
    }

    /** Une tournée sans arrêt est refusée. */
    @Test(expected = IllegalArgumentException.class)
    public void tourneeVideRefusee() throws Exception {
        FeuilleDeRoute.generer(plan(), 1, new Tournee(List.of(), List.of(), true));
    }

    /** Sur une vraie tournée calculée, chaque arrêt et trajet est écrit. */
    @Test
    public void tourneeCalculeeSurLePetitPlan() throws Exception {
        Plan petit = PlansPourTests.petitPlan();
        ProgrammeLivraisons programme = new ProgrammeLivraisons(
                getClass().getResourceAsStream("/data/demandePetit2.xml"),
                petit);
        Tournee tournee = petit.calculerTournees(programme).get(1);
        String html = FeuilleDeRoute.generer(petit, 1, tournee);
        assertEquals(tournee.arrets().size() - 1,
                html.split("class=\"itineraire\"", -1).length - 1);
        assertEquals(2, html.split("Point de collecte", -1).length - 1);
        assertEquals(2, html.split("Point de livraison", -1).length - 1);
    }

    /** Les heures sont écrites sans les secondes. */
    @Test
    public void heuresSansSecondes() throws Exception {
        Plan plan = plan();
        LocalTime huit = LocalTime.of(8, 0);
        LocalTime retour = LocalTime.of(8, 49, 32);
        Noeud n1 = plan.obtenirNoeud(1);
        Tournee tournee = new Tournee(List.of(
                new Arret(n1, huit, huit, TypeArret.ENTREPOT, null),
                new Arret(n1, retour, retour, TypeArret.ENTREPOT, null)),
                List.of(), true);
        String html = FeuilleDeRoute.generer(plan, 1, tournee);
        assertTrue(html, html.contains("retour prévue à l'entrepôt : 08:49<"));
        assertFalse(html, html.contains("08:49:32"));
    }
}
