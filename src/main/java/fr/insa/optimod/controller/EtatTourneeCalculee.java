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
     * la nouvelle tournée est d'abord calculée localement (A*, sans relancer
     * le voyageur de commerce) et rangée dans le cache du contrôleur, puis la
     * suppression est exécutée comme d'habitude. En retrouvant l'état du
     * programme, le contrôleur trouve cette tournée dans le cache et
     * l'affiche tout de suite. Si le recalcul local est impossible, rien n'est
     * rangé dans le cache et on retombe sur le comportement de
     * {@link EtatProgrammeCharge} : la tournée affichée est retirée.
     */
    @Override
    public void supprimerDemande(MainController controleur, int position) {
        controleur.memoriserTourneeSansDemande(position);
        super.supprimerDemande(controleur, position);
    }
}
