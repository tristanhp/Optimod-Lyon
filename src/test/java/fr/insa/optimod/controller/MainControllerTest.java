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
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import fr.insa.optimod.algo.CalculateurTournee;
import fr.insa.optimod.controller.MapScreen.Action;
import fr.insa.optimod.model.Demande;
import fr.insa.optimod.model.Etape;
import fr.insa.optimod.model.Livraison;
import fr.insa.optimod.model.Passage;
import fr.insa.optimod.model.Plan;

public class MainControllerTest {

    /** Écran factice qui enregistre ce que le contrôleur lui demande. */
    private static final class FakeScreen implements MapScreen {
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
        private Livraison livraisonAffichee;
        private Set<Action> actions;
        private boolean attente;
        private List<Passage> tournee;
        private String erreurCalcul;

        @Override
        public void definirActionImportPlan(Runnable action) {
            actionImportPlan = action;
        }

        @Override
        public void definirActionImportLivraison(Runnable action) {
            actionImportLivraison = action;
        }

        @Override
        public void setOnCalculate(Runnable action) {
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
        public Optional<File> choisirFichierLivraison() {
            return Optional.ofNullable(fichierChoisi);
        }

        @Override
        public void afficherPlan(Plan plan) {
            planAffiche = plan;
            evenements.add("plan");
        }

        @Override
        public void afficherLivraisonChargee(Livraison livraison) {
            livraisonAffichee = livraison;
            evenements.add("livraison chargée");
        }

        @Override
        public void afficherErreurXml(String detail) {
            evenements.add("invalide");
        }

        @Override
        public void reinitialiser() {
            planAffiche = null;
            livraisonAffichee = null;
            evenements.add("reinitialiser");
        }

        @Override
        public void showWaiting(boolean enAttente) {
            attente = enAttente;
            evenements.add("attente:" + enAttente);
        }

        @Override
        public void showTour(List<Passage> passages) {
            tournee = passages;
            evenements.add("tournee");
        }

        @Override
        public void showCalculationError(String detail) {
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

    private static final List<Passage> TOURNEE = List.of(new Passage(
            new Etape(1, 0), LocalTime.of(8, 0), LocalTime.of(8, 0)));

    /** Actions permises avec des demandes chargées, sans historique. */
    private static final Set<Action> MODIFIABLE = EnumSet.of(
            Action.IMPORTER_PLAN, Action.IMPORTER_LIVRAISON, Action.CALCULER,
            Action.AJOUTER_DEMANDE, Action.SUPPRIMER_DEMANDE);

    private FakeScreen ecran;
    private MainController controleur;
    private final Queue<Runnable> arrierePlan = new ArrayDeque<>();
    private Plan planRecu;
    private Livraison lotRecu;
    private RuntimeException erreurDeCalcul;

    @Before
    public void creer() {
        ecran = new FakeScreen();
        CalculateurTournee calcul = (plan, livraison) -> {
            planRecu = plan;
            lotRecu = livraison;
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

    private void importerLivraison(File fichier) {
        ecran.fichierChoisi = fichier;
        ecran.actionImportLivraison.run();
    }

    /** Charge petitPlan.xml puis demandePetit1.xml, et vide les événements. */
    private void chargerPlanEtLivraison() throws Exception {
        importerPlan(ressource("/data/petitPlan.xml"));
        importerLivraison(ressource("/data/demandePetit1.xml"));
        ecran.evenements.clear();
    }

    /** Charge petitPlan.xml puis demandePetit2.xml (2 demandes). */
    private void chargerDeuxDemandes() throws Exception {
        importerPlan(ressource("/data/petitPlan.xml"));
        importerLivraison(ressource("/data/demandePetit2.xml"));
        ecran.evenements.clear();
    }

    private List<Demande> demandes() {
        return controleur.obtenirLivraison().obtenirDemandes();
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
        assertEquals(EnumSet.of(Action.IMPORTER_PLAN), ecran.actions);
        assertNull(controleur.obtenirPlan());
        assertTrue(ecran.evenements.isEmpty());
    }

    @Test
    public void livraisonSansPlanIgnoree() {
        ecran.actionImportLivraison.run();
        ecran.actionCalculer.run();
        assertNull(controleur.obtenirLivraison());
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
                Action.IMPORTER_LIVRAISON), ecran.actions);
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
        assertNull(controleur.obtenirLivraison());
        assertTrue(controleur.obtenirEtat() instanceof EtatPlanCharge);
        assertEquals(EnumSet.of(Action.IMPORTER_PLAN,
                Action.IMPORTER_LIVRAISON), ecran.actions);
    }

    @Test
    public void planInvalideOublieLaLivraison() throws Exception {
        chargerPlanEtLivraison();
        importerPlan(ecrire("<reseau>"));
        assertNull(controleur.obtenirLivraison());
        assertNull(controleur.obtenirPlan());
        assertTrue(controleur.obtenirEtat() instanceof EtatInitial);
    }

    // --- Import des demandes ---

    @Test
    public void importeLesDemandesEtActiveLeCalcul() throws Exception {
        importerPlan(ressource("/data/petitPlan.xml"));
        importerLivraison(ressource("/data/demandePetit1.xml"));
        assertEquals(List.of("plan", "livraison chargée"), ecran.evenements);
        assertEquals(1, controleur.obtenirLivraison().obtenirDemandes().size());
        assertEquals("8:0:0",
                controleur.obtenirLivraison().obtenirHeureDepart());
        assertSame(controleur.obtenirLivraison(), ecran.livraisonAffichee);
        assertTrue(controleur.obtenirEtat() instanceof EtatLivraisonChargee);
        assertEquals(MODIFIABLE, ecran.actions);
    }

    @Test
    public void livraisonHorsPlanRefusee() throws Exception {
        importerPlan(ecrire(PLAN_MINUSCULE));
        importerLivraison(ressource("/data/demandePetit1.xml"));
        assertEquals(List.of("plan", "invalide"), ecran.evenements);
        assertNull(controleur.obtenirLivraison());
        assertNull(ecran.livraisonAffichee);
        assertTrue(controleur.obtenirEtat() instanceof EtatPlanCharge);
    }

    @Test
    public void livraisonInvalideConserveLesDemandes() throws Exception {
        chargerPlanEtLivraison();
        Livraison precedente = controleur.obtenirLivraison();
        importerLivraison(ecrire(PLAN_MINUSCULE));
        assertEquals(List.of("invalide"), ecran.evenements);
        assertSame(precedente, controleur.obtenirLivraison());
        assertNotNull(controleur.obtenirPlan());
        assertTrue(controleur.obtenirEtat() instanceof EtatLivraisonChargee);
    }

    @Test
    public void annulerLImportDesDemandesLesConserve() throws Exception {
        chargerPlanEtLivraison();
        Livraison precedente = controleur.obtenirLivraison();
        importerLivraison(null);
        assertSame(precedente, controleur.obtenirLivraison());
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
        assertSame(controleur.obtenirLivraison(), lotRecu);
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
        Livraison livraison = controleur.obtenirLivraison();
        ecran.actionCalculer.run();
        importerPlan(ecrire(PLAN_MINUSCULE));
        importerLivraison(ressource("/data/demandePetit1.xml"));
        assertSame(plan, controleur.obtenirPlan());
        assertSame(livraison, controleur.obtenirLivraison());
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
        assertTrue(controleur.obtenirEtat() instanceof EtatLivraisonChargee);
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
    public void controleurSansCalculAfficheUneErreur() throws Exception {
        controleur = new MainController(ecran);
        chargerPlanEtLivraison();
        ecran.actionCalculer.run();
        assertEquals("Le calcul de la tournée n'est pas encore branché.",
                ecran.erreurCalcul);
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
                Action.IMPORTER_LIVRAISON, Action.CALCULER,
                Action.AJOUTER_DEMANDE, Action.SUPPRIMER_DEMANDE,
                Action.ANNULER), ecran.actions);

        ecran.actionAnnuler.run();
        assertEquals(avant, demandes());
        assertTrue(ecran.actions.contains(Action.REFAIRE));
        assertFalse(ecran.actions.contains(Action.ANNULER));

        ecran.actionRefaire.run();
        assertEquals(List.of(avant.get(1)), demandes());
        assertTrue(controleur.obtenirEtat() instanceof EtatLivraisonChargee);
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
        assertTrue(controleur.obtenirEtat() instanceof EtatLivraisonChargee);
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
        assertTrue(controleur.obtenirEtat() instanceof EtatLivraisonChargee);
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
        importerLivraison(ressource("/data/demandePetit2.xml"));
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

        ecran.actionClicNoeud.accept(11L);
        assertTrue(controleur.obtenirEtat() instanceof EtatAjoutLivraison);
        assertEquals(EtatAjoutLivraison.CONSIGNE, ecran.consigne);

        ecran.actionClicNoeud.accept(22L);
        assertEquals(3, demandes().size());
        Demande ajoutee = demandes().get(2);
        assertEquals(11L, ajoutee.obtenirIdentifiantCollecte());
        assertEquals(22L, ajoutee.obtenirIdentifiantLivraison());
        assertEquals(EtatAjoutLivraison.DUREE_PAR_DEFAUT,
                ajoutee.obtenirDureeCollecte());
        assertEquals("", ecran.consigne);
        assertTrue(controleur.obtenirEtat() instanceof EtatLivraisonChargee);

        ecran.actionAnnuler.run();
        assertEquals(2, demandes().size());
    }

    @Test
    public void livraisonAuMemeEndroitQueLaCollecteRefusee() throws Exception {
        chargerDeuxDemandes();
        ecran.actionAjouter.run();
        ecran.actionClicNoeud.accept(11L);
        ecran.actionClicNoeud.accept(11L);
        assertEquals(2, demandes().size());
        assertTrue(controleur.obtenirEtat() instanceof EtatAjoutLivraison);
        assertTrue(ecran.consigne.startsWith("La livraison doit"));
    }

    @Test
    public void unSecondClicSurAjouterAnnuleLAjout() throws Exception {
        chargerDeuxDemandes();
        ecran.actionAjouter.run();
        ecran.actionClicNoeud.accept(11L);
        ecran.actionAjouter.run();
        assertTrue(controleur.obtenirEtat() instanceof EtatLivraisonChargee);
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
        importerLivraison(ressource("/data/demandePetit1.xml"));
        assertSame(plan, controleur.obtenirPlan());
        assertEquals(2, demandes().size());
    }

    @Test
    public void clicSurLaCarteHorsAjoutIgnore() throws Exception {
        chargerDeuxDemandes();
        ecran.actionClicNoeud.accept(11L);
        assertEquals(2, demandes().size());
        assertTrue(controleur.obtenirEtat() instanceof EtatLivraisonChargee);
    }
}
