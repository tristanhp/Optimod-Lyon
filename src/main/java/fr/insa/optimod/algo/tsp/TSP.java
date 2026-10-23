package fr.insa.optimod.algo.tsp;

public interface TSP {

    /**
     * Cherche une solution au TSP pour le graphe <code>g</code> dans la limite de
     * <code>tpsLimite</code> millisecondes
     * Attention : la solution calculee commence necessairement par le sommet 0
     *
     * @param tpsLimite (int) Le temps limite de la recherche de solution en ms
     * @param g         (Graphe) Le graphe dans lequelle la solution est cherche
     */
    public void chercherSolution(int tpsLimite, GrapheDepandance g);

    /**
     * @param i (int)
     * @return le ieme sommet visite dans la solution calculee par
     *         <code>chercherSolution</code> (-1 si <code>chercherSolution</code> n'a
     *         pas encore ete appele, ou si i {@literal <} 0 ou i
     *         {@literal >}= g.obtenirNombreSommets())
     */
    public Integer obtenirSommetSolution(int i);

    /**
     * @return la somme des couts des arcs de la solution calculee par
     *         <code>chercherSolution</code> (-1 si <code>chercherSolution</code> n'a
     *         pas encore ete appele).
     */
    public int obtenirCoutSolution();
}
