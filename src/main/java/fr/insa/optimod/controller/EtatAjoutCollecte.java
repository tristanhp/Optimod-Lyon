package fr.insa.optimod.controller;

import java.util.EnumSet;
import java.util.Set;

import fr.insa.optimod.controller.MapScreen.Action;

/**
 * Ajout d'une demande, première étape : l'utilisateur clique sur le point de
 * collecte. Un nouveau clic sur « Ajouter une demande » annule l'ajout.
 */
final class EtatAjoutCollecte implements Etat {

    /** Consigne affichée en entrant dans cet état. */
    static final String CONSIGNE = "Cliquez sur le point de collecte "
            + "(« Ajouter une demande » pour annuler).";

    @Override
    public void importerPlan(MainController controleur) {
    }

    @Override
    public void importerProgramme(MainController controleur) {
    }

    @Override
    public void ajouterDemande(MainController controleur) {
        controleur.retrouverEtatProgramme();
    }

    @Override
    public void cliquerNoeud(MainController controleur, long idNoeud) {
        controleur.changerEtat(new EtatAjoutLivraison(idNoeud));
        controleur.afficherConsigne(EtatAjoutLivraison.CONSIGNE);
    }

    @Override
    public Set<Action> actionsPermises(MainController controleur) {
        return EnumSet.of(Action.AJOUTER_DEMANDE);
    }
}
