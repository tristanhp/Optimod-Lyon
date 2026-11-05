package fr.insa.optimod.controller;

import java.util.Map;

import fr.insa.optimod.model.ProgrammeLivraisons;
import fr.insa.optimod.model.ProgrammeLivraisons.Demande;
import fr.insa.optimod.model.Tournee;

/**
 * Confie une demande à un autre livreur ; l'annulation la rend à son livreur
 * d'avant. La demande garde son rang dans le programme. La commande garde les
 * tournées d'avant et d'après, calculées avant elle : exécuter, annuler ou
 * rétablir ne fait que les ranger dans le programme, sans aucun calcul, et
 * les observateurs ne sont prévenus qu'une fois, quand la demande et les
 * tournées sont cohérentes.
 */
final class CommandeChangerLivreur implements Commande {

    private final ProgrammeLivraisons programme;
    private final int position;
    private final Demande apres;

    /** Demande avant le changement, gardée pour l'annulation. */
    private Demande avant;

    /** Tournées d'avant le changement, remises par l'annulation. */
    private Map<Integer, Tournee> tourneesAvant;

    /** Tournées d'après le changement, rangées en exécutant. */
    private Map<Integer, Tournee> tourneesApres;

    /**
     * @param programme     demandes à modifier
     * @param position      rang de la demande dont le livreur change
     * @param apres         la demande confiée à son nouveau livreur
     * @param tourneesAvant tournées par numéro de livreur avant le changement
     * @param tourneesApres les mêmes, avec les tournées recalculées de
     *                      l'ancien et du nouveau livreur
     */
    CommandeChangerLivreur(ProgrammeLivraisons programme, int position,
            Demande apres, Map<Integer, Tournee> tourneesAvant,
            Map<Integer, Tournee> tourneesApres) {
        this.programme = programme;
        this.position = position;
        this.apres = apres;
        this.tourneesAvant = tourneesAvant;
        this.tourneesApres = tourneesApres;
    }

    @Override
    public void executer() {
        avant = programme.remplacerDemandeEtTournees(position, apres,
                tourneesApres);
    }

    @Override
    public void annuler() {
        programme.remplacerDemandeEtTournees(position, avant, tourneesAvant);
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
