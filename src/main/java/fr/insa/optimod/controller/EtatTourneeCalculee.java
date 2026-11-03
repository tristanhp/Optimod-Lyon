package fr.insa.optimod.controller;

import java.util.Set;

import fr.insa.optimod.controller.MapScreen.Action;

/**
 * La tournée des demandes actuelles est calculée et affichée. Les actions
 * sont celles de {@link EtatProgrammeCharge} : recalculer, importer, modifier
 * les demandes, annuler ou rétablir. Ajouter une demande retire la tournée
 * affichée ; en supprimer une la met à jour (recalcul local). On peut en plus
 * changer l'ordre des arrêts (« Monter », « Descendre »).
 */
final class EtatTourneeCalculee extends EtatProgrammeCharge {

    /**
     * Supprime une demande en gardant l'ordre de passage des autres arrêts :
     * le contrôleur recalcule la tournée localement (A*, sans relancer le
     * voyageur de commerce) hors du thread de l'interface, puis exécute la
     * suppression et affiche la nouvelle tournée. Si le recalcul est
     * impossible, la tournée affichée est retirée et l'utilisateur est
     * prévenu.
     */
    @Override
    public void supprimerDemande(MainController controleur, int position) {
        controleur.supprimerDemandeEnGardantLOrdre(position);
    }

    @Override
    public void changerOrdre(MainController controleur, int rang,
            int decalage) {
        controleur.changerOrdreTournee(rang, decalage);
    }

    @Override
    public Set<Action> actionsPermises(MainController controleur) {
        // EtatProgrammeCharge renvoie déjà un ensemble neuf et modifiable : pas
        // besoin de le copier.
        Set<Action> actions = super.actionsPermises(controleur);
        actions.add(Action.CHANGER_ORDRE);
        return actions;
    }
}
