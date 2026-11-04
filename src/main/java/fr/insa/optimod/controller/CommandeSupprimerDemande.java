package fr.insa.optimod.controller;

import java.util.Map;

import fr.insa.optimod.model.ProgrammeLivraisons.Demande;
import fr.insa.optimod.model.ProgrammeLivraisons;
import fr.insa.optimod.model.Tournee;

/**
 * Retire une demande ; l'annulation la remet à son rang. Les tournées sont
 * retirées (le recalcul local viendra avec TIG-101) ; l'annulation remet
 * celles d'avant.
 */
final class CommandeSupprimerDemande implements Commande {

    private final ProgrammeLivraisons programme;
    private final int position;

    /** Demande retirée, gardée pour l'annulation. */
    private Demande retiree;

    /** Tournées d'avant la modification, remises par l'annulation. */
    private Map<Integer, Tournee> tourneesAvant;

    /**
     * Tournées d'après la modification, remises en rétablissant ;
     * {@code null} avant la première exécution.
     */
    private Map<Integer, Tournee> tourneesApres;

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
        if (tourneesApres == null) {
            tourneesAvant = programme.obtenirTournees();
        }
        programme.effacerTournees();
        retiree = programme.retirerDemande(position);
        if (tourneesApres == null) {
            tourneesApres = programme.obtenirTournees();
        } else {
            programme.definirTournees(tourneesApres);
        }
    }

    @Override
    public void annuler() {
        programme.effacerTournees();
        programme.ajouterDemande(position, retiree);
        programme.definirTournees(tourneesAvant);
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
