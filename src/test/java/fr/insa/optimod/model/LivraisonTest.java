package fr.insa.optimod.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.xml.sax.SAXException;

/**
 * Tests de {@link Livraison}.
 */
public class LivraisonTest {

    /** La livraison garde ses demandes et son entrepôt, sans dépendre de la liste d'origine. */
    @Test
    public void demandesEtEntrepot() {
        Demande demande = new Demande(1, 2, 180, 240);
        Entrepot entrepot = new Entrepot(3, "8:0:0");
        List<Demande> demandes = new ArrayList<>(List.of(demande));
        Livraison livraison = new Livraison(demandes, entrepot);
        demandes.clear();

        assertEquals(List.of(demande), livraison.obtenirDemandes());
        assertSame(entrepot, livraison.obtenirEntrepot());
        assertEquals(1, demande.obtenirIdentifiantCollecte());
        assertEquals(2, demande.obtenirIdentifiantLivraison());
        assertEquals(180, demande.obtenirDureeCollecte());
        assertEquals(240, demande.obtenirDureeLivraison());
        assertEquals(3, entrepot.obtenirIdentifiant());
        assertEquals("8:0:0", entrepot.obtenirHeureDepart());
        assertEquals("8:0:0", livraison.obtenirHeureDepart());
    }

    /** La liste des demandes n'est pas modifiable. */
    @Test(expected = UnsupportedOperationException.class)
    public void demandesNonModifiables() {
        new Livraison(List.of(), new Entrepot(1, "8:0:0")).obtenirDemandes().clear();
    }

    /** Le fichier conserve toutes les valeurs des demandes et du dépôt. */
    @Test
    public void lectureDepuisFichier() throws Exception {
        Path fichier = Path.of(getClass().getResource(
                "/data/demandePetit1.xml").toURI());
        Livraison livraison = new Livraison(fichier.toString());
        assertEquals(342873658L, livraison.obtenirEntrepot().obtenirIdentifiant());
        assertEquals("8:0:0", livraison.obtenirHeureDepart());
        assertEquals(livraison.obtenirHeureDepart(),
                livraison.obtenirEntrepot().obtenirHeureDepart());
        assertEquals(1, livraison.obtenirDemandes().size());
        Demande demande = livraison.obtenirDemandes().get(0);
        assertEquals(208769039L, demande.obtenirIdentifiantCollecte());
        assertEquals(25173820L, demande.obtenirIdentifiantLivraison());
        assertEquals(180, demande.obtenirDureeCollecte());
        assertEquals(240, demande.obtenirDureeLivraison());
        assertThrows(UnsupportedOperationException.class,
                () -> livraison.obtenirDemandes().clear());
    }

    /** Les six jeux fournis sont lus depuis un flux dans l'ordre XML. */
    @Test
    public void fichiersFournis() throws Exception {
        String[] noms = {"Petit1", "Petit2", "Moyen3", "Moyen5",
            "Grand7", "Grand9"};
        int[] tailles = {1, 2, 3, 5, 7, 9};
        for (int indiceFichier = 0; indiceFichier < noms.length; indiceFichier++) {
            try (InputStream xml = getClass().getResourceAsStream(
                    "/data/demande" + noms[indiceFichier] + ".xml")) {
                Livraison livraison = new Livraison(xml);
                assertEquals(tailles[indiceFichier], livraison.obtenirDemandes().size());
                assertEquals("8:0:0", livraison.obtenirHeureDepart());
                if ("Moyen5".equals(noms[indiceFichier])) {
                    assertEquals(4150019167L,
                            livraison.obtenirEntrepot().obtenirIdentifiant());
                    assertEquals(0, livraison.obtenirDemandes().get(1)
                            .obtenirDureeLivraison());
                    assertEquals(60755991L, livraison.obtenirDemandes().get(4)
                            .obtenirIdentifiantLivraison());
                }
            }
        }
    }

    /** Les données manquantes ou invalides sont signalées. */
    @Test
    public void xmlInvalidesRefuses() {
        String depot = "<entrepot adresse='1' heureDepart='8:0:0'/>";
        String[] invalides = {
            "<demandeDeLivraisons>",
            "<reseau/>",
            "<demandeDeLivraisons/>",
            "<demandeDeLivraisons>" + depot + depot
                    + "</demandeDeLivraisons>",
            "<demandeDeLivraisons><entrepot adresse='1'/>"
                    + "</demandeDeLivraisons>",
            "<demandeDeLivraisons>" + depot
                    + "<livraison adresseEnlevement='abc'/>"
                    + "</demandeDeLivraisons>",
            "<demandeDeLivraisons>" + depot
                    + "<livraison adresseEnlevement='1'"
                    + " adresseLivraison='2' dureeEnlevement='-1'"
                    + " dureeLivraison='0'/></demandeDeLivraisons>",
            "<!DOCTYPE demandeDeLivraisons [<!ENTITY heure '8:0:0'>]>"
                    + "<demandeDeLivraisons>" + depot
                    + "</demandeDeLivraisons>"
        };
        for (String xml : invalides) {
            assertThrows(SAXException.class, () -> new Livraison(
                    new ByteArrayInputStream(
                            xml.getBytes(StandardCharsets.UTF_8))));
        }
    }
}
