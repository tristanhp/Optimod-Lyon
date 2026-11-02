package fr.insa.optimod.controller;

import java.util.Set;

import fr.insa.optimod.controller.MapScreen.Action;

/**
 * La tournée des demandes actuelles est calculée et affichée. Les actions
 * sont celles de {@link EtatProgrammeCharge} : recalculer, importer, modifier
 * les demandes (ce qui retire la tournée affichée), annuler ou rétablir.
 * La sauvegarde de la tournée est également permise.
 */
final class EtatTourneeCalculee extends EtatProgrammeCharge {
    @Override
    public void sauvegarder(MainController controleur) {
        controleur.sauvegarderTournee();
    }

    @Override
    public Set<Action> actionsPermises(MainController controleur) {
        Set<Action> actions = super.actionsPermises(controleur);
        actions.add(Action.SAUVEGARDER);
        return actions;
    }
}
