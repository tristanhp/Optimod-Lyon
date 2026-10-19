package fr.insa.optimod.algo;

import java.util.List;

import fr.insa.optimod.model.Livraison;
import fr.insa.optimod.model.Passage;
import fr.insa.optimod.model.Plan;

/**
 * Calcul de la tournée d'un lot de demandes sur un plan (Cas 5). Le contrôleur
 * s'appuie sur cette interface pour lancer le calcul sans connaître
 * l'algorithme (plus courts chemins, TSP).
 */
@FunctionalInterface
public interface CalculateurTournee {

    /**
     * Calcule la tournée.
     *
     * @param plan plan de la ville
     * @param livraison demandes à livrer et entrepôt de départ
     * @return les points de la tournée dans l'ordre de visite, avec leurs
     *         heures d'arrivée et de départ
     * @throws IllegalStateException si aucune tournée n'est possible
     */
    List<Passage> calculer(Plan plan, Livraison livraison);
}
