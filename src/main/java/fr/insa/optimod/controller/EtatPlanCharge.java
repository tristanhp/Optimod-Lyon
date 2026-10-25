package fr.insa.optimod.controller;

import java.util.EnumSet;
import java.util.Set;

import fr.insa.optimod.controller.MapScreen.Action;

/**
 * Un plan est chargé, sans demandes : on peut importer un autre plan ou des
 * demandes de livraison.
 */
final class EtatPlanCharge implements Etat {

    @Override
    public Set<Action> actionsPermises(MainController controleur) {
        return EnumSet.of(Action.IMPORTER_PLAN, Action.IMPORTER_PROGRAMME);
    }
}
