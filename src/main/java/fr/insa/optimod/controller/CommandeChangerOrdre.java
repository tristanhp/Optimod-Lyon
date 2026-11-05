package fr.insa.optimod.controller;

import java.util.Map;

import fr.insa.optimod.model.ProgrammeLivraisons;
import fr.insa.optimod.model.Tournee;

/**
 * Change l'ordre des arrêts de la tournée d'un livreur ; l'annulation remet
 * l'ordre précédent. Les demandes ne changent pas, seules les tournées : la
 * commande garde celles d'avant et celles d'après, calculées par
 * {@code Plan.calculerTourneeOrdreChange} avant la commande. Exécuter ou
 * annuler ne fait que ranger les unes ou les autres dans le programme, sans
 * aucun calcul.
 */
final class CommandeChangerOrdre implements Commande {

    private final ProgrammeLivraisons programme;

    /** Tournées d'avant le changement, remises par l'annulation. */
    private Map<Integer, Tournee> tourneesAvant;

    /** Tournées d'après le changement, rangées en exécutant. */
    private Map<Integer, Tournee> tourneesApres;

    /**
     * @param programme     programme dont les tournées changent
     * @param tourneesAvant tournées par numéro de livreur avant le changement
     * @param tourneesApres les mêmes, avec la nouvelle tournée du livreur
     */
    CommandeChangerOrdre(ProgrammeLivraisons programme,
            Map<Integer, Tournee> tourneesAvant,
            Map<Integer, Tournee> tourneesApres) {
        this.programme = programme;
        this.tourneesAvant = tourneesAvant;
        this.tourneesApres = tourneesApres;
    }

    @Override
    public void executer() {
        programme.definirTournees(tourneesApres);
    }

    @Override
    public void annuler() {
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
