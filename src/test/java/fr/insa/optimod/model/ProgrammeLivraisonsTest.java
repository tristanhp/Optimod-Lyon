package fr.insa.optimod.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

import fr.insa.optimod.model.ProgrammeLivraisons.Demande;

/**
 * Tests de {@link ProgrammeLivraisons}.
 */
public class ProgrammeLivraisonsTest {

    private static final Noeud A = new Noeud(1, 4.85, 45.75);
    private static final Noeud B = new Noeud(2, 4.86, 45.76);
    private static final Noeud C = new Noeud(3, 4.87, 45.77);

    private static Plan petitPlan;

    /** Charge le petit plan fourni, où se trouvent les adresses des demandes. */
    @BeforeClass
    public static void chargerPlan() throws Exception {
        petitPlan = PlansPourTests.petitPlan();
    }

    private static ProgrammeLivraisons lire(String xml, Plan plan)
            throws Exception {
        return new ProgrammeLivraisons(new ByteArrayInputStream(
                xml.getBytes(StandardCharsets.UTF_8)), plan);
    }

    /** Le programme garde ses demandes sans dépendre de la liste d'origine. */
    @Test
    public void demandesEtEntrepot() {
        Demande demande = new Demande(A, B, 180, 240);
        List<Demande> demandes = new ArrayList<>(List.of(demande));
        ProgrammeLivraisons programme = new ProgrammeLivraisons(C,
                LocalTime.of(8, 0), demandes);
        demandes.clear();
        assertEquals(List.of(demande), programme.obtenirDemandes());
        assertSame(C, programme.obtenirEntrepot());
        assertEquals(LocalTime.of(8, 0), programme.obtenirHeureDepart());
    }

    /** La liste des demandes n'est pas modifiable par l'appelant. */
    @Test(expected = UnsupportedOperationException.class)
    public void demandesNonModifiables() {
        new ProgrammeLivraisons(A, LocalTime.of(8, 0), List.of())
                .obtenirDemandes().clear();
    }

    /** Une durée sur place négative est refusée. */
    @Test(expected = IllegalArgumentException.class)
    public void dureeNegativeRefusee() {
        new Demande(A, B, -1, 0);
    }

    /** Le fichier conserve les adresses (nœuds du plan) et les durées. */
    @Test
    public void lectureDepuisFichier() throws Exception {
        Path fichier = Path.of(getClass().getResource(
                "/data/demandePetit1.xml").toURI());
        ProgrammeLivraisons programme = new ProgrammeLivraisons(
                fichier.toString(), petitPlan);
        assertSame(petitPlan.obtenirNoeud(342873658L),
                programme.obtenirEntrepot());
        assertEquals(LocalTime.of(8, 0), programme.obtenirHeureDepart());
        assertEquals(1, programme.obtenirDemandes().size());
        Demande demande = programme.obtenirDemandes().get(0);
        assertEquals(208769039L, demande.collecte().identifiant());
        assertEquals(25173820L, demande.livraison().identifiant());
        assertEquals(180, demande.dureeCollecte());
        assertEquals(240, demande.dureeLivraison());
    }

    /** Les six fichiers de demandes fournis se lisent sur leur plan. */
    @Test
    public void fichiersFournis() throws Exception {
        String[][] jeux = {{"Petit1", "petitPlan"}, {"Petit2", "petitPlan"},
            {"Moyen3", "moyenPlan"}, {"Moyen5", "moyenPlan"},
            {"Grand7", "grandPlan"}, {"Grand9", "grandPlan"}};
        int[] tailles = {1, 2, 3, 5, 7, 9};
        for (int indice = 0; indice < jeux.length; indice++) {
            Plan plan;
            try (InputStream xml = getClass().getResourceAsStream(
                    "/data/" + jeux[indice][1] + ".xml")) {
                plan = new Plan(xml);
            }
            try (InputStream xml = getClass().getResourceAsStream(
                    "/data/demande" + jeux[indice][0] + ".xml")) {
                ProgrammeLivraisons programme = new ProgrammeLivraisons(xml,
                        plan);
                assertEquals(tailles[indice], programme.obtenirDemandes().size());
                assertEquals(LocalTime.of(8, 0), programme.obtenirHeureDepart());
            }
        }
    }

    /** L'heure de départ s'écrit 8:0:0, 08:00 ou 08:00:30. */
    @Test
    public void formatsDHeure() throws Exception {
        assertEquals(LocalTime.of(8, 0), ProgrammeLivraisons.lireHeure("8:0:0"));
        assertEquals(LocalTime.of(8, 0), ProgrammeLivraisons.lireHeure("08:00"));
        assertEquals(LocalTime.of(8, 0, 30),
                ProgrammeLivraisons.lireHeure("08:00:30"));
        for (String invalide : new String[] {"8", "25:00", "8:x", "1:2:3:4"}) {
            assertThrows(invalide, SAXException.class,
                    () -> ProgrammeLivraisons.lireHeure(invalide));
        }
    }

    /** Les données manquantes, invalides ou hors du plan sont refusées. */
    @Test
    public void xmlInvalidesRefuses() {
        String depot = "<entrepot adresse='342873658' heureDepart='8:0:0'/>";
        String[] invalides = {
            "<demandeDeLivraisons>",
            "<reseau/>",
            "<demandeDeLivraisons/>",
            "<demandeDeLivraisons>" + depot + depot + "</demandeDeLivraisons>",
            "<demandeDeLivraisons><entrepot adresse='342873658'/>"
                    + "</demandeDeLivraisons>",
            "<demandeDeLivraisons><entrepot adresse='99'"
                    + " heureDepart='8:0:0'/></demandeDeLivraisons>",
            "<demandeDeLivraisons>" + depot
                    + "<livraison adresseEnlevement='abc'/>"
                    + "</demandeDeLivraisons>",
            "<demandeDeLivraisons>" + depot
                    + "<livraison adresseEnlevement='208769039'"
                    + " adresseLivraison='25173820' dureeEnlevement='-1'"
                    + " dureeLivraison='0'/></demandeDeLivraisons>",
            "<!DOCTYPE demandeDeLivraisons [<!ENTITY heure '8:0:0'>]>"
                    + "<demandeDeLivraisons>" + depot
                    + "</demandeDeLivraisons>"
        };
        for (String xml : invalides) {
            assertThrows(xml, SAXException.class, () -> lire(xml, petitPlan));
        }
    }

    /** Une adresse absente du plan est signalée avec son identifiant. */
    @Test
    public void adresseHorsPlanSignalee() {
        SAXException erreur = assertThrows(SAXException.class, () -> lire(
                "<demandeDeLivraisons><entrepot adresse='99'"
                + " heureDepart='8:0:0'/></demandeDeLivraisons>", petitPlan));
        assertTrue(erreur.getMessage().contains("99"));
    }

    /** Ajouter et retirer une demande prévient les observateurs. */
    @Test
    public void ajouterEtRetirerPreviennentLesObservateurs() {
        Demande premiere = new Demande(A, B, 60, 60);
        Demande seconde = new Demande(B, C, 60, 60);
        ProgrammeLivraisons programme = new ProgrammeLivraisons(C,
                LocalTime.of(8, 0), List.of(premiere));
        List<Demande> vue = programme.obtenirDemandes();
        List<Integer> tailles = new ArrayList<>();
        programme.ajouterObservateur(source -> tailles.add(
                ((ProgrammeLivraisons) source).obtenirDemandes().size()));

        programme.ajouterDemande(0, seconde);
        assertEquals(List.of(seconde, premiere), vue);
        assertSame(seconde, programme.retirerDemande(0));
        assertEquals(List.of(premiere), vue);
        assertEquals(List.of(2, 1), tailles);
    }

    /** Une position invalide est refusée sans prévenir les observateurs. */
    @Test
    public void positionInvalideRefusee() {
        ProgrammeLivraisons programme = new ProgrammeLivraisons(C,
                LocalTime.of(8, 0), List.of());
        List<Integer> appels = new ArrayList<>();
        programme.ajouterObservateur(source -> appels.add(1));
        assertThrows(IndexOutOfBoundsException.class,
                () -> programme.retirerDemande(0));
        assertThrows(IndexOutOfBoundsException.class,
                () -> programme.ajouterDemande(1, new Demande(A, B, 0, 0)));
        assertEquals(List.of(), appels);
    }
}
