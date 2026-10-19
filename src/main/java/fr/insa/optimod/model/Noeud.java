package fr.insa.optimod.model;

/**
 * Noeud du plan (intersection), identifié par son identifiant dans le XML.
 */
public final class Noeud {

    private final long identifiant;
    private final double longitude;
    private final double latitude;

    /**
     * Crée un noeud.
     *
     * @param identifiant identifiant du noeud dans le XML
     * @param longitude longitude en degrés
     * @param latitude latitude en degrés
     */
    public Noeud(long identifiant, double longitude, double latitude) {
        this.identifiant = identifiant;
        this.longitude = longitude;
        this.latitude = latitude;
    }

    /** @return identifiant du noeud */
    public long obtenirIdentifiant() {
        return identifiant;
    }

    /** @return longitude en degrés */
    public double obtenirLongitude() {
        return longitude;
    }

    /** @return latitude en degrés */
    public double obtenirLatitude() {
        return latitude;
    }
}
