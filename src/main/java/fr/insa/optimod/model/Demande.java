package fr.insa.optimod.model;

public class Demande {
    private final int idCollecte;
    private final int idLivraison;
    private final int tempsCollecte; //secondes
    private final int tempsLivraison; //secondes

    public Demande(int idCollecte, int idLivraison, int tempsCollecte, int tempsLivraison) {
        this.idCollecte = idCollecte;
        this.idLivraison = idLivraison;
        this.tempsCollecte = tempsCollecte;
        this.tempsLivraison = tempsLivraison;
    }

    public int getIdCollecte() {
        return idCollecte;
    }

    public int getIdLivraison() {
        return idLivraison;
    }

    public int getTempsCollecte() {
        return tempsCollecte;
    }

    public int getTempsLivraison() {
        return tempsLivraison;
    }

    public int setIdCollecte(int idCollecte) {
        return this.idCollecte;
    }

    public int setIdLivraison(int idLivraison) {
        return this.idLivraison;
    }

    public int setTempsCollecte(int tempsCollecte) {
        return this.tempsCollecte;
    }

    public int setTempsLivraison(int tempsLivraison) {
        return this.tempsLivraison;
    }

    
}