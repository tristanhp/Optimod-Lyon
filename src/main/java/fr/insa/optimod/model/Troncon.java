package fr.insa.optimod.model;

/**
 * Tronçon de rue orienté entre deux noeuds.
 */
public final class Troncon {

    private final Noeud noeudOrigine;
    private final Noeud noeudDestination;
    private final double longueur; //en mètres
    private final String nomRue;

    /**
     * Crée un tronçon.
     *
     * @param noeudOrigine noeud de départ
     * @param noeudDestination noeud d'arrivée
     * @param longueur longueur en mètres
     * @param nomRue nom de la rue (peut être vide)
     */
    public Troncon(Noeud noeudOrigine, Noeud noeudDestination, double longueur, String nomRue) {
        this.noeudOrigine = noeudOrigine;
        this.noeudDestination = noeudDestination;
        this.longueur = longueur;
        this.nomRue = nomRue;
    }

    /** @return noeud de départ */
    public Noeud getNoeudOrigine() {
        return noeudOrigine;
    }

    /** @return noeud d'arrivée */
    public Noeud getNoeudDestination() {
        return noeudDestination;
    }

    /** @return longueur en mètres */
    public double getLongueur() {
        return longueur;
    }

    /** @return nom de la rue */
    public String getNomRue() {
        return nomRue;
    }
}
