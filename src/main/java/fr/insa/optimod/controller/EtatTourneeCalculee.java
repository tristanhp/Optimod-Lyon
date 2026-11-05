package fr.insa.optimod.controller;

import java.util.Set;

import fr.insa.optimod.controller.MapScreen.Action;

/**
 * Les tournées des demandes actuelles sont calculées et rangées dans le
 * programme. Les actions sont celles de {@link EtatProgrammeCharge} :
 * recalculer (après confirmation), importer, modifier les demandes, régler
 * le nombre de livreurs (un livreur ajouté a une tournée vide), annuler ou
 * rétablir. Ajouter ou supprimer une demande met à jour la tournée de son
 * livreur (recalcul local). On peut en plus changer l'ordre des arrêts d'un
 * livreur (« Monter », « Descendre »), déplacer un point de collecte ou de
 * livraison (« Déplacer »), changer le livreur d'une demande (plusieurs
 * livreurs) et sauvegarder la tournée (un seul livreur, seul cas que le
 * format de sauvegarde gère). Un livreur qui rentre après 13 h n'est pas bloqué : la
 * consigne le signale.
 */
final class EtatTourneeCalculee extends EtatProgrammeCharge {

    /** Consigne affichée en entrant dans cet état. */
    static final String CONSIGNE = "Tournée prête. Vous pouvez modifier le "
            + "programme (ajouter, supprimer, déplacer, réordonner, "
            + "changer de livreur) ou régler le nombre de livreurs.";

    /**
     * Garde la consigne « ordre non optimisé » tant que les tournées affichées
     * viennent d'une modification faite à la main, même après un refus,
     * Annuler ou Rétablir. Y ajoute l'avertissement « retour après 13 h » des
     * tournées affichées, s'il y en a.
     */
    @Override
    public String consigne(MainController controleur) {
        String ordreGarde = controleur.obtenirConsigneOrdreGarde();
        return (ordreGarde.isEmpty() ? CONSIGNE : ordreGarde)
                + controleur.obtenirAvertissementRetour();
    }

    /**
     * « Recalculer » : relance le voyageur de commerce pour chaque livreur,
     * seulement si l'administrateur accepte de perdre les modifications
     * manuelles et les plages annoncées. Sinon rien ne change.
     */
    @Override
    public void calculer(MainController controleur) {
        if (controleur.confirmerRecalcul()) {
            super.calculer(controleur);
        }
    }

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

    /**
     * Confie une demande à un autre livreur : le contrôleur fait choisir le
     * livreur et la position, puis recalcule les deux tournées localement,
     * hors du thread de l'interface, avant d'exécuter la commande.
     */
    @Override
    public void changerLivreur(MainController controleur, int position) {
        controleur.changerLivreurDemande(position);
    }

    /**
     * Génère une feuille de route HTML par livreur dans le dossier choisi par
     * l'utilisateur : numéro du livreur, heures de départ et de retour, arrêts
     * dans l'ordre et itinéraire rue par rue.
     */
    @Override
    public void genererFeuillesDeRoute(MainController controleur) {
        controleur.genererFeuillesDeRoute();
    }

    @Override
    public Set<Action> actionsPermises(MainController controleur) {
        // EtatProgrammeCharge renvoie déjà un ensemble neuf et modifiable : pas
        // besoin de le copier.
        Set<Action> actions = super.actionsPermises(controleur);
        actions.add(Action.RECALCULER);
        actions.add(Action.CHANGER_ORDRE);
        actions.add(Action.DEPLACER_POINT);
        actions.add(Action.GENERER_FEUILLES_ROUTE);
        if (controleur.obtenirProgramme().obtenirNombreLivreurs() > 1) {
            actions.add(Action.CHANGER_LIVREUR);
        } else {
            // Le format de sauvegarde ne gère qu'un livreur.
            actions.add(Action.SAUVEGARDER);
        }
        return actions;
    }
}
