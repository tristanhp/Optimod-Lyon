package fr.insa.optimod.controller;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import org.xml.sax.SAXException;

import fr.insa.optimod.algo.CalculateurTournee;
import fr.insa.optimod.model.Demande;
import fr.insa.optimod.model.Livraison;
import fr.insa.optimod.model.Passage;
import fr.insa.optimod.model.Plan;

/**
 * Contrôleur de la page principale : importe un plan puis des demandes XML
 * dans le modèle, demande à la vue de les afficher, puis lance le calcul de la
 * tournée quand l'utilisateur clique sur « Calculer ».
 *
 * <p>Les clics sont délégués à l'état courant ({@link Etat}, design pattern
 * State) : c'est lui qui décide si l'action est permise et quel est l'état
 * suivant. Le contrôleur fournit aux états les opérations élémentaires (lire
 * un fichier, lancer le calcul, mettre à jour l'écran).
 *
 * <p>Les modifications des demandes passent par des commandes annulables
 * ({@link Commande}, design pattern Command). Les tournées déjà calculées sont
 * gardées en cache par liste de demandes : annuler ou rétablir une
 * modification réaffiche la tournée correspondante sans la recalculer.
 */
public final class MainController {

    /** Issue de la lecture d'un fichier choisi par l'utilisateur. */
    enum Lecture {
        /** L'utilisateur a annulé le choix du fichier. */
        ANNULEE,
        /** Le fichier a été lu et le modèle mis à jour. */
        REUSSIE,
        /** Le fichier est invalide ; l'erreur a été affichée. */
        ECHOUEE
    }

    /** Écran piloté. */
    private final MapScreen ecran;

    /** Calcul de la tournée. */
    private final CalculateurTournee calculateur;

    /** Exécute le calcul hors du thread de l'interface. */
    private final Executor arrierePlan;

    /** Exécute les mises à jour de la vue sur le thread de l'interface. */
    private final Executor interfaceUtilisateur;

    /** État courant. */
    private Etat etat;

    /** Plan actuellement chargé, ou {@code null}. */
    private Plan plan;

    /** Demandes actuellement chargées, ou {@code null}. */
    private Livraison livraison;

    /** Modifications des demandes, pour annuler et rétablir. */
    private final ListeDeCommandes historique = new ListeDeCommandes();

    /**
     * Tournées déjà calculées pour le plan et les demandes chargés, indexées
     * par la liste des demandes (dans l'ordre).
     */
    private final Map<List<Demande>, List<Passage>> tourneesCalculees =
            new HashMap<>();

    /**
     * Crée un contrôleur sans calcul de tournée (le clic sur « Calculer »
     * affiche une erreur).
     *
     * @param ecran écran à piloter
     */
    public MainController(MapScreen ecran) {
        this(ecran, (plan, livraison) -> {
            throw new IllegalStateException(
                    "Le calcul de la tournée n'est pas encore branché.");
        }, Runnable::run, Runnable::run);
    }

    /**
     * Crée le contrôleur, le branche aux boutons de l'écran et le place dans
     * l'état initial (seul l'import d'un plan est possible).
     *
     * @param ecran écran à piloter
     * @param calculateur calcul de la tournée
     * @param arrierePlan exécute le calcul hors du thread de l'interface
     * @param interfaceUtilisateur exécute les mises à jour de la vue sur le
     *        thread de l'interface
     */
    public MainController(MapScreen ecran, CalculateurTournee calculateur,
            Executor arrierePlan, Executor interfaceUtilisateur) {
        this.ecran = ecran;
        this.calculateur = calculateur;
        this.arrierePlan = arrierePlan;
        this.interfaceUtilisateur = interfaceUtilisateur;
        ecran.definirActionImportPlan(() -> etat.importerPlan(this));
        ecran.definirActionImportLivraison(() -> etat.importerLivraison(this));
        ecran.setOnCalculate(() -> etat.calculer(this));
        ecran.definirActionAjouterDemande(() -> etat.ajouterDemande(this));
        ecran.definirActionSupprimerDemande(
                position -> etat.supprimerDemande(this, position));
        ecran.definirActionClicNoeud(
                idNoeud -> etat.cliquerNoeud(this, idNoeud));
        ecran.definirActionAnnuler(() -> etat.annuler(this));
        ecran.definirActionRefaire(() -> etat.refaire(this));
        changerEtat(new EtatInitial());
    }

    /** @return plan chargé, ou {@code null} si aucun plan n'est chargé */
    public Plan obtenirPlan() {
        return plan;
    }

    /** @return demandes chargées, ou {@code null} avant leur import */
    public Livraison obtenirLivraison() {
        return livraison;
    }

    /** @return état courant (pour les tests) */
    Etat obtenirEtat() {
        return etat;
    }

    /**
     * Passe dans un nouvel état et met à jour les boutons de l'écran.
     *
     * @param nouvelEtat état suivant
     */
    void changerEtat(Etat nouvelEtat) {
        etat = nouvelEtat;
        ecran.activerActions(etat.actionsPermises(this));
    }

    /**
     * Fait choisir un plan XML et l'affiche. Un nouveau plan rend les demandes
     * précédentes caduques ; un plan invalide ouvre la fenêtre d'erreur et
     * vide la page.
     *
     * @return l'issue de la lecture
     */
    Lecture lirePlan() {
        File fichier = ecran.choisirFichierPlan().orElse(null);
        if (fichier == null) {
            return Lecture.ANNULEE;
        }
        livraison = null;
        oublierModifications();
        try {
            plan = new Plan(fichier.getPath());
            ecran.afficherPlan(plan);
            return Lecture.REUSSIE;
        } catch (IOException | SAXException | IllegalArgumentException e) {
            plan = null;
            ecran.afficherErreurXml(e.getMessage());
            ecran.reinitialiser();
            return Lecture.ECHOUEE;
        }
    }

    /**
     * Fait choisir des demandes XML et les affiche, sans modifier le plan. Des
     * demandes invalides ou hors du plan ouvrent la fenêtre d'erreur et les
     * demandes précédentes sont conservées.
     *
     * @return l'issue de la lecture
     */
    Lecture lireLivraison() {
        File fichier = ecran.choisirFichierLivraison().orElse(null);
        if (fichier == null) {
            return Lecture.ANNULEE;
        }
        try {
            Livraison nouvelle = new Livraison(fichier.getPath());
            verifierNoeudsConnus(nouvelle);
            livraison = nouvelle;
            oublierModifications();
            ecran.afficherLivraisonChargee(nouvelle);
            return Lecture.REUSSIE;
        } catch (IOException | SAXException | IllegalArgumentException e) {
            ecran.afficherErreurXml(e.getMessage());
            return Lecture.ECHOUEE;
        }
    }

    /** Refuse des demandes dont une adresse n'est pas un noeud du plan. */
    private void verifierNoeudsConnus(Livraison lot) {
        verifierNoeudConnu(lot.obtenirEntrepot().obtenirIdentifiant());
        for (Demande demande : lot.obtenirDemandes()) {
            verifierNoeudConnu(demande.obtenirIdentifiantCollecte());
            verifierNoeudConnu(demande.obtenirIdentifiantLivraison());
        }
    }

    private void verifierNoeudConnu(long identifiant) {
        if (plan.obtenirNoeud(identifiant) == null) {
            throw new IllegalArgumentException("Adresse " + identifiant
                    + " absente du plan chargé");
        }
    }

    /**
     * Affiche l'attente et lance le calcul de la tournée hors du thread de
     * l'interface. Le résultat est transmis à l'état courant
     * ({@link Etat#finirCalcul}) sur le thread de l'interface.
     */
    void lancerCalcul() {
        Plan planCalcule = plan;
        Livraison lotCalcule = livraison;
        List<Demande> demandesCalculees = List.copyOf(
                livraison.obtenirDemandes());
        ecran.showWaiting(true);
        arrierePlan.execute(() -> {
            try {
                List<Passage> tournee = calculateur.calculer(planCalcule,
                        lotCalcule);
                interfaceUtilisateur.execute(() -> {
                    tourneesCalculees.put(demandesCalculees, tournee);
                    etat.finirCalcul(this, tournee, null);
                });
            } catch (RuntimeException e) {
                String detail = e.getMessage() != null ? e.getMessage()
                        : e.getClass().getSimpleName();
                interfaceUtilisateur.execute(
                        () -> etat.finirCalcul(this, null, detail));
            }
        });
    }

    /** Retire l'indicateur d'attente du calcul. */
    void arreterAttente() {
        ecran.showWaiting(false);
    }

    /** @param tournee tournée calculée à afficher */
    void afficherTournee(List<Passage> tournee) {
        ecran.showTour(tournee);
    }

    /** @param detail explication de l'échec du calcul */
    void afficherErreurCalcul(String detail) {
        ecran.showCalculationError(detail);
    }

    /** @param consigne consigne à afficher, vide pour l'effacer */
    void afficherConsigne(String consigne) {
        ecran.afficherConsigne(consigne);
    }

    /**
     * Exécute une modification des demandes et l'ajoute à l'historique.
     *
     * @param commande modification à exécuter
     */
    void executer(Commande commande) {
        historique.executer(commande);
        retrouverEtatLivraison();
    }

    /** Annule la dernière modification des demandes. */
    void annulerCommande() {
        historique.annuler();
        retrouverEtatLivraison();
    }

    /** Rétablit la dernière modification annulée. */
    void refaireCommande() {
        historique.refaire();
        retrouverEtatLivraison();
    }

    /** @return vrai s'il reste une modification à annuler */
    boolean peutAnnuler() {
        return historique.peutAnnuler();
    }

    /** @return vrai s'il reste une modification annulée à rétablir */
    boolean peutRefaire() {
        return historique.peutRefaire();
    }

    /**
     * Revient à l'état qui correspond aux demandes actuelles : si leur tournée
     * a déjà été calculée, elle est réaffichée depuis le cache
     * ({@link EtatTourneeCalculee}) ; sinon la tournée est retirée
     * ({@link EtatLivraisonChargee}).
     */
    void retrouverEtatLivraison() {
        ecran.afficherConsigne("");
        List<Passage> tournee = tourneesCalculees.get(
                List.copyOf(livraison.obtenirDemandes()));
        if (tournee != null) {
            changerEtat(new EtatTourneeCalculee());
            ecran.showTour(tournee);
        } else {
            changerEtat(new EtatLivraisonChargee());
            ecran.effacerTournee();
        }
    }

    /** Oublie l'historique et le cache : le plan ou les demandes changent. */
    private void oublierModifications() {
        historique.vider();
        tourneesCalculees.clear();
    }
}
