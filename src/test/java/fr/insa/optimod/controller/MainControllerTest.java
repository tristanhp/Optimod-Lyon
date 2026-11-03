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
        private Tournee tournee;
        private String erreurCalcul;
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
        public void effacerTournee() {
            tournee = null;
            evenements.add("effacer tournee");
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
        public void afficherTournee(Tournee passages) {
            tournee = passages;
            evenements.add("tournee");
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

    private static final Tournee TOURNEE = new Tournee(List.of(
            new Tournee.Arret(new Noeud(1, 4.85, 45.75), LocalTime.of(8, 0),
                    LocalTime.of(8, 0))), List.of());

    /** Deux nœuds du petit plan fourni, pour ajouter une demande. */
    private static final long NOEUD_A = 25175791L;
    private static final long NOEUD_B = 2129259178L;

    /** Actions permises avec des demandes chargées, sans historique. */
    private static final Set<Action> MODIFIABLE = EnumSet.of(
            Action.IMPORTER_PLAN, Action.IMPORTER_PROGRAMME, Action.CALCULER,
            Action.AJOUTER_DEMANDE, Action.SUPPRIMER_DEMANDE, Action.RESTAURER);

    private EcranFactice ecran;
    private MainController controleur;
    private final Queue<Runnable> arrierePlan = new ArrayDeque<>();
    private Plan planRecu;
    private ProgrammeLivraisons lotRecu;
    private RuntimeException erreurDeCalcul;

    @Before
    public void creer() {
        ecran = new EcranFactice();
        BiFunction<Plan, ProgrammeLivraisons, Tournee> calcul =
                (plan, programme) -> {
            planRecu = plan;
            lotRecu = programme;
            if (erreurDeCalcul != null) {
                throw erreurDeCalcul;
            }
            return TOURNEE;
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

    private void executerArrierePlan() {
        while (!arrierePlan.isEmpty()) {
            arrierePlan.remove().run();
        }
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
        assertNull(ecran.tournee);
    }

    @Test
    public void calculAfficheLaTournee() throws Exception {
        chargerPlanEtLivraison();
        ecran.actionCalculer.run();
        executerArrierePlan();
        assertEquals(List.of("attente:true", "attente:false", "tournee"),
                ecran.evenements);
        assertSame(TOURNEE, ecran.tournee);
        assertSame(controleur.obtenirPlan(), planRecu);
        assertSame(controleur.obtenirProgramme(), lotRecu);
        assertTrue(controleur.obtenirEtat() instanceof EtatTourneeCalculee);
        Set<Action> permises = EnumSet.copyOf(MODIFIABLE);
        permises.add(Action.SAUVEGARDER);
        assertEquals(permises, ecran.actions);
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
        assertSame(TOURNEE, ecran.tournee);
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
        assertNull(ecran.tournee);
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
        assertNotNull(ecran.tournee);
        assertEquals(4, ecran.tournee.arrets().size());
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

    @Test
    public void modifierRetireLaTourneeEtAnnulerLaRetrouveDuCache()
            throws Exception {
        chargerDeuxDemandes();
        ecran.actionCalculer.run();
        executerArrierePlan();
        ecran.evenements.clear();

        ecran.actionSupprimer.accept(1);
        assertTrue(controleur.obtenirEtat() instanceof EtatProgrammeCharge);
        assertNull(ecran.tournee);

        ecran.actionAnnuler.run();
        assertTrue(controleur.obtenirEtat() instanceof EtatTourneeCalculee);
        assertSame(TOURNEE, ecran.tournee);
        assertTrue("pas de nouveau calcul", arrierePlan.isEmpty());
        assertEquals(List.of("effacer tournee", "tournee"), ecran.evenements);
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
        assertSame(TOURNEE, ecran.tournee);
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
        controleur = new MainController(ecran, Plan::calculerTournee,
                Runnable::run, Runnable::run);
        chargerDeuxDemandes();
        ecran.actionCalculer.run();
        Tournee avant = ecran.tournee;
        ecran.actionAjouter.run();
        ecran.actionClicNoeud.accept(NOEUD_A);
        ecran.actionAnnuler.run();
        assertNull(ecran.collecteProvisoire);
        assertNull(ecran.livraisonProvisoire);
        assertSame(avant, ecran.tournee);
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
            return plan.calculerTournee(programme);
        }, arrierePlan::add, Runnable::run);
        chargerDeuxDemandes();
        ecran.actionCalculer.run();
        arrierePlan.remove().run();
        Tournee avant = ecran.tournee;
        ecran.positionInsertion = Optional.of(avant.arrets().size() - 1);
        ecran.actionAjouter.run();
        ecran.actionClicNoeud.accept(NOEUD_A);
        ecran.actionClicNoeud.accept(NOEUD_B);
        assertEquals(2, demandes().size());
        assertTrue(ecran.actions.isEmpty());
        arrierePlan.remove().run();
        Tournee apres = ecran.tournee;
        assertEquals(3, demandes().size());
        assertEquals(avant.arrets().size() + 2, apres.arrets().size());
        int position = avant.arrets().size() - 1;
        assertEquals(NOEUD_A,
                apres.arrets().get(position).noeud().identifiant());
        assertEquals(NOEUD_B,
                apres.arrets().get(position + 1).noeud().identifiant());
        assertEquals(1, appels[0]);
        ecran.actionAnnuler.run();
        assertSame(avant, ecran.tournee);
        ecran.actionRefaire.run();
        assertSame(apres, ecran.tournee);
        assertEquals(1, appels[0]);
    }

    /** Annuler le choix de position ne crée ni demande ni calcul. */
    @Test
    public void annulerPositionConserveTournee() throws Exception {
        controleur = new MainController(ecran, Plan::calculerTournee,
                arrierePlan::add, Runnable::run);
        chargerDeuxDemandes();
        ecran.actionCalculer.run();
        arrierePlan.remove().run();
        Tournee avant = ecran.tournee;
        ecran.positionInsertion = Optional.empty();
        ecran.actionAjouter.run();
        ecran.actionClicNoeud.accept(NOEUD_A);
        ecran.actionClicNoeud.accept(NOEUD_B);
        assertSame(avant, ecran.tournee);
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
        controleur = new MainController(ecran, Plan::calculerTournee,
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
        controleur = new MainController(ecran, Plan::calculerTournee,
                arrierePlan::add, Runnable::run);
        chargerDeuxDemandes();
        ecran.actionCalculer.run();
        arrierePlan.remove().run();
        Tournee avant = ecran.tournee;
        ecran.saisie = Optional.of(new MapScreen.SaisieDemande("300", "5", 1));
        ecran.actionAjouter.run();
        ecran.actionClicNoeud.accept(NOEUD_A);
        ecran.actionClicNoeud.accept(NOEUD_B);
        arrierePlan.remove().run();
        assertSame(avant, ecran.tournee);
        assertEquals(2, demandes().size());
        assertFalse(controleur.peutAnnuler());
        assertNotNull(ecran.erreurCalcul);
        assertFalse(ecran.attente);
        assertNull(ecran.collecteProvisoire);
        assertNull(ecran.livraisonProvisoire);
    }

    /** La liste des dépassements est transmise à l'écran. */
    @Test
    public void signalementHorsPlage() {
        Tournee.Arret arret = new Tournee.Arret(new Noeud(1, 0, 0),
                LocalTime.of(11, 0), LocalTime.of(11, 5),
                LocalTime.of(8, 0), LocalTime.of(10, 0));
        controleur.afficherTournee(new Tournee(List.of(arret), List.of()));
        assertEquals(List.of(arret), ecran.horsPlage);
        controleur.afficherTournee(TOURNEE);
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
        Tournee avant = ecran.tournee;
        File fichier = dossier.newFile("sauvegarde.xml");
        ecran.fichierChoisi = fichier;
        ecran.actionSauvegarder.run();
        assertTrue(fichier.length() > 0);
        ecran.actionSupprimer.accept(0);
        ecran.actionRestaurer.run();
        assertEquals(avant, ecran.tournee);
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
}
