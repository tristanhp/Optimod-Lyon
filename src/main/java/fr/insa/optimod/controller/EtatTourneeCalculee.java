package fr.insa.optimod.controller;

import java.util.Set;

import fr.insa.optimod.controller.MapScreen.Action;

/**
 * Les tournées des demandes actuelles sont calculées et rangées dans le
 * programme. Les actions sont celles de {@link EtatProgrammeCharge} :
 * recalculer, importer, modifier les demandes, annuler ou rétablir. Ajouter
 * une demande retire les tournées ; en supprimer une met à jour celle de son
 * livreur (recalcul local). On peut en plus changer l'ordre des arrêts d'un
 * livreur (« Monter », « Descendre »), déplacer un point de collecte ou de
 * livraison (« Déplacer ») et sauvegarder les tournées.
 */
final class EtatTourneeCalculee extends EtatProgrammeCharge {

    @Override
    public void sauvegarder(MainController controleur) {
        controleur.sauvegarderTournee();
    }

    /**
     * Supprime une demande en gardant l'ordre de passage des autres arrêts du
     * livreur : le contrôleur recalcule sa tournée localement (A*, sans
     * relancer le voyageur de commerce) hors du thread de l'interface, puis
     * exécute la suppression et affiche les tournées. Si le recalcul est
     * impossible, les tournées sont retirées et l'utilisateur est prévenu.
     */
    @Override
    public void supprimerDemande(MainController controleur, int position) {
        controleur.supprimerDemandeEnGardantLOrdre(position);
    }

    /**
     * Change l'ordre d'un arrêt de la tournée d'un livreur : le contrôleur
     * recalcule cette tournée localement, hors du thread de l'interface, puis
     * exécute la commande. Un changement refusé n'est pas exécuté.
     */
    @Override
    public void changerOrdre(MainController controleur, int livreur,
            int rang, int decalage) {
        controleur.changerOrdreTournee(livreur, rang, decalage);
    }

    /**
     * Commence le déplacement du point de collecte ou de livraison
     * sélectionné : le contrôleur attend alors un clic sur sa nouvelle adresse.
     */
    @Override
    public void deplacerPoint(MainController controleur, int livreur,
            int rang) {
        controleur.choisirPointADeplacer(livreur, rang);
    }

    @Override
    public Set<Action> actionsPermises(MainController controleur) {
        // EtatProgrammeCharge renvoie déjà un ensemble neuf et modifiable : pas
        // besoin de le copier.
        Set<Action> actions = super.actionsPermises(controleur);
        actions.add(Action.CHANGER_ORDRE);
        actions.add(Action.DEPLACER_POINT);
        actions.add(Action.SAUVEGARDER);
        return actions;
    }
}
