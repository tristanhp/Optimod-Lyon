package fr.insa.optimod.controller;

import fr.insa.optimod.model.Demande;
import fr.insa.optimod.model.Livraison;

/**
 * Ajoute une demande à la fin des demandes ; l'annulation la retire.
 */
final class CommandeAjouterDemande implements Commande {

    private final Livraison livraison;
    private final Demande demande;

    /** Rang de la demande ajoutée, connu après l'exécution. */
    private int position;

    /**
     * @param livraison demandes à modifier
     * @param demande demande à ajouter
     */
    CommandeAjouterDemande(Livraison livraison, Demande demande) {
        this.livraison = livraison;
        this.demande = demande;
    }

    @Override
    public void executer() {
        position = livraison.obtenirDemandes().size();
        livraison.ajouterDemande(position, demande);
    }

    @Override
    public void annuler() {
        livraison.retirerDemande(position);
    }
}
