package fr.insa.optimod.algo.tsp;

public interface Graphe {

    /**
     * Permet d'obtenir le nombre de sommet de ce graphe
     *
     * @return le nombre de sommets de <code>this</code>
     */
    public abstract int getNbSommets();

    /**
     * Permet d'obtenir le cout d'un arc du graphe
     *
     * @param i sommet
     * @param j sommet
     * @return le cout de l'arc (i,j) si (i,j) est un arc ; -1 sinon
     */
    public abstract int getCout(int i, int j);

    /**
     * Permet de vérifier s'il y a un arc entre 2 sommet
     *
     * @param i sommet
     * @param j sommet
     * @return true si <code>(i,j)</code> est un arc de <code>this</code>
     */
    public abstract boolean estArc(int i, int j);

    /**
     * Retourne la depandance d'un sommet
     *
     * @param indice
     * @return la depandance si indice a une depandance, -1 sinon
     */
    public int getDepandance(int indice);

}
