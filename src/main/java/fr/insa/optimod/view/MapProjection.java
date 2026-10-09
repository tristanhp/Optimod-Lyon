package fr.insa.optimod.view;

import fr.insa.optimod.model.Intersection;
import fr.insa.optimod.model.Plan;

/**
 * Passage des coordonnées géographiques (latitude, longitude) aux pixels du
 * canevas. Projection équirectangulaire corrigée par le cosinus de la latitude
 * moyenne, nord en haut, le plan entier étant centré et rendu visible.
 */
final class MapProjection {

    /** Longitude minimale du plan. */
    private final double minLongitude;

    /** Latitude maximale du plan. */
    private final double maxLatitude;

    /** Pixels par degré de latitude. */
    private final double scale;

    /** Rapport de longueur d'un degré de longitude sur un de latitude. */
    private final double stretch;

    /** Décalage horizontal en pixels. */
    private final double offsetX;

    /** Décalage vertical en pixels. */
    private final double offsetY;

    /**
     * Calcule la projection qui fait tenir le plan dans la zone donnée.
     *
     * @param plan plan à afficher (au moins une intersection)
     * @param width largeur disponible, en pixels
     * @param height hauteur disponible, en pixels
     * @param padding marge intérieure, en pixels
     */
    MapProjection(Plan plan, double width, double height, double padding) {
        double minLat = Double.POSITIVE_INFINITY;
        double maxLat = Double.NEGATIVE_INFINITY;
        double minLon = Double.POSITIVE_INFINITY;
        double maxLon = Double.NEGATIVE_INFINITY;
        for (Intersection i : plan.getIntersections()) {
            minLat = Math.min(minLat, i.latitude());
            maxLat = Math.max(maxLat, i.latitude());
            minLon = Math.min(minLon, i.longitude());
            maxLon = Math.max(maxLon, i.longitude());
        }
        stretch = Math.cos(Math.toRadians((minLat + maxLat) / 2));
        double spanX = Math.max((maxLon - minLon) * stretch, 1e-9);
        double spanY = Math.max(maxLat - minLat, 1e-9);
        double availableX = Math.max(width - 2 * padding, 1);
        double availableY = Math.max(height - 2 * padding, 1);
        scale = Math.min(availableX / spanX, availableY / spanY);
        minLongitude = minLon;
        maxLatitude = maxLat;
        offsetX = padding + (availableX - spanX * scale) / 2;
        offsetY = padding + (availableY - spanY * scale) / 2;
    }

    /**
     * Abscisse en pixels d'une longitude.
     *
     * @param longitude longitude en degrés
     * @return abscisse, croissante vers l'est
     */
    double toScreenX(double longitude) {
        return offsetX + (longitude - minLongitude) * stretch * scale;
    }

    /**
     * Ordonnée en pixels d'une latitude.
     *
     * @param latitude latitude en degrés
     * @return ordonnée, croissante vers le sud (le nord est en haut)
     */
    double toScreenY(double latitude) {
        return offsetY + (maxLatitude - latitude) * scale;
    }
}
