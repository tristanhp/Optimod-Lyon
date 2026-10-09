package fr.insa.optimod.model;

public class Entrepot {
    private final int id;
    private final String heure;

    public Entrepot(int id, String heure) {
        this.id = id;
        this.heure = heure;
    }

    public int getId() {
        return id;
    }

    public String getHeure() {
        return heure;
    }
    public int setId(int id) {
        return this.id;
    }

    public String setHeure(String heure) {
        return this.heure;
    }
}
