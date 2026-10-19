package fr.insa.optimod.controller;

import java.io.File;
import java.util.Optional;

import fr.insa.optimod.model.Plan;

/**
 * Ce que le contrôleur attend de l'écran de la carte. Séparer cette interface
 * de la vue JavaFX permet de tester le contrôleur sans afficher de fenêtre.
 */
public interface MapScreen {

    /**
     * Définit l'action déclenchée par le bouton « Importer ».
     *
     * @param action action à exécuter à chaque clic
     */
    void definirActionImportPlan(Runnable action);

    /**
     * Définit l'action du bouton d'import des demandes.
     * @param action action à exécuter au clic
     */
    void definirActionImportLivraison(Runnable action);

    /** @return fichier XML de demandes choisi, ou vide après annulation */
    Optional<File> choisirFichierLivraison();

    /** Affiche la confirmation du chargement des demandes. */
    void afficherLivraisonChargee();

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
     * Retire de la page les informations du plan (carte vide).
     */
    void reinitialiser();
}
