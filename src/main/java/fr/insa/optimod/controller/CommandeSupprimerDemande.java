package fr.insa.optimod.controller;

import fr.insa.optimod.model.Demande;
import fr.insa.optimod.model.Livraison;

/**
 * Retire une demande ; l'annulation la remet à son rang.
 */
final class CommandeSupprimerDemande implements Commande {

    private final Livraison livraison;
    private final int position;

    /** Demande retirée, gardée pour l'annulation. */
    private Demande retiree;

    /**
     * @param livraison demandes à modifier
     * @param position rang de la demande à retirer
     */
    CommandeSupprimerDemande(Livraison livraison, int position) {
        this.livraison = livraison;
        this.position = position;
    }

    @Override
    public void executer() {
        retiree = livraison.retirerDemande(position);
    }

    @Override
    public void annuler() {
        livraison.ajouterDemande(position, retiree);
    }
}
