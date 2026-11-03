package fr.insa.optimod.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import fr.insa.optimod.controller.MapScreen.Action;
import fr.insa.optimod.model.ProgrammeLivraisons;
import fr.insa.optimod.model.ProgrammeLivraisons.Demande;
import fr.insa.optimod.model.Tournee;
import fr.insa.optimod.model.Tournee.TypeArret;
import fr.insa.optimod.model.Noeud;
import fr.insa.optimod.model.Plan;

public class MainControllerTest {

    /** Écran factice qui enregistre ce que le contrôleur lui demande. */
    private static final class EcranFactice implements MapScreen {
        private Runnable actionImportPlan;
        private Runnable actionImportLivraison;
        private Runnable actionCalculer;
        private Runnable actionAjouter;
        private IntConsumer actionSupprimer;
        private Runnable actionAnnuler;
        private Runnable actionRefaire;
        private LongConsumer actionClicNoeud;
        private String consigne = "";
        private File fichierChoisi;
        private final List<String> evenements = new ArrayList<>();
        private Plan planAffiche;
        private ProgrammeLivraisons programmeAffiche;
        private Set<Action> actions;
        private boolean attente;
        private String erreurCalcul;

        @Override
        public void definirActionImportPlan(Runnable action) {
            actionImportPlan = action;
        }

        @Override
        public void definirActionImportProgramme(Runnable action) {
            actionImportLivraison = action;
        }

        @Override
        public void definirActionCalculer(Runnable action) {
            actionCalculer = action;
        }

        @Override
        public void activerActions(Set<Action> permises) {
            actions = Set.copyOf(permises);
        }

        @Override
        public void definirActionAjouterDemande(Runnable action) {
            actionAjouter = action;
        }

        @Override
        public void definirActionSupprimerDemande(IntConsumer action) {
            actionSupprimer = action;
        }

        @Override
        public void definirActionAnnuler(Runnable action) {
            actionAnnuler = action;
        }

        @Override
        public void definirActionRefaire(Runnable action) {
            actionRefaire = action;
        }

        @Override
        public void definirActionClicNoeud(LongConsumer action) {
            actionClicNoeud = action;
        }

        @Override
        public void afficherConsigne(String texte) {
            consigne = texte;
        }

        @Override
        public Optional<File> choisirFichierPlan() {
            return Optional.ofNullable(fichierChoisi);
        }

        @Override
        public Optional<File> choisirFichierDemandes() {
            return Optional.ofNullable(fichierChoisi);
        }

        @Override
        public void afficherPlan(Plan plan) {
            planAffiche = plan;
            evenements.add("plan");
        }

        @Override
        public void afficherProgramme(ProgrammeLivraisons programme) {
            programmeAffiche = programme;
            evenements.add("programme");
        }

        @Override
        public void afficherErreurXml(String detail) {
            evenements.add("invalide");
        }

        @Override
        public void reinitialiser() {
            planAffiche = null;
            programmeAffiche = null;
            evenements.add("reinitialiser");
        }

        @Override
        public void afficherAttente(boolean enAttente) {
            attente = enAttente;
            evenements.add("attente:" + enAttente);
        }

        @Override
        public void afficherErreurCalcul(String detail) {
            erreurCalcul = detail;
            evenements.add("erreur");
        }
    }

    @Rule
    public TemporaryFolder dossier = new TemporaryFolder();

    /** Plan d'un seul noeud, qui ne contient pas les adresses des demandes. */
    private static final String PLAN_MINUSCULE = "<reseau>"
            + "<noeud id=\"1\" latitude=\"45.75\" longitude=\"4.85\"/>"
            + "</reseau>";

    private static final Noeud ENTREPOT = new Noeud(1, 4.85, 45.75);

    /** Tournée qui part de l'entrepôt et y revient à cette heure. */
    private static Tournee tourneeRevenantA(LocalTime retour) {
        return new Tournee(List.of(
                new Tournee.Arret(ENTREPOT, LocalTime.of(8, 0),
                        LocalTime.of(8, 0), TypeArret.ENTREPOT, null),
                new Tournee.Arret(ENTREPOT, retour, retour,
                        TypeArret.ENTREPOT, null)), List.of());
    }

    private static final Tournee TOURNEE = tourneeRevenantA(
            LocalTime.of(8, 0));

    /** Tournées rendues par le calcul factice : un livreur. */
    private static final Map<Integer, Tournee> TOURNEES = Map.of(1, TOURNEE);

    /** Deux nœuds du petit plan fourni, pour ajouter une demande. */
    private static final long NOEUD_A = 25175791L;
    private static final long NOEUD_B = 2129259178L;

    /** Actions permises avec des demandes chargées, sans historique. */
    private static final Set<Action> MODIFIABLE = EnumSet.of(
            Action.IMPORTER_PLAN, Action.IMPORTER_PROGRAMME, Action.CALCULER,
            Action.AJOUTER_DEMANDE, Action.SUPPRIMER_DEMANDE);

    /** Actions permises juste après une suppression : on peut annuler. */
    private static final Set<Action> MODIFIABLE_APRES_SUPPRESSION =
            EnumSet.of(Action.IMPORTER_PLAN, Action.IMPORTER_PROGRAMME,
                    Action.CALCULER, Action.AJOUTER_DEMANDE,
                    Action.SUPPRIMER_DEMANDE, Action.ANNULER);

    private EcranFactice ecran;
    private MainController controleur;
    private final Queue<Runnable> arrierePlan = new ArrayDeque<>();
    private Plan planRecu;
    private ProgrammeLivraisons lotRecu;
    private RuntimeException erreurDeCalcul;
    private Map<Integer, Tournee> tourneesCalculees = TOURNEES;

    /** Nombre de calculs complets (voyageur de commerce) lancés. */
    private int nombreDeCalculs;

    @Before
    public void creer() {
        ecran = new EcranFactice();
        BiFunction<Plan, ProgrammeLivraisons, Map<Integer, Tournee>> calcul =
                (plan, programme) -> {
            planRecu = plan;
            lotRecu = programme;
            if (erreurDeCalcul != null) {
                throw erreurDeCalcul;
            }
            return tourneesCalculees;
        };
        controleur = new MainController(ecran, calcul, arrierePlan::add,
                Runnable::run);
    }

    private File ecrire(String contenu) throws Exception {
        File fichier = dossier.newFile();
        Files.writeString(fichier.toPath(), contenu);
        return fichier;
    }

    private File ressource(String nom) throws Exception {
        return new File(getClass().getResource(nom).toURI());
    }

    private void importerPlan(File fichier) {
        ecran.fichierChoisi = fichier;
        ecran.actionImportPlan.run();
    }

    private void importerProgramme(File fichier) {
        ecran.fichierChoisi = fichier;
        ecran.actionImportLivraison.run();
    }

    /** Charge petitPlan.xml puis demandePetit1.xml, et vide les événements. */
    private void chargerPlanEtLivraison() throws Exception {
        importerPlan(ressource("/data/petitPlan.xml"));
        importerProgramme(ressource("/data/demandePetit1.xml"));
        ecran.evenements.clear();
    }

    /** Charge petitPlan.xml puis demandePetit2.xml (2 demandes). */
    private void chargerDeuxDemandes() throws Exception {
        importerPlan(ressource("/data/petitPlan.xml"));
        importerProgramme(ressource("/data/demandePetit2.xml"));
        ecran.evenements.clear();
    }

    private List<Demande> demandes() {
        return controleur.obtenirProgramme().obtenirDemandes();
    }

    /** Tournées rangées dans le programme chargé. */
    private Map<Integer, Tournee> tournees() {
        return controleur.obtenirProgramme().obtenirTournees();
    }

    private void executerArrierePlan() {
        while (!arrierePlan.isEmpty()) {
            arrierePlan.remove().run();
        }
    }

    /**
     * Remplace le contrôleur par un contrôleur qui fait le vrai calcul de
     * {@link Plan#calculerTournees}, sur le thread appelant, en comptant les
     * calculs complets lancés (donc les passages par le voyageur de commerce).
     */
    private void utiliserLeVraiCalcul() {
        controleur = new MainController(ecran, (plan, programme) -> {
            nombreDeCalculs++;
            return plan.calculerTournees(programme);
        }, Runnable::run, Runnable::run);
    }

    /**
     * Comme {@link #utiliserLeVraiCalcul}, mais les calculs sont mis dans la
     * file {@code arrierePlan} : ils ne tournent que quand le test appelle
     * {@link #executerArrierePlan}, comme sur le thread de calcul.
     */
    private void utiliserLeVraiCalculEnArrierePlan() {
        controleur = new MainController(ecran, (plan, programme) -> {
            nombreDeCalculs++;
            return plan.calculerTournees(programme);
        }, arrierePlan::add, Runnable::run);
    }

    /**
     * Fichier de demandes dont deux demandes ont le même point de collecte et
     * la même durée sur place : avant que les arrêts connaissent leur demande,
     * on ne pouvait pas les distinguer dans la tournée.
     */
    private File demandesAmbigues() throws Exception {
        return ecrire("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<demandeDeLivraisons>"
                + "<entrepot adresse=\"2835339774\" heureDepart=\"8:0:0\"/>"
                + "<livraison adresseEnlevement=\"1679901320\" "
                + "adresseLivraison=\"208769457\" dureeEnlevement=\"420\" "
                + "dureeLivraison=\"600\"/>"
                + "<livraison adresseEnlevement=\"1679901320\" "
                + "adresseLivraison=\"25336179\" dureeEnlevement=\"420\" "
                + "dureeLivraison=\"480\"/>"
                + "</demandeDeLivraisons>");
    }

    /** Nœuds d'une tournée, dans l'ordre de passage. */
    private static List<Noeud> noeudsDe(Tournee tournee) {
        List<Noeud> noeuds = new ArrayList<>();
        for (Tournee.Arret arret : tournee.arrets()) {
            noeuds.add(arret.noeud());
        }
        return noeuds;
    }

    // --- État initial ---

    @Test
    public void brancheLesBoutons() {
        assertNotNull(ecran.actionImportPlan);
        assertNotNull(ecran.actionImportLivraison);
        assertNotNull(ecran.actionCalculer);
    }

    @Test
    public void auDepartSeulLImportDuPlanEstPossible() {
        assertTrue(controleur.obtenirEtat() instanceof EtatInitial);
        assertEquals(EnumSet.of(Action.IMPORTER_PLAN), ecran.actions);
        assertNull(controleur.obtenirPlan());
        assertTrue(ecran.evenements.isEmpty());
    }

    @Test
    public void programmeSansPlanIgnore() {
        ecran.actionImportLivraison.run();
        ecran.actionCalculer.run();
        assertNull(controleur.obtenirProgramme());
        assertTrue(ecran.evenements.isEmpty());
        assertTrue(arrierePlan.isEmpty());
    }

    // --- Import du plan ---

    @Test
    public void afficheLePlanImporte() throws Exception {
        importerPlan(ecrire(PLAN_MINUSCULE));
        assertEquals(List.of("plan"), ecran.evenements);
        assertEquals(1, ecran.planAffiche.obtenirNoeuds().size());
        assertSame(ecran.planAffiche, controleur.obtenirPlan());
        assertTrue(controleur.obtenirEtat() instanceof EtatPlanCharge);
        assertEquals(EnumSet.of(Action.IMPORTER_PLAN,
                Action.IMPORTER_PROGRAMME), ecran.actions);
    }

    @Test
    public void planInvalideOuvreLaFenetreEtVideLaPage() throws Exception {
        importerPlan(ecrire(PLAN_MINUSCULE));
        importerPlan(ecrire("<reseau>"));
        assertEquals(List.of("plan", "invalide", "reinitialiser"),
                ecran.evenements);
        assertNull(ecran.planAffiche);
        assertNull(controleur.obtenirPlan());
        assertTrue(controleur.obtenirEtat() instanceof EtatInitial);
    }

    @Test
    public void annulerLImportDuPlanNeChangeRien() throws Exception {
        importerPlan(ecrire(PLAN_MINUSCULE));
        importerPlan(null);
        assertEquals(List.of("plan"), ecran.evenements);
        assertNotNull(controleur.obtenirPlan());
        assertTrue(controleur.obtenirEtat() instanceof EtatPlanCharge);
    }

    @Test
    public void nouveauPlanOublieLaLivraison() throws Exception {
        chargerPlanEtLivraison();
        importerPlan(ecrire(PLAN_MINUSCULE));
        assertNull(controleur.obtenirProgramme());
        assertTrue(controleur.obtenirEtat() instanceof EtatPlanCharge);
        assertEquals(EnumSet.of(Action.IMPORTER_PLAN,
                Action.IMPORTER_PROGRAMME), ecran.actions);
    }

    @Test
    public void planInvalideOublieLaLivraison() throws Exception {
        chargerPlanEtLivraison();
        importerPlan(ecrire("<reseau>"));
        assertNull(controleur.obtenirProgramme());
        assertNull(controleur.obtenirPlan());
        assertTrue(controleur.obtenirEtat() instanceof EtatInitial);
    }

    // --- Import des demandes ---

    @Test
    public void importeLesDemandesEtActiveLeCalcul() throws Exception {
        importerPlan(ressource("/data/petitPlan.xml"));
        importerProgramme(ressource("/data/demandePetit1.xml"));
        assertEquals(List.of("plan", "programme"), ecran.evenements);
        assertEquals(1, controleur.obtenirProgramme().obtenirDemandes().size());
        assertEquals(LocalTime.of(8, 0),
                controleur.obtenirProgramme().obtenirHeureDepart());
        assertSame(controleur.obtenirProgramme(), ecran.programmeAffiche);
        assertTrue(controleur.obtenirEtat() instanceof EtatProgrammeCharge);
        assertEquals(MODIFIABLE, ecran.actions);
    }

    @Test
    public void programmeHorsPlanRefuse() throws Exception {
        importerPlan(ecrire(PLAN_MINUSCULE));
        importerProgramme(ressource("/data/demandePetit1.xml"));
        assertEquals(List.of("plan", "invalide"), ecran.evenements);
        assertNull(controleur.obtenirProgramme());
        assertNull(ecran.programmeAffiche);
        assertTrue(controleur.obtenirEtat() instanceof EtatPlanCharge);
    }

    @Test
    public void programmeInvalideConserveLePrecedent() throws Exception {
        chargerPlanEtLivraison();
        ProgrammeLivraisons precedente = controleur.obtenirProgramme();
        importerProgramme(ecrire(PLAN_MINUSCULE));
        assertEquals(List.of("invalide"), ecran.evenements);
        assertSame(precedente, controleur.obtenirProgramme());
        assertNotNull(controleur.obtenirPlan());
        assertTrue(controleur.obtenirEtat() instanceof EtatProgrammeCharge);
    }

    @Test
    public void annulerLImportDesDemandesLesConserve() throws Exception {
        chargerPlanEtLivraison();
        ProgrammeLivraisons precedente = controleur.obtenirProgramme();
        importerProgramme(null);
        assertSame(precedente, controleur.obtenirProgramme());
        assertTrue(ecran.evenements.isEmpty());
    }

    // --- Calcul de la tournée ---

    @Test
    public void calculerSansDemandesNeLanceRien() throws Exception {
        importerPlan(ecrire(PLAN_MINUSCULE));
        ecran.actionCalculer.run();
        assertTrue(arrierePlan.isEmpty());
        assertEquals(List.of("plan"), ecran.evenements);
    }

    @Test
    public void calculBloqueToutesLesActions() throws Exception {
        chargerPlanEtLivraison();
        ecran.actionCalculer.run();
        assertTrue(controleur.obtenirEtat() instanceof EtatCalculEnCours);
        assertTrue(ecran.actions.isEmpty());
        assertTrue(ecran.attente);
        assertTrue(tournees().isEmpty());
    }

    /** Le calcul range les tournées dans le programme, que la vue observe. */
    @Test
    public void calculRangeLesTourneesDansLeProgramme() throws Exception {
        chargerPlanEtLivraison();
        ecran.actionCalculer.run();
        executerArrierePlan();
        assertEquals(List.of("attente:true", "attente:false"),
                ecran.evenements);
        assertEquals(TOURNEES, tournees());
        assertSame(controleur.obtenirPlan(), planRecu);
        assertSame(controleur.obtenirProgramme(), lotRecu);
        assertTrue(controleur.obtenirEtat() instanceof EtatTourneeCalculee);
        assertEquals(MODIFIABLE, ecran.actions);
    }

    @Test
    public void unSecondClicPendantLeCalculEstIgnore() throws Exception {
        chargerPlanEtLivraison();
        ecran.actionCalculer.run();
        ecran.actionCalculer.run();
        assertEquals(1, arrierePlan.size());
    }

    @Test
    public void importsIgnoresPendantLeCalcul() throws Exception {
        chargerPlanEtLivraison();
        Plan plan = controleur.obtenirPlan();
        ProgrammeLivraisons programme = controleur.obtenirProgramme();
        ecran.actionCalculer.run();
        importerPlan(ecrire(PLAN_MINUSCULE));
        importerProgramme(ressource("/data/demandePetit1.xml"));
        assertSame(plan, controleur.obtenirPlan());
        assertSame(programme, controleur.obtenirProgramme());
        executerArrierePlan();
        assertSame(TOURNEE, tournees().get(1));
    }

    @Test
    public void erreurDeCalculAfficheLErreur() throws Exception {
        erreurDeCalcul = new IllegalStateException("Tournée impossible");
        chargerPlanEtLivraison();
        ecran.actionCalculer.run();
        executerArrierePlan();
        assertEquals(List.of("attente:true", "attente:false", "erreur"),
                ecran.evenements);
        assertEquals("Tournée impossible", ecran.erreurCalcul);
        assertTrue(tournees().isEmpty());
        assertTrue(controleur.obtenirEtat() instanceof EtatProgrammeCharge);
        assertEquals(MODIFIABLE, ecran.actions);
    }

    @Test
    public void erreurSansMessageAfficheLeTypeDeLErreur() throws Exception {
        erreurDeCalcul = new IllegalStateException();
        chargerPlanEtLivraison();
        ecran.actionCalculer.run();
        executerArrierePlan();
        assertEquals("IllegalStateException", ecran.erreurCalcul);
    }

    @Test
    public void recalculerApresUneTournee() throws Exception {
        chargerPlanEtLivraison();
        ecran.actionCalculer.run();
        executerArrierePlan();
        ecran.actionCalculer.run();
        assertTrue(controleur.obtenirEtat() instanceof EtatCalculEnCours);
        assertEquals(1, arrierePlan.size());
    }

    @Test
    public void controleurParDefautCalculeLaTourneeAvecLePlan()
            throws Exception {
        controleur = new MainController(ecran);
        chargerPlanEtLivraison();
        ecran.actionCalculer.run();
        assertNull(ecran.erreurCalcul);
        assertEquals(4, tournees().get(1).arrets().size());
        assertTrue(controleur.obtenirEtat() instanceof EtatTourneeCalculee);
    }

    // --- Modification des demandes (design pattern Command) ---

    @Test
    public void brancheLesBoutonsDeModification() {
        assertNotNull(ecran.actionAjouter);
        assertNotNull(ecran.actionSupprimer);
        assertNotNull(ecran.actionAnnuler);
        assertNotNull(ecran.actionRefaire);
        assertNotNull(ecran.actionClicNoeud);
    }

    @Test
    public void supprimerPuisAnnulerEtRetablir() throws Exception {
        chargerDeuxDemandes();
        List<Demande> avant = List.copyOf(demandes());
        ecran.actionSupprimer.accept(0);
        assertEquals(List.of(avant.get(1)), demandes());
        assertEquals(EnumSet.of(Action.IMPORTER_PLAN,
                Action.IMPORTER_PROGRAMME, Action.CALCULER,
                Action.AJOUTER_DEMANDE, Action.SUPPRIMER_DEMANDE,
                Action.ANNULER), ecran.actions);

        ecran.actionAnnuler.run();
        assertEquals(avant, demandes());
        assertTrue(ecran.actions.contains(Action.REFAIRE));
        assertFalse(ecran.actions.contains(Action.ANNULER));

        ecran.actionRefaire.run();
        assertEquals(List.of(avant.get(1)), demandes());
        assertTrue(controleur.obtenirEtat() instanceof EtatProgrammeCharge);
    }

    @Test
    public void supprimerUnePositionInvalideEstIgnore() throws Exception {
        chargerDeuxDemandes();
        ecran.actionSupprimer.accept(-1);
        ecran.actionSupprimer.accept(2);
        assertEquals(2, demandes().size());
        assertEquals(MODIFIABLE, ecran.actions);
    }

    @Test
    public void annulerSansHistoriqueEstIgnore() throws Exception {
        chargerDeuxDemandes();
        ecran.actionAnnuler.run();
        ecran.actionRefaire.run();
        assertEquals(2, demandes().size());
    }

    /**
     * La tournée factice ne contient pas les arrêts de la demande supprimée :
     * le recalcul local est impossible, les tournées sont retirées,
     * l'utilisateur en est informé par une consigne (pas de fenêtre d'erreur)
     * et Annuler les remet sans nouveau calcul.
     */
    @Test
    public void modifierRetireLesTourneesEtAnnulerLesRemet()
            throws Exception {
        chargerDeuxDemandes();
        ecran.actionCalculer.run();
        executerArrierePlan();

        ecran.actionSupprimer.accept(1);
        executerArrierePlan();
        assertTrue(controleur.obtenirEtat() instanceof EtatProgrammeCharge);
        assertTrue(tournees().isEmpty());
        assertEquals(MainController.CONSIGNE_ORDRE_NON_GARDE, ecran.consigne);
        assertNull(ecran.erreurCalcul);

        ecran.actionAnnuler.run();
        assertTrue(controleur.obtenirEtat() instanceof EtatTourneeCalculee);
        assertEquals(TOURNEES, tournees());
        assertEquals("", ecran.consigne);
        assertTrue("pas de nouveau calcul", arrierePlan.isEmpty());
    }

    @Test
    public void retablirRetrouveLaTourneeRecalculee() throws Exception {
        chargerDeuxDemandes();
        ecran.actionSupprimer.accept(0);
        ecran.actionCalculer.run();
        executerArrierePlan();
        ecran.actionAnnuler.run();
        assertTrue(controleur.obtenirEtat() instanceof EtatProgrammeCharge);
        ecran.actionRefaire.run();
        assertTrue(controleur.obtenirEtat() instanceof EtatTourneeCalculee);
        assertEquals(TOURNEES, tournees());
    }

    /**
     * Les tournées calculées après une annulation sont celles de l'état
     * d'avant la commande : annuler à nouveau après « Rétablir » les remet.
     */
    @Test
    public void annulerRetrouveLaTourneeCalculeeApresAnnulation()
            throws Exception {
        chargerDeuxDemandes();
        ecran.actionSupprimer.accept(0);
        ecran.actionAnnuler.run();
        ecran.actionCalculer.run();
        executerArrierePlan();
        ecran.actionRefaire.run();
        assertTrue(tournees().isEmpty());
        ecran.actionAnnuler.run();
        assertTrue(controleur.obtenirEtat() instanceof EtatTourneeCalculee);
        assertEquals(TOURNEES, tournees());
    }

    // --- Plusieurs livreurs (TIG-95) ---

    /**
     * Une tournée qui revient après 13 h est rangée et affichée comme les
     * autres : « Calculer » n'échoue pas à cause de 13 h, aucune fenêtre
     * d'erreur ne s'ouvre. Le repérage des arrêts en retard est affaire de la
     * vue ({@code Arret.estApresHeureLimite}).
     */
    @Test
    public void tourneeApres13hRangeeSansErreur() throws Exception {
        tourneesCalculees = Map.of(1, tourneeRevenantA(LocalTime.of(13, 42)));
        chargerPlanEtLivraison();
        ecran.actionCalculer.run();
        executerArrierePlan();
        assertEquals(tourneesCalculees, tournees());
        assertFalse(tournees().get(1).estPossible());
        assertTrue(controleur.obtenirEtat() instanceof EtatTourneeCalculee);
        assertNull(ecran.erreurCalcul);
        assertEquals(List.of("attente:true", "attente:false"),
                ecran.evenements);
    }

    /** Deux livreurs : les deux tournées calculées sont rangées. */
    @Test
    public void deuxLivreursDeuxTourneesRangees() throws Exception {
        chargerDeuxDemandes();
        controleur.obtenirProgramme().definirNombreLivreurs(2);
        tourneesCalculees = Map.of(1, TOURNEE,
                2, tourneeRevenantA(LocalTime.of(9, 0)));
        ecran.actionCalculer.run();
        executerArrierePlan();
        assertEquals(List.of(1, 2), List.copyOf(tournees().keySet()));
        assertEquals(tourneesCalculees, tournees());
        assertNull(ecran.erreurCalcul);
    }

    @Test
    public void modificationsBloqueesPendantLeCalcul() throws Exception {
        chargerDeuxDemandes();
        ecran.actionSupprimer.accept(0);
        ecran.actionCalculer.run();
        ecran.actionSupprimer.accept(0);
        ecran.actionAnnuler.run();
        ecran.actionAjouter.run();
        assertEquals(1, demandes().size());
        assertTrue(controleur.obtenirEtat() instanceof EtatCalculEnCours);
    }

    @Test
    public void nouvellesDemandesOublientLHistorique() throws Exception {
        chargerDeuxDemandes();
        ecran.actionSupprimer.accept(0);
        importerProgramme(ressource("/data/demandePetit2.xml"));
        assertEquals(MODIFIABLE, ecran.actions);
        ecran.actionAnnuler.run();
        assertEquals(2, demandes().size());
    }

    @Test
    public void ajouterUneDemandeEnCliquantDeuxNoeuds() throws Exception {
        chargerDeuxDemandes();
        ecran.actionAjouter.run();
        assertTrue(controleur.obtenirEtat() instanceof EtatAjoutCollecte);
        assertEquals(EtatAjoutCollecte.CONSIGNE, ecran.consigne);
        assertEquals(EnumSet.of(Action.AJOUTER_DEMANDE), ecran.actions);

        ecran.actionClicNoeud.accept(NOEUD_A);
        assertTrue(controleur.obtenirEtat() instanceof EtatAjoutLivraison);
        assertEquals(EtatAjoutLivraison.CONSIGNE, ecran.consigne);

        ecran.actionClicNoeud.accept(NOEUD_B);
        assertEquals(3, demandes().size());
        Demande ajoutee = demandes().get(2);
        assertEquals(NOEUD_A, ajoutee.collecte().identifiant());
        assertEquals(NOEUD_B, ajoutee.livraison().identifiant());
        assertEquals(EtatAjoutLivraison.DUREE_PAR_DEFAUT,
                ajoutee.dureeCollecte());
        assertEquals("", ecran.consigne);
        assertTrue(controleur.obtenirEtat() instanceof EtatProgrammeCharge);

        ecran.actionAnnuler.run();
        assertEquals(2, demandes().size());
    }

    @Test
    public void livraisonAuMemeEndroitQueLaCollecteRefusee() throws Exception {
        chargerDeuxDemandes();
        ecran.actionAjouter.run();
        ecran.actionClicNoeud.accept(NOEUD_A);
        ecran.actionClicNoeud.accept(NOEUD_A);
        assertEquals(2, demandes().size());
        assertTrue(controleur.obtenirEtat() instanceof EtatAjoutLivraison);
        assertTrue(ecran.consigne.startsWith("La livraison doit"));
    }

    @Test
    public void unSecondClicSurAjouterAnnuleLAjout() throws Exception {
        chargerDeuxDemandes();
        ecran.actionAjouter.run();
        ecran.actionClicNoeud.accept(NOEUD_A);
        ecran.actionAjouter.run();
        assertTrue(controleur.obtenirEtat() instanceof EtatProgrammeCharge);
        assertEquals("", ecran.consigne);
        assertEquals(2, demandes().size());
        assertEquals(MODIFIABLE, ecran.actions);
    }

    @Test
    public void importsIgnoresPendantLAjout() throws Exception {
        chargerDeuxDemandes();
        Plan plan = controleur.obtenirPlan();
        ecran.actionAjouter.run();
        importerPlan(ecrire(PLAN_MINUSCULE));
        importerProgramme(ressource("/data/demandePetit1.xml"));
        assertSame(plan, controleur.obtenirPlan());
        assertEquals(2, demandes().size());
    }

    @Test
    public void clicSurLaCarteHorsAjoutIgnore() throws Exception {
        chargerDeuxDemandes();
        ecran.actionClicNoeud.accept(NOEUD_A);
        assertEquals(2, demandes().size());
        assertTrue(controleur.obtenirEtat() instanceof EtatProgrammeCharge);
    }

    // --- Suppression d'une demande avec des tournées calculées (TIG-101) ---

    /**
     * Supprimer une demande quand les tournées sont calculées met la tournée du
     * livreur à jour tout de suite, sans relancer le voyageur de commerce, et
     * garde l'ordre des autres arrêts.
     */
    @Test
    public void supprimerAvecTourneesCalculeesNeRelancePasLeTsp()
            throws Exception {
        utiliserLeVraiCalcul();
        chargerDeuxDemandes();
        ecran.actionCalculer.run();
        Tournee complete = tournees().get(1);
        assertEquals(1, nombreDeCalculs);
        assertEquals(6, complete.arrets().size());
        Demande supprimee = demandes().get(0);
        ecran.evenements.clear();

        ecran.actionSupprimer.accept(0);

        assertEquals("pas de nouveau calcul complet", 1, nombreDeCalculs);
        assertTrue(controleur.obtenirEtat() instanceof EtatTourneeCalculee);
        assertEquals(List.of("attente:true", "attente:false"),
                ecran.evenements);
        assertEquals(MainController.CONSIGNE_ORDRE_GARDE, ecran.consigne);
        assertFalse("pas de numéro de livreur avec un seul livreur",
                ecran.consigne.contains("Livreur"));
        assertFalse(ecran.consigne.contains("Calculer"));
        List<Noeud> attendus = noeudsDe(complete);
        attendus.remove(supprimee.collecte());
        attendus.remove(supprimee.livraison());
        assertEquals(attendus, noeudsDe(tournees().get(1)));
        assertEquals(MODIFIABLE_APRES_SUPPRESSION, ecran.actions);
    }

    /**
     * Annuler remet la tournée complète et Rétablir la tournée recalculée, sans
     * aucun calcul : les commandes ont gardé les tournées d'avant et d'après.
     */
    @Test
    public void annulerEtRetablirApresSuppressionLocaleSansCalcul()
            throws Exception {
        utiliserLeVraiCalcul();
        chargerDeuxDemandes();
        ecran.actionCalculer.run();
        Tournee complete = tournees().get(1);
        ecran.actionSupprimer.accept(0);
        Tournee reduite = tournees().get(1);

        ecran.actionAnnuler.run();
        assertSame(complete, tournees().get(1));
        assertEquals(2, demandes().size());
        ecran.actionRefaire.run();
        assertSame(reduite, tournees().get(1));
        assertEquals(1, demandes().size());
        assertEquals(1, nombreDeCalculs);
        assertTrue(controleur.obtenirEtat() instanceof EtatTourneeCalculee);
    }

    /** Une position hors de la liste est ignorée, les tournées restent. */
    @Test
    public void supprimerUnePositionInvalideAvecTourneesCalculeesEstIgnore()
            throws Exception {
        utiliserLeVraiCalcul();
        chargerDeuxDemandes();
        ecran.actionCalculer.run();
        Tournee complete = tournees().get(1);
        ecran.actionSupprimer.accept(-1);
        ecran.actionSupprimer.accept(2);
        assertEquals(2, demandes().size());
        assertSame(complete, tournees().get(1));
        assertTrue(controleur.obtenirEtat() instanceof EtatTourneeCalculee);
        assertEquals(1, nombreDeCalculs);
    }

    /**
     * Le recalcul local garde l'ordre affiché : il ne reprend jamais un ordre
     * du voyageur de commerce trouvé plus tôt pour les mêmes demandes. Ici le
     * voyageur de commerce a déjà calculé les demandes restantes, puis on
     * annule ; supprimer à nouveau recalcule quand même en local.
     */
    @Test
    public void suppressionNeReprendPasUnOrdreDuTspPlusAncien()
            throws Exception {
        utiliserLeVraiCalcul();
        chargerDeuxDemandes();
        ecran.actionCalculer.run();
        ecran.actionSupprimer.accept(0);
        ecran.actionCalculer.run(); // calcul complet des demandes restantes
        assertEquals(2, nombreDeCalculs);

        ecran.actionAnnuler.run();
        ecran.actionSupprimer.accept(0);

        assertEquals(2, nombreDeCalculs);
        assertEquals(MainController.CONSIGNE_ORDRE_GARDE, ecran.consigne);
        assertTrue(controleur.obtenirEtat() instanceof EtatTourneeCalculee);
    }

    /**
     * Le recalcul local tourne dans la file de calcul, pas sur le thread
     * appelant : tant qu'elle n'est pas exécutée, l'attente est affichée, les
     * actions sont bloquées et ni les demandes ni les tournées ne changent.
     */
    @Test
    public void suppressionRecalculeHorsDuThreadDeLInterface()
            throws Exception {
        utiliserLeVraiCalculEnArrierePlan();
        chargerDeuxDemandes();
        ecran.actionCalculer.run();
        executerArrierePlan();
        Tournee complete = tournees().get(1);
        ecran.evenements.clear();

        ecran.actionSupprimer.accept(0);

        assertEquals(1, arrierePlan.size());
        assertTrue(ecran.attente);
        assertTrue(controleur.obtenirEtat() instanceof EtatCalculEnCours);
        assertTrue(ecran.actions.isEmpty());
        assertEquals(2, demandes().size());
        assertSame(complete, tournees().get(1));

        executerArrierePlan();

        assertFalse(ecran.attente);
        assertEquals(1, demandes().size());
        assertTrue(controleur.obtenirEtat() instanceof EtatTourneeCalculee);
        assertEquals(complete.arrets().size() - 2,
                tournees().get(1).arrets().size());
        assertEquals(1, nombreDeCalculs);
        assertEquals(MODIFIABLE_APRES_SUPPRESSION, ecran.actions);
    }

    /**
     * Supprimer toutes les demandes une à une garde l'ordre à chaque fois (la
     * tournée se réduit jusqu'au seul départ) sans nouveau calcul complet, et
     * Annuler revient pas à pas aux tournées d'avant.
     */
    @Test
    public void suppressionsSuccessivesGardentLOrdre() throws Exception {
        utiliserLeVraiCalcul();
        chargerDeuxDemandes();
        ecran.actionCalculer.run();
        Tournee complete = tournees().get(1);
        ecran.actionSupprimer.accept(0);
        Tournee uneDemande = tournees().get(1);
        assertEquals(4, uneDemande.arrets().size());
        ecran.actionSupprimer.accept(0);
        assertEquals(1, tournees().get(1).arrets().size());
        assertTrue(demandes().isEmpty());
        assertEquals(1, nombreDeCalculs);

        ecran.actionAnnuler.run();
        assertSame(uneDemande, tournees().get(1));
        ecran.actionAnnuler.run();
        assertSame(complete, tournees().get(1));
        assertEquals(1, nombreDeCalculs);
    }

    /**
     * Deux demandes de même point de collecte et de même durée ne se
     * confondent plus : chaque arrêt connaît sa demande, le recalcul local
     * retire les bons arrêts et garde ceux de l'autre demande.
     */
    @Test
    public void suppressionDeDemandesDeMemePointDeCollecteGardeLAutre()
            throws Exception {
        utiliserLeVraiCalcul();
        importerPlan(ressource("/data/petitPlan.xml"));
        importerProgramme(demandesAmbigues());
        ecran.actionCalculer.run();
        Tournee complete = tournees().get(1);
        Demande gardee = demandes().get(1);

        ecran.actionSupprimer.accept(0);

        assertEquals(List.of(gardee), demandes());
        assertTrue(controleur.obtenirEtat() instanceof EtatTourneeCalculee);
        assertNull(ecran.erreurCalcul);
        assertEquals(MainController.CONSIGNE_ORDRE_GARDE, ecran.consigne);
        for (Tournee.Arret arret : tournees().get(1).arrets()) {
            assertTrue(arret.demande() == null
                    || arret.demande().equals(gardee));
        }
        assertEquals(complete.arrets().size() - 2,
                tournees().get(1).arrets().size());
        assertEquals(1, nombreDeCalculs);

        ecran.actionAnnuler.run();
        assertSame(complete, tournees().get(1));
        assertEquals("", ecran.consigne);
    }

    /**
     * Avec deux livreurs, seule la tournée du livreur de la demande supprimée
     * est recalculée : l'autre est la même, et Annuler remet les deux.
     */
    @Test
    public void suppressionNeRecalculeQueLeLivreurDeLaDemande()
            throws Exception {
        utiliserLeVraiCalcul();
        chargerDeuxDemandes();
        ProgrammeLivraisons programme = controleur.obtenirProgramme();
        programme.definirNombreLivreurs(2);
        programme.remplacerDemande(1, demandes().get(1).avecLivreur(2));
        ecran.actionCalculer.run();
        Tournee premierLivreur = tournees().get(1);
        Tournee secondLivreur = tournees().get(2);
        assertEquals(4, premierLivreur.arrets().size());
        assertEquals(4, secondLivreur.arrets().size());

        ecran.actionSupprimer.accept(1); // demande du livreur 2

        assertEquals(1, demandes().size());
        assertSame(premierLivreur, tournees().get(1));
        assertEquals(1, tournees().get(2).arrets().size());
        assertEquals(List.of(1, 2), List.copyOf(tournees().keySet()));
        assertEquals(1, nombreDeCalculs);
        assertEquals(MainController.CONSIGNE_ORDRE_GARDE_LIVREUR.formatted(2),
                ecran.consigne);
        assertTrue(ecran.consigne.startsWith("Livreur 2 :"));
        assertFalse(ecran.consigne.contains("Calculer"));

        ecran.actionAnnuler.run();
        assertSame(premierLivreur, tournees().get(1));
        assertSame(secondLivreur, tournees().get(2));
        assertEquals(2, demandes().size());
    }

    /**
     * La consigne « ordre gardé » ne doit pas rester affichée une fois les
     * tournées recalculées par le voyageur de commerce.
     */
    @Test
    public void calculerEffaceLaConsigneDuRecalculLocal() throws Exception {
        utiliserLeVraiCalcul();
        chargerDeuxDemandes();
        ecran.actionCalculer.run();
        ecran.actionSupprimer.accept(0);
        assertEquals(MainController.CONSIGNE_ORDRE_GARDE, ecran.consigne);

        ecran.actionCalculer.run();

        assertEquals("", ecran.consigne);
        assertEquals(2, nombreDeCalculs);
    }
}
