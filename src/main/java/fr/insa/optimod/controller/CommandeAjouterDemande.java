package fr.insa.optimod.controller;

import fr.insa.optimod.model.ProgrammeLivraisons.Demande;
import fr.insa.optimod.model.ProgrammeLivraisons;

/**
 * Ajoute une demande à la fin des demandes ; l'annulation la retire.
 */
final class CommandeAjouterDemande implements Commande {

    private final ProgrammeLivraisons programme;
    private final Demande demande;

    /** Rang de la demande ajoutée, connu après l'exécution. */
    private int position;

    /**
     * @param programme demandes à modifier
     * @param demande demande à ajouter
     */
    CommandeAjouterDemande(ProgrammeLivraisons programme, Demande demande) {
        this.programme = programme;
        this.demande = demande;
    }

    @Override
    public void executer() {
        position = programme.obtenirDemandes().size();
        programme.ajouterDemande(position, demande);
    }

    @Override
    public void annuler() {
        programme.retirerDemande(position);
    }
}
