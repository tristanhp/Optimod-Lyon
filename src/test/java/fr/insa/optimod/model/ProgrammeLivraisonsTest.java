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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    /**
     * Charge le petit plan fourni, où se trouvent les adresses des demandes.
     */
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
        String[][] jeux = { { "Petit1", "petitPlan" }, { "Petit2", "petitPlan" },
                { "Moyen3", "moyenPlan" }, { "Moyen5", "moyenPlan" },
                { "Grand7", "grandPlan" }, { "Grand9", "grandPlan" } };
        int[] tailles = { 1, 2, 3, 5, 7, 9 };
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
                assertEquals(tailles[indice], programme.obtenirDemandes()
                        .size());
                assertEquals(LocalTime.of(8, 0),
                        programme.obtenirHeureDepart());
            }
        }
    }

    /** L'heure de départ s'écrit 8:0:0, 08:00 ou 08:00:30. */
    @Test
    public void formatsDHeure() throws Exception {
        assertEquals(LocalTime.of(8, 0),
                ProgrammeLivraisons.lireHeure("8:0:0"));
        assertEquals(LocalTime.of(8, 0),
                ProgrammeLivraisons.lireHeure("08:00"));
        assertEquals(LocalTime.of(8, 0, 30),
                ProgrammeLivraisons.lireHeure("08:00:30"));
        for (String invalide : new String[] { "8", "25:00", "8:x", "1:2:3:4" }) {
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
                        + " heureDepart='8:0:0'/></demandeDeLivraisons>",
                petitPlan));
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

    // --- Plusieurs livreurs (TIG-95) ---

    /** Programme vide de deux livreurs, entrepôt en C. */
    private static ProgrammeLivraisons deuxLivreurs() {
        ProgrammeLivraisons programme = new ProgrammeLivraisons(C,
                LocalTime.of(8, 0), List.of());
        programme.definirNombreLivreurs(2);
        return programme;
    }

    /** Une tournée par livreur, toutes vides. */
    private static Map<Integer, Tournee> tourneesVides(int nombre) {
        Map<Integer, Tournee> tournees = new HashMap<>();
        for (int livreur = 1; livreur <= nombre; livreur++) {
            tournees.put(livreur, Tournee.vide(C, LocalTime.of(8, 0)));
        }
        return tournees;
    }

    /** Une demande créée sans livreur est confiée au livreur 1. */
    @Test
    public void demandeConfieeAuLivreur1ParDefaut() {
        assertEquals(1, new Demande(A, B, 60, 60).livreur());
    }

    /** Un numéro de livreur inférieur à 1 est refusé. */
    @Test(expected = IllegalArgumentException.class)
    public void numeroDeLivreurInvalideRefuse() {
        new Demande(A, B, 60, 60, 0);
    }

    /** Changer de livreur donne une copie ; l'original ne change pas. */
    @Test
    public void avecLivreurDonneUneCopie() {
        Demande demande = new Demande(A, B, 60, 120);
        Demande copie = demande.avecLivreur(2);
        assertEquals(2, copie.livreur());
        assertEquals(1, demande.livreur());
        assertEquals(new Demande(A, B, 60, 120, 2), copie);
    }

    /** Un fichier de demandes donne un livreur, à qui tout est confié. */
    @Test
    public void demandesDuFichierAuLivreur1() throws Exception {
        try (InputStream xml = getClass().getResourceAsStream(
                "/data/demandePetit2.xml")) {
            ProgrammeLivraisons programme = new ProgrammeLivraisons(xml,
                    petitPlan);
            assertEquals(1, programme.obtenirNombreLivreurs());
            for (Demande demande : programme.obtenirDemandes()) {
                assertEquals(1, demande.livreur());
            }
            assertTrue(programme.obtenirTournees().isEmpty());
        }
    }

    /** Un programme a un seul livreur au départ : livreur 2 refusé. */
    @Test(expected = IllegalArgumentException.class)
    public void unSeulLivreurALaCreation() {
        new ProgrammeLivraisons(C, LocalTime.of(8, 0),
                List.of(new Demande(A, B, 0, 0, 2)));
    }

    /** Il faut au moins un livreur. */
    @Test(expected = IllegalArgumentException.class)
    public void zeroLivreurRefuse() {
        deuxLivreurs().definirNombreLivreurs(0);
    }

    /** On ne retire pas un livreur qui a encore des demandes. */
    @Test
    public void retirerUnLivreurQuiADesDemandesRefuse() {
        ProgrammeLivraisons programme = deuxLivreurs();
        programme.ajouterDemande(0, new Demande(A, B, 0, 0, 2));
        assertThrows(IllegalArgumentException.class,
                () -> programme.definirNombreLivreurs(1));
        assertEquals(2, programme.obtenirNombreLivreurs());
    }

    /** Une demande pour un livreur qui n'existe pas est refusée. */
    @Test
    public void demandePourUnLivreurInconnuRefusee() {
        ProgrammeLivraisons programme = deuxLivreurs();
        assertThrows(IllegalArgumentException.class,
                () -> programme.ajouterDemande(0, new Demande(A, B, 0, 0, 3)));
        assertTrue(programme.obtenirDemandes().isEmpty());
    }

    /** Remplacer une demande (changer de livreur) prévient les observateurs. */
    @Test
    public void remplacerUneDemandePrevientLesObservateurs() {
        ProgrammeLivraisons programme = deuxLivreurs();
        Demande demande = new Demande(A, B, 0, 0);
        programme.ajouterDemande(0, demande);
        List<Integer> appels = new ArrayList<>();
        programme.ajouterObservateur(source -> appels.add(1));
        programme.remplacerDemande(0, demande.avecLivreur(2));
        assertEquals(2, programme.obtenirDemandes().get(0).livreur());
        assertEquals(List.of(1), appels);
    }

    /**
     * Remplacer une demande et ranger les tournées d'un coup ne prévient les
     * observateurs qu'une fois, avec la demande et les tournées déjà à jour.
     */
    @Test
    public void remplacerDemandeEtTourneesNotifieUneSeuleFois() {
        ProgrammeLivraisons programme = deuxLivreurs();
        Demande demande = new Demande(A, B, 0, 0);
        programme.ajouterDemande(0, demande);
        Map<Integer, Tournee> tournees = tourneesVides(2);
        List<String> vues = new ArrayList<>();
        programme.ajouterObservateur(source -> vues.add(
                ((ProgrammeLivraisons) source).obtenirDemandes().get(0)
                        .livreur() + "/"
                        + ((ProgrammeLivraisons) source).obtenirTournees()
                                .size()));
        Demande remplacee = programme.remplacerDemandeEtTournees(0,
                demande.avecLivreur(2), tournees);
        assertSame(demande, remplacee);
        assertEquals(List.of("2/2"), vues);
        assertSame(tournees.get(2), programme.obtenirTournees().get(2));
    }

    /**
     * Un refus (livreur inconnu, tournées incomplètes) ne change ni les
     * demandes ni les tournées, et ne prévient personne.
     */
    @Test
    public void remplacerDemandeEtTourneesRefuseNeChangeRien() {
        ProgrammeLivraisons programme = deuxLivreurs();
        Demande demande = new Demande(A, B, 0, 0);
        programme.ajouterDemande(0, demande);
        Map<Integer, Tournee> avant = tourneesVides(2);
        programme.definirTournees(avant);
        List<Integer> appels = new ArrayList<>();
        programme.ajouterObservateur(source -> appels.add(1));
        assertThrows(IllegalArgumentException.class,
                () -> programme.remplacerDemandeEtTournees(0,
                        demande.avecLivreur(3), tourneesVides(2)));
        assertThrows(IllegalArgumentException.class,
                () -> programme.remplacerDemandeEtTournees(0,
                        demande.avecLivreur(2), tourneesVides(1)));
        assertThrows(IndexOutOfBoundsException.class,
                () -> programme.remplacerDemandeEtTournees(1,
                        demande.avecLivreur(2), tourneesVides(2)));
        assertEquals(List.of(demande), programme.obtenirDemandes());
        assertSame(avant.get(1), programme.obtenirTournees().get(1));
        assertSame(avant.get(2), programme.obtenirTournees().get(2));
        assertEquals(List.of(), appels);
    }

    /** Les tournées rangées sont relues par livreur et notifiées. */
    @Test
    public void tourneesRangeesParLivreur() {
        ProgrammeLivraisons programme = deuxLivreurs();
        List<Integer> appels = new ArrayList<>();
        programme.ajouterObservateur(source -> appels.add(1));
        Map<Integer, Tournee> tournees = tourneesVides(2);
        programme.definirTournees(tournees);
        assertEquals(List.of(1, 2),
                new ArrayList<>(programme.obtenirTournees().keySet()));
        assertSame(tournees.get(2), programme.obtenirTournees().get(2));
        assertEquals(List.of(1), appels);
    }

    /** Il faut exactement une tournée par livreur. */
    @Test
    public void tourneesIncompletesRefusees() {
        ProgrammeLivraisons programme = deuxLivreurs();
        assertThrows(IllegalArgumentException.class,
                () -> programme.definirTournees(tourneesVides(1)));
        assertTrue(programme.obtenirTournees().isEmpty());
    }

    /** Les tournées renvoyées sont une copie qu'on ne peut pas modifier. */
    @Test
    public void tourneesRenvoyeesNonModifiables() {
        ProgrammeLivraisons programme = deuxLivreurs();
        programme.definirTournees(tourneesVides(2));
        Map<Integer, Tournee> copie = programme.obtenirTournees();
        assertThrows(UnsupportedOperationException.class,
                () -> copie.remove(1));
        programme.effacerTournees();
        assertEquals(2, copie.size());
        assertTrue(programme.obtenirTournees().isEmpty());
    }

    /** Remplacer la tournée d'un livreur après un recalcul local. */
    @Test
    public void remplacerLaTourneeDUnLivreur() {
        ProgrammeLivraisons programme = deuxLivreurs();
        programme.definirTournees(tourneesVides(2));
        Tournee nouvelle = Tournee.vide(C, LocalTime.of(9, 0));
        programme.definirTournee(2, nouvelle);
        assertSame(nouvelle, programme.obtenirTournees().get(2));
    }

    /** Sans tournées calculées, on ne peut pas en remplacer une seule. */
    @Test(expected = IllegalStateException.class)
    public void remplacerUneTourneeSansCalculRefuse() {
        deuxLivreurs().definirTournee(1, Tournee.vide(C, LocalTime.of(8, 0)));
    }

    /** Un livreur ajouté après le calcul a une tournée vide. */
    @Test
    public void livreurAjouteApresLeCalculATourneeVide() {
        ProgrammeLivraisons programme = deuxLivreurs();
        programme.definirTournees(tourneesVides(2));
        programme.definirNombreLivreurs(3);
        Tournee ajoutee = programme.obtenirTournees().get(3);
        assertEquals(1, ajoutee.arrets().size());
        assertEquals(LocalTime.of(8, 0), ajoutee.obtenirHeureRetour());
        programme.definirNombreLivreurs(1);
        assertEquals(List.of(1),
                new ArrayList<>(programme.obtenirTournees().keySet()));
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
