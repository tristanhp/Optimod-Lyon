package fr.insa.optimod.view;

import java.util.Collection;

import fr.insa.optimod.model.Noeud;

/**
 * Passage des coordonnées géographiques (latitude, longitude) aux pixels du
 * canevas. Projection équirectangulaire corrigée par le cosinus de la latitude
 * moyenne, nord en haut, le plan entier étant centré et rendu visible.
 */
final class MapProjection {

    /** Longitude minimale du plan. */
    private final double longitudeMinimale;

    /** Latitude maximale du plan. */
    private final double latitudeMaximale;

    /** Pixels par degré de latitude. */
    private final double echelle;

    /** Rapport de longueur d'un degré de longitude sur un de latitude. */
    private final double correctionLongitude;

    /** Décalage horizontal en pixels. */
    private final double decalageHorizontal;

    /** Décalage vertical en pixels. */
    private final double decalageVertical;

    /**
     * Calcule la projection qui fait tenir les noeuds dans la zone donnée.
     *
     * @param noeuds noeuds du plan à afficher (au moins un)
     * @param largeur largeur disponible, en pixels
     * @param hauteur hauteur disponible, en pixels
     * @param margeInterieure marge intérieure, en pixels
     */
    MapProjection(Collection<Noeud> noeuds, double largeur, double hauteur, double margeInterieure) {
        double latitudeMinimale = Double.POSITIVE_INFINITY;
        double latitudeMaximaleLue = Double.NEGATIVE_INFINITY;
        double longitudeMinimaleLue = Double.POSITIVE_INFINITY;
        double longitudeMaximale = Double.NEGATIVE_INFINITY;
        for (Noeud noeud : noeuds) {
            latitudeMinimale = Math.min(latitudeMinimale, noeud.obtenirLatitude());
            latitudeMaximaleLue = Math.max(latitudeMaximaleLue, noeud.obtenirLatitude());
            longitudeMinimaleLue = Math.min(longitudeMinimaleLue, noeud.obtenirLongitude());
            longitudeMaximale = Math.max(longitudeMaximale, noeud.obtenirLongitude());
        }
        correctionLongitude = Math.cos(Math.toRadians((latitudeMinimale + latitudeMaximaleLue) / 2));
        double etendueHorizontale = Math.max((longitudeMaximale - longitudeMinimaleLue) * correctionLongitude, 1e-9);
        double etendueVerticale = Math.max(latitudeMaximaleLue - latitudeMinimale, 1e-9);
        double largeurDisponible = Math.max(largeur - 2 * margeInterieure, 1);
        double hauteurDisponible = Math.max(hauteur - 2 * margeInterieure, 1);
        echelle = Math.min(largeurDisponible / etendueHorizontale, hauteurDisponible / etendueVerticale);
        longitudeMinimale = longitudeMinimaleLue;
        latitudeMaximale = latitudeMaximaleLue;
        decalageHorizontal = margeInterieure + (largeurDisponible - etendueHorizontale * echelle) / 2;
        decalageVertical = margeInterieure + (hauteurDisponible - etendueVerticale * echelle) / 2;
    }

    /**
     * Abscisse en pixels d'une longitude.
     *
     * @param longitude longitude en degrés
     * @return abscisse, croissante vers l'est
     */
    double calculerAbscisseEcran(double longitude) {
        return decalageHorizontal + (longitude - longitudeMinimale) * correctionLongitude * echelle;
    }

    /**
     * Ordonnée en pixels d'une latitude.
     *
     * @param latitude latitude en degrés
     * @return ordonnée, croissante vers le sud (le nord est en haut)
     */
    double calculerOrdonneeEcran(double latitude) {
        return decalageVertical + (latitudeMaximale - latitude) * echelle;
    }
}
