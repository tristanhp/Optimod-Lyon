package fr.insa.optimod.controller;

import java.util.Map;

import fr.insa.optimod.model.ProgrammeLivraisons;
import fr.insa.optimod.model.Tournee;

/**
 * Change le nombre de livreurs ; l'annulation remet le nombre d'avant. Un
 * livreur ajouté après le calcul reçoit une tournée vide. La commande garde les
 * tournées d'avant et d'après : exécuter, annuler ou rétablir ne fait que les
 * ranger dans le programme, sans aucun calcul.
 */
final class CommandeChangerNombreLivreurs implements Commande {

    private final ProgrammeLivraisons programme;
    private final int nombreAvant;
    private final int nombreApres;

    /** Tournées d'avant le réglage, remises par l'annulation. */
    private Map<Integer, Tournee> tourneesAvant;

    /**
     * Tournées d'après le réglage, rangées en rétablissant ; {@code null} tant
     * que la commande n'a pas été exécutée une première fois.
     */
    private Map<Integer, Tournee> tourneesApres;

    /**
     * @param programme programme dont on règle le nombre de livreurs
     * @param nombre nouveau nombre de livreurs, déjà vérifié par
     *        {@link ProgrammeLivraisons#verifierNombreLivreurs}
     */
    CommandeChangerNombreLivreurs(ProgrammeLivraisons programme, int nombre) {
        this.programme = programme;
        this.nombreAvant = programme.obtenirNombreLivreurs();
        this.nombreApres = nombre;
        this.tourneesAvant = programme.obtenirTournees();
    }

    @Override
    public void executer() {
        programme.definirNombreLivreurs(nombreApres);
        if (tourneesApres == null) {
            tourneesApres = programme.obtenirTournees();
        } else {
            programme.definirTournees(tourneesApres);
        }
    }

    @Override
    public void annuler() {
        programme.definirNombreLivreurs(nombreAvant);
        programme.definirTournees(tourneesAvant);
    }

    @Override
    public String confirmation() {
        if (nombreApres > nombreAvant) {
            return nombreApres == nombreAvant + 1
                    ? "Livreur " + nombreApres
                            + " ajouté : il n'a encore aucune demande."
                    : nombreApres + " livreurs : les nouveaux n'ont encore "
                            + "aucune demande.";
        }
        return nombreApres == 1 ? "Il reste 1 livreur."
                : "Il reste " + nombreApres + " livreurs.";
    }

    @Override
    public String description() {
        return "réglage du nombre de livreurs à " + nombreApres;
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
