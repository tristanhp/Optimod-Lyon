package fr.insa.optimod.controller;

import java.util.EnumSet;
import java.util.Set;

import fr.insa.optimod.controller.MapScreen.Action;

/**
 * Un plan est chargé, sans demandes : on peut importer un autre plan ou des
 * demandes de livraison, ou restaurer une tournée.
 */
final class EtatPlanCharge implements Etat {
    /** Consigne affichée en entrant dans cet état. */
    static final String CONSIGNE =
            "Importez un programme de livraisons "
            + "(bouton « Importer les demandes »).";

    @Override
    public String consigne(MainController controleur) {
        return CONSIGNE;
    }


    @Override
    public void restaurer(MainController controleur) {
        controleur.restaurerSauvegarde();
    }

    @Override
    public Set<Action> actionsPermises(MainController controleur) {
        return EnumSet.of(Action.IMPORTER_PLAN, Action.IMPORTER_PROGRAMME,
                Action.RESTAURER);
    }
}
