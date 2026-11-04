package fr.insa.optimod.controller;

import java.util.EnumSet;
import java.util.Set;

import fr.insa.optimod.controller.MapScreen.Action;

/**
 * Aucun plan chargé : importer un plan ou restaurer une tournée est possible.
 */
final class EtatInitial implements Etat {
    /** Consigne affichée en entrant dans cet état. */
    static final String CONSIGNE =
            "Commencez par importer un plan (bouton « Importer le plan »).";

    @Override
    public String consigne(MainController controleur) {
        return CONSIGNE;
    }


    @Override
    public void restaurer(MainController controleur) {
        controleur.restaurerSauvegarde();
    }

    @Override
    public void importerProgramme(MainController controleur) {
    }

    @Override
    public void erreurDessin(MainController controleur, String detail) {
    }

    @Override
    public Set<Action> actionsPermises(MainController controleur) {
        return EnumSet.of(Action.IMPORTER_PLAN, Action.RESTAURER);
    }
}
