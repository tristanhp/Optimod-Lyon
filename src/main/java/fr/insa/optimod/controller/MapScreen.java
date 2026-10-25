package fr.insa.optimod.controller;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

import fr.insa.optimod.model.Livraison;
import fr.insa.optimod.model.Passage;
import fr.insa.optimod.model.Plan;

/**
 * Ce que le contrôleur attend de l'écran de la carte. Séparer cette interface
 * de la vue JavaFX permet de tester le contrôleur sans afficher de fenêtre.
 */
public interface MapScreen {

    /** Actions de l'utilisateur que l'écran peut activer ou désactiver. */
    enum Action {
        /** Bouton « Importer le plan ». */
        IMPORTER_PLAN,
        /** Bouton « Importer la livraison ». */
        IMPORTER_LIVRAISON,
        /** Bouton « Calculer ». */
        CALCULER,
        /** Bouton « Ajouter une demande » (ou annuler l'ajout en cours). */
        AJOUTER_DEMANDE,
        /** Bouton « Supprimer la demande » (demande sélectionnée). */
        SUPPRIMER_DEMANDE,
        /** Bouton « Annuler » (Ctrl+Z). */
        ANNULER,
        /** Bouton « Rétablir » (Ctrl+Y). */
        REFAIRE
    }

    /** Définit l'action d'import des demandes. @param action action au clic */
    void definirActionImportLivraison(Runnable action);

    /** @return fichier XML de demandes choisi, ou vide après annulation */
    Optional<File> choisirFichierLivraison();

    /**
     * Affiche les demandes chargées sur la carte et dans une liste.
     *
     * @param livraison demandes et entrepôt chargés
     */
    void afficherLivraisonChargee(Livraison livraison);


    /**
     * Définit l'action déclenchée par le bouton « Importer ».
     *
     * @param action action à exécuter à chaque clic
     */
    void definirActionImportPlan(Runnable action);

    /**
     * Demande à l'utilisateur de choisir un fichier XML.
     *
     * @return le fichier choisi, ou vide si l'utilisateur annule
     */
    Optional<File> choisirFichierPlan();

    /**
     * Affiche la carte d'un plan.
     *
     * @param plan plan à afficher
     */
    void afficherPlan(Plan plan);

    /**
     * Ouvre la fenêtre d'information « fichier XML invalide ».
     *
     * @param detail explication de l'erreur
     */
    void afficherErreurXml(String detail);

    /**
     * Retire de la page les informations du plan et de la tournée (carte
     * vide).
     */
    void reinitialiser();

    /**
     * Définit l'action déclenchée par le bouton « Calculer ».
     *
     * @param action action à exécuter à chaque clic
     */
    void setOnCalculate(Runnable action);

    /**
     * Active les boutons des actions permises et désactive les autres. C'est
     * l'état du contrôleur qui décide des actions permises ; la vue se
     * contente de les appliquer.
     *
     * @param actions actions permises
     */
    void activerActions(Set<Action> actions);

    /**
     * Définit l'action du bouton « Ajouter une demande ».
     *
     * @param action action à exécuter à chaque clic
     */
    void definirActionAjouterDemande(Runnable action);

    /**
     * Définit l'action du bouton « Supprimer la demande ».
     *
     * @param action reçoit le rang de la demande sélectionnée
     */
    void definirActionSupprimerDemande(IntConsumer action);

    /**
     * Définit l'action du bouton « Annuler » et de Ctrl+Z.
     *
     * @param action action à exécuter
     */
    void definirActionAnnuler(Runnable action);

    /**
     * Définit l'action du bouton « Rétablir » et de Ctrl+Y.
     *
     * @param action action à exécuter
     */
    void definirActionRefaire(Runnable action);

    /**
     * Définit l'action d'un clic sur une intersection de la carte.
     *
     * @param action reçoit l'identifiant du noeud cliqué
     */
    void definirActionClicNoeud(LongConsumer action);

    /**
     * Affiche une consigne pour l'utilisateur (ex. « cliquez sur le point de
     * collecte »).
     *
     * @param consigne texte à afficher, vide pour l'effacer
     */
    void afficherConsigne(String consigne);

    /** Retire la tournée affichée (elle ne correspond plus aux demandes). */
    void effacerTournee();

    /**
     * Affiche ou retire l'état d'attente pendant le calcul.
     *
     * @param waiting {@code true} tant que le calcul est en cours
     */
    void showWaiting(boolean waiting);

    /**
     * Affiche la tournée calculée.
     *
     * @param passages points de la tournée avec leurs heures, dans l'ordre
     */
    void showTour(List<Passage> passages);

    /**
     * Signale que le calcul a échoué.
     *
     * @param detail explication de l'erreur
     */
    void showCalculationError(String detail);
}
