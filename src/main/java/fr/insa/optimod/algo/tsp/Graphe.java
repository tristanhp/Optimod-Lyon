package fr.insa.optimod.algo.tsp;

public interface Graphe {

    /**
     * Permet d'obtenir le nombre de sommet de ce graphe
     *
     * @return le nombre de sommets de <code>this</code>
     */
    public abstract int obtenirNombreSommets();

    /**
     * Permet d'obtenir le cout d'un arc du graphe
     *
     * @param indiceSommet sommet
     * @param indiceDestination sommet
     * @return le cout de l'arc (indiceSommet,indiceDestination) si (indiceSommet,indiceDestination) est un arc ; -1 sinon
     */
    public abstract int obtenirCoutArc(int indiceSommet, int indiceDestination);

    /**
     * Permet de vérifier sommetCandidat'il y a un arc entre 2 sommet
     *
     * @param indiceSommet sommet
     * @param indiceDestination sommet
     * @return true si <code>(indiceSommet,indiceDestination)</code> est un arc de <code>this</code>
     */
    public abstract boolean estArc(int indiceSommet, int indiceDestination);

    /**
     * Retourne la depandance d'un sommet
     *
     * @param indice
     * @return la depandance si indice a une depandance, -1 sinon
     */
    public int getDepandance(int indice);

}
