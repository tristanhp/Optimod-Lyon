package fr.insa.optimod.controller;

/**
 * La tournée des demandes actuelles est calculée et affichée. Les actions
 * sont celles de {@link EtatProgrammeCharge} : recalculer, importer, modifier
 * les demandes, annuler ou rétablir. Ajouter une demande retire la tournée
 * affichée ; en supprimer une la met à jour (recalcul local).
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
}
