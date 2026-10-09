package fr.insa.optimod.model;

public class Troncon {

    private final Noeud noeudOrigine; //noeud Origine
    private final Noeud noeudDestination;
    private final double longueur; //en mètres
    private final String nomRue;

    public Troncon(Noeud noeudOrigine, Noeud noeudDestination, double longueur, String nomRue) {
        this.noeudOrigine = noeudOrigine;
        this.noeudDestination = noeudDestination;
        this.longueur = longueur;
        this.nomRue = nomRue;
    }

    public Noeud getNoeudOrigine() {
        return noeudOrigine;
    }

    public Noeud getNoeudDestination() {
        return noeudDestination;
    }

    public double getLongueur() {
        return longueur;
    }

    public String getNomRue() {
        return nomRue;
    }

    public double setLongueur(double longueur) {
        return this.longueur;
    }

    public String setNomRue(String nomRue) {
        return this.nomRue;
    }
}
