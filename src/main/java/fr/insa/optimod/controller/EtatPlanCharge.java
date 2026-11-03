package fr.insa.optimod.controller;

import java.util.EnumSet;
import java.util.Set;

import fr.insa.optimod.controller.MapScreen.Action;

/**
 * Un plan est chargé, sans demandes : on peut importer un autre plan ou des
 * demandes de livraison, ou restaurer une tournée.
 */
final class EtatPlanCharge implements Etat {

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
