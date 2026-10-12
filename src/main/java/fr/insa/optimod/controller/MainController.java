package fr.insa.optimod.controller;

import java.io.File;
import java.io.IOException;

import org.xml.sax.SAXException;

import fr.insa.optimod.model.Plan;

/**
 * Contrôleur de la page principale : importe un plan XML dans le modèle et
 * demande à la vue de l'afficher.
 */
public final class MainController {

    /** Plan actuellement chargé, ou {@code null}. */
    private Plan plan;

    /**
     * Crée le contrôleur et le branche au bouton « Importer ». L'écran n'est
     * pas conservé dans un champ : il n'est utilisé que par l'action du
     * bouton.
     *
     * @param screen écran à piloter
     */
    public MainController(MapScreen screen) {
        screen.setOnImport(() -> importPlan(screen));
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
    private void importPlan(MapScreen screen) {
        File fichier = screen.chooseXmlFile().orElse(null);
        if (fichier == null) {
            return;
        }
        try {
            plan = new Plan(fichier.getPath());
            screen.showPlan(plan);
        } catch (IOException | SAXException | IllegalArgumentException e) {
            screen.showInvalidXml(e.getMessage());
            plan = null;
            screen.reset();
        }
    }
}
