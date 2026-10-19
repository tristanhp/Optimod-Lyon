package fr.insa.optimod.controller;

import java.io.File;
import java.io.IOException;

import org.xml.sax.SAXException;

import fr.insa.optimod.model.Livraison;
import fr.insa.optimod.model.Plan;

/**
 * Contrôleur de la page principale : importe un plan XML dans le modèle et
 * demande à la vue de l'afficher.
 */
public final class MainController {



    /** Plan actuellement chargé, ou {@code null}. */
    private Plan plan;
    private Livraison livraison;
    private Boolean planCharge = false;

    /**
     * Crée le contrôleur et le branche au bouton « Importer ». L'écran n'est
     * pas conservé dans un champ : il n'est utilisé que par l'action du
     * bouton.
     *
     * @param ecran écran à piloter
     */
    public MainController(MapScreen ecran) {
        ecran.definirActionImportPlan(() -> importerPlan(ecran));
        ecran.definirActionImportLivraison(() -> importerLivraison(ecran));
    }

    /**
     * Retourne le plan chargé.
     *
     * @return le plan, ou {@code null} si aucun plan n'est chargé
     */
    public Plan obtenirPlan() {
        return plan;
    }

    /** @return livraison chargée, ou {@code null} avant son import */
    public Livraison obtenirLivraison() {
        return livraison;
    }

    /**
     * Fait choisir un fichier XML, le lit puis affiche la carte. Si le fichier
     * est invalide, une fenêtre d'information s'ouvre et la page est vidée.
     * Si l'utilisateur annule le choix, rien ne change.
     */
    private void importerPlan(MapScreen ecran) {
        File fichier = ecran.choisirFichierPlan().orElse(null);
        if (fichier == null) {
            return;
        }
        try {
            plan = new Plan(fichier.getPath());
            livraison = null;
            ecran.afficherPlan(plan);
            planCharge = true;
        } catch (IOException | SAXException | IllegalArgumentException erreur) {
            ecran.afficherErreurXml(erreur.getMessage());
            plan = null;
            livraison = null;
            ecran.reinitialiser();
            planCharge = false;
        }
    }

    /** Charge les demandes et confirme leur lecture sans modifier la carte. */
    private void importerLivraison(MapScreen ecran) {
        if (!planCharge) {
            return;
        }
        File fichier = ecran.choisirFichierLivraison().orElse(null);
        if (fichier == null) {
            return;
        }
        try {
            livraison = new Livraison(fichier.getPath());
            ecran.afficherLivraisonChargee();
        } catch (IOException | SAXException | IllegalArgumentException erreur) {
            ecran.afficherErreurXml(erreur.getMessage());
        }
    }

}
