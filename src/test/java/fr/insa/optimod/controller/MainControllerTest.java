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

    /** Nombre de calculs complets (voyageur de commerce) lancés. */
    private int nombreDeCalculs;

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

    /**
     * Remplace le contrôleur par un contrôleur qui fait le vrai calcul de
     * {@link Plan#calculerTournee}, sur le thread appelant, en comptant les
     * calculs complets lancés (donc les passages par le voyageur de commerce).
     */
    private void utiliserLeVraiCalcul() {
        controleur = new MainController(ecran, (plan, programme) -> {
            nombreDeCalculs++;
            return plan.calculerTournee(programme);
        }, Runnable::run, Runnable::run);
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
     * le recalcul local est impossible, on retombe sur le comportement
     * d'avant (la tournée affichée est retirée) et Annuler la retrouve dans le
     * cache.
     */
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

    // --- Suppression d'une demande avec une tournée affichée (TIG-101) ---

    /**
     * Supprimer une demande quand la tournée est affichée met la tournée à
     * jour tout de suite, sans relancer le voyageur de commerce, et garde
     * l'ordre des autres arrêts.
     */
    @Test
    public void supprimerAvecTourneeAfficheeNeRelancePasLeTsp()
            throws Exception {
        utiliserLeVraiCalcul();
        chargerDeuxDemandes();
        ecran.actionCalculer.run();
        Tournee complete = ecran.tournee;
        assertEquals(1, nombreDeCalculs);
        assertEquals(6, complete.arrets().size());
        Demande supprimee = demandes().get(0);
        ecran.evenements.clear();

        ecran.actionSupprimer.accept(0);

        assertEquals("pas de nouveau calcul complet", 1, nombreDeCalculs);
        assertTrue(controleur.obtenirEtat() instanceof EtatTourneeCalculee);
        assertEquals(List.of("tournee"), ecran.evenements);
        List<Noeud> attendus = noeudsDe(complete);
        attendus.remove(supprimee.collecte());
        attendus.remove(supprimee.livraison());
        assertEquals(attendus, noeudsDe(ecran.tournee));
        assertEquals(MODIFIABLE_APRES_SUPPRESSION, ecran.actions);
    }

    /**
     * Annuler réaffiche la tournée complète et Rétablir la tournée recalculée,
     * toutes deux depuis le cache : aucun calcul n'est relancé.
     */
    @Test
    public void annulerEtRetablirApresSuppressionLocaleSansCalcul()
            throws Exception {
        utiliserLeVraiCalcul();
        chargerDeuxDemandes();
        ecran.actionCalculer.run();
        Tournee complete = ecran.tournee;
        ecran.actionSupprimer.accept(0);
        Tournee reduite = ecran.tournee;

        ecran.actionAnnuler.run();
        assertSame(complete, ecran.tournee);
        assertEquals(2, demandes().size());
        ecran.actionRefaire.run();
        assertSame(reduite, ecran.tournee);
        assertEquals(1, demandes().size());
        assertEquals(1, nombreDeCalculs);
        assertTrue(controleur.obtenirEtat() instanceof EtatTourneeCalculee);
    }

    /** Une position hors de la liste est ignorée, la tournée reste affichée. */
    @Test
    public void supprimerUnePositionInvalideAvecTourneeAfficheeEstIgnore()
            throws Exception {
        utiliserLeVraiCalcul();
        chargerDeuxDemandes();
        ecran.actionCalculer.run();
        Tournee complete = ecran.tournee;
        ecran.actionSupprimer.accept(-1);
        ecran.actionSupprimer.accept(2);
        assertEquals(2, demandes().size());
        assertSame(complete, ecran.tournee);
        assertTrue(controleur.obtenirEtat() instanceof EtatTourneeCalculee);
        assertEquals(1, nombreDeCalculs);
    }

    /**
     * Si la tournée des demandes restantes a déjà été calculée par le voyageur
     * de commerce, elle est réaffichée telle quelle : le recalcul local ne
     * l'écrase pas.
     */
    @Test
    public void tourneeDuTspDejaConnueNEstPasEcrasee() throws Exception {
        utiliserLeVraiCalcul();
        chargerDeuxDemandes();
        ecran.actionCalculer.run();
        ecran.actionSupprimer.accept(0);
        ecran.actionCalculer.run(); // calcul complet des demandes restantes
        Tournee duTsp = ecran.tournee;
        assertEquals(2, nombreDeCalculs);

        ecran.actionAnnuler.run();
        ecran.actionSupprimer.accept(0);

        assertSame(duTsp, ecran.tournee);
        assertEquals(2, nombreDeCalculs);
    }
}
