package fr.insa.optimod.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.xml.sax.SAXException;

import fr.insa.optimod.model.ProgrammeLivraisons.Demande;

/**
 * Tests de {@link Plan} : lecture du fichier de plan, plus courts chemins et
 * calcul de la tournée.
 */
public class PlanTest {

    private static final double PRECISION = 1e-9;

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
        return new Plan(new ByteArrayInputStream(
                xml.getBytes(StandardCharsets.UTF_8)));
    }

    // --- Lecture du fichier de plan ---

    /** Les nœuds sont lus avec leur latitude et leur longitude. */
    @Test
    public void noeudsLus() throws Exception {
        Plan plan = lire(PETIT_PLAN);
        assertEquals(3, plan.obtenirNoeuds().size());
        Noeud noeud = plan.obtenirNoeud(1);
        assertEquals(45.75, noeud.latitude(), PRECISION);
        assertEquals(4.85, noeud.longitude(), PRECISION);
    }

    /** Les identifiants qui dépassent un int sont acceptés. */
    @Test
    public void grandIdAccepte() throws Exception {
        assertNotNull(lire(PETIT_PLAN).obtenirNoeud(2129259178L));
    }

    /** Les tronçons relient les bons nœuds. */
    @Test
    public void tronconsLus() throws Exception {
        Plan plan = lire(PETIT_PLAN);
        assertEquals(3, plan.obtenirTroncons().size());
        Troncon troncon = plan.obtenirTroncons().get(0);
        assertEquals(1, troncon.origine().identifiant());
        assertEquals(2129259178L, troncon.destination().identifiant());
        assertEquals(69.9, troncon.longueur(), PRECISION);
        assertEquals("Rue Danton", troncon.nomRue());
    }

    /** Un identifiant inconnu ne renvoie pas de nœud. */
    @Test
    public void noeudInconnu() throws Exception {
        assertNull(lire(PETIT_PLAN).obtenirNoeud(42));
    }

    /** Un tronçon vers un nœud absent est refusé. */
    @Test(expected = SAXException.class)
    public void tronconVersNoeudInconnuRefuse() throws Exception {
        lire("<reseau><noeud id=\"1\" latitude=\"0\" longitude=\"0\"/>"
                + "<troncon destination=\"2\" longueur=\"1\" nomRue=\"\""
                + " origine=\"1\"/></reseau>");
    }

    /** Un nombre illisible est refusé avec une SAXException. */
    @Test(expected = SAXException.class)
    public void nombreIllisibleRefuse() throws Exception {
        lire("<reseau><noeud id=\"x\" latitude=\"0\" longitude=\"0\"/></reseau>");
    }

    /** Un XML mal formé est refusé. */
    @Test(expected = SAXException.class)
    public void xmlMalFormeRefuse() throws Exception {
        lire("<reseau>");
    }

    /** Le petit plan fourni se lit en entier. */
    @Test
    public void petitPlanFourni() throws Exception {
        Plan plan = PlansPourTests.petitPlan();
        assertFalse(plan.obtenirNoeuds().isEmpty());
        assertFalse(plan.obtenirTroncons().isEmpty());
    }

    /** Un plan se lit aussi depuis un chemin de fichier. */
    @Test
    public void lectureDepuisFichier() throws Exception {
        File fichier = File.createTempFile("plan", ".xml");
        fichier.deleteOnExit();
        Files.writeString(fichier.toPath(), PETIT_PLAN);
        assertEquals(3, new Plan(fichier.getPath()).obtenirNoeuds().size());
    }

    // --- Plus courts chemins (A*) ---

    private static Chemin chemin(Plan plan, long source, long cible) {
        return plan.plusCourtChemin(plan.obtenirNoeud(source),
                plan.obtenirNoeud(cible));
    }

    /** Vérifie que le chemin relie la source à la cible sans trou. */
    private static void verifierContigu(Chemin chemin, Noeud source,
            Noeud cible) {
        Noeud courant = source;
        double somme = 0;
        for (Troncon troncon : chemin.troncons()) {
            assertEquals(courant, troncon.origine());
            courant = troncon.destination();
            somme += troncon.longueur();
        }
        assertEquals(cible, courant);
        assertEquals(chemin.longueur(), somme, PRECISION);
    }

    /** Le détour plus court est préféré au tronçon direct. */
    @Test
    public void detourPlusCourtChoisi() throws Exception {
        Plan plan = PlansPourTests.petitReseau();
        Chemin chemin = chemin(plan, 1, 2);
        assertEquals(249.6, chemin.longueur(), PRECISION);
        assertEquals(2, chemin.troncons().size());
        verifierContigu(chemin, plan.obtenirNoeud(1), plan.obtenirNoeud(2));
    }

    /** Les tronçons sont orientés : le retour peut être plus long. */
    @Test
    public void sensUniqueRespecte() throws Exception {
        Plan plan = PlansPourTests.petitReseau();
        assertEquals(100, chemin(plan, 1, 3).longueur(), PRECISION);
        Chemin retour = chemin(plan, 3, 1);
        assertEquals(449.6, retour.longueur(), PRECISION);
        verifierContigu(retour, plan.obtenirNoeud(3), plan.obtenirNoeud(1));
    }

    /** Un nœud isolé est inaccessible, dans les deux sens. */
    @Test
    public void noeudIsoleInaccessible() throws Exception {
        Plan plan = PlansPourTests.petitReseau();
        for (Chemin chemin : List.of(chemin(plan, 1, 4), chemin(plan, 4, 1))) {
            assertFalse(chemin.estAccessible());
            assertTrue(chemin.troncons().isEmpty());
        }
    }

    /** L'heuristique ne surestime jamais un tronçon du plan fourni. */
    @Test
    public void heuristiqueAdmissibleSurPetitPlan() throws Exception {
        for (Troncon troncon : PlansPourTests.petitPlan().obtenirTroncons()) {
            assertTrue(Plan.heuristique(troncon.origine(),
                    troncon.destination()) <= troncon.longueur());
        }
    }

    // --- Calcul de la tournée ---

    /**
     * Programme sur le petit réseau : entrepôt en 1, une demande collectée
     * en {@code collecte} (60 s) et livrée en {@code livraison} (120 s).
     */
    private static ProgrammeLivraisons programme(Plan plan, LocalTime depart,
            long collecte, long livraison) {
        return new ProgrammeLivraisons(plan.obtenirNoeud(1), depart,
                List.of(new Demande(plan.obtenirNoeud(collecte),
                        plan.obtenirNoeud(livraison), 60, 120)));
    }

    private static void assertArret(Tournee.Arret arret, long noeud,
            String arrivee, String depart) {
        assertEquals(noeud, arret.noeud().identifiant());
        assertEquals(LocalTime.parse(arrivee), arret.arrivee());
        assertEquals(LocalTime.parse(depart), arret.depart());
    }

    /**
     * Heures de passage calculées à la main à 15 km/h : 100 m = 24 s,
     * 149,6 m = 35,9 s arrondi à 36 s, 300 m = 72 s.
     */
    @Test
    public void heuresDePassageCalculeesALaMain() throws Exception {
        Plan plan = PlansPourTests.petitReseau();
        Tournee tournee = plan.calculerTournee(
                programme(plan, LocalTime.of(8, 0), 3, 2));
        assertEquals(4, tournee.arrets().size());
        assertArret(tournee.arrets().get(0), 1, "08:00", "08:00");
        assertArret(tournee.arrets().get(1), 3, "08:00:24", "08:01:24");
        assertArret(tournee.arrets().get(2), 2, "08:02", "08:04");
        assertArret(tournee.arrets().get(3), 1, "08:05:12", "08:05:12");
        assertEquals(LocalTime.of(8, 5, 12), tournee.obtenirHeureRetour());
        assertEquals(3, tournee.chemins().size());
        verifierContigu(tournee.chemins().get(1), plan.obtenirNoeud(3),
                plan.obtenirNoeud(2));
    }

    /**
     * Contrainte d'ordre : collecte en 2, livraison en 3. L'ordre 1, 3, 2
     * serait plus court mais livrerait avant de collecter.
     */
    @Test
    public void collecteAvantLivraison() throws Exception {
        Plan plan = PlansPourTests.petitReseau();
        Tournee tournee = plan.calculerTournee(
                programme(plan, LocalTime.of(8, 0), 2, 3));
        List<Long> ordre = new ArrayList<>();
        for (Tournee.Arret arret : tournee.arrets()) {
            ordre.add(arret.noeud().identifiant());
        }
        assertEquals(List.of(1L, 2L, 3L, 1L), ordre);
    }

    /** Sans demande, la tournée se réduit au départ de l'entrepôt. */
    @Test
    public void programmeSansDemande() throws Exception {
        Plan plan = PlansPourTests.petitReseau();
        Tournee tournee = plan.calculerTournee(new ProgrammeLivraisons(
                plan.obtenirNoeud(1), LocalTime.of(8, 0), List.of()));
        assertEquals(1, tournee.arrets().size());
        assertTrue(tournee.chemins().isEmpty());
        assertEquals(LocalTime.of(8, 0), tournee.obtenirHeureRetour());
    }

    /** Une adresse inaccessible rend la tournée impossible. */
    @Test
    public void adresseInaccessibleRefusee() throws Exception {
        Plan plan = PlansPourTests.petitReseau();
        IllegalStateException erreur = assertThrows(
                IllegalStateException.class, () -> plan.calculerTournee(
                        programme(plan, LocalTime.of(8, 0), 4, 2)));
        assertTrue(erreur.getMessage().contains("inaccessible"));
    }

    /** Le retour à l'entrepôt doit avoir lieu avant 13 h (TIG-46). */
    @Test
    public void retourApres13hRefuse() throws Exception {
        Plan plan = PlansPourTests.petitReseau();
        // La tournée dure 5 min 12 s.
        assertEquals(LocalTime.of(12, 59, 12), plan.calculerTournee(
                programme(plan, LocalTime.of(12, 54), 3, 2))
                .obtenirHeureRetour());
        IllegalStateException erreur = assertThrows(
                IllegalStateException.class, () -> plan.calculerTournee(
                        programme(plan, LocalTime.of(12, 58), 3, 2)));
        assertTrue(erreur.getMessage().contains("13:00"));
    }

    /**
     * Sur les fichiers fournis : chaque colis est collecté avant d'être
     * livré, les heures se suivent et les chemins relient les arrêts.
     */
    @Test
    public void tourneesDesFichiersFournis() throws Exception {
        String[][] jeux = {{"petitPlan", "demandePetit2"},
            {"moyenPlan", "demandeMoyen5"}};
        for (String[] jeu : jeux) {
            Plan plan = lirePlanFourni(jeu[0]);
            ProgrammeLivraisons programme;
            try (InputStream xml = getClass().getResourceAsStream(
                    "/data/" + jeu[1] + ".xml")) {
                programme = new ProgrammeLivraisons(xml, plan);
            }
            Tournee tournee = plan.calculerTournee(programme);
            List<Tournee.Arret> arrets = tournee.arrets();
            assertEquals(2 * programme.obtenirDemandes().size() + 2,
                    arrets.size());
            assertSame(programme.obtenirEntrepot(), arrets.get(0).noeud());
            assertEquals(programme.obtenirEntrepot(),
                    arrets.get(arrets.size() - 1).noeud());
            for (int rang = 1; rang < arrets.size(); rang++) {
                assertFalse(arrets.get(rang).arrivee().isBefore(
                        arrets.get(rang - 1).depart()));
                verifierContigu(tournee.chemins().get(rang - 1),
                        arrets.get(rang - 1).noeud(), arrets.get(rang).noeud());
            }
            for (Demande demande : programme.obtenirDemandes()) {
                assertTrue(jeu[1], rang(arrets, demande.collecte())
                        < rang(arrets, demande.livraison()));
            }
        }
    }

    private Plan lirePlanFourni(String nom) throws Exception {
        try (InputStream xml = getClass().getResourceAsStream(
                "/data/" + nom + ".xml")) {
            return new Plan(xml);
        }
    }

    /** Le recalcul conserve un ordre imposé et ses durées, sans TSP. */
    @Test
    public void recalculConserveOrdreImpose() throws Exception {
        Plan plan = PlansPourTests.petitReseau();
        List<Noeud> adresses = List.of(plan.obtenirNoeud(1),
                plan.obtenirNoeud(2), plan.obtenirNoeud(3),
                plan.obtenirNoeud(1));
        Tournee tournee = plan.recalculerTournee(LocalTime.of(8, 0),
                adresses, List.of(0, 120, 240, 0));
        assertEquals(adresses,
                tournee.arrets().stream().map(Tournee.Arret::noeud).toList());
        assertEquals(LocalTime.of(8, 1), tournee.arrets().get(1).arrivee());
        assertEquals(LocalTime.of(8, 3), tournee.arrets().get(1).depart());
        assertEquals(3, tournee.chemins().size());
    }

    /** Le recalcul conserve la règle actuelle de retour avant 13 h. */
    @Test
    public void recalculApres13hRefuse() throws Exception {
        Plan plan = PlansPourTests.petitReseau();
        assertThrows(IllegalStateException.class,
                () -> plan.recalculerTournee(LocalTime.of(12, 59),
                        List.of(plan.obtenirNoeud(1), plan.obtenirNoeud(2),
                                plan.obtenirNoeud(1)), List.of(0, 300, 0)));
    }

    /** Rang du premier arrêt (hors départ) sur un nœud. */
    private static int rang(List<Tournee.Arret> arrets, Noeud noeud) {
        for (int rang = 1; rang < arrets.size(); rang++) {
            if (arrets.get(rang).noeud().equals(noeud)) {
                return rang;
            }
        }
        throw new AssertionError("Nœud absent de la tournée : " + noeud);
    }
    /** La position choisie est respectée même si elle allonge le parcours. */
    @Test
    public void insertionRespectePositionChoisie() throws Exception {
        Plan plan = PlansPourTests.petitReseau();
        Noeud entrepot = plan.obtenirNoeud(1);
        Noeud collecte = plan.obtenirNoeud(3);
        Noeud livraison = plan.obtenirNoeud(2);
        Tournee avant = plan.recalculerTournee(LocalTime.of(8, 0),
                List.of(entrepot, collecte, livraison, entrepot),
                List.of(0, 60, 60, 0));
        Tournee apres = plan.insererDemande(avant,
                new Demande(collecte, livraison, 60, 60), 3);
        assertEquals(List.of(entrepot, collecte, livraison, collecte,
                livraison, entrepot), apres.arrets().stream()
                        .map(Tournee.Arret::noeud).toList());
        assertEquals(LocalTime.of(8, 8, 24), apres.obtenirHeureRetour());
        assertSame(avant.chemins().get(0), apres.chemins().get(0));
        assertSame(avant.chemins().get(1), apres.chemins().get(1));
        assertEquals(avant.arrets().get(1).debutPlage(),
                apres.arrets().get(1).debutPlage());
    }

    /** L'insertion décale les arrêts suivants sans changer leurs plages. */
    @Test
    public void insertionDecaleEtSignaleLesArretsSuivants() throws Exception {
        Plan plan = PlansPourTests.petitReseau();
        Noeud entrepot = plan.obtenirNoeud(1);
        Noeud collecte = plan.obtenirNoeud(3);
        Noeud livraison = plan.obtenirNoeud(2);
        Tournee avant = plan.recalculerTournee(LocalTime.of(8, 0),
                List.of(entrepot, collecte, livraison, entrepot),
                List.of(0, 60, 60, 0));
        Tournee apres = plan.insererDemande(avant,
                new Demande(collecte, livraison, 3600, 60), 1);
        assertEquals(3792, apres.arrets().get(3).obtenirDecalageSecondes());
        assertTrue(apres.arrets().get(3).estHorsPlage());
        assertEquals(avant.arrets().get(1).debutPlage(),
                apres.arrets().get(3).debutPlage());
        assertEquals(avant.arrets().get(1).finPlage(),
                apres.arrets().get(3).finPlage());
        assertSame(avant.chemins().get(1), apres.chemins().get(3));
        assertSame(avant.chemins().get(2), apres.chemins().get(4));
        assertEquals(LocalTime.of(8, 0, 24), avant.arrets().get(1).arrivee());
    }

    /** Les positions hors des intervalles de la tournée sont refusées. */
    @Test
    public void positionInsertionInvalide() throws Exception {
        Plan plan = PlansPourTests.petitReseau();
        Tournee avant = plan.recalculerTournee(LocalTime.of(8, 0),
                List.of(plan.obtenirNoeud(1)), List.of(0));
        Demande demande = new Demande(plan.obtenirNoeud(3),
                plan.obtenirNoeud(2), 60, 60);
        assertThrows(IllegalArgumentException.class,
                () -> plan.insererDemande(avant, demande, 0));
        assertThrows(IllegalArgumentException.class,
                () -> plan.insererDemande(avant, demande, 2));
        Tournee apres = plan.insererDemande(avant, demande, 1);
        assertEquals(4, apres.arrets().size());
    }

    /** Une insertion impossible laisse la tournée d'origine intacte. */
    @Test(expected = IllegalStateException.class)
    public void insertionImpossible() throws Exception {
        Plan plan = PlansPourTests.petitReseau();
        Noeud entrepot = plan.obtenirNoeud(1);
        Tournee avant = plan.recalculerTournee(LocalTime.of(12, 59),
                List.of(entrepot), List.of(0));
        plan.insererDemande(avant, new Demande(plan.obtenirNoeud(3),
                plan.obtenirNoeud(2), 60, 60), 1);
    }

    /** Une durée d'une journée ne doit pas être ramenée à zéro à minuit. */
    @Test(expected = IllegalStateException.class)
    public void dureeJourneeEntiereRefusee() throws Exception {
        Plan plan = PlansPourTests.petitReseau();
        Tournee avant = plan.recalculerTournee(LocalTime.of(8, 0),
                List.of(plan.obtenirNoeud(1)), List.of(0));
        plan.insererDemande(avant, new Demande(plan.obtenirNoeud(3),
                plan.obtenirNoeud(2), 24 * 3600, 60), 1);
    }

    /** Une adresse inaccessible ne peut être insérée. */
    @Test(expected = IllegalStateException.class)
    public void insertionInaccessible() throws Exception {
        Plan plan = PlansPourTests.petitReseau();
        Tournee avant = plan.recalculerTournee(LocalTime.of(8, 0),
                List.of(plan.obtenirNoeud(1)), List.of(0));
        plan.insererDemande(avant, new Demande(plan.obtenirNoeud(4),
                plan.obtenirNoeud(2), 60, 60), 1);
    }

}
