package fr.insa.optimod.controller;

import java.util.Map;

import fr.insa.optimod.model.ProgrammeLivraisons;
import fr.insa.optimod.model.ProgrammeLivraisons.Demande;
import fr.insa.optimod.model.Tournee;

/**
 * Ajoute une demande à la fin des demandes ; l'annulation la retire. Les
 * tournées sont retirées (pas encore d'insertion sans recalcul) ;
 * l'annulation remet celles d'avant.
 */
final class CommandeAjouterDemande implements Commande {

    private final ProgrammeLivraisons programme;
    private final Demande demande;

    /** Rang de la demande ajoutée, connu après l'exécution. */
    private int position;

    /** Tournées d'avant la modification, remises par l'annulation. */
    private Map<Integer, Tournee> tourneesAvant;

    /**
     * Tournées d'après la modification, remises en rétablissant ;
     * {@code null} avant la première exécution.
     */
    private Map<Integer, Tournee> tourneesApres;

    /**
     * @param programme demandes à modifier
     * @param demande   demande à ajouter
     */
    CommandeAjouterDemande(ProgrammeLivraisons programme, Demande demande) {
        this.programme = programme;
        this.demande = demande;
    }

    @Override
    public void executer() {
        if (tourneesApres == null) {
            tourneesAvant = programme.obtenirTournees();
        }
        programme.effacerTournees();
        position = programme.obtenirDemandes().size();
        programme.ajouterDemande(position, demande);
        if (tourneesApres == null) {
            tourneesApres = programme.obtenirTournees();
        } else {
            programme.definirTournees(tourneesApres);
        }
    }

    @Override
    public void annuler() {
        programme.effacerTournees();
        programme.retirerDemande(position);
        programme.definirTournees(tourneesAvant);
    }

    @Override
    public String confirmation() {
        return "Demande ajoutée au livreur " + demande.livreur() + ".";
    }

    @Override
    public String description() {
        return "ajout d'une demande au livreur " + demande.livreur();
    }

    @Override
    public void memoriserTourneesAvant(Map<Integer, Tournee> tournees) {
        tourneesAvant = tournees;
    }

    @Override
    public void memoriserTourneesApres(Map<Integer, Tournee> tournees) {
        tourneesApres = tournees;
    }
}
