package fr.insa.optimod.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiConsumer;
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
        private Runnable actionSauvegarder;
        private Runnable actionRestaurer;
        private Runnable actionAjouter;
        private IntConsumer actionSupprimer;
        private BiConsumer<Integer, Integer> actionMonter;
        private BiConsumer<Integer, Integer> actionDescendre;
        private BiConsumer<Integer, Integer> actionDeplacer;
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
        private String refusOrdre;
        private String refusDeplacement;
        private IntConsumer actionChangerLivreur;
        private Optional<Integer> livreurChoisi = Optional.of(2);
        private int nombreDeChoixDeLivreur;
        private int livreurActuelPropose;
        private int nombreDeLivreursProposes;
        private String refusChangementLivreur;
        private int livreurSelectionne = -1;
        private int arretSelectionne = -1;
        private Optional<SaisieDemande> saisie = Optional.of(
                new SaisieDemande("5", "5", 1));
        private List<Tournee.Arret> horsPlage = List.of();
        private Optional<Integer> positionInsertion = Optional.of(1);
        private Noeud collecteProvisoire;
        private Noeud livraisonProvisoire;
        private Runnable pendantSaisie = () -> { };

        @Override
        public void afficherPointsProvisoires(Noeud collecte, Noeud livraison) {
            collecteProvisoire = collecte;
            livraisonProvisoire = livraison;
        }

        @Override
        public Optional<Integer> choisirPositionInsertion(Tournee tournee) {
            return positionInsertion;
        }

        @Override
        public Optional<SaisieDemande> saisirDemande() {
            pendantSaisie.run();
            return saisie;
        }

        @Override
        public void signalerArretsHorsPlage(List<Tournee.Arret> arrets) {
            horsPlage = List.copyOf(arrets);
        }

        @Override
        public void definirActionSauvegarder(Runnable action) {
            actionSauvegarder = action;
        }

        @Override
        public void definirActionRestaurer(Runnable action) {
            actionRestaurer = action;
        }

        @Override
        public Optional<File> choisirFichierSauvegarde() {
            return Optional.ofNullable(fichierChoisi);
        }

        @Override
        public Optional<File> choisirFichierRestauration() {
            return Optional.ofNullable(fichierChoisi);
        }

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
        public void definirActionMonterArret(
                BiConsumer<Integer, Integer> action) {
            actionMonter = action;
        }

        @Override
        public void definirActionDescendreArret(
                BiConsumer<Integer, Integer> action) {
            actionDescendre = action;
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

        @Override
        public void selectionnerArretTournee(int livreur, int rang) {
            livreurSelectionne = livreur;
            arretSelectionne = rang;
            evenements.add("selection:" + livreur + ":" + rang);
        }

        @Override
        public void definirActionDeplacerPoint(
                BiConsumer<Integer, Integer> action) {
            actionDeplacer = action;
        }

        @Override
        public void afficherRefusDeplacement(String detail) {
            refusDeplacement = detail;
            evenements.add("refus deplacement");
        }

        @Override
        public void afficherRefusOrdre(String detail) {
            refusOrdre = detail;
            evenements.add("refus");
        }

        @Override
        public void definirActionChangerLivreur(IntConsumer action) {
            actionChangerLivreur = action;
        }

        @Override
        public Optional<Integer> choisirLivreur(int livreurActuel,
                int nombreDeLivreurs) {
            nombreDeChoixDeLivreur++;
            livreurActuelPropose = livreurActuel;
            nombreDeLivreursProposes = nombreDeLivreurs;
            return livreurChoisi;
        }

        @Override
        public void afficherRefusChangementLivreur(String detail) {
            refusChangementLivreur = detail;
            evenements.add("refus livreur");
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
            Action.AJOUTER_DEMANDE, Action.SUPPRIMER_DEMANDE, Action.RESTAURER);

    /** Actions permises avec la tournée affichée : on peut la réordonner. */
    private static final Set<Action> MODIFIABLE_AVEC_TOURNEE = EnumSet.of(
            Action.IMPORTER_PLAN, Action.IMPORTER_PROGRAMME, Action.CALCULER,
            Action.AJOUTER_DEMANDE, Action.SUPPRIMER_DEMANDE,
            Action.CHANGER_ORDRE, Action.DEPLACER_POINT, Action.RESTAURER,
            Action.SAUVEGARDER);

    /**
     * Actions permises juste après une suppression avec tournée affichée : on
     * peut annuler.
     */
    private static final Set<Action> MODIFIABLE_APRES_SUPPRESSION =
            EnumSet.of(Action.IMPORTER_PLAN, Action.IMPORTER_PROGRAMME,
                    Action.CALCULER, Action.AJOUTER_DEMANDE,
                    Action.SUPPRIMER_DEMANDE, Action.CHANGER_ORDRE,
                    Action.DEPLACER_POINT, Action.RESTAURER,
                    Action.SAUVEGARDER, Action.ANNULER);

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
        assertEquals(EnumSet.of(Action.IMPORTER_PLAN, Action.RESTAURER),
                ecran.actions);
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
                Action.IMPORTER_PROGRAMME, Action.RESTAURER), ecran.actions);
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
                Action.IMPORTER_PROGRAMME, Action.RESTAURER), ecran.actions);
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
        assertEquals(MODIFIABLE_AVEC_TOURNEE, ecran.actions);
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
                Action.ANNULER, Action.RESTAURER), ecran.actions);

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
        assertEquals(EnumSet.of(Action.AJOUTER_DEMANDE, Action.ANNULER),
                ecran.actions);

        ecran.actionClicNoeud.accept(NOEUD_A);
        assertTrue(controleur.obtenirEtat() instanceof EtatAjoutLivraison);
        assertEquals(EtatAjoutLivraison.CONSIGNE, ecran.consigne);

        ecran.actionClicNoeud.accept(NOEUD_B);
        assertEquals(3, demandes().size());
        Demande ajoutee = demandes().get(2);
        assertEquals(NOEUD_A, ajoutee.collecte().identifiant());
        assertEquals(NOEUD_B, ajoutee.livraison().identifiant());
        assertEquals(300,
                ajoutee.dureeCollecte());
        assertEquals("", ecran.consigne);
        assertTrue(controleur.obtenirEtat() instanceof EtatProgrammeCharge);

        ecran.actionAnnuler.run();
        assertEquals(2, demandes().size());
    }

    /** Le premier clic affiche la collecte sans modifier le programme. */
    @Test
    public void collecteVisibleDesLePremierClic() throws Exception {
        chargerDeuxDemandes();
        ecran.actionAjouter.run();
        ecran.actionClicNoeud.accept(NOEUD_A);
        assertEquals(NOEUD_A, ecran.collecteProvisoire.identifiant());
        assertNull(ecran.livraisonProvisoire);
        assertEquals(2, demandes().size());
        assertFalse(controleur.peutAnnuler());
        assertTrue(ecran.actions.contains(Action.ANNULER));
    }

    /** Annuler le premier point efface l'aperçu et conserve la tournée. */
    @Test
    public void annulerCollecteProvisoireConserveTournee() throws Exception {
        controleur = new MainController(ecran, Plan::calculerTournees,
                Runnable::run, Runnable::run);
        chargerDeuxDemandes();
        ecran.actionCalculer.run();
        Tournee avant = tournees().get(1);
        ecran.actionAjouter.run();
        ecran.actionClicNoeud.accept(NOEUD_A);
        ecran.actionAnnuler.run();
        assertNull(ecran.collecteProvisoire);
        assertNull(ecran.livraisonProvisoire);
        assertSame(avant, tournees().get(1));
        assertEquals(2, demandes().size());
        assertFalse(controleur.peutAnnuler());
        assertTrue(controleur.obtenirEtat() instanceof EtatTourneeCalculee);
    }

    /** Les deux points sont visibles pendant le dialogue puis effacés. */
    @Test
    public void livraisonVisibleAvantValidationPuisAnnulation()
            throws Exception {
        chargerDeuxDemandes();
        ecran.saisie = Optional.empty();
        ecran.pendantSaisie = () -> {
            assertEquals(NOEUD_A, ecran.collecteProvisoire.identifiant());
            assertEquals(NOEUD_B, ecran.livraisonProvisoire.identifiant());
            assertEquals(2, demandes().size());
        };
        ecran.actionAjouter.run();
        ecran.actionClicNoeud.accept(NOEUD_A);
        ecran.actionClicNoeud.accept(NOEUD_B);
        assertNull(ecran.collecteProvisoire);
        assertNull(ecran.livraisonProvisoire);
        assertEquals(2, demandes().size());
        assertFalse(controleur.peutAnnuler());
    }

    /** Une validation remplace l'aperçu par une demande annulable. */
    @Test
    public void validerRemplacePointsProvisoiresPuisAnnulerEtRetablir()
            throws Exception {
        chargerDeuxDemandes();
        ecran.actionAjouter.run();
        ecran.actionClicNoeud.accept(NOEUD_A);
        ecran.actionClicNoeud.accept(NOEUD_B);
        assertNull(ecran.collecteProvisoire);
        assertNull(ecran.livraisonProvisoire);
        assertEquals(3, demandes().size());
        ecran.actionAnnuler.run();
        assertEquals(2, demandes().size());
        ecran.actionRefaire.run();
        assertEquals(3, demandes().size());
        assertNull(ecran.collecteProvisoire);
    }

    /** Un clic hors du plan ne crée pas de point provisoire. */
    @Test
    public void clicInconnuIgnorePendantAjout() throws Exception {
        chargerDeuxDemandes();
        ecran.actionAjouter.run();
        ecran.actionClicNoeud.accept(Long.MIN_VALUE);
        assertTrue(controleur.obtenirEtat() instanceof EtatAjoutCollecte);
        assertNull(ecran.collecteProvisoire);
        ecran.actionClicNoeud.accept(NOEUD_A);
        ecran.actionClicNoeud.accept(Long.MIN_VALUE);
        assertEquals(NOEUD_A, ecran.collecteProvisoire.identifiant());
        assertNull(ecran.livraisonProvisoire);
        assertTrue(controleur.obtenirEtat() instanceof EtatAjoutLivraison);
    }

    /** Les deux durées saisies sont converties en secondes et annulables. */
    @Test
    public void dureesSaisiesEtRetablissement() throws Exception {
        chargerDeuxDemandes();
        ecran.saisie = Optional.of(new MapScreen.SaisieDemande("2", "7", 1));
        ecran.actionAjouter.run();
        ecran.actionClicNoeud.accept(NOEUD_A);
        ecran.actionClicNoeud.accept(NOEUD_B);
        assertEquals(120, demandes().get(2).dureeCollecte());
        assertEquals(420, demandes().get(2).dureeLivraison());
        ecran.actionAnnuler.run();
        assertEquals(2, demandes().size());
        ecran.actionRefaire.run();
        assertEquals(420, demandes().get(2).dureeLivraison());
    }

    /** Une annulation de dialogue ne crée aucune commande. */
    @Test
    public void annulationSaisieSansModification() throws Exception {
        chargerDeuxDemandes();
        ecran.saisie = Optional.empty();
        ecran.actionAjouter.run();
        ecran.actionClicNoeud.accept(NOEUD_A);
        ecran.actionClicNoeud.accept(NOEUD_B);
        assertEquals(2, demandes().size());
        assertFalse(controleur.peutAnnuler());
        assertTrue(controleur.obtenirEtat() instanceof EtatProgrammeCharge);
    }

    /** Les saisies invalides sont refusées sans toucher à l'historique. */
    @Test
    public void saisiesInvalidesRefusees() throws Exception {
        chargerDeuxDemandes();
        ecran.actionAjouter.run();
        ecran.actionClicNoeud.accept(NOEUD_A);
        for (String valeur : List.of("", "abc", "0", "-1", "2147483647")) {
            ecran.saisie = Optional.of(new MapScreen.SaisieDemande(
                    valeur, "5", 1));
            ecran.actionClicNoeud.accept(NOEUD_B);
            assertEquals(2, demandes().size());
            assertFalse(controleur.peutAnnuler());
            assertTrue(ecran.consigne.contains("strictement positive"));
            assertEquals(NOEUD_A, ecran.collecteProvisoire.identifiant());
            assertEquals(NOEUD_B, ecran.livraisonProvisoire.identifiant());
        }
        ecran.actionAnnuler.run();
        assertNull(ecran.collecteProvisoire);
        assertNull(ecran.livraisonProvisoire);
    }

    /** L'ajout après calcul conserve la tournée et ne relance pas le TSP. */
    @Test
    public void insertionLocaleAnnulableSansTsp() throws Exception {
        int[] appels = {0};
        controleur = new MainController(ecran, (plan, programme) -> {
            appels[0]++;
            return plan.calculerTournees(programme);
        }, arrierePlan::add, Runnable::run);
        chargerDeuxDemandes();
        ecran.actionCalculer.run();
        arrierePlan.remove().run();
        Tournee avant = tournees().get(1);
        ecran.positionInsertion = Optional.of(avant.arrets().size() - 1);
        ecran.actionAjouter.run();
        ecran.actionClicNoeud.accept(NOEUD_A);
        ecran.actionClicNoeud.accept(NOEUD_B);
        assertEquals(2, demandes().size());
        assertTrue(ecran.actions.isEmpty());
        arrierePlan.remove().run();
        Tournee apres = tournees().get(1);
        assertEquals(3, demandes().size());
        assertEquals(avant.arrets().size() + 2, apres.arrets().size());
        int position = avant.arrets().size() - 1;
        assertEquals(NOEUD_A,
                apres.arrets().get(position).noeud().identifiant());
        assertEquals(NOEUD_B,
                apres.arrets().get(position + 1).noeud().identifiant());
        assertEquals(1, appels[0]);
        ecran.actionAnnuler.run();
        assertSame(avant, tournees().get(1));
        ecran.actionRefaire.run();
        assertSame(apres, tournees().get(1));
        assertEquals(1, appels[0]);
    }

    /** Annuler le choix de position ne crée ni demande ni calcul. */
    @Test
    public void annulerPositionConserveTournee() throws Exception {
        controleur = new MainController(ecran, Plan::calculerTournees,
                arrierePlan::add, Runnable::run);
        chargerDeuxDemandes();
        ecran.actionCalculer.run();
        arrierePlan.remove().run();
        Tournee avant = tournees().get(1);
        ecran.positionInsertion = Optional.empty();
        ecran.actionAjouter.run();
        ecran.actionClicNoeud.accept(NOEUD_A);
        ecran.actionClicNoeud.accept(NOEUD_B);
        assertSame(avant, tournees().get(1));
        assertEquals(2, demandes().size());
        assertTrue(arrierePlan.isEmpty());
        assertFalse(controleur.peutAnnuler());
        assertTrue(controleur.obtenirEtat() instanceof EtatTourneeCalculee);
        assertNull(ecran.collecteProvisoire);
        assertNull(ecran.livraisonProvisoire);
    }

    /** Le retard hors plage est signalé et annulable sans refuser l'ajout. */
    @Test
    public void insertionManuelleSignaleRetardAnnulable() throws Exception {
        controleur = new MainController(ecran, Plan::calculerTournees,
                arrierePlan::add, Runnable::run);
        chargerDeuxDemandes();
        ecran.actionCalculer.run();
        arrierePlan.remove().run();
        ecran.saisie = Optional.of(new MapScreen.SaisieDemande("70", "5", 1));
        ecran.actionAjouter.run();
        ecran.actionClicNoeud.accept(NOEUD_A);
        ecran.actionClicNoeud.accept(NOEUD_B);
        arrierePlan.remove().run();
        assertEquals(3, demandes().size());
        assertFalse(ecran.horsPlage.isEmpty());
        assertTrue(ecran.horsPlage.stream().allMatch(arret ->
                arret.obtenirDecalageSecondes() > 3600));
        ecran.actionAnnuler.run();
        assertTrue(ecran.horsPlage.isEmpty());
        ecran.actionRefaire.run();
        assertFalse(ecran.horsPlage.isEmpty());
    }

    /** Un ajout impossible ne touche ni au programme ni à l'historique. */
    @Test
    public void insertionImpossibleConserveEtat() throws Exception {
        controleur = new MainController(ecran, Plan::calculerTournees,
                arrierePlan::add, Runnable::run);
        chargerDeuxDemandes();
        ecran.actionCalculer.run();
        arrierePlan.remove().run();
        Tournee avant = tournees().get(1);
        ecran.saisie = Optional.of(new MapScreen.SaisieDemande("300", "5", 1));
        ecran.actionAjouter.run();
        ecran.actionClicNoeud.accept(NOEUD_A);
        ecran.actionClicNoeud.accept(NOEUD_B);
        arrierePlan.remove().run();
        assertSame(avant, tournees().get(1));
        assertEquals(2, demandes().size());
        assertFalse(controleur.peutAnnuler());
        assertNotNull(ecran.erreurCalcul);
        assertFalse(ecran.attente);
        assertNull(ecran.collecteProvisoire);
        assertNull(ecran.livraisonProvisoire);
    }

    /** La liste des dépassements est transmise à l'écran. */
    @Test
    public void signalementHorsPlage() throws Exception {
        chargerDeuxDemandes();
        Tournee.Arret arret = new Tournee.Arret(new Noeud(1, 0, 0),
                LocalTime.of(11, 0), LocalTime.of(11, 5), TypeArret.ENTREPOT,
                null, LocalTime.of(8, 0), LocalTime.of(10, 0));
        controleur.obtenirProgramme().definirTournees(
                Map.of(1, new Tournee(List.of(arret), List.of())));
        controleur.retrouverEtatProgramme();
        assertEquals(List.of(arret), ecran.horsPlage);
        controleur.obtenirProgramme().definirTournees(TOURNEES);
        controleur.retrouverEtatProgramme();
        assertTrue(ecran.horsPlage.isEmpty());
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
        assertNull(ecran.collecteProvisoire);
        assertNull(ecran.livraisonProvisoire);
        ecran.actionAjouter.run();
        ecran.actionClicNoeud.accept(NOEUD_B);
        assertEquals(NOEUD_B, ecran.collecteProvisoire.identifiant());
        assertNull(ecran.livraisonProvisoire);
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

    /** Sauvegarder et restaurer garde la tournée et vide les commandes. */
    @Test
    public void sauvegardeRestaurationVideHistorique() throws Exception {
        controleur = new MainController(ecran);
        chargerDeuxDemandes();
        ecran.actionSupprimer.accept(0);
        ecran.actionCalculer.run();
        Tournee avant = tournees().get(1);
        File fichier = dossier.newFile("sauvegarde.xml");
        ecran.fichierChoisi = fichier;
        ecran.actionSauvegarder.run();
        assertTrue(fichier.length() > 0);
        ecran.actionSupprimer.accept(0);
        ecran.actionRestaurer.run();
        assertEquals(avant, tournees().get(1));
        assertEquals(1, demandes().size());
        assertFalse(controleur.peutAnnuler());
        assertFalse(controleur.peutRefaire());
        assertTrue(controleur.obtenirEtat() instanceof EtatTourneeCalculee);
    }

    /** Un échec restaure l'état et conserve les commandes. */
    @Test
    public void restaurationInvalideConserveEtatEtHistorique() throws Exception {
        chargerDeuxDemandes();
        ecran.actionSupprimer.accept(0);
        Etat avant = controleur.obtenirEtat();
        ProgrammeLivraisons programme = controleur.obtenirProgramme();
        ecran.fichierChoisi = ecrire("<invalide/>");
        ecran.actionRestaurer.run();
        assertTrue(ecran.actions.isEmpty());
        ecran.actionImportPlan.run();
        ecran.actionRestaurer.run();
        executerArrierePlan();
        assertSame(avant, controleur.obtenirEtat());
        assertSame(programme, controleur.obtenirProgramme());
        assertTrue(controleur.peutAnnuler());
        assertFalse(ecran.attente);
        assertTrue(ecran.evenements.contains("invalide"));
    }

    /** Le sélecteur annulé ne provoque aucune lecture ni changement. */
    @Test
    public void restaurationAnnuleeConserveEtat() {
        Etat avant = controleur.obtenirEtat();
        ecran.fichierChoisi = null;
        ecran.actionRestaurer.run();
        assertSame(avant, controleur.obtenirEtat());
        assertTrue(arrierePlan.isEmpty());
    }

    /** Sauvegarder avant le calcul est ignoré par l'état. */
    @Test
    public void sauvegardeSansTourneeIgnoree() throws Exception {
        chargerDeuxDemandes();
        File fichier = dossier.newFile();
        ecran.fichierChoisi = fichier;
        ecran.actionSauvegarder.run();
        assertEquals(0, fichier.length());
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
     * Avec deux livreurs, ajouter une demande au livreur 2 n'insère que dans
     * sa tournée, sans relancer le voyageur de commerce : celle du livreur 1
     * est la même, et Annuler remet les deux tournées d'avant.
     */
    @Test
    public void insertionNeChangeQueLeLivreurDeLaDemande() throws Exception {
        utiliserLeVraiCalcul();
        chargerDeuxDemandes();
        ProgrammeLivraisons programme = controleur.obtenirProgramme();
        programme.definirNombreLivreurs(2);
        programme.remplacerDemande(1, demandes().get(1).avecLivreur(2));
        ecran.actionCalculer.run();
        Tournee premierLivreur = tournees().get(1);
        Tournee secondLivreur = tournees().get(2);
        ecran.positionInsertion = Optional.of(1);
        ecran.saisie = Optional.of(new MapScreen.SaisieDemande("2", "3", 2));

        ecran.actionAjouter.run();
        ecran.actionClicNoeud.accept(NOEUD_A);
        ecran.actionClicNoeud.accept(NOEUD_B);

        assertEquals(3, demandes().size());
        assertEquals(2, demandes().get(2).livreur());
        assertSame(premierLivreur, tournees().get(1));
        assertEquals(secondLivreur.arrets().size() + 2,
                tournees().get(2).arrets().size());
        assertEquals(NOEUD_A,
                tournees().get(2).arrets().get(1).noeud().identifiant());
        assertEquals(1, nombreDeCalculs);
        assertTrue(ecran.consigne.startsWith("Livreur 2 :"));

        ecran.actionAnnuler.run();
        assertSame(premierLivreur, tournees().get(1));
        assertSame(secondLivreur, tournees().get(2));
        assertEquals(2, demandes().size());
    }

    /** Un livreur qui n'existe pas est refusé avant tout calcul. */
    @Test
    public void livreurInconnuRefuseALAjout() throws Exception {
        chargerDeuxDemandes();
        ecran.saisie = Optional.of(new MapScreen.SaisieDemande("2", "3", 2));
        ecran.actionAjouter.run();
        ecran.actionClicNoeud.accept(NOEUD_A);
        ecran.actionClicNoeud.accept(NOEUD_B);
        assertEquals(2, demandes().size());
        assertFalse(controleur.peutAnnuler());
        assertTrue(ecran.consigne.contains("Livreur 2 inconnu"));
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

    // --- Changer l'ordre des arrêts (TIG-44) ---

    /**
     * Charge les deux demandes, calcule la tournée avec le vrai calcul et
     * rend la tournée du livreur 1.
     */
    private Tournee afficherUneTourneeDeDeuxDemandes() throws Exception {
        utiliserLeVraiCalcul();
        chargerDeuxDemandes();
        ecran.actionCalculer.run();
        return tournees().get(1);
    }

    /**
     * Descend le premier arrêt du livreur 1 qui peut l'être (la contrainte
     * d'ordre en interdit certains) et rend son rang.
     */
    private int descendreUnArretPermis(Tournee tournee) {
        for (int rang = 1; rang < tournee.arrets().size() - 2; rang++) {
            ecran.refusOrdre = null;
            ecran.actionDescendre.accept(1, rang);
            if (ecran.refusOrdre == null) {
                return rang;
            }
        }
        throw new AssertionError("Aucun arrêt ne peut descendre.");
    }

    /**
     * Descendre un arrêt met la tournée à jour tout de suite, sans relancer le
     * voyageur de commerce, et ouvre l'annulation.
     */
    @Test
    public void descendreUnArretChangeLOrdreSansRelancerLeTsp()
            throws Exception {
        Tournee avant = afficherUneTourneeDeDeuxDemandes();
        int rang = descendreUnArretPermis(avant);
        List<Noeud> attendus = new ArrayList<>(noeudsDe(avant));
        Collections.swap(attendus, rang, rang + 1);
        assertEquals(attendus, noeudsDe(tournees().get(1)));
        assertEquals(1, nombreDeCalculs);
        assertTrue(controleur.obtenirEtat() instanceof EtatTourneeCalculee);
        assertTrue(ecran.actions.contains(Action.ANNULER));
        assertTrue(ecran.actions.contains(Action.CHANGER_ORDRE));
    }

    /** Annuler remet l'ordre précédent, rétablir remet le nouvel ordre. */
    @Test
    public void annulerEtRetablirLeChangementDOrdre() throws Exception {
        Tournee avant = afficherUneTourneeDeDeuxDemandes();
        descendreUnArretPermis(avant);
        Tournee apres = tournees().get(1);
        ecran.actionAnnuler.run();
        assertSame(avant, tournees().get(1));
        assertTrue(ecran.actions.contains(Action.REFAIRE));
        ecran.actionRefaire.run();
        assertSame(apres, tournees().get(1));
        assertEquals(1, nombreDeCalculs);
    }

    /** Monter l'arrêt qu'on vient de descendre redonne la tournée d'avant. */
    @Test
    public void monterApresDescendreRedonneLaTournee() throws Exception {
        Tournee avant = afficherUneTourneeDeDeuxDemandes();
        int rang = descendreUnArretPermis(avant);
        ecran.actionMonter.accept(1, rang + 1);
        assertEquals(avant, tournees().get(1));
    }

    /**
     * Un ordre refusé affiche le message, ne change pas la tournée et ne
     * laisse rien à annuler.
     */
    @Test
    public void ordreRefuseAfficheLeMessageEtNeChangeRien() throws Exception {
        Tournee avant = afficherUneTourneeDeDeuxDemandes();
        ecran.actionMonter.accept(1, 1);
        assertTrue(ecran.refusOrdre.contains("premier"));
        assertSame(avant, tournees().get(1));
        assertFalse(ecran.actions.contains(Action.ANNULER));
        ecran.actionMonter.accept(1, 0);
        assertTrue(ecran.refusOrdre.contains("entrepôt"));
        assertSame(avant, tournees().get(1));
    }

    /** Sans tournée affichée, Monter et Descendre sont ignorés. */
    @Test
    public void changerLOrdreSansTourneeEstIgnore() throws Exception {
        chargerDeuxDemandes();
        ecran.actionMonter.accept(1, 1);
        ecran.actionDescendre.accept(1, 1);
        assertTrue(ecran.evenements.isEmpty());
        assertNull(ecran.refusOrdre);
        assertFalse(ecran.actions.contains(Action.CHANGER_ORDRE));
    }

    /**
     * Descend le premier arrêt du livreur 1 qui peut l'être (la contrainte
     * d'ordre en interdit certains) sans toucher aux deux arrêts de la demande
     * à supprimer, et rend son rang.
     */
    private int descendreUnArretHorsDeLaDemande(Tournee tournee,
            Demande demande) {
        for (int rang = 1; rang < tournee.arrets().size() - 2; rang++) {
            if (demande.equals(tournee.arrets().get(rang).demande())
                    || demande.equals(
                            tournee.arrets().get(rang + 1).demande())) {
                continue;
            }
            ecran.refusOrdre = null;
            ecran.actionDescendre.accept(1, rang);
            if (ecran.refusOrdre == null) {
                return rang;
            }
        }
        throw new AssertionError("Aucun arrêt hors de la demande ne descend.");
    }

    /**
     * Après un déplacement réussi, l'arrêt déplacé reste sélectionné à sa
     * nouvelle place (la sélection est remise après le réaffichage des
     * tournées), pour pouvoir le déplacer de plusieurs rangs de suite.
     */
    @Test
    public void arretDeplaceResteSelectionne() throws Exception {
        Tournee avant = afficherUneTourneeDeDeuxDemandes();
        ecran.evenements.clear();
        int rang = descendreUnArretPermis(avant);
        assertEquals(1, ecran.livreurSelectionne);
        assertEquals(rang + 1, ecran.arretSelectionne);
        assertEquals("selection:1:" + (rang + 1), ecran.evenements.get(
                ecran.evenements.size() - 1));
    }

    /**
     * Le changement d'ordre tourne dans la file de calcul, pas sur le thread
     * appelant : tant qu'elle n'est pas exécutée, l'attente est affichée, les
     * actions sont bloquées et la tournée ne change pas.
     */
    @Test
    public void changerLOrdreCalculeHorsDuThreadDeLInterface()
            throws Exception {
        utiliserLeVraiCalculEnArrierePlan();
        chargerDeuxDemandes();
        ecran.actionCalculer.run();
        executerArrierePlan();
        Tournee avant = tournees().get(1);
        int rang = -1;
        for (int i = 1; i < avant.arrets().size() - 2 && rang < 0; i++) {
            ecran.refusOrdre = null;
            ecran.actionDescendre.accept(1, i);
            executerArrierePlan();
            if (ecran.refusOrdre == null) {
                rang = i;
            }
        }
        assertTrue("un arrêt peut descendre", rang > 0);
        ecran.actionAnnuler.run();
        assertSame(avant, tournees().get(1));

        ecran.actionDescendre.accept(1, rang);

        assertEquals(1, arrierePlan.size());
        assertTrue(ecran.attente);
        assertTrue(controleur.obtenirEtat() instanceof EtatCalculEnCours);
        assertTrue(ecran.actions.isEmpty());
        assertSame(avant, tournees().get(1));
        executerArrierePlan();
        assertFalse(ecran.attente);
        assertTrue(controleur.obtenirEtat() instanceof EtatTourneeCalculee);
        assertEquals(1, nombreDeCalculs);
    }

    /**
     * Réordonner, supprimer, annuler, réordonner, supprimer : l'ordre choisi à
     * la main est gardé. La première suppression recalculée localement ne doit
     * pas masquer l'ordre de la tournée réordonnée.
     */
    @Test
    public void reordonnerSupprimerAnnulerGardeLOrdreChoisi()
            throws Exception {
        utiliserLeVraiCalcul();
        importerPlan(ressource("/data/moyenPlan.xml"));
        importerProgramme(ressource("/data/demandeMoyen5.xml"));
        ecran.actionCalculer.run();
        Tournee t1 = tournees().get(1);
        Demande supprimee = demandes().get(0);

        ecran.actionSupprimer.accept(0);
        List<Noeud> localeSansReordre = noeudsDe(tournees().get(1));
        ecran.actionAnnuler.run();
        assertSame(t1, tournees().get(1));
        descendreUnArretHorsDeLaDemande(t1, supprimee);
        Tournee t2 = tournees().get(1);
        ecran.actionSupprimer.accept(0);

        List<Noeud> attendus = new ArrayList<>(noeudsDe(t2));
        attendus.remove(supprimee.collecte());
        attendus.remove(supprimee.livraison());
        assertEquals(attendus, noeudsDe(tournees().get(1)));
        assertNotEquals(localeSansReordre, noeudsDe(tournees().get(1)));
        assertEquals(1, nombreDeCalculs);
    }

    /**
     * Annuler après un nouveau « Calculer » revient à l'état d'avant le
     * changement d'ordre, et rétablir remet les tournées de ce calcul : les
     * commandes voisines ont retenu les tournées calculées après coup.
     */
    @Test
    public void annulerApresUnNouveauCalculRevientAvantLeChangement()
            throws Exception {
        Tournee t1 = afficherUneTourneeDeDeuxDemandes();
        descendreUnArretPermis(t1);
        ecran.actionCalculer.run();
        Tournee t3 = tournees().get(1);
        assertEquals(2, nombreDeCalculs);

        ecran.actionAnnuler.run();
        assertSame(t1, tournees().get(1));
        ecran.actionRefaire.run();
        assertSame(t3, tournees().get(1));
        assertEquals(2, nombreDeCalculs);
    }

    /**
     * Refus par la contrainte d'ordre avec le vrai calcul : la tournée est
     * [entrepôt, collecte, livraison, entrepôt]. Descendre la collecte ou
     * monter la livraison est refusé avec le message de la contrainte, la
     * tournée reste affichée et il n'y a rien à annuler.
     */
    @Test
    public void contrainteDOrdreRefuseeAvecLeVraiCalcul() throws Exception {
        utiliserLeVraiCalcul();
        importerPlan(ressource("/data/petitPlan.xml"));
        importerProgramme(ressource("/data/demandePetit1.xml"));
        ecran.actionCalculer.run();
        Tournee avant = tournees().get(1);
        assertEquals(4, avant.arrets().size());

        ecran.actionDescendre.accept(1, 1);
        assertTrue(ecran.refusOrdre.contains("Contrainte d'ordre"));
        ecran.refusOrdre = null;
        ecran.actionMonter.accept(1, 2);
        assertTrue(ecran.refusOrdre.contains("Contrainte d'ordre"));

        assertSame(avant, tournees().get(1));
        assertFalse(ecran.attente);
        assertTrue(controleur.obtenirEtat() instanceof EtatTourneeCalculee);
        assertFalse(ecran.actions.contains(Action.ANNULER));
        assertEquals(1, nombreDeCalculs);
    }

    /** Fichier de deux demandes sur le petit plan, partant à l'heure dite. */
    private File deuxDemandesPartantA(LocalTime depart) throws Exception {
        return ecrire("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<demandeDeLivraisons><entrepot adresse=\"2835339774\" "
                + "heureDepart=\"" + depart.getHour() + ":"
                + depart.getMinute() + ":" + depart.getSecond() + "\"/>"
                + "<livraison adresseEnlevement=\"1679901320\" "
                + "adresseLivraison=\"208769457\" dureeEnlevement=\"420\" "
                + "dureeLivraison=\"600\"/>"
                + "<livraison adresseEnlevement=\"208769120\" "
                + "adresseLivraison=\"25336179\" dureeEnlevement=\"420\" "
                + "dureeLivraison=\"480\"/></demandeDeLivraisons>");
    }

    /**
     * Refus après 13 h avec le vrai calcul : le départ est fixé pour que la
     * tournée du voyageur de commerce rentre exactement à 13:00:00. Tout autre
     * ordre permis est plus long et rentre après 13 h : il est refusé avec le
     * message, et la tournée affichée ne change pas.
     */
    @Test
    public void retourApres13hRefuseAvecLeVraiCalcul() throws Exception {
        Tournee deHuitHeures = afficherUneTourneeDeDeuxDemandes();
        long secondes = deHuitHeures.arrets().get(0).arrivee().until(
                deHuitHeures.obtenirHeureRetour(),
                java.time.temporal.ChronoUnit.SECONDS);
        importerProgramme(deuxDemandesPartantA(
                LocalTime.of(13, 0).minusSeconds(secondes)));
        ecran.actionCalculer.run();
        Tournee limite = tournees().get(1);
        assertEquals(LocalTime.of(13, 0), limite.obtenirHeureRetour());

        int refus13h = 0;
        for (int rang = 1; rang < limite.arrets().size() - 2; rang++) {
            ecran.refusOrdre = null;
            ecran.actionDescendre.accept(1, rang);
            if (ecran.refusOrdre == null) {
                ecran.actionAnnuler.run(); // échange accepté : on le défait
            } else if (ecran.refusOrdre.contains("13:00")) {
                refus13h++;
            }
        }

        assertTrue("au moins un ordre refusé après 13 h", refus13h > 0);
        assertSame(limite, tournees().get(1));
    }

    /**
     * Une tournée déjà en retard n'est pas bloquée : on peut encore changer
     * l'ordre de ses arrêts. Seul un changement qui rend impossible une tournée
     * possible est refusé.
     */
    @Test
    public void changerLOrdreDUneTourneeDejaEnRetardEstAccepte()
            throws Exception {
        utiliserLeVraiCalcul();
        importerPlan(ressource("/data/petitPlan.xml"));
        importerProgramme(deuxDemandesPartantA(LocalTime.of(12, 59)));
        ecran.actionCalculer.run();
        Tournee enRetard = tournees().get(1);
        assertFalse(enRetard.estPossible());

        descendreUnArretPermis(enRetard);

        assertNull(ecran.refusOrdre);
        assertTrue(ecran.actions.contains(Action.ANNULER));
        assertNotEquals(noeudsDe(enRetard), noeudsDe(tournees().get(1)));
    }

    /**
     * Avec deux livreurs, seule la tournée du livreur de l'arrêt change : l'autre
     * est la même, la consigne nomme le livreur, c'est son arrêt qui reste
     * sélectionné, et Annuler remet les deux tournées.
     */
    @Test
    public void changerLOrdreNeChangeQueLeLivreurConcerne() throws Exception {
        utiliserLeVraiCalcul();
        importerPlan(ressource("/data/moyenPlan.xml"));
        importerProgramme(ressource("/data/demandeMoyen3.xml"));
        ProgrammeLivraisons programme = controleur.obtenirProgramme();
        programme.definirNombreLivreurs(2);
        programme.remplacerDemande(2, demandes().get(2).avecLivreur(2));
        ecran.actionCalculer.run();
        Tournee premierLivreur = tournees().get(1);
        Tournee secondLivreur = tournees().get(2);

        int rang = descendreUnArretPermis(premierLivreur);

        assertNotEquals(noeudsDe(premierLivreur), noeudsDe(tournees().get(1)));
        assertSame(secondLivreur, tournees().get(2));
        assertEquals(MainController.CONSIGNE_ORDRE_GARDE_LIVREUR.formatted(1),
                ecran.consigne);
        assertEquals(1, ecran.livreurSelectionne);
        assertEquals(rang + 1, ecran.arretSelectionne);
        assertEquals(1, nombreDeCalculs);

        ecran.actionAnnuler.run();
        assertSame(premierLivreur, tournees().get(1));
        assertSame(secondLivreur, tournees().get(2));
    }

    /**
     * Après un changement d'ordre, la consigne prévient que l'ordre n'est plus
     * celui du voyageur de commerce ; avec un seul livreur, elle ne le nomme
     * pas et ne conseille pas « Calculer ».
     */
    @Test
    public void consigneApresUnChangementDOrdre() throws Exception {
        Tournee avant = afficherUneTourneeDeDeuxDemandes();
        descendreUnArretPermis(avant);
        assertEquals(MainController.CONSIGNE_ORDRE_GARDE, ecran.consigne);
        assertFalse(ecran.consigne.contains("Livreur"));
        assertFalse(ecran.consigne.contains("Calculer"));
    }

    // --- Déplacer un point de collecte ou de livraison (TIG-43) ---

    /** Nœud du petit plan qui n'est un point d'aucune des deux demandes. */
    private static final long NOEUD_AILLEURS = 26086130L;

    /**
     * Déplacer un point sans relancer le voyageur de commerce : l'arrêt
     * change d'adresse à la même place, reste sélectionné ; Annuler remet les
     * demandes et la tournée d'avant, Rétablir celles d'après, sans calcul.
     */
    @Test
    public void deplacerUnPointPuisAnnulerEtRetablir() throws Exception {
        Tournee avant = afficherUneTourneeDeDeuxDemandes();
        List<Demande> demandesAvant = List.copyOf(demandes());
        ecran.actionDeplacer.accept(1, 1);
        assertTrue(controleur.obtenirEtat() instanceof EtatDeplacementPoint);
        ecran.actionClicNoeud.accept(NOEUD_AILLEURS);

        Tournee apres = tournees().get(1);
        assertEquals(NOEUD_AILLEURS,
                apres.arrets().get(1).noeud().identifiant());
        assertEquals(avant.arrets().size(), apres.arrets().size());
        assertNotEquals(demandesAvant, demandes());
        assertEquals(apres.arrets().get(1).demande(),
                demandes().stream().filter(d -> d.collecte().identifiant()
                        == NOEUD_AILLEURS || d.livraison().identifiant()
                        == NOEUD_AILLEURS).findFirst().orElseThrow());
        assertEquals(1, ecran.livreurSelectionne);
        assertEquals(1, ecran.arretSelectionne);
        assertTrue(controleur.obtenirEtat() instanceof EtatTourneeCalculee);
        ecran.actionAnnuler.run();
        assertSame(avant, tournees().get(1));
        assertEquals(demandesAvant, demandes());
        ecran.actionRefaire.run();
        assertSame(apres, tournees().get(1));
        assertEquals(1, nombreDeCalculs);
    }

    /**
     * Sans arrêt sélectionné, ou avec l'entrepôt, « Déplacer » explique quoi
     * faire par une consigne ; un second clic sur « Déplacer » annule la
     * saisie en cours.
     */
    @Test
    public void deplacerSansPointChoisiOuAnnulerLaSaisie() throws Exception {
        afficherUneTourneeDeDeuxDemandes();
        ecran.actionDeplacer.accept(-1, -1);
        assertEquals(MainController.CONSIGNE_AUCUN_POINT, ecran.consigne);
        ecran.actionDeplacer.accept(1, 0);
        assertTrue(controleur.obtenirEtat() instanceof EtatTourneeCalculee);

        ecran.actionDeplacer.accept(1, 1);
        ecran.actionDeplacer.accept(1, 1);
        assertTrue(controleur.obtenirEtat() instanceof EtatTourneeCalculee);
        assertFalse(ecran.actions.contains(Action.ANNULER));
    }

    /**
     * Avec deux livreurs, seule la tournée du livreur de l'arrêt change :
     * l'autre est la même, la consigne nomme le livreur, et Annuler remet les
     * deux tournées.
     */
    @Test
    public void deplacerNeChangeQueLeLivreurConcerne() throws Exception {
        utiliserLeVraiCalcul();
        chargerDeuxDemandes();
        ProgrammeLivraisons programme = controleur.obtenirProgramme();
        programme.definirNombreLivreurs(2);
        programme.remplacerDemande(1, demandes().get(1).avecLivreur(2));
        ecran.actionCalculer.run();
        Tournee premierLivreur = tournees().get(1);
        Tournee secondLivreur = tournees().get(2);

        ecran.actionDeplacer.accept(2, 1);
        ecran.actionClicNoeud.accept(NOEUD_AILLEURS);

        assertSame(premierLivreur, tournees().get(1));
        assertEquals(NOEUD_AILLEURS,
                tournees().get(2).arrets().get(1).noeud().identifiant());
        assertEquals(MainController.CONSIGNE_ORDRE_GARDE_LIVREUR.formatted(2),
                ecran.consigne);
        assertEquals(1, nombreDeCalculs);

        ecran.actionAnnuler.run();
        assertSame(premierLivreur, tournees().get(1));
        assertSame(secondLivreur, tournees().get(2));
    }

    /**
     * Les déplacements qui rendent la tournée impossible (adresse inaccessible,
     * ou retour après 13 h, le départ étant réglé pour rentrer à 13 h pile)
     * sont refusés avec un message : demandes, tournée affichée et historique
     * ne changent pas.
     */
    @Test
    public void deplacerUnPointImpossibleNeChangeRien() throws Exception {
        Tournee deHuitHeures = afficherUneTourneeDeDeuxDemandes();
        LocalTime depart = LocalTime.of(13, 0).minusSeconds(
                deHuitHeures.arrets().get(0).arrivee().until(
                        deHuitHeures.obtenirHeureRetour(),
                        java.time.temporal.ChronoUnit.SECONDS));
        importerProgramme(ecrire("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<demandeDeLivraisons><entrepot adresse=\"2835339774\" "
                + "heureDepart=\"" + depart.getHour() + ":"
                + depart.getMinute() + ":" + depart.getSecond() + "\"/>"
                + "<livraison adresseEnlevement=\"1679901320\" "
                + "adresseLivraison=\"208769457\" dureeEnlevement=\"420\" "
                + "dureeLivraison=\"600\"/>"
                + "<livraison adresseEnlevement=\"208769120\" "
                + "adresseLivraison=\"25336179\" dureeEnlevement=\"420\" "
                + "dureeLivraison=\"480\"/></demandeDeLivraisons>"));
        ecran.actionCalculer.run();
        Tournee limite = tournees().get(1);

        int refus = 0;
        for (Noeud noeud : ecran.planAffiche.obtenirNoeuds()) {
            ecran.refusDeplacement = null;
            ecran.actionDeplacer.accept(1, 1);
            ecran.actionClicNoeud.accept(noeud.identifiant());
            if (ecran.refusDeplacement == null) {
                if (ecran.actions.contains(Action.ANNULER)) {
                    ecran.actionAnnuler.run();
                }
                continue;
            }
            // Une adresse inaccessible est aussi refusée, avec un autre texte.
            if (ecran.refusDeplacement.contains("13:00")) {
                refus++;
            }
            assertSame(limite, tournees().get(1));
            assertTrue(controleur.obtenirEtat() instanceof EtatTourneeCalculee);
            assertFalse(ecran.actions.contains(Action.ANNULER));
        }
        assertTrue("au moins un retour refusé après 13 h", refus > 0);
    }

    // --- Changer le livreur d'une demande (TIG-80) ---

    /**
     * Sur les demandes déjà chargées : deux livreurs, la seconde demande est
     * confiée au livreur dit, puis les tournées sont calculées (vrai calcul).
     */
    private void repartirEtCalculer(int livreurDeLaSeconde) {
        ProgrammeLivraisons programme = controleur.obtenirProgramme();
        programme.definirNombreLivreurs(2);
        programme.remplacerDemande(1,
                demandes().get(1).avecLivreur(livreurDeLaSeconde));
        ecran.actionCalculer.run();
    }

    /** Charge les deux demandes, les répartit et calcule les tournées. */
    private void calculerAvecDeuxLivreurs(int livreurDeLaSeconde)
            throws Exception {
        utiliserLeVraiCalcul();
        chargerDeuxDemandes();
        repartirEtCalculer(livreurDeLaSeconde);
    }

    /** Vérifie que la tournée ne sert que cette demande (et l'entrepôt). */
    private static void verifierSeulesLesArretsDe(Tournee tournee,
            Demande demande) {
        for (Tournee.Arret arret : tournee.arrets()) {
            assertTrue(arret.demande() == null
                    || arret.demande().equals(demande));
        }
    }

    /**
     * Changer le livreur d'une demande la retire de la tournée de l'ancien
     * livreur et l'insère dans celle du nouveau, sans relancer le voyageur de
     * commerce. Elle garde son rang dans le programme et la consigne nomme les
     * deux livreurs.
     */
    @Test
    public void changerLivreurRetireChezLAncienEtInsereChezLeNouveau()
            throws Exception {
        calculerAvecDeuxLivreurs(1);
        Demande premiere = demandes().get(0);
        Demande seconde = demandes().get(1);
        assertEquals(6, tournees().get(1).arrets().size());
        assertEquals(1, tournees().get(2).arrets().size());

        ecran.actionChangerLivreur.accept(1);

        assertEquals(1, ecran.nombreDeChoixDeLivreur);
        assertEquals(1, ecran.livreurActuelPropose);
        assertEquals(2, ecran.nombreDeLivreursProposes);
        assertEquals(List.of(premiere, seconde.avecLivreur(2)), demandes());
        assertEquals(4, tournees().get(1).arrets().size());
        assertEquals(4, tournees().get(2).arrets().size());
        verifierSeulesLesArretsDe(tournees().get(1), premiere);
        verifierSeulesLesArretsDe(tournees().get(2), seconde.avecLivreur(2));
        assertEquals(1, nombreDeCalculs);
        assertTrue(controleur.obtenirEtat() instanceof EtatTourneeCalculee);
        assertFalse(ecran.attente);
        assertEquals(MainController.CONSIGNE_ORDRE_GARDE_LIVREURS
                .formatted(1, 2), ecran.consigne);
        assertTrue(ecran.actions.contains(Action.ANNULER));
    }

    /** Donner sa seule demande à un autre livreur laisse une tournée vide. */
    @Test
    public void changerLivreurDeLaDerniereDemandeVideLaTournee()
            throws Exception {
        calculerAvecDeuxLivreurs(2);
        ecran.actionChangerLivreur.accept(0);
        assertEquals(1, tournees().get(1).arrets().size());
        assertEquals(6, tournees().get(2).arrets().size());
    }

    /**
     * Le scénario de la démo : deux demandes passent du livreur 1 au livreur 2
     * puis Ctrl+Z, deux fois, remet les tournées d'origine ; rétablir les
     * range de nouveau. Rien n'est recalculé.
     */
    @Test
    public void deuxChangementsPuisDeuxAnnulations() throws Exception {
        calculerAvecDeuxLivreurs(1);
        Map<Integer, Tournee> origine = tournees();

        ecran.actionChangerLivreur.accept(0);
        Map<Integer, Tournee> apresLePremier = tournees();
        ecran.actionChangerLivreur.accept(1);
        assertEquals(1, tournees().get(1).arrets().size());
        assertEquals(6, tournees().get(2).arrets().size());
        assertEquals(2, demandes().get(0).livreur());
        assertEquals(2, demandes().get(1).livreur());

        ecran.actionAnnuler.run();
        assertSame(apresLePremier.get(1), tournees().get(1));
        assertSame(apresLePremier.get(2), tournees().get(2));
        assertEquals(2, demandes().get(0).livreur());
        assertEquals(1, demandes().get(1).livreur());

        ecran.actionAnnuler.run();
        assertSame(origine.get(1), tournees().get(1));
        assertSame(origine.get(2), tournees().get(2));
        assertEquals(1, demandes().get(0).livreur());
        assertEquals(1, demandes().get(1).livreur());

        ecran.actionRefaire.run();
        ecran.actionRefaire.run();
        assertEquals(1, tournees().get(1).arrets().size());
        assertEquals(6, tournees().get(2).arrets().size());
        assertEquals(2, demandes().get(0).livreur());
        assertEquals(2, demandes().get(1).livreur());
        assertEquals(1, nombreDeCalculs);
    }

    /**
     * L'entrée « Moindre coût » du dialogue de position insère la demande à la
     * position qui fait rentrer le livreur le plus tôt : aucune autre position
     * ne donne un retour plus matinal.
     */
    @Test
    public void changerLivreurAuMoindreCoutInsereALaMeilleurePosition()
            throws Exception {
        calculerAvecDeuxLivreurs(2);
        Tournee cible = tournees().get(2);
        Demande deplacee = demandes().get(0).avecLivreur(2);
        ecran.positionInsertion = Optional.of(MapScreen.POSITION_MOINDRE_COUT);

        ecran.actionChangerLivreur.accept(0);

        Plan plan = controleur.obtenirPlan();
        int position = plan.trouverPositionMoindreCout(cible, deplacee);
        assertEquals(plan.insererDemande(cible, deplacee, position),
                tournees().get(2));
        LocalTime retour = tournees().get(2).obtenirHeureRetour();
        for (int autre = 1; autre < cible.arrets().size(); autre++) {
            assertFalse(plan.insererDemande(cible, deplacee, autre)
                    .obtenirHeureRetour().isBefore(retour));
        }
        assertEquals(1, nombreDeCalculs);
    }

    /**
     * L'entrée « Moindre coût » sert aussi à l'ajout d'une demande : elle est
     * insérée à la meilleure position de la tournée du livreur choisi.
     */
    @Test
    public void ajouterAuMoindreCoutInsereALaMeilleurePosition()
            throws Exception {
        calculerAvecDeuxLivreurs(2);
        Tournee cible = tournees().get(2);
        ecran.positionInsertion = Optional.of(MapScreen.POSITION_MOINDRE_COUT);
        ecran.saisie = Optional.of(new MapScreen.SaisieDemande("2", "3", 2));

        ecran.actionAjouter.run();
        ecran.actionClicNoeud.accept(NOEUD_A);
        ecran.actionClicNoeud.accept(NOEUD_B);

        Demande ajoutee = demandes().get(2);
        Plan plan = controleur.obtenirPlan();
        int position = plan.trouverPositionMoindreCout(cible, ajoutee);
        assertEquals(plan.insererDemande(cible, ajoutee, position),
                tournees().get(2));
        LocalTime retour = tournees().get(2).obtenirHeureRetour();
        for (int autre = 1; autre < cible.arrets().size(); autre++) {
            assertFalse(plan.insererDemande(cible, ajoutee, autre)
                    .obtenirHeureRetour().isBefore(retour));
        }
        assertEquals(1, nombreDeCalculs);
    }

    /**
     * Un changement qui ferait rentrer après 13 h un livreur qui rentrait à
     * l'heure est refusé avec un message : demandes, tournées et historique ne
     * changent pas.
     */
    @Test
    public void changerLivreurImpossibleNeChangeRien() throws Exception {
        calculerAvecDeuxLivreurs(2);
        Tournee deHuitHeures = tournees().get(2);
        long secondes = deHuitHeures.arrets().get(0).arrivee().until(
                deHuitHeures.obtenirHeureRetour(),
                java.time.temporal.ChronoUnit.SECONDS);
        importerProgramme(deuxDemandesPartantA(
                LocalTime.of(13, 0).minusSeconds(secondes)));
        repartirEtCalculer(2);
        Map<Integer, Tournee> limite = tournees();
        assertEquals(LocalTime.of(13, 0), limite.get(2).obtenirHeureRetour());
        List<Demande> avant = List.copyOf(demandes());

        ecran.actionChangerLivreur.accept(0);

        assertNotNull(ecran.refusChangementLivreur);
        assertTrue(ecran.refusChangementLivreur.contains("13:00"));
        assertEquals(avant, demandes());
        assertSame(limite.get(1), tournees().get(1));
        assertSame(limite.get(2), tournees().get(2));
        assertFalse(controleur.peutAnnuler());
        assertFalse(ecran.attente);
        assertTrue(controleur.obtenirEtat() instanceof EtatTourneeCalculee);
    }

    /**
     * Un livreur qui rentrait déjà après 13 h peut recevoir une demande de
     * plus : seul le changement qui rend impossible une tournée possible est
     * refusé.
     */
    @Test
    public void changerLivreurVersTourneeDejaEnRetardAccepte()
            throws Exception {
        calculerAvecDeuxLivreurs(2);
        Tournee deHuitHeures = tournees().get(2);
        long secondes = deHuitHeures.arrets().get(0).arrivee().until(
                deHuitHeures.obtenirHeureRetour(),
                java.time.temporal.ChronoUnit.SECONDS);
        importerProgramme(deuxDemandesPartantA(
                LocalTime.of(13, 30).minusSeconds(secondes)));
        repartirEtCalculer(2);
        assertFalse(tournees().get(2).estPossible());

        ecran.actionChangerLivreur.accept(0);

        assertNull(ecran.refusChangementLivreur);
        assertEquals(2, demandes().get(0).livreur());
        assertTrue(controleur.peutAnnuler());
    }

    /** Annuler le choix du livreur ne change rien, pas même l'historique. */
    @Test
    public void annulerLeChoixDuLivreurNeChangeRien() throws Exception {
        calculerAvecDeuxLivreurs(1);
        Map<Integer, Tournee> avant = tournees();
        ecran.livreurChoisi = Optional.empty();

        ecran.actionChangerLivreur.accept(1);

        assertEquals(1, demandes().get(1).livreur());
        assertSame(avant.get(1), tournees().get(1));
        assertSame(avant.get(2), tournees().get(2));
        assertFalse(controleur.peutAnnuler());
        assertTrue(controleur.obtenirEtat() instanceof EtatTourneeCalculee);
    }

    /** Annuler le choix de la position ne change rien non plus. */
    @Test
    public void annulerLeChoixDeLaPositionNeChangeRien() throws Exception {
        calculerAvecDeuxLivreurs(1);
        Map<Integer, Tournee> avant = tournees();
        ecran.positionInsertion = Optional.empty();

        ecran.actionChangerLivreur.accept(1);

        assertEquals(1, demandes().get(1).livreur());
        assertSame(avant.get(1), tournees().get(1));
        assertSame(avant.get(2), tournees().get(2));
        assertFalse(controleur.peutAnnuler());
        assertTrue(controleur.obtenirEtat() instanceof EtatTourneeCalculee);
    }

    /** Choisir le livreur qu'a déjà la demande ne change rien. */
    @Test
    public void changerVersLeMemeLivreurNeChangeRien() throws Exception {
        calculerAvecDeuxLivreurs(1);
        Map<Integer, Tournee> avant = tournees();
        ecran.livreurChoisi = Optional.of(1);

        ecran.actionChangerLivreur.accept(1);

        assertSame(avant.get(1), tournees().get(1));
        assertFalse(controleur.peutAnnuler());
    }

    /** Une demande qui n'existe pas est ignorée, sans ouvrir de dialogue. */
    @Test
    public void changerLivreurPositionInvalideIgnoree() throws Exception {
        calculerAvecDeuxLivreurs(1);
        Map<Integer, Tournee> avant = tournees();

        ecran.actionChangerLivreur.accept(-1);
        ecran.actionChangerLivreur.accept(2);

        assertEquals(0, ecran.nombreDeChoixDeLivreur);
        assertSame(avant.get(1), tournees().get(1));
        assertFalse(controleur.peutAnnuler());
    }

    /**
     * Le recalcul des deux tournées tourne hors du thread de l'interface :
     * l'attente est affichée et les actions sont bloquées jusqu'au résultat.
     */
    @Test
    public void changerLivreurSeFaitHorsDuThreadInterface() throws Exception {
        utiliserLeVraiCalculEnArrierePlan();
        chargerDeuxDemandes();
        controleur.obtenirProgramme().definirNombreLivreurs(2);
        ecran.actionCalculer.run();
        executerArrierePlan();
        Map<Integer, Tournee> avant = tournees();

        ecran.actionChangerLivreur.accept(1);

        assertTrue(controleur.obtenirEtat() instanceof EtatCalculEnCours);
        assertTrue(ecran.attente);
        assertEquals(1, demandes().get(1).livreur());
        assertSame(avant.get(1), tournees().get(1));
        assertEquals(1, arrierePlan.size());

        executerArrierePlan();

        assertFalse(ecran.attente);
        assertEquals(2, demandes().get(1).livreur());
        assertTrue(controleur.obtenirEtat() instanceof EtatTourneeCalculee);
    }

    /** Avant le calcul des tournées, l'action est ignorée et inactive. */
    @Test
    public void changerLivreurIgnoreHorsTourneeCalculee() throws Exception {
        chargerDeuxDemandes();
        controleur.obtenirProgramme().definirNombreLivreurs(2);
        ecran.actionChangerLivreur.accept(0);
        assertEquals(0, ecran.nombreDeChoixDeLivreur);
        assertEquals(1, demandes().get(0).livreur());
        assertFalse(ecran.actions.contains(Action.CHANGER_LIVREUR));
    }

    /**
     * Avec un seul livreur, il n'y a personne à qui confier la demande : le
     * bouton est inactif, et l'action ignorée si elle arrive quand même.
     */
    @Test
    public void changerLivreurInactifAvecUnSeulLivreur() throws Exception {
        utiliserLeVraiCalcul();
        chargerDeuxDemandes();
        ecran.actionCalculer.run();
        Tournee avant = tournees().get(1);

        assertFalse(ecran.actions.contains(Action.CHANGER_LIVREUR));
        ecran.actionChangerLivreur.accept(0);

        assertEquals(0, ecran.nombreDeChoixDeLivreur);
        assertSame(avant, tournees().get(1));
    }

    /** Avec plusieurs livreurs, l'action est permise après le calcul. */
    @Test
    public void changerLivreurActifAvecPlusieursLivreurs() throws Exception {
        calculerAvecDeuxLivreurs(1);
        assertTrue(ecran.actions.contains(Action.CHANGER_LIVREUR));
    }
}
