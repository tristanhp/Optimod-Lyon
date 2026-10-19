package fr.insa.optimod.algo.tsp;

public interface TSP {

    /**
     * Cherche une solution au TSP pour le graphe <code>graphe</code> dans la limite de
     * <code>dureeLimiteMillis</code> millisecondes
     * Attention : la solution calculee commence necessairement par le sommet 0
     *
     * @param dureeLimiteMillis (int) Le temps limite de la recherche de solution en ms
     * @param graphe         (Graphe) Le graphe dans lequelle la solution est cherche
     */
    public void chercherSolution(int dureeLimiteMillis, Graphe graphe);

    /**
     * @param indiceSommet (int)
     * @return le ieme sommet visite dans la solution calculee par
     *         <code>chercherSolution</code> (-1 si <code>chercherSolution</code> n'a
     *         pas encore ete appele, ou si indiceSommet {@literal <} 0 ou indiceSommet
     *         {@literal >}= graphe.obtenirNombreSommets())
     */
    public Integer obtenirSommetSolution(int indiceSommet);

    /**
     * @return la somme des couts des arcs de la solution calculee par
     *         <code>chercherSolution</code> (-1 si <code>chercherSolution</code> n'a
     *         pas encore ete appele).
     */
    public int obtenirCoutSolution();
}
