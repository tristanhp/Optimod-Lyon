package fr.insa.optimod.controller;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executor;

import org.xml.sax.SAXException;

import fr.insa.optimod.algo.CalculateurTournee;
import fr.insa.optimod.model.Demande;
import fr.insa.optimod.model.Livraison;
import fr.insa.optimod.model.Passage;
import fr.insa.optimod.model.Plan;

/**
 * Contrôleur de la page principale : importe un plan XML dans le modèle,
 * demande à la vue de l'afficher, puis lance le calcul de la tournée quand
 * l'utilisateur clique sur « Calculer ».
 */
public final class MainController {

    /** Crée le contrôleur d'import. @param ecran écran à piloter */
    public MainController(MapScreen ecran) {
        this(ecran, (plan, livraison) -> {
            throw new IllegalStateException(
                    "Le calcul de la tournée n'est pas encore branché.");
        }, Runnable::run, Runnable::run);
    }

    /** @return livraison chargée, ou null avant son import */
    public Livraison obtenirLivraison() {
        return livraison;
    }

    /** Charge les demandes sans modifier le plan affiché. */
    private void importerLivraison(MapScreen ecran) {
        if (plan == null) {
            return;
        }
        File fichier = ecran.choisirFichierLivraison().orElse(null);
        if (fichier == null) {
            return;
        }
        try {
            Livraison nouvelle = new Livraison(fichier.getPath());
            verifierNoeudsConnus(nouvelle);
            setLivraison(nouvelle);
            ecran.afficherLivraisonChargee(nouvelle);
        } catch (IOException | SAXException | IllegalArgumentException erreur) {
            ecran.afficherErreurXml(erreur.getMessage());
        }
    }

    /** Refuse un lot dont une adresse n'est pas un noeud du plan chargé. */
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

    /** Calcul de la tournée. */
    private final CalculateurTournee calculateur;

    /** Exécute le calcul hors du thread de l'interface. */
    private final Executor arrierePlan;

    /** Exécute les mises à jour de la vue sur le thread de l'interface. */
    private final Executor interfaceUtilisateur;

    /** Met à jour l'état (actif ou non) du bouton « Calculer ». */
    private final Runnable actualiserBoutonCalculer;

    /** Plan actuellement chargé, ou {@code null}. */
    private Plan plan;

    /** Lot de requêtes actuellement chargé, ou {@code null}. */
    private Livraison livraison;

    /** Vrai pendant qu'un calcul est en cours. */
    private boolean calculEnCours;

    /**
     * Crée le contrôleur et le branche aux boutons « Importer » et
     * « Calculer ». Le bouton « Calculer » reste désactivé tant qu'un plan et
     * un lot de requêtes ne sont pas chargés.
     *
     * @param screen écran à piloter
     * @param calculateur calcul de la tournée
     * @param arrierePlan exécute le calcul hors du thread de l'interface
     * @param interfaceUtilisateur exécute les mises à jour de la vue sur le
     *        thread de l'interface
     */
    public MainController(MapScreen screen, CalculateurTournee calculateur,
            Executor arrierePlan, Executor interfaceUtilisateur) {
        this.calculateur = calculateur;
        this.arrierePlan = arrierePlan;
        this.interfaceUtilisateur = interfaceUtilisateur;
        this.actualiserBoutonCalculer = () -> screen.setCalculateEnabled(
                plan != null && livraison != null && !calculEnCours);
        screen.definirActionImportPlan(() -> importerPlan(screen));
        screen.definirActionImportLivraison(() -> importerLivraison(screen));
        screen.setOnCalculate(() -> calculer(screen));
        actualiserBoutonCalculer.run();
    }

    /**
     * Retourne le plan chargé.
     *
     * @return le plan, ou {@code null} si aucun plan n'est chargé
     */
    public Plan obtenirPlan() {
        return plan;
    }

    /**
     * Définit le lot de requêtes à livrer (importé au Cas 3) et autorise le
     * calcul si un plan est chargé.
     *
     * @param nouvelleLivraison lot de requêtes, ou {@code null} pour l'oublier
     */
    public void setLivraison(Livraison nouvelleLivraison) {
        livraison = nouvelleLivraison;
        actualiserBoutonCalculer.run();
    }

    /**
     * Fait choisir un fichier XML, le lit puis affiche la carte. Si le fichier
     * est invalide, une fenêtre d'information s'ouvre et la page est vidée.
     * Si l'utilisateur annule le choix, rien ne change. Un nouveau plan rend
     * le lot de requêtes précédent caduc.
     */
    private void importerPlan(MapScreen screen) {
        File fichier = screen.choisirFichierPlan().orElse(null);
        if (fichier == null) {
            return;
        }
        try {
            plan = new Plan(fichier.getPath());
            livraison = null;
            screen.afficherPlan(plan);
        } catch (IOException | SAXException | IllegalArgumentException e) {
            screen.afficherErreurXml(e.getMessage());
            plan = null;
            livraison = null;
            screen.reinitialiser();
        }
        actualiserBoutonCalculer.run();
    }

    /**
     * Lance le calcul de la tournée hors du thread de l'interface, avec un
     * état d'attente, puis affiche le résultat ou l'erreur.
     */
    private void calculer(MapScreen screen) {
        if (plan == null || livraison == null || calculEnCours) {
            return;
        }
        Plan planCalcule = plan;
        Livraison lotCalcule = livraison;
        calculEnCours = true;
        actualiserBoutonCalculer.run();
        screen.showWaiting(true);
        arrierePlan.execute(() -> {
            try {
                List<Passage> tournee = calculateur.calculer(planCalcule,
                        lotCalcule);
                interfaceUtilisateur.execute(() -> finirCalcul(screen,
                        planCalcule, lotCalcule, tournee, null));
            } catch (RuntimeException e) {
                String detail = e.getMessage() != null ? e.getMessage()
                        : e.getClass().getSimpleName();
                interfaceUtilisateur.execute(() -> finirCalcul(screen,
                        planCalcule, lotCalcule, null, detail));
            }
        });
    }

    /**
     * Termine le calcul sur le thread de l'interface. Le résultat est
     * ignoré si un autre plan a été chargé entre-temps.
     */
    private void finirCalcul(MapScreen screen, Plan planCalcule,
            Livraison lotCalcule, List<Passage> tournee, String erreur) {
        calculEnCours = false;
        screen.showWaiting(false);
        actualiserBoutonCalculer.run();
        if (planCalcule != plan || lotCalcule != livraison) {
            return;
        }
        if (tournee != null) {
            screen.showTour(tournee);
        } else {
            screen.showCalculationError(erreur);
        }
    }
}
