package fr.insa.optimod.controller;

import java.util.EnumSet;
import java.util.Set;

import fr.insa.optimod.controller.MapScreen.Action;

/**
 * Aucun plan chargé : seul l'import d'un plan est possible.
 */
final class EtatInitial implements Etat {

    @Override
    public void importerProgramme(MainController controleur) {
    }

    @Override
    public Set<Action> actionsPermises(MainController controleur) {
        return EnumSet.of(Action.IMPORTER_PLAN);
    }
}
