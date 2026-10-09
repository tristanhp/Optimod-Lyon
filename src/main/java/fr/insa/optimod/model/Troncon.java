package fr.insa.optimod.model;

/**
 * Tronçon de route orienté entre deux intersections (balise
 * {@code <troncon>} du XML).
 *
 * @param origine intersection de départ
 * @param destination intersection d'arrivée
 * @param nomRue nom de la rue
 * @param longueur longueur du tronçon, en mètres
 */
public record Troncon(Intersection origine, Intersection destination,
        String nomRue, double longueur) {
}
