package fr.insa.optimod.controller;

import java.io.File;
import java.util.List;
import java.util.Optional;

import fr.insa.optimod.model.Passage;
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
    void setOnImport(Runnable action);

    /**
     * Demande à l'utilisateur de choisir un fichier XML.
     *
     * @return le fichier choisi, ou vide si l'utilisateur annule
     */
    Optional<File> chooseXmlFile();

    /**
     * Affiche la carte d'un plan.
     *
     * @param plan plan à afficher
     */
    void showPlan(Plan plan);

    /**
     * Ouvre la fenêtre d'information « fichier XML invalide ».
     *
     * @param detail explication de l'erreur
     */
    void showInvalidXml(String detail);

    /**
     * Retire de la page les informations du plan et de la tournée (carte
     * vide).
     */
    void reset();

    /**
     * Définit l'action déclenchée par le bouton « Calculer ».
     *
     * @param action action à exécuter à chaque clic
     */
    void setOnCalculate(Runnable action);

    /**
     * Active ou désactive le bouton « Calculer » (désactivé tant qu'aucun lot
     * de requêtes n'est importé).
     *
     * @param enabled {@code true} pour autoriser le calcul
     */
    void setCalculateEnabled(boolean enabled);

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
