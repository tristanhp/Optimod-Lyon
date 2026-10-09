package fr.insa.optimod.model;

public class Noeud {

    private final Integer id;
    private final double longitude;
    private final double latitude;

    public Noeud(Integer id, double longitude, double latitude) {
        this.id = id;
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public Integer getId() {
        return id;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public Integer setId(int id) {
        return this.id;
    }

    public double setLongitude(double longitude) {
        return this.longitude;
    }

    public double setLatitude(double latitude) {
        return this.latitude;
    }
}
