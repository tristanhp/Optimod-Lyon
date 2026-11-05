package fr.insa.optimod.controller;

import java.util.Map;

import fr.insa.optimod.model.ProgrammeLivraisons;
import fr.insa.optimod.model.ProgrammeLivraisons.Demande;
import fr.insa.optimod.model.Tournee;

/**
 * Déplace un point de collecte ou de livraison : remplace la demande par la
 * même demande dont un point a changé de nœud ; l'annulation remet l'ancienne.
 * La commande garde les tournées d'avant et d'après, calculées avant elle :
 * exécuter, annuler ou rétablir ne fait que les ranger dans le programme,
 * sans aucun calcul, et les observateurs ne sont prévenus qu'une fois, quand
 * la demande et les tournées sont cohérentes.
 */
final class CommandeDeplacerPoint implements Commande {

    private final ProgrammeLivraisons programme;
    private final int position;
    private final Demande apres;

    /** Demande avant le déplacement, gardée pour l'annulation. */
    private Demande avant;

    /** Tournées d'avant le déplacement, remises par l'annulation. */
    private Map<Integer, Tournee> tourneesAvant;

    /** Tournées d'après le déplacement, rangées en exécutant. */
    private Map<Integer, Tournee> tourneesApres;

    /**
     * @param programme demandes à modifier
     * @param position rang de la demande dont un point est déplacé
     * @param apres la demande avec le point à son nouveau nœud
     * @param tourneesAvant tournées par numéro de livreur avant le déplacement
     * @param tourneesApres les mêmes, avec la nouvelle tournée du livreur
     */
    CommandeDeplacerPoint(ProgrammeLivraisons programme, int position,
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
    public String confirmation() {
        return "Point du livreur " + apres.livreur()
                + " déplacé à la nouvelle adresse.";
    }

    @Override
    public String description() {
        return "déplacement d'un point du livreur " + apres.livreur();
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
