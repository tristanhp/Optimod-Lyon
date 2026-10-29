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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
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

    /** Rang du premier arrêt (hors départ) sur un nœud. */
    private static int rang(List<Tournee.Arret> arrets, Noeud noeud) {
        for (int rang = 1; rang < arrets.size(); rang++) {
            if (arrets.get(rang).noeud().equals(noeud)) {
                return rang;
            }
        }
        throw new AssertionError("Nœud absent de la tournée : " + noeud);
    }

    // --- Recalcul local d'une tournée, sans relancer le TSP (TIG-101) ---

    /** Adresses d'une tournée, dans l'ordre de passage (départ et retour). */
    private static List<Noeud> adressesDe(Tournee tournee) {
        List<Noeud> adresses = new ArrayList<>();
        for (Tournee.Arret arret : tournee.arrets()) {
            adresses.add(arret.noeud());
        }
        return adresses;
    }

    /** Durée sur place de chaque arrêt : départ moins arrivée, en secondes. */
    private static List<Integer> dureesDe(Tournee tournee) {
        List<Integer> durees = new ArrayList<>();
        for (Tournee.Arret arret : tournee.arrets()) {
            durees.add((int) arret.arrivee().until(arret.depart(),
                    ChronoUnit.SECONDS));
        }
        return durees;
    }

    /** Lit un fichier de demandes fourni, pour un plan déjà lu. */
    private ProgrammeLivraisons lireProgrammeFourni(Plan plan, String nom)
            throws Exception {
        try (InputStream xml = getClass().getResourceAsStream(
                "/data/" + nom + ".xml")) {
            return new ProgrammeLivraisons(xml, plan);
        }
    }

    /**
     * Rendre à {@code recalculerTournee} l'ordre trouvé par le TSP redonne la
     * même tournée que {@code calculerTournee} : mêmes arrêts, mêmes heures,
     * mêmes chemins.
     */
    @Test
    public void ordreDuTspRedonneLesMemesHeures() throws Exception {
        String[][] jeux = {{"petitPlan", "demandePetit2"},
            {"moyenPlan", "demandeMoyen5"}};
        for (String[] jeu : jeux) {
            Plan plan = lirePlanFourni(jeu[0]);
            ProgrammeLivraisons programme = lireProgrammeFourni(plan, jeu[1]);
            Tournee attendue = plan.calculerTournee(programme);
            Tournee recalculee = plan.recalculerTournee(
                    programme.obtenirHeureDepart(), adressesDe(attendue),
                    dureesDe(attendue));
            assertEquals(jeu[1], attendue, recalculee);
        }
    }

    /**
     * L'ordre donné est suivi même s'il est plus long que le meilleur : 1, 2,
     * 3, 1 passe par le détour 2 vers 1 vers 3 (400 m = 96 s) et revient par
     * 449,6 m (108 s), alors que 1, 3, 2, 1 revient à 08:05:12. Heures
     * calculées à la main à 15 km/h : 249,6 m = 59,9 s arrondi à 60 s.
     */
    @Test
    public void ordreImposeRespecteMemeSiPlusLong() throws Exception {
        Plan plan = PlansPourTests.petitReseau();
        List<Noeud> adresses = List.of(plan.obtenirNoeud(1),
                plan.obtenirNoeud(2), plan.obtenirNoeud(3),
                plan.obtenirNoeud(1));
        Tournee tournee = plan.recalculerTournee(LocalTime.of(8, 0), adresses,
                List.of(0, 60, 120, 0));
        assertEquals(4, tournee.arrets().size());
        assertArret(tournee.arrets().get(0), 1, "08:00", "08:00");
        assertArret(tournee.arrets().get(1), 2, "08:01", "08:02");
        assertArret(tournee.arrets().get(2), 3, "08:03:36", "08:05:36");
        assertArret(tournee.arrets().get(3), 1, "08:07:24", "08:07:24");
        assertEquals(3, tournee.chemins().size());
        verifierContigu(tournee.chemins().get(1), plan.obtenirNoeud(2),
                plan.obtenirNoeud(3));
    }

    /** Une tournée sans arrêt se réduit au départ de l'entrepôt. */
    @Test
    public void recalculSansArretReduitAuDepart() throws Exception {
        Plan plan = PlansPourTests.petitReseau();
        Noeud entrepot = plan.obtenirNoeud(1);
        for (List<Noeud> adresses : List.of(List.of(entrepot),
                List.of(entrepot, entrepot))) {
            Tournee tournee = plan.recalculerTournee(LocalTime.of(8, 0),
                    adresses, Collections.nCopies(adresses.size(), 0));
            assertEquals(1, tournee.arrets().size());
            assertTrue(tournee.chemins().isEmpty());
            assertEquals(LocalTime.of(8, 0), tournee.obtenirHeureRetour());
        }
    }

    /** Le retour à l'entrepôt doit aussi avoir lieu avant 13 h au recalcul. */
    @Test
    public void recalculRetourApres13hRefuse() throws Exception {
        Plan plan = PlansPourTests.petitReseau();
        List<Noeud> adresses = List.of(plan.obtenirNoeud(1),
                plan.obtenirNoeud(3), plan.obtenirNoeud(2),
                plan.obtenirNoeud(1));
        List<Integer> durees = List.of(0, 60, 120, 0);
        // La tournée dure 5 min 12 s.
        assertEquals(LocalTime.of(12, 59, 12), plan.recalculerTournee(
                LocalTime.of(12, 54), adresses, durees).obtenirHeureRetour());
        IllegalStateException erreur = assertThrows(
                IllegalStateException.class, () -> plan.recalculerTournee(
                        LocalTime.of(12, 58), adresses, durees));
        assertTrue(erreur.getMessage().contains("13:00"));
    }

    /** Une adresse inaccessible rend le recalcul impossible. */
    @Test
    public void recalculAdresseInaccessibleRefusee() throws Exception {
        Plan plan = PlansPourTests.petitReseau();
        List<Noeud> adresses = List.of(plan.obtenirNoeud(1),
                plan.obtenirNoeud(4), plan.obtenirNoeud(2),
                plan.obtenirNoeud(1));
        IllegalStateException erreur = assertThrows(
                IllegalStateException.class, () -> plan.recalculerTournee(
                        LocalTime.of(8, 0), adresses, List.of(0, 60, 120, 0)));
        assertTrue(erreur.getMessage().contains("inaccessible"));
    }

    /** Une tournée sans retour à l'entrepôt ou sans durées est refusée. */
    @Test
    public void recalculRefuseDesListesIncoherentes() throws Exception {
        Plan plan = PlansPourTests.petitReseau();
        List<Noeud> sansRetour = List.of(plan.obtenirNoeud(1),
                plan.obtenirNoeud(2), plan.obtenirNoeud(3));
        assertThrows(IllegalArgumentException.class,
                () -> plan.recalculerTournee(LocalTime.of(8, 0), sansRetour,
                        List.of(0, 60, 120)));
        List<Noeud> avecRetour = List.of(plan.obtenirNoeud(1),
                plan.obtenirNoeud(2), plan.obtenirNoeud(1));
        assertThrows(IllegalArgumentException.class,
                () -> plan.recalculerTournee(LocalTime.of(8, 0), avecRetour,
                        List.of(0, 60)));
        assertThrows(IllegalArgumentException.class,
                () -> plan.recalculerTournee(LocalTime.of(8, 0), List.of(),
                        List.of()));
    }

    /**
     * Supprimer une demande retire ses deux arrêts et garde l'ordre de tous les
     * autres ; les heures sont celles d'un recalcul sur ce nouvel ordre, et le
     * retour à l'entrepôt n'est jamais plus tard qu'avant.
     */
    @Test
    public void suppressionGardeLOrdreDesAutresArrets() throws Exception {
        Plan plan = lirePlanFourni("moyenPlan");
        ProgrammeLivraisons programme = lireProgrammeFourni(plan,
                "demandeMoyen5");
        Tournee tournee = plan.calculerTournee(programme);
        for (Demande demande : programme.obtenirDemandes()) {
            List<Noeud> adresses = adressesDe(tournee);
            List<Integer> durees = dureesDe(tournee);
            // Les deux arrêts de la demande, retrouvés par leur nœud : dans
            // les fichiers fournis, aucun nœud n'est partagé entre demandes.
            for (Noeud noeud : List.of(demande.livraison(),
                    demande.collecte())) {
                int rang = adresses.indexOf(noeud);
                adresses.remove(rang);
                durees.remove(rang);
            }
            Tournee sans = plan.calculerTourneeSansDemande(tournee, demande);
            assertEquals(adresses, adressesDe(sans));
            assertEquals(tournee.arrets().size() - 2, sans.arrets().size());
            assertEquals(plan.recalculerTournee(programme.obtenirHeureDepart(),
                    adresses, durees), sans);
            assertFalse(sans.obtenirHeureRetour().isAfter(
                    tournee.obtenirHeureRetour()));
        }
    }

    /** Supprimer la seule demande laisse la tournée sans arrêt. */
    @Test
    public void suppressionDeLaDerniereDemandeLaisseLeDepart()
            throws Exception {
        Plan plan = PlansPourTests.petitReseau();
        ProgrammeLivraisons programme = programme(plan, LocalTime.of(8, 0), 3,
                2);
        Tournee sans = plan.calculerTourneeSansDemande(
                plan.calculerTournee(programme),
                programme.obtenirDemandes().get(0));
        assertEquals(plan.calculerTournee(new ProgrammeLivraisons(
                plan.obtenirNoeud(1), LocalTime.of(8, 0), List.of())), sans);
    }

    /** Une demande dont les arrêts ne sont pas dans la tournée est refusée. */
    @Test
    public void suppressionDUneDemandeAbsenteRefusee() throws Exception {
        Plan plan = PlansPourTests.petitReseau();
        Tournee tournee = plan.calculerTournee(
                programme(plan, LocalTime.of(8, 0), 3, 2));
        Demande autre = new Demande(plan.obtenirNoeud(2),
                plan.obtenirNoeud(3), 60, 120);
        IllegalArgumentException erreur = assertThrows(
                IllegalArgumentException.class,
                () -> plan.calculerTourneeSansDemande(tournee, autre));
        assertTrue(erreur.getMessage().contains("introuvable"));
    }

    /**
     * Si le même nœud avec la même durée sert deux fois (deux demandes ont le
     * même point de collecte), on ne sait pas quel arrêt retirer : refusé, pour
     * ne jamais casser la contrainte d'ordre d'une autre demande.
     */
    @Test
    public void suppressionRefuseUnArretAmbigu() throws Exception {
        Plan plan = PlansPourTests.petitReseau();
        Demande premiere = new Demande(plan.obtenirNoeud(3),
                plan.obtenirNoeud(2), 60, 120);
        Demande seconde = new Demande(plan.obtenirNoeud(3),
                plan.obtenirNoeud(1), 60, 120);
        Tournee tournee = plan.calculerTournee(new ProgrammeLivraisons(
                plan.obtenirNoeud(1), LocalTime.of(8, 0),
                List.of(premiere, seconde)));
        IllegalArgumentException erreur = assertThrows(
                IllegalArgumentException.class,
                () -> plan.calculerTourneeSansDemande(tournee, premiere));
        assertTrue(erreur.getMessage().contains("plusieurs fois"));
    }

    // --- Changer l'ordre des arrêts (TIG-44) ---

    /**
     * Tournée à la main sur le petit réseau, avec deux demandes dont les arrêts
     * se distinguent par leur durée sur place : la première collecte en 3
     * (60 s) et livre en 2 (120 s), la seconde collecte en 2 (30 s) et livre en
     * 3 (45 s). Ordre : 1, 3 (60), 2 (30), 2 (120), 3 (45), 1.
     */
    private static List<Demande> deuxDemandesDuPetitReseau(Plan plan) {
        return List.of(new Demande(plan.obtenirNoeud(3),
                plan.obtenirNoeud(2), 60, 120),
                new Demande(plan.obtenirNoeud(2), plan.obtenirNoeud(3), 30,
                        45));
    }

    /** Tournée de {@link #deuxDemandesDuPetitReseau} partant à l'heure dite. */
    private static Tournee tourneeDeDeuxDemandes(Plan plan, LocalTime depart) {
        List<Noeud> adresses = List.of(plan.obtenirNoeud(1),
                plan.obtenirNoeud(3), plan.obtenirNoeud(2),
                plan.obtenirNoeud(2), plan.obtenirNoeud(3),
                plan.obtenirNoeud(1));
        return plan.recalculerTournee(depart, adresses,
                List.of(0, 60, 30, 120, 45, 0));
    }

    /**
     * Descendre un arrêt l'échange avec le suivant : les autres arrêts gardent
     * leur ordre, et les heures sont celles d'un recalcul sur ce nouvel ordre.
     */
    @Test
    public void descendreUnArretEchangeAvecLeSuivant() throws Exception {
        Plan plan = PlansPourTests.petitReseau();
        List<Demande> demandes = deuxDemandesDuPetitReseau(plan);
        Tournee tournee = tourneeDeDeuxDemandes(plan, LocalTime.of(8, 0));
        Tournee apres = plan.calculerTourneeOrdreChange(tournee, demandes, 1,
                1);
        // Ordre attendu : 1, 2 (30), 3 (60), 2 (120), 3 (45), 1.
        List<Noeud> adresses = new ArrayList<>(adressesDe(tournee));
        List<Integer> durees = new ArrayList<>(dureesDe(tournee));
        Collections.swap(adresses, 1, 2);
        Collections.swap(durees, 1, 2);
        assertEquals(durees, dureesDe(apres));
        assertEquals(plan.recalculerTournee(LocalTime.of(8, 0), adresses,
                durees), apres);
        assertEquals(30, dureesDe(apres).get(1).intValue());
    }

    /** Monter l'arrêt qu'on vient de descendre redonne la tournée de départ. */
    @Test
    public void monterApresDescendreRedonneLaTournee() throws Exception {
        Plan plan = PlansPourTests.petitReseau();
        List<Demande> demandes = deuxDemandesDuPetitReseau(plan);
        Tournee tournee = tourneeDeDeuxDemandes(plan, LocalTime.of(8, 0));
        Tournee apres = plan.calculerTourneeOrdreChange(tournee, demandes, 1,
                1);
        assertEquals(tournee, plan.calculerTourneeOrdreChange(apres, demandes,
                2, -1));
    }

    /**
     * Sur une tournée calculée par le voyageur de commerce, chaque échange est
     * soit accepté et égal à un recalcul sur le nouvel ordre, soit refusé
     * parce qu'il placerait un point de livraison avant son point de collecte.
     */
    @Test
    public void echangesSurUneTourneeCalculee() throws Exception {
        Plan plan = lirePlanFourni("moyenPlan");
        ProgrammeLivraisons programme = lireProgrammeFourni(plan,
                "demandeMoyen5");
        List<Demande> demandes = programme.obtenirDemandes();
        Tournee tournee = plan.calculerTournee(programme);
        int dernier = tournee.arrets().size() - 1;
        int acceptes = 0;
        for (int rang = 1; rang < dernier - 1; rang++) {
            Noeud haut = tournee.arrets().get(rang).noeud();
            Noeud bas = tournee.arrets().get(rang + 1).noeud();
            boolean paire = false;
            for (Demande demande : demandes) {
                paire |= demande.collecte().equals(haut)
                        && demande.livraison().equals(bas);
            }
            if (paire) {
                int unRang = rang;
                assertThrows(IllegalArgumentException.class,
                        () -> plan.calculerTourneeOrdreChange(tournee,
                                demandes, unRang, 1));
                continue;
            }
            List<Noeud> adresses = new ArrayList<>(adressesDe(tournee));
            List<Integer> durees = new ArrayList<>(dureesDe(tournee));
            Collections.swap(adresses, rang, rang + 1);
            Collections.swap(durees, rang, rang + 1);
            Tournee descendue = plan.calculerTourneeOrdreChange(tournee,
                    demandes, rang, 1);
            assertEquals(plan.recalculerTournee(programme.obtenirHeureDepart(),
                    adresses, durees), descendue);
            assertEquals(tournee, plan.calculerTourneeOrdreChange(descendue,
                    demandes, rang + 1, -1));
            acceptes++;
        }
        assertTrue(acceptes > 0);
    }

    /**
     * La contrainte d'ordre est vérifiée avant le recalcul : un point de
     * collecte ne passe pas après son point de livraison, ni un point de
     * livraison avant son point de collecte.
     */
    @Test
    public void collecteApresLivraisonRefusee() throws Exception {
        Plan plan = PlansPourTests.petitReseau();
        ProgrammeLivraisons programme = programme(plan, LocalTime.of(8, 0), 3,
                2);
        List<Demande> demandes = programme.obtenirDemandes();
        Tournee tournee = plan.calculerTournee(programme);
        // Ordre : 1, 3 (collecte), 2 (livraison), 1.
        IllegalArgumentException descendre = assertThrows(
                IllegalArgumentException.class,
                () -> plan.calculerTourneeOrdreChange(tournee, demandes, 1,
                        1));
        assertTrue(descendre.getMessage().contains("Contrainte d'ordre"));
        IllegalArgumentException monter = assertThrows(
                IllegalArgumentException.class,
                () -> plan.calculerTourneeOrdreChange(tournee, demandes, 2,
                        -1));
        assertTrue(monter.getMessage().contains("Contrainte d'ordre"));
    }

    /** Le départ et le retour de l'entrepôt ne se déplacent pas. */
    @Test
    public void entrepotNonDeplacable() throws Exception {
        Plan plan = PlansPourTests.petitReseau();
        List<Demande> demandes = deuxDemandesDuPetitReseau(plan);
        Tournee tournee = tourneeDeDeuxDemandes(plan, LocalTime.of(8, 0));
        int dernier = tournee.arrets().size() - 1;
        for (int rang : new int[] {0, dernier}) {
            for (int decalage : new int[] {-1, 1}) {
                IllegalArgumentException erreur = assertThrows(
                        IllegalArgumentException.class,
                        () -> plan.calculerTourneeOrdreChange(tournee,
                                demandes, rang, decalage));
                assertTrue(erreur.getMessage().contains("entrepôt"));
            }
        }
    }

    /** Le premier arrêt ne monte pas, le dernier ne descend pas. */
    @Test
    public void premierArretNeMontePasEtDernierNeDescendPas()
            throws Exception {
        Plan plan = PlansPourTests.petitReseau();
        List<Demande> demandes = deuxDemandesDuPetitReseau(plan);
        Tournee tournee = tourneeDeDeuxDemandes(plan, LocalTime.of(8, 0));
        int dernierArret = tournee.arrets().size() - 2;
        IllegalArgumentException monter = assertThrows(
                IllegalArgumentException.class,
                () -> plan.calculerTourneeOrdreChange(tournee, demandes, 1,
                        -1));
        assertTrue(monter.getMessage().contains("premier"));
        IllegalArgumentException descendre = assertThrows(
                IllegalArgumentException.class,
                () -> plan.calculerTourneeOrdreChange(tournee, demandes,
                        dernierArret, 1));
        assertTrue(descendre.getMessage().contains("dernier"));
    }

    /**
     * Un ordre permis par la contrainte d'ordre mais qui rallonge la tournée
     * jusqu'après 13 h est refusé. Ici, descendre le premier arrêt ajoute
     * 800 m (environ 3 min 12 s) à une tournée qui rentre à 12:59.
     */
    @Test
    public void retourApres13hRefuseAuChangementDOrdre() throws Exception {
        Plan plan = PlansPourTests.petitReseau();
        List<Demande> demandes = deuxDemandesDuPetitReseau(plan);
        Tournee tournee = tourneeDeDeuxDemandes(plan, LocalTime.of(12, 51));
        assertFalse(tournee.obtenirHeureRetour().isAfter(
                Plan.HEURE_LIMITE_RETOUR));
        IllegalStateException erreur = assertThrows(
                IllegalStateException.class,
                () -> plan.calculerTourneeOrdreChange(tournee, demandes, 1,
                        1));
        assertTrue(erreur.getMessage().contains("13:00"));
    }
}
