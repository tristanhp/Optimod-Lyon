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
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.xml.sax.SAXException;

import fr.insa.optimod.model.ProgrammeLivraisons.Demande;
import fr.insa.optimod.model.Tournee.TypeArret;

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

    /** Tournée du livreur 1 (programme d'un seul livreur). */
    private static Tournee premiereTournee(Plan plan,
            ProgrammeLivraisons programme) {
        Map<Integer, Tournee> tournees = plan.calculerTournees(programme);
        assertEquals(programme.obtenirNombreLivreurs(), tournees.size());
        return tournees.get(1);
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
        Tournee tournee = premiereTournee(plan,
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
        Tournee tournee = premiereTournee(plan,
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
        Tournee tournee = premiereTournee(plan, new ProgrammeLivraisons(
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
                IllegalStateException.class, () -> premiereTournee(plan,
                        programme(plan, LocalTime.of(8, 0), 4, 2)));
        assertTrue(erreur.getMessage().contains("inaccessible"));
    }

    /**
     * Le retour à l'entrepôt doit avoir lieu avant 13 h (TIG-46). Une tournée
     * qui revient trop tard est renvoyée mais signalée impossible (TIG-95),
     * pour que le cas 6 puisse la montrer.
     */
    @Test
    public void retourApres13hSignale() throws Exception {
        Plan plan = PlansPourTests.petitReseau();
        // La tournée dure 5 min 12 s.
        Tournee aLHeure = premiereTournee(plan,
                programme(plan, LocalTime.of(12, 54), 3, 2));
        assertEquals(LocalTime.of(12, 59, 12), aLHeure.obtenirHeureRetour());
        assertTrue(aLHeure.estPossible());
        Tournee enRetard = premiereTournee(plan,
                programme(plan, LocalTime.of(12, 58), 3, 2));
        assertEquals(LocalTime.of(13, 3, 12), enRetard.obtenirHeureRetour());
        assertFalse(enRetard.estPossible());
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
            Tournee tournee = premiereTournee(plan, programme);
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

    // --- Plusieurs livreurs (TIG-95) ---

    /** Chaque arrêt connaît son type et sa demande. */
    @Test
    public void arretsPortentTypeEtDemande() throws Exception {
        Plan plan = PlansPourTests.petitReseau();
        ProgrammeLivraisons programme = programme(plan, LocalTime.of(8, 0),
                3, 2);
        Demande demande = programme.obtenirDemandes().get(0);
        List<Tournee.Arret> arrets = premiereTournee(plan, programme).arrets();
        List<TypeArret> types = new ArrayList<>();
        for (Tournee.Arret arret : arrets) {
            types.add(arret.type());
        }
        assertEquals(List.of(TypeArret.ENTREPOT, TypeArret.COLLECTE,
                TypeArret.LIVRAISON, TypeArret.ENTREPOT), types);
        assertNull(arrets.get(0).demande());
        assertSame(demande, arrets.get(1).demande());
        assertSame(demande, arrets.get(2).demande());
        assertNull(arrets.get(3).demande());
    }

    /**
     * Deux livreurs : deux tournées indépendantes, chacune avec ses seules
     * demandes, partant toutes deux de l'entrepôt à l'heure de départ.
     */
    @Test
    public void deuxLivreursDeuxTourneesIndependantes() throws Exception {
        Plan plan = PlansPourTests.petitReseau();
        Demande premiere = new Demande(plan.obtenirNoeud(3),
                plan.obtenirNoeud(2), 60, 120, 1);
        Demande seconde = new Demande(plan.obtenirNoeud(2),
                plan.obtenirNoeud(3), 60, 120, 2);
        ProgrammeLivraisons programme = new ProgrammeLivraisons(
                plan.obtenirNoeud(1), LocalTime.of(8, 0), List.of());
        programme.definirNombreLivreurs(2);
        programme.ajouterDemande(0, premiere);
        programme.ajouterDemande(1, seconde);

        Map<Integer, Tournee> tournees = plan.calculerTournees(programme);
        assertEquals(List.of(1, 2), new ArrayList<>(tournees.keySet()));
        for (int livreur = 1; livreur <= 2; livreur++) {
            List<Tournee.Arret> arrets = tournees.get(livreur).arrets();
            assertEquals(4, arrets.size());
            assertEquals(LocalTime.of(8, 0), arrets.get(0).depart());
            Demande attendue = livreur == 1 ? premiere : seconde;
            assertSame(attendue, arrets.get(1).demande());
            assertSame(attendue, arrets.get(2).demande());
            assertTrue(tournees.get(livreur).estPossible());
        }
        // La tournée du livreur 1 est celle qu'il aurait seul.
        Tournee seul = premiereTournee(plan, programme(plan,
                LocalTime.of(8, 0), 3, 2));
        assertEquals(seul.obtenirHeureRetour(),
                tournees.get(1).obtenirHeureRetour());
    }

    /** Un livreur sans demande a une tournée vide. */
    @Test
    public void livreurSansDemandeATourneeVide() throws Exception {
        Plan plan = PlansPourTests.petitReseau();
        ProgrammeLivraisons programme = programme(plan, LocalTime.of(8, 0),
                3, 2);
        programme.definirNombreLivreurs(2);
        Tournee vide = plan.calculerTournees(programme).get(2);
        assertEquals(1, vide.arrets().size());
        assertEquals(TypeArret.ENTREPOT, vide.arrets().get(0).type());
        assertTrue(vide.chemins().isEmpty());
    }

    /**
     * Fichier fourni partagé entre deux livreurs : chaque demande est servie
     * une fois, par son livreur, collecte avant livraison.
     */
    @Test
    public void fichierFourniPartageEntreDeuxLivreurs() throws Exception {
        Plan plan = lirePlanFourni("moyenPlan");
        ProgrammeLivraisons lu;
        try (InputStream xml = getClass().getResourceAsStream(
                "/data/demandeMoyen5.xml")) {
            lu = new ProgrammeLivraisons(xml, plan);
        }
        List<Demande> reparties = new ArrayList<>();
        for (int rang = 0; rang < lu.obtenirDemandes().size(); rang++) {
            reparties.add(lu.obtenirDemandes().get(rang)
                    .avecLivreur(rang % 2 + 1));
        }
        ProgrammeLivraisons programme = new ProgrammeLivraisons(
                lu.obtenirEntrepot(), lu.obtenirHeureDepart(), List.of());
        programme.definirNombreLivreurs(2);
        for (Demande demande : reparties) {
            programme.ajouterDemande(
                    programme.obtenirDemandes().size(), demande);
        }

        Map<Integer, Tournee> tournees = plan.calculerTournees(programme);
        int arretsDeDemandes = 0;
        for (Map.Entry<Integer, Tournee> entree : tournees.entrySet()) {
            List<Tournee.Arret> arrets = entree.getValue().arrets();
            assertSame(programme.obtenirEntrepot(), arrets.get(0).noeud());
            for (Tournee.Arret arret : arrets) {
                if (arret.demande() != null) {
                    assertEquals((int) entree.getKey(),
                            arret.demande().livreur());
                    arretsDeDemandes++;
                }
            }
            for (Demande demande : reparties) {
                if (demande.livreur() == entree.getKey()) {
                    assertTrue(rangDe(arrets, demande, TypeArret.COLLECTE)
                            < rangDe(arrets, demande, TypeArret.LIVRAISON));
                }
            }
        }
        assertEquals(2 * reparties.size(), arretsDeDemandes);
    }

    /** Rang de l'arrêt d'une demande, d'un type donné. */
    private static int rangDe(List<Tournee.Arret> arrets, Demande demande,
            TypeArret type) {
        for (int rang = 0; rang < arrets.size(); rang++) {
            if (arrets.get(rang).demande() == demande
                    && arrets.get(rang).type() == type) {
                return rang;
            }
        }
        throw new AssertionError("Arrêt absent : " + type);
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
        Tournee tournee = tourneeImposee(plan, LocalTime.of(8, 0),
                adresses, List.of(0, 120, 240, 0));
        assertEquals(adresses,
                tournee.arrets().stream().map(Tournee.Arret::noeud).toList());
        assertEquals(LocalTime.of(8, 1), tournee.arrets().get(1).arrivee());
        assertEquals(LocalTime.of(8, 3), tournee.arrets().get(1).depart());
        assertEquals(3, tournee.chemins().size());
    }

    /** Le recalcul renvoie une tournée en retard, que l'appelant refuse. */
    @Test
    public void recalculApres13hImpossible() throws Exception {
        Plan plan = PlansPourTests.petitReseau();
        Tournee tournee = tourneeImposee(plan, LocalTime.of(12, 59),
                List.of(plan.obtenirNoeud(1), plan.obtenirNoeud(2),
                        plan.obtenirNoeud(3), plan.obtenirNoeud(1)),
                List.of(0, 300, 300, 0));
        assertFalse(tournee.estPossible());
    }

    /**
     * Recalcule une tournée dans l'ordre imposé : les arrêts du milieu vont
     * par paires (collecte puis livraison d'une même demande).
     */
    private static Tournee tourneeImposee(Plan plan, LocalTime heureDepart,
            List<Noeud> adresses, List<Integer> durees) {
        List<TypeArret> types = new ArrayList<>();
        List<Demande> demandes = new ArrayList<>();
        for (int rang = 0; rang < adresses.size(); rang++) {
            boolean extremite = rang == 0 || rang == adresses.size() - 1;
            boolean collecte = rang % 2 == 1;
            types.add(extremite ? TypeArret.ENTREPOT
                    : collecte ? TypeArret.COLLECTE
                            : TypeArret.LIVRAISON);
            if (extremite) {
                demandes.add(null);
            } else {
                int debut = collecte ? rang : rang - 1;
                demandes.add(new Demande(adresses.get(debut),
                        adresses.get(debut + 1), durees.get(debut),
                        durees.get(debut + 1)));
            }
        }
        return plan.recalculerTournee(heureDepart, adresses, durees, types,
                demandes);
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
        Tournee avant = tourneeImposee(plan, LocalTime.of(8, 0),
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
        Tournee avant = tourneeImposee(plan, LocalTime.of(8, 0),
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
        Tournee avant = tourneeImposee(plan, LocalTime.of(8, 0),
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
        Tournee avant = tourneeImposee(plan, LocalTime.of(12, 59),
                List.of(entrepot), List.of(0));
        plan.insererDemande(avant, new Demande(plan.obtenirNoeud(3),
                plan.obtenirNoeud(2), 60, 60), 1);
    }

    /** Une durée d'une journée ne doit pas être ramenée à zéro à minuit. */
    @Test(expected = IllegalStateException.class)
    public void dureeJourneeEntiereRefusee() throws Exception {
        Plan plan = PlansPourTests.petitReseau();
        Tournee avant = tourneeImposee(plan, LocalTime.of(8, 0),
                List.of(plan.obtenirNoeud(1)), List.of(0));
        plan.insererDemande(avant, new Demande(plan.obtenirNoeud(3),
                plan.obtenirNoeud(2), 24 * 3600, 60), 1);
    }

    /** Une adresse inaccessible ne peut être insérée. */
    @Test(expected = IllegalStateException.class)
    public void insertionInaccessible() throws Exception {
        Plan plan = PlansPourTests.petitReseau();
        Tournee avant = tourneeImposee(plan, LocalTime.of(8, 0),
                List.of(plan.obtenirNoeud(1)), List.of(0));
        plan.insererDemande(avant, new Demande(plan.obtenirNoeud(4),
                plan.obtenirNoeud(2), 60, 60), 1);
    }



    // --- Mémoire de l'ordre de passage du voyageur de commerce ---

    /**
     * Calculer deux fois les mêmes demandes garde un seul ordre en mémoire et
     * redonne la même tournée.
     */
    @Test
    public void ordreDePassageMemorise() throws Exception {
        Plan plan = PlansPourTests.petitReseau();
        ProgrammeLivraisons programme = programme(plan, LocalTime.of(8, 0), 3,
                2);
        assertEquals(0, plan.nombreOrdresMemorises());
        Tournee premiere = premiereTournee(plan, programme);
        assertEquals(1, plan.nombreOrdresMemorises());
        assertEquals(premiere, premiereTournee(plan, programme));
        assertEquals(1, plan.nombreOrdresMemorises());
    }

    /**
     * L'heure de départ ne change pas l'ordre : l'ordre mémorisé sert, et les
     * heures de passage sont refaites à partir de la nouvelle heure.
     */
    @Test
    public void ordreMemoriseServiPourUneAutreHeureDeDepart()
            throws Exception {
        Plan plan = PlansPourTests.petitReseau();
        premiereTournee(plan, programme(plan, LocalTime.of(8, 0), 3, 2));
        Tournee tardive = premiereTournee(plan,
                programme(plan, LocalTime.of(9, 0), 3, 2));
        assertEquals(1, plan.nombreOrdresMemorises());
        assertArret(tardive.arrets().get(0), 1, "09:00", "09:00");
        assertArret(tardive.arrets().get(3), 1, "09:05:12", "09:05:12");
    }

    /** D'autres demandes donnent un autre ordre, mémorisé à côté du premier. */
    @Test
    public void autresDemandesAutreOrdreMemorise() throws Exception {
        Plan plan = PlansPourTests.petitReseau();
        premiereTournee(plan, programme(plan, LocalTime.of(8, 0), 3, 2));
        Tournee autre = premiereTournee(plan,
                programme(plan, LocalTime.of(8, 0), 2, 3));
        assertEquals(2, plan.nombreOrdresMemorises());
        List<Long> ordre = new ArrayList<>();
        for (Tournee.Arret arret : autre.arrets()) {
            ordre.add(arret.noeud().identifiant());
        }
        assertEquals(List.of(1L, 2L, 3L, 1L), ordre);
    }

    /**
     * Un calcul qui échoue ne laisse rien en mémoire : le voyageur de commerce
     * renvoie un ordre même par une adresse inaccessible, et l'erreur n'est
     * levée qu'au calcul des heures de passage.
     */
    @Test
    public void calculEnEchecNeMemorisePas() throws Exception {
        Plan plan = PlansPourTests.petitReseau();
        assertThrows(IllegalStateException.class, () -> premiereTournee(plan,
                programme(plan, LocalTime.of(8, 0), 4, 2)));
        assertEquals(0, plan.nombreOrdresMemorises());
    }

    /**
     * Une tournée qui dépasse minuit est refusée : les heures sont des
     * {@code LocalTime}, elle reviendrait sinon « tôt le lendemain » et
     * paraîtrait à l'heure.
     */
    @Test
    public void tourneeQuiDepasseMinuitRefusee() throws Exception {
        Plan plan = PlansPourTests.petitReseau();
        ProgrammeLivraisons programme = new ProgrammeLivraisons(
                plan.obtenirNoeud(1), LocalTime.of(8, 0),
                List.of(new Demande(plan.obtenirNoeud(3),
                        plan.obtenirNoeud(2), 24 * 3600, 60)));
        IllegalStateException erreur = assertThrows(
                IllegalStateException.class,
                () -> plan.calculerTournees(programme));
        assertTrue(erreur.getMessage().contains("minuit"));
        assertEquals(0, plan.nombreOrdresMemorises());
    }

    /**
     * Le recalcul local ne lit ni n'écrit la mémoire : il garde l'ordre
     * affiché, même si le voyageur de commerce en a trouvé un autre.
     */
    @Test
    public void recalculLocalIgnoreLaMemoire() throws Exception {
        Plan plan = PlansPourTests.petitReseau();
        Tournee actuelle = tourneeAvecUneDemandeASupprimer(plan,
                LocalTime.of(8, 0));
        plan.calculerTourneeSansDemande(actuelle, demandeASupprimer(plan));
        assertEquals(0, plan.nombreOrdresMemorises());
    }

    // --- Recalcul local d'une tournée, sans relancer le TSP (TIG-101) ---

    /**
     * Arrêt à fabriquer pour une tournée écrite à la main : seuls l'adresse, la
     * durée sur place (en secondes), le type et la demande comptent, les
     * heures sont calculées par {@link #tourneeDansLOrdre}.
     */
    private record ArretPrevu(Noeud noeud, int duree, TypeArret type,
            Demande demande) {

        /** Reprend l'adresse, la durée sur place, le type et la demande. */
        ArretPrevu(Tournee.Arret arret) {
            this(arret.noeud(), (int) arret.arrivee().until(arret.depart(),
                    ChronoUnit.SECONDS), arret.type(), arret.demande());
        }
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
     * Tournée attendue pour un ordre de passage donné, calculée ici sans
     * passer par le code testé : A* entre arrêts consécutifs
     * ({@code plusCourtChemin}), trajets à 15 km/h arrondis à la seconde, durée
     * sur place ajoutée à chaque arrêt. Sert aussi à fabriquer une tournée
     * « actuelle » dans un ordre choisi, sans passer par le voyageur de
     * commerce.
     */
    private static Tournee tourneeDansLOrdre(Plan plan, LocalTime heureDepart,
            List<ArretPrevu> prevus) {
        List<Tournee.Arret> arrets = new ArrayList<>();
        List<Chemin> chemins = new ArrayList<>();
        LocalTime heure = heureDepart;
        for (int rang = 0; rang < prevus.size(); rang++) {
            ArretPrevu prevu = prevus.get(rang);
            if (rang > 0) {
                Chemin chemin = plan.plusCourtChemin(
                        prevus.get(rang - 1).noeud(), prevu.noeud());
                chemins.add(chemin);
                heure = heure.plusSeconds(Math.round(chemin.longueur()
                        / (Plan.VITESSE_KMH * 1000 / 3600)));
            }
            LocalTime arrivee = heure;
            heure = arrivee.plusSeconds(prevu.duree());
            arrets.add(new Tournee.Arret(prevu.noeud(), arrivee, heure,
                    prevu.type(), prevu.demande()));
        }
        return new Tournee(arrets, chemins);
    }

    /** Les arrêts d'une tournée, sauf ceux d'une demande. */
    private static List<ArretPrevu> arretsSans(Tournee tournee,
            Demande demande) {
        List<ArretPrevu> prevus = new ArrayList<>();
        for (Tournee.Arret arret : tournee.arrets()) {
            if (!demande.equals(arret.demande())) {
                prevus.add(new ArretPrevu(arret));
            }
        }
        return prevus;
    }

    /**
     * Tournée du petit réseau dans l'ordre 1, 3 (collecte de la demande
     * retirée), 2 (collecte de l'autre demande), 1 (livraison de la demande
     * retirée), 3 (livraison de l'autre demande), 1, départ à l'heure donnée.
     * Les durées sur place sont 0, 30, 60, 45, 120 et 0 s.
     */
    private static Tournee tourneeAvecUneDemandeASupprimer(Plan plan,
            LocalTime heureDepart) {
        Noeud entrepot = plan.obtenirNoeud(1);
        Demande retiree = demandeASupprimer(plan);
        Demande autre = new Demande(plan.obtenirNoeud(2),
                plan.obtenirNoeud(3), 60, 120);
        return tourneeDansLOrdre(plan, heureDepart, List.of(
                new ArretPrevu(entrepot, 0, TypeArret.ENTREPOT, null),
                new ArretPrevu(plan.obtenirNoeud(3), 30, TypeArret.COLLECTE,
                        retiree),
                new ArretPrevu(plan.obtenirNoeud(2), 60, TypeArret.COLLECTE,
                        autre),
                new ArretPrevu(entrepot, 45, TypeArret.LIVRAISON, retiree),
                new ArretPrevu(plan.obtenirNoeud(3), 120, TypeArret.LIVRAISON,
                        autre),
                new ArretPrevu(entrepot, 0, TypeArret.ENTREPOT, null)));
    }

    /** Demande retirée par {@link #tourneeAvecUneDemandeASupprimer}. */
    private static Demande demandeASupprimer(Plan plan) {
        return new Demande(plan.obtenirNoeud(3), plan.obtenirNoeud(1), 30, 45);
    }

    /**
     * L'ordre des autres arrêts est suivi même s'il est plus long que le
     * meilleur : après la suppression, la tournée est 1, 2, 3, 1, qui passe par
     * le détour 2 vers 1 vers 3 (400 m = 96 s) et revient par 449,6 m (108 s),
     * alors que 1, 3, 2, 1 revient à 08:05:12. Heures calculées à la main à
     * 15 km/h : 249,6 m = 59,9 s arrondi à 60 s.
     */
    @Test
    public void ordreGardeRespecteMemeSiPlusLong() throws Exception {
        Plan plan = PlansPourTests.petitReseau();
        Tournee actuelle = tourneeAvecUneDemandeASupprimer(plan,
                LocalTime.of(8, 0));
        Tournee tournee = plan.calculerTourneeSansDemande(actuelle,
                demandeASupprimer(plan));
        assertEquals(4, tournee.arrets().size());
        assertArret(tournee.arrets().get(0), 1, "08:00", "08:00");
        assertArret(tournee.arrets().get(1), 2, "08:01", "08:02");
        assertArret(tournee.arrets().get(2), 3, "08:03:36", "08:05:36");
        assertArret(tournee.arrets().get(3), 1, "08:07:24", "08:07:24");
        assertEquals(3, tournee.chemins().size());
        verifierContigu(tournee.chemins().get(1), plan.obtenirNoeud(2),
                plan.obtenirNoeud(3));
    }

    /** Les arrêts qui restent gardent leur type et leur demande. */
    @Test
    public void suppressionGardeTypeEtDemandeDesArretsRestants()
            throws Exception {
        Plan plan = PlansPourTests.petitReseau();
        Demande autre = new Demande(plan.obtenirNoeud(2),
                plan.obtenirNoeud(3), 60, 120);
        Tournee tournee = plan.calculerTourneeSansDemande(
                tourneeAvecUneDemandeASupprimer(plan, LocalTime.of(8, 0)),
                demandeASupprimer(plan));
        List<TypeArret> types = new ArrayList<>();
        for (Tournee.Arret arret : tournee.arrets()) {
            types.add(arret.type());
        }
        assertEquals(List.of(TypeArret.ENTREPOT, TypeArret.COLLECTE,
                TypeArret.LIVRAISON, TypeArret.ENTREPOT), types);
        assertNull(tournee.arrets().get(0).demande());
        assertEquals(autre, tournee.arrets().get(1).demande());
        assertEquals(autre, tournee.arrets().get(2).demande());
        assertNull(tournee.arrets().get(3).demande());
    }

    /**
     * Seuls les trajets des jonctions créées sont calculés par A* : ici 1 vers
     * 2 et 2 vers 3 sont nouveaux, et le trajet 3 vers 1 (dernier) est repris
     * tel quel de la tournée actuelle (même objet).
     */
    @Test
    public void suppressionReprendLesTrajetsInchanges() throws Exception {
        Plan plan = PlansPourTests.petitReseau();
        Tournee actuelle = tourneeAvecUneDemandeASupprimer(plan,
                LocalTime.of(8, 0));
        Tournee tournee = plan.calculerTourneeSansDemande(actuelle,
                demandeASupprimer(plan));
        assertSame(actuelle.chemins().get(4), tournee.chemins().get(2));
        assertEquals(plan.plusCourtChemin(plan.obtenirNoeud(1),
                plan.obtenirNoeud(2)), tournee.chemins().get(0));
        assertEquals(plan.plusCourtChemin(plan.obtenirNoeud(2),
                plan.obtenirNoeud(3)), tournee.chemins().get(1));
    }

    /**
     * Comme le calcul complet, le recalcul local renvoie la tournée même si le
     * retour à l'entrepôt est après 13 h : elle est signalée impossible. La
     * tournée qui reste dure 7 min 24 s : retour à 13:00:00 possible, une
     * seconde plus tard impossible.
     */
    @Test
    public void suppressionRetourApres13hSignale() throws Exception {
        Plan plan = PlansPourTests.petitReseau();
        Demande demande = demandeASupprimer(plan);
        Tournee limite = plan.calculerTourneeSansDemande(
                tourneeAvecUneDemandeASupprimer(plan, LocalTime.of(12, 52, 36)),
                demande);
        assertEquals(LocalTime.of(13, 0), limite.obtenirHeureRetour());
        assertTrue(limite.estPossible());
        Tournee trop = plan.calculerTourneeSansDemande(
                tourneeAvecUneDemandeASupprimer(plan, LocalTime.of(12, 52, 37)),
                demande);
        assertEquals(LocalTime.of(13, 0, 1), trop.obtenirHeureRetour());
        assertFalse(trop.estPossible());
    }

    /**
     * Une jonction vers une adresse inaccessible rend la suppression
     * impossible. La tournée actuelle est écrite à la main (le nœud 4 n'est pas
     * atteignable) : 1, 3 (collecte), 4 (collecte d'une autre demande), 2
     * (livraison), 1 (livraison de l'autre demande), 1. Une fois la demande
     * retirée, il faudrait aller de 1 à 4.
     */
    @Test
    public void suppressionVersUneAdresseInaccessibleRefusee()
            throws Exception {
        Plan plan = PlansPourTests.petitReseau();
        Demande demande = new Demande(plan.obtenirNoeud(3),
                plan.obtenirNoeud(2), 60, 15);
        Demande autre = new Demande(plan.obtenirNoeud(4),
                plan.obtenirNoeud(1), 0, 0);
        Noeud entrepot = plan.obtenirNoeud(1);
        List<Tournee.Arret> arrets = new ArrayList<>();
        LocalTime heure = LocalTime.of(8, 0);
        arrets.add(new Tournee.Arret(entrepot, heure, heure,
                TypeArret.ENTREPOT, null));
        arrets.add(new Tournee.Arret(plan.obtenirNoeud(3), heure,
                heure.plusSeconds(60), TypeArret.COLLECTE, demande));
        arrets.add(new Tournee.Arret(plan.obtenirNoeud(4), heure, heure,
                TypeArret.COLLECTE, autre));
        arrets.add(new Tournee.Arret(plan.obtenirNoeud(2), heure,
                heure.plusSeconds(15), TypeArret.LIVRAISON, demande));
        arrets.add(new Tournee.Arret(entrepot, heure, heure,
                TypeArret.LIVRAISON, autre));
        arrets.add(new Tournee.Arret(entrepot, heure, heure,
                TypeArret.ENTREPOT, null));
        Tournee actuelle = new Tournee(arrets,
                Collections.nCopies(5, Chemin.INACCESSIBLE));
        IllegalStateException erreur = assertThrows(
                IllegalStateException.class,
                () -> plan.calculerTourneeSansDemande(actuelle, demande));
        assertTrue(erreur.getMessage().contains("inaccessible"));
    }

    /**
     * Supprimer une demande retire ses deux arrêts et garde l'ordre de tous les
     * autres ; la tournée est celle d'un calcul indépendant sur ce nouvel
     * ordre, et le retour à l'entrepôt n'est jamais plus tard qu'avant. Au plus
     * deux trajets sont nouveaux (une jonction par arrêt retiré) : tous les
     * autres sont repris de la tournée actuelle (même objet).
     */
    @Test
    public void suppressionGardeLOrdreDesAutresArrets() throws Exception {
        Plan plan = lirePlanFourni("moyenPlan");
        ProgrammeLivraisons programme = lireProgrammeFourni(plan,
                "demandeMoyen5");
        Tournee tournee = premiereTournee(plan, programme);
        Set<Chemin> anciens = Collections.newSetFromMap(
                new IdentityHashMap<>());
        anciens.addAll(tournee.chemins());
        for (Demande demande : programme.obtenirDemandes()) {
            List<ArretPrevu> restants = arretsSans(tournee, demande);
            Tournee sans = plan.calculerTourneeSansDemande(tournee, demande);
            assertEquals(tournee.arrets().size() - 2, sans.arrets().size());
            assertEquals(tourneeDansLOrdre(plan, programme.obtenirHeureDepart(),
                    restants), sans);
            assertFalse(sans.obtenirHeureRetour().isAfter(
                    tournee.obtenirHeureRetour()));
            long nouveaux = sans.chemins().stream()
                    .filter(chemin -> !anciens.contains(chemin)).count();
            assertTrue("trajets calculés : " + nouveaux, nouveaux <= 2);
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
                premiereTournee(plan, programme),
                programme.obtenirDemandes().get(0));
        assertEquals(premiereTournee(plan, new ProgrammeLivraisons(
                plan.obtenirNoeud(1), LocalTime.of(8, 0), List.of())), sans);
    }

    /** Une demande dont les arrêts ne sont pas dans la tournée est refusée. */
    @Test
    public void suppressionDUneDemandeAbsenteRefusee() throws Exception {
        Plan plan = PlansPourTests.petitReseau();
        Tournee tournee = premiereTournee(plan,
                programme(plan, LocalTime.of(8, 0), 3, 2));
        Demande autre = new Demande(plan.obtenirNoeud(2),
                plan.obtenirNoeud(3), 60, 120);
        IllegalArgumentException erreur = assertThrows(
                IllegalArgumentException.class,
                () -> plan.calculerTourneeSansDemande(tournee, autre));
        assertTrue(erreur.getMessage().contains("introuvable"));
    }

    /**
     * Deux demandes qui ont le même point de collecte et la même durée sur
     * place ne se confondent pas, puisque chaque arrêt connaît sa demande :
     * retirer la première laisse exactement les arrêts de la seconde.
     */
    @Test
    public void suppressionAvecLeMemePointDeCollecteGardeLAutreDemande()
            throws Exception {
        Plan plan = PlansPourTests.petitReseau();
        Demande premiere = new Demande(plan.obtenirNoeud(3),
                plan.obtenirNoeud(2), 60, 120);
        Demande seconde = new Demande(plan.obtenirNoeud(3),
                plan.obtenirNoeud(1), 60, 120);
        Tournee tournee = premiereTournee(plan, new ProgrammeLivraisons(
                plan.obtenirNoeud(1), LocalTime.of(8, 0),
                List.of(premiere, seconde)));
        Tournee sans = plan.calculerTourneeSansDemande(tournee, premiere);
        List<Demande> demandes = new ArrayList<>();
        for (Tournee.Arret arret : sans.arrets()) {
            if (arret.demande() != null) {
                demandes.add(arret.demande());
            }
        }
        assertEquals(List.of(seconde, seconde), demandes);
    }

    /**
     * Deux demandes identiques : retirer l'une garde l'autre entière, avec sa
     * collecte avant sa livraison.
     */
    @Test
    public void suppressionDeDemandesIdentiquesGardeUneDemandeValide()
            throws Exception {
        Plan plan = PlansPourTests.petitReseau();
        Demande demande = new Demande(plan.obtenirNoeud(3),
                plan.obtenirNoeud(2), 60, 120);
        Tournee tournee = premiereTournee(plan, new ProgrammeLivraisons(
                plan.obtenirNoeud(1), LocalTime.of(8, 0),
                List.of(demande, demande)));
        Tournee sans = plan.calculerTourneeSansDemande(tournee, demande);
        List<TypeArret> types = new ArrayList<>();
        for (Tournee.Arret arret : sans.arrets()) {
            types.add(arret.type());
        }
        assertEquals(List.of(TypeArret.ENTREPOT, TypeArret.COLLECTE,
                TypeArret.LIVRAISON, TypeArret.ENTREPOT), types);
    }

    // --- Changer l'ordre des arrêts (TIG-44) ---

    /** Les arrêts d'une tournée, modifiables, pour en changer l'ordre. */
    private static List<ArretPrevu> prevusDe(Tournee tournee) {
        List<ArretPrevu> prevus = new ArrayList<>();
        for (Tournee.Arret arret : tournee.arrets()) {
            prevus.add(new ArretPrevu(arret));
        }
        return prevus;
    }

    /**
     * Deux demandes du petit réseau dont les arrêts se distinguent par leur
     * durée sur place : la première collecte en 3 (60 s) et livre en 2 (120 s),
     * la seconde collecte en 2 (30 s) et livre en 3 (45 s).
     */
    private static List<Demande> deuxDemandesDuPetitReseau(Plan plan) {
        return List.of(new Demande(plan.obtenirNoeud(3),
                plan.obtenirNoeud(2), 60, 120),
                new Demande(plan.obtenirNoeud(2), plan.obtenirNoeud(3), 30,
                        45));
    }

    /**
     * Tournée de {@link #deuxDemandesDuPetitReseau} partant à l'heure dite, dans
     * l'ordre 1, 3 (60), 2 (30), 2 (120), 3 (45), 1.
     */
    private static Tournee tourneeDeDeuxDemandes(Plan plan, LocalTime depart) {
        Noeud entrepot = plan.obtenirNoeud(1);
        Demande premiere = deuxDemandesDuPetitReseau(plan).get(0);
        Demande seconde = deuxDemandesDuPetitReseau(plan).get(1);
        return tourneeDansLOrdre(plan, depart, List.of(
                new ArretPrevu(entrepot, 0, TypeArret.ENTREPOT, null),
                new ArretPrevu(plan.obtenirNoeud(3), 60, TypeArret.COLLECTE,
                        premiere),
                new ArretPrevu(plan.obtenirNoeud(2), 30, TypeArret.COLLECTE,
                        seconde),
                new ArretPrevu(plan.obtenirNoeud(2), 120, TypeArret.LIVRAISON,
                        premiere),
                new ArretPrevu(plan.obtenirNoeud(3), 45, TypeArret.LIVRAISON,
                        seconde),
                new ArretPrevu(entrepot, 0, TypeArret.ENTREPOT, null)));
    }

    /**
     * Descendre un arrêt l'échange avec le suivant : les autres arrêts gardent
     * leur ordre, et les heures sont celles d'un recalcul sur ce nouvel ordre.
     */
    @Test
    public void descendreUnArretEchangeAvecLeSuivant() throws Exception {
        Plan plan = PlansPourTests.petitReseau();
        Tournee tournee = tourneeDeDeuxDemandes(plan, LocalTime.of(8, 0));
        Tournee apres = plan.calculerTourneeOrdreChange(tournee, 1, 1);
        // Ordre attendu : 1, 2 (30), 3 (60), 2 (120), 3 (45), 1.
        List<ArretPrevu> attendus = prevusDe(tournee);
        Collections.swap(attendus, 1, 2);
        assertEquals(tourneeDansLOrdre(plan, LocalTime.of(8, 0), attendus),
                apres);
        assertEquals(30, new ArretPrevu(apres.arrets().get(1)).duree());
    }

    /** Les arrêts déplacés gardent leur type et leur demande. */
    @Test
    public void echangeGardeTypeEtDemandeDesArrets() throws Exception {
        Plan plan = PlansPourTests.petitReseau();
        Demande seconde = deuxDemandesDuPetitReseau(plan).get(1);
        Tournee apres = plan.calculerTourneeOrdreChange(
                tourneeDeDeuxDemandes(plan, LocalTime.of(8, 0)), 1, 1);
        assertEquals(TypeArret.COLLECTE, apres.arrets().get(1).type());
        assertEquals(seconde, apres.arrets().get(1).demande());
    }

    /** Monter l'arrêt qu'on vient de descendre redonne la tournée de départ. */
    @Test
    public void monterApresDescendreRedonneLaTournee() throws Exception {
        Plan plan = PlansPourTests.petitReseau();
        Tournee tournee = tourneeDeDeuxDemandes(plan, LocalTime.of(8, 0));
        Tournee apres = plan.calculerTourneeOrdreChange(tournee, 1, 1);
        assertEquals(tournee, plan.calculerTourneeOrdreChange(apres, 2, -1));
    }

    /**
     * Sur une tournée calculée par le voyageur de commerce, chaque échange est
     * soit accepté et égal à un calcul indépendant sur le nouvel ordre, soit
     * refusé parce qu'il placerait un point de livraison avant son point de
     * collecte. Un échange accepté ne calcule par A* que les trois trajets
     * autour des deux arrêts échangés : tous les autres sont repris de la
     * tournée actuelle (même objet).
     */
    @Test
    public void echangesSurUneTourneeCalculee() throws Exception {
        Plan plan = lirePlanFourni("moyenPlan");
        ProgrammeLivraisons programme = lireProgrammeFourni(plan,
                "demandeMoyen5");
        Tournee tournee = premiereTournee(plan, programme);
        int dernier = tournee.arrets().size() - 1;
        Set<Chemin> anciens = Collections.newSetFromMap(
                new IdentityHashMap<>());
        anciens.addAll(tournee.chemins());
        int acceptes = 0;
        for (int rang = 1; rang < dernier - 1; rang++) {
            Tournee.Arret haut = tournee.arrets().get(rang);
            Tournee.Arret bas = tournee.arrets().get(rang + 1);
            boolean paire = haut.type() == TypeArret.COLLECTE
                    && bas.type() == TypeArret.LIVRAISON
                    && haut.demande().equals(bas.demande());
            if (paire) {
                int unRang = rang;
                assertThrows(IllegalArgumentException.class,
                        () -> plan.calculerTourneeOrdreChange(tournee,
                                unRang, 1));
                continue;
            }
            List<ArretPrevu> attendus = prevusDe(tournee);
            Collections.swap(attendus, rang, rang + 1);
            Tournee descendue = plan.calculerTourneeOrdreChange(tournee, rang,
                    1);
            assertEquals(tourneeDansLOrdre(plan, programme.obtenirHeureDepart(),
                    attendus), descendue);
            long nouveaux = descendue.chemins().stream()
                    .filter(chemin -> !anciens.contains(chemin)).count();
            assertEquals(3, nouveaux);
            assertEquals(tournee, plan.calculerTourneeOrdreChange(descendue,
                    rang + 1, -1));
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
        Tournee tournee = premiereTournee(plan,
                programme(plan, LocalTime.of(8, 0), 3, 2));
        // Ordre : 1, 3 (collecte), 2 (livraison), 1.
        IllegalArgumentException descendre = assertThrows(
                IllegalArgumentException.class,
                () -> plan.calculerTourneeOrdreChange(tournee, 1, 1));
        assertTrue(descendre.getMessage().contains("Contrainte d'ordre"));
        IllegalArgumentException monter = assertThrows(
                IllegalArgumentException.class,
                () -> plan.calculerTourneeOrdreChange(tournee, 2, -1));
        assertTrue(monter.getMessage().contains("Contrainte d'ordre"));
    }

    /**
     * Deux demandes de même point de collecte et de même durée ne se confondent
     * pas : chaque arrêt connaît sa demande, donc échanger deux collectes de
     * demandes différentes est permis, même au même nœud.
     */
    @Test
    public void echangeDeDeuxCollectesDeMemeNoeud() throws Exception {
        Plan plan = PlansPourTests.petitReseau();
        Noeud entrepot = plan.obtenirNoeud(1);
        Demande premiere = new Demande(plan.obtenirNoeud(3),
                plan.obtenirNoeud(2), 60, 120);
        Demande seconde = new Demande(plan.obtenirNoeud(3), entrepot, 60,
                120);
        // Ordre : 1, 3 (collecte de la première), 3 (collecte de la seconde),
        // 2 (livraison de la première), 1 (livraison de la seconde), 1.
        Tournee tournee = tourneeDansLOrdre(plan, LocalTime.of(8, 0), List.of(
                new ArretPrevu(entrepot, 0, TypeArret.ENTREPOT, null),
                new ArretPrevu(plan.obtenirNoeud(3), 60, TypeArret.COLLECTE,
                        premiere),
                new ArretPrevu(plan.obtenirNoeud(3), 60, TypeArret.COLLECTE,
                        seconde),
                new ArretPrevu(plan.obtenirNoeud(2), 120, TypeArret.LIVRAISON,
                        premiere),
                new ArretPrevu(entrepot, 120, TypeArret.LIVRAISON, seconde),
                new ArretPrevu(entrepot, 0, TypeArret.ENTREPOT, null)));

        Tournee apres = plan.calculerTourneeOrdreChange(tournee, 1, 1);

        assertEquals(seconde, apres.arrets().get(1).demande());
        assertEquals(premiere, apres.arrets().get(2).demande());
        assertEquals(TypeArret.COLLECTE, apres.arrets().get(1).type());
        assertEquals(TypeArret.COLLECTE, apres.arrets().get(2).type());
    }

    /** Le départ et le retour de l'entrepôt ne se déplacent pas. */
    @Test
    public void entrepotNonDeplacable() throws Exception {
        Plan plan = PlansPourTests.petitReseau();
        Tournee tournee = tourneeDeDeuxDemandes(plan, LocalTime.of(8, 0));
        int dernier = tournee.arrets().size() - 1;
        for (int rang : new int[] {0, dernier}) {
            for (int decalage : new int[] {-1, 1}) {
                IllegalArgumentException erreur = assertThrows(
                        IllegalArgumentException.class,
                        () -> plan.calculerTourneeOrdreChange(tournee, rang,
                                decalage));
                assertTrue(erreur.getMessage().contains("entrepôt"));
            }
        }
    }

    /** Le premier arrêt ne monte pas, le dernier ne descend pas. */
    @Test
    public void premierArretNeMontePasEtDernierNeDescendPas()
            throws Exception {
        Plan plan = PlansPourTests.petitReseau();
        Tournee tournee = tourneeDeDeuxDemandes(plan, LocalTime.of(8, 0));
        int dernierArret = tournee.arrets().size() - 2;
        IllegalArgumentException monter = assertThrows(
                IllegalArgumentException.class,
                () -> plan.calculerTourneeOrdreChange(tournee, 1, -1));
        assertTrue(monter.getMessage().contains("premier"));
        IllegalArgumentException descendre = assertThrows(
                IllegalArgumentException.class,
                () -> plan.calculerTourneeOrdreChange(tournee, dernierArret,
                        1));
        assertTrue(descendre.getMessage().contains("dernier"));
    }

    /**
     * Un ordre permis par la contrainte d'ordre mais qui rallonge la tournée
     * jusqu'après 13 h est renvoyé, signalé impossible : comme pour le calcul
     * complet, refuser le changement est affaire du contrôleur. Ici, descendre
     * le premier arrêt ajoute 800 m (environ 3 min 12 s) à une tournée qui
     * rentre à 12:59.
     */
    @Test
    public void changementDOrdreApres13hSignale() throws Exception {
        Plan plan = PlansPourTests.petitReseau();
        Tournee tournee = tourneeDeDeuxDemandes(plan, LocalTime.of(12, 51));
        assertTrue(tournee.estPossible());
        Tournee apres = plan.calculerTourneeOrdreChange(tournee, 1, 1);
        assertTrue(apres.obtenirHeureRetour().isAfter(
                Plan.HEURE_LIMITE_RETOUR));
        assertFalse(apres.estPossible());
    }

    /**
     * Déplacer un point garde l'ordre, les types et les durées sur place : la
     * tournée est celle d'un recalcul sur les nouvelles adresses, les deux
     * arrêts de la demande portent la demande déplacée, et seuls les deux
     * trajets autour de l'arrêt déplacé sont calculés, les autres sont repris
     * de la tournée actuelle (même objet).
     */
    @Test
    public void deplacerUnPointRecalculeLesDeuxTrajetsAutourDeLArret()
            throws Exception {
        Plan plan = lirePlanFourni("moyenPlan");
        ProgrammeLivraisons programme = lireProgrammeFourni(plan,
                "demandeMoyen5");
        Tournee tournee = premiereTournee(plan, programme);
        List<Noeud> adresses = tournee.arrets().stream()
                .map(Tournee.Arret::noeud).toList();
        Noeud nouveau = plan.obtenirNoeuds().stream()
                .filter(noeud -> !adresses.contains(noeud)).findFirst()
                .orElseThrow();
        Set<Chemin> anciens = Collections.newSetFromMap(
                new IdentityHashMap<>());
        anciens.addAll(tournee.chemins());
        for (int rang = 1; rang < adresses.size() - 1; rang++) {
            Tournee deplacee = plan.calculerTourneeAvecPointDeplace(tournee,
                    rang, nouveau);
            Tournee.Arret arret = deplacee.arrets().get(rang);
            Demande demande = arret.demande();
            assertEquals(nouveau, arret.type() == TypeArret.COLLECTE
                    ? demande.collecte() : demande.livraison());
            List<ArretPrevu> attendus = new ArrayList<>();
            for (int i = 0; i < adresses.size(); i++) {
                Tournee.Arret courant = tournee.arrets().get(i);
                ArretPrevu prevu = new ArretPrevu(courant);
                boolean memeDemande = courant.demande() != null
                        && courant.demande().equals(
                                tournee.arrets().get(rang).demande());
                attendus.add(new ArretPrevu(
                        i == rang ? nouveau : prevu.noeud(), prevu.duree(),
                        prevu.type(),
                        memeDemande ? demande : prevu.demande()));
            }
            assertEquals(tourneeDansLOrdre(plan, programme.obtenirHeureDepart(),
                    attendus), deplacee);
            assertEquals(2, deplacee.chemins().stream()
                    .filter(chemin -> !anciens.contains(chemin)).count());
        }
    }

    /**
     * L'entrepôt ne se déplace pas, et deux demandes identiques ne permettent
     * pas d'identifier le point à déplacer.
     */
    @Test
    public void deplacerUnPointRefuseEntrepotOuDemandesIdentiques()
            throws Exception {
        Plan plan = PlansPourTests.petitReseau();
        Tournee tournee = tourneeDeDeuxDemandes(plan, LocalTime.of(8, 0));
        int dernier = tournee.arrets().size() - 1;
        assertThrows(IllegalArgumentException.class,
                () -> plan.calculerTourneeAvecPointDeplace(tournee, 0,
                        plan.obtenirNoeud(2)));
        assertThrows(IllegalArgumentException.class,
                () -> plan.calculerTourneeAvecPointDeplace(tournee, dernier,
                        plan.obtenirNoeud(2)));
        Noeud entrepot = plan.obtenirNoeud(1);
        Demande identique = new Demande(plan.obtenirNoeud(3),
                plan.obtenirNoeud(2), 60, 120);
        Tournee jumelles = tourneeDansLOrdre(plan, LocalTime.of(8, 0), List.of(
                new ArretPrevu(entrepot, 0, TypeArret.ENTREPOT, null),
                new ArretPrevu(plan.obtenirNoeud(3), 60, TypeArret.COLLECTE,
                        identique),
                new ArretPrevu(plan.obtenirNoeud(3), 60, TypeArret.COLLECTE,
                        identique),
                new ArretPrevu(plan.obtenirNoeud(2), 120, TypeArret.LIVRAISON,
                        identique),
                new ArretPrevu(plan.obtenirNoeud(2), 120, TypeArret.LIVRAISON,
                        identique),
                new ArretPrevu(entrepot, 0, TypeArret.ENTREPOT, null)));
        assertThrows(IllegalArgumentException.class,
                () -> plan.calculerTourneeAvecPointDeplace(jumelles, 1,
                        entrepot));
    }

    /**
     * Un déplacement qui fait rentrer après 13 h est renvoyé comme les autres
     * tournées impossibles : c'est à l'appelant de le refuser.
     */
    @Test
    public void deplacerUnPointPeutRendreLaTourneeImpossible()
            throws Exception {
        Plan plan = PlansPourTests.petitReseau();
        Tournee tournee = tourneeDeDeuxDemandes(plan, LocalTime.of(12, 59, 59));
        Tournee deplacee = plan.calculerTourneeAvecPointDeplace(tournee, 1,
                plan.obtenirNoeud(2));
        assertFalse(deplacee.estPossible());
    }
}
