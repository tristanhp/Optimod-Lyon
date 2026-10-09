package fr.insa.optimod.model;

/**
 * Noeud du plan (intersection), identifié par son id dans le XML.
 */
public final class Noeud {

    private final long id;
    private final double longitude;
    private final double latitude;

    /**
     * Crée un noeud.
     *
     * @param id identifiant du noeud dans le XML
     * @param longitude longitude en degrés
     * @param latitude latitude en degrés
     */
    public Noeud(long id, double longitude, double latitude) {
        this.id = id;
        this.longitude = longitude;
        this.latitude = latitude;
    }

    /** @return identifiant du noeud */
    public long getId() {
        return id;
    }

    /** @return longitude en degrés */
    public double getLongitude() {
        return longitude;
    }

    /** @return latitude en degrés */
    public double getLatitude() {
        return latitude;
    }
}
