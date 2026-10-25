package fr.insa.optimod.controller;

import fr.insa.optimod.model.ProgrammeLivraisons.Demande;
import fr.insa.optimod.model.ProgrammeLivraisons;

/**
 * Retire une demande ; l'annulation la remet à son rang.
 */
final class CommandeSupprimerDemande implements Commande {

    private final ProgrammeLivraisons programme;
    private final int position;

    /** Demande retirée, gardée pour l'annulation. */
    private Demande retiree;

    /**
     * @param programme demandes à modifier
     * @param position rang de la demande à retirer
     */
    CommandeSupprimerDemande(ProgrammeLivraisons programme, int position) {
        this.programme = programme;
        this.position = position;
    }

    @Override
    public void executer() {
        retiree = programme.retirerDemande(position);
    }

    @Override
    public void annuler() {
        programme.ajouterDemande(position, retiree);
    }
}
