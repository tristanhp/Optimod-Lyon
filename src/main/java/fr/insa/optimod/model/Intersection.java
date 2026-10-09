package fr.insa.optimod.model;

/**
 * Intersection du plan de la ville (balise {@code <noeud>} du XML).
 *
 * @param id identifiant de l'intersection dans le fichier XML
 * @param latitude latitude en degrés (positive vers le nord)
 * @param longitude longitude en degrés (positive vers l'est)
 */
public record Intersection(long id, double latitude, double longitude) {
}
