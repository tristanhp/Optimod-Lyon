package fr.insa.optimod.controller;

/**
 * Les tournées des demandes actuelles sont calculées et rangées dans le
 * programme. Les actions sont celles de {@link EtatProgrammeCharge} :
 * recalculer, importer, modifier les demandes, annuler ou rétablir. Ajouter
 * une demande retire les tournées ; en supprimer une met à jour celle de son
 * livreur (recalcul local).
 */
final class EtatTourneeCalculee extends EtatProgrammeCharge {

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
}
