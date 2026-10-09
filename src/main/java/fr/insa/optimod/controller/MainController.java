package fr.insa.optimod.controller;

import java.io.File;

import fr.insa.optimod.model.Plan;
import fr.insa.optimod.xml.PlanXmlParser;
import fr.insa.optimod.xml.XmlInvalideException;

/**
 * Contrôleur de la page principale : importe un plan XML dans le modèle et
 * demande à la vue de l'afficher.
 */
public final class MainController {

    /** Écran piloté par ce contrôleur. */
    private final MapScreen screen;

    /** Plan actuellement chargé, ou {@code null}. */
    private Plan plan;

    /**
     * Crée le contrôleur et le branche au bouton « Importer ».
     *
     * @param screen écran à piloter
     */
    public MainController(MapScreen screen) {
        this.screen = screen;
        screen.setOnImport(this::importPlan);
    }

    /**
     * Retourne le plan chargé.
     *
     * @return le plan, ou {@code null} si aucun plan n'est chargé
     */
    public Plan getPlan() {
        return plan;
    }

    /**
     * Fait choisir un fichier XML, le lit puis affiche la carte. Si le fichier
     * est invalide, une fenêtre d'information s'ouvre et la page est vidée.
     * Si l'utilisateur annule le choix, rien ne change.
     */
    private void importPlan() {
        File fichier = screen.chooseXmlFile().orElse(null);
        if (fichier == null) {
            return;
        }
        try {
            plan = PlanXmlParser.lire(fichier);
            screen.showPlan(plan);
        } catch (XmlInvalideException e) {
            screen.showInvalidXml(e.getMessage());
            plan = null;
            screen.reset();
        }
    }
}
