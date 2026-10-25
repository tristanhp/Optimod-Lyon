package fr.insa.optimod.model;

import java.util.List;

/**
 * Plus court chemin entre deux noeuds du plan.
 *
 * @param longueur longueur totale en mètres, infinie si la cible est
 *        inaccessible
 * @param troncons tronçons parcourus dans l'ordre, de la source à la cible
 */
public record Chemin(double longueur, List<Troncon> troncons) {

    /** Chemin renvoyé quand la cible ne peut pas être atteinte. */
    public static final Chemin INACCESSIBLE =
            new Chemin(Double.POSITIVE_INFINITY, List.of());

    /**
     * Crée un chemin en copiant la liste des tronçons.
     *
     * @param longueur longueur totale en mètres
     * @param troncons tronçons parcourus dans l'ordre
     */
    public Chemin {
        troncons = List.copyOf(troncons);
    }

    /**
     * Indique si la cible peut être atteinte.
     *
     * @return vrai si la longueur est finie
     */
    public boolean estAccessible() {
        return longueur != Double.POSITIVE_INFINITY;
    }
}
