package fr.insa.optimod.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;

import org.junit.Test;

import fr.insa.optimod.model.Noeud;
import fr.insa.optimod.model.Plan;
import fr.insa.optimod.model.ProgrammeLivraisons.Demande;
import fr.insa.optimod.model.Tournee.Arret;
import fr.insa.optimod.model.Tournee.TypeArret;

/** Tests des textes d'un arrêt (liste de la tournée et popup de la carte). */
public class LibellesArretsTest {

    /** Après 13 h, même dans sa plage, l'arrêt est rouge et explicite. */
    @Test
    public void apres13hDansLaPlageAfficheEnRouge() {
        Arret arret = new Arret(new Noeud(1, 4, 45),
                LocalTime.of(13, 0, 1), LocalTime.of(13, 0, 1),
                TypeArret.ENTREPOT, null);
        assertTrue(LibellesArrets.style(arret).contains("#c62828"));
        assertTrue(LibellesArrets.decrire(null, arret).contains("Après 13 h"));
    }

    /** À 13 h pile et avant, le style normal est rétabli. */
    @Test
    public void a13hSansDepassementAfficheNormalement() {
        for (LocalTime heure : new LocalTime[] {
                LocalTime.of(12, 59, 59), LocalTime.of(13, 0)}) {
            Arret arret = new Arret(new Noeud(1, 4, 45), heure, heure,
                    TypeArret.ENTREPOT, null);
            assertEquals("", LibellesArrets.style(arret));
        }
    }

    /** Un arrêt hors plage reste rouge même avant 13 h. */
    @Test
    public void horsPlageAvant13hAfficheEnRouge() {
        Arret arret = new Arret(new Noeud(1, 4, 45), LocalTime.of(11, 1),
                LocalTime.of(11, 1), TypeArret.ENTREPOT, null,
                LocalTime.of(9, 0), LocalTime.of(11, 0));
        assertTrue(LibellesArrets.style(arret).contains("#c62828"));
    }

    /** Trois nœuds : le nœud 2 touche deux rues, le nœud 3 une rue sans nom. */
    private static Plan planDeTest() throws Exception {
        String xml = "<reseau>"
                + "<noeud id=\"1\" latitude=\"45.75\" longitude=\"4.85\"/>"
                + "<noeud id=\"2\" latitude=\"45.76\" longitude=\"4.86\"/>"
                + "<noeud id=\"3\" latitude=\"45.77\" longitude=\"4.87\"/>"
                + "<troncon origine=\"1\" destination=\"2\" longueur=\"10\" "
                + "nomRue=\"Rue Pasteur\"/>"
                + "<troncon origine=\"2\" destination=\"3\" longueur=\"10\" "
                + "nomRue=\"\"/>"
                + "<troncon origine=\"3\" destination=\"2\" longueur=\"10\" "
                + "nomRue=\"Avenue Berthelot\"/>"
                + "<troncon origine=\"2\" destination=\"1\" longueur=\"10\" "
                + "nomRue=\"Rue Pasteur\"/>"
                + "</reseau>";
        return new Plan(new ByteArrayInputStream(
                xml.getBytes(StandardCharsets.UTF_8)));
    }

    /** Les rues d'un nœud sont triées et sans doublon. */
    @Test
    public void adresseListeLesRuesDuNoeudSansDoublon() throws Exception {
        Plan plan = planDeTest();
        assertEquals("Avenue Berthelot, Rue Pasteur",
                LibellesArrets.adresse(plan, plan.obtenirNoeud(2)));
    }

    /** Sans nom de rue, l'adresse se réduit au numéro du nœud. */
    @Test
    public void adresseSansNomDeRueDonneLeNoeud() throws Exception {
        Plan plan = planDeTest();
        assertEquals("nœud 99", LibellesArrets.adresse(plan,
                new Noeud(99, 4.0, 45.0)));
    }

    /** Le texte donne le type, l'adresse, les heures et la plage. */
    @Test
    public void decritLeTypeLAdresseLesHeuresEtLaPlage() throws Exception {
        Plan plan = planDeTest();
        Demande demande = new Demande(plan.obtenirNoeud(2),
                plan.obtenirNoeud(1), 300, 300);
        Arret arret = new Arret(plan.obtenirNoeud(1), LocalTime.of(8, 30),
                LocalTime.of(8, 35), TypeArret.COLLECTE, demande);
        String texte = LibellesArrets.decrire(plan, arret);
        assertTrue(texte, texte.startsWith("Point de collecte — Rue Pasteur"));
        assertTrue(texte, texte.contains("Arrivée 08:30 → départ 08:35"));
        assertTrue(texte, texte.contains("Plage : 07:30 – 09:30"));
    }

    /** Les trois types d'arrêt ont un libellé du glossaire. */
    @Test
    public void typesEnToutesLettres() {
        assertEquals("Entrepôt",
                LibellesArrets.typeEnToutesLettres(TypeArret.ENTREPOT));
        assertEquals("Point de collecte",
                LibellesArrets.typeEnToutesLettres(TypeArret.COLLECTE));
        assertEquals("Point de livraison",
                LibellesArrets.typeEnToutesLettres(TypeArret.LIVRAISON));
    }
}
