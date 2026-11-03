package fr.insa.optimod.algo.tsp;

/**
 * Voyageur de commerce : cherche l'ordre de passage de coût minimal dans
 * un graphe de dépendances.
 */
public interface TSP {

    enum ResultatCalculTSP {
        // Le temps de calcul depasse le temps maximum autorise
        CALCUL_TROP_LONG,
        // Le temps de calcul depasse le temps maximum autorise et aucun
        // resultat non
        CALCUL_TROP_LONG_SANS_RESULTAT,
                                        // optimal n'a ete trouve
        TEMPS_LIMITE_NEGATIF, // Le temps de calcul maximum autorise est negatif
        // Le cout de la meilleur solution est superieur au cout limite
        COUT_TROP_ELEVE,
                         // => La meilleur solution a une arrive a l'entrepot
                         // apres 13h
        PAS_DE_SOLUTION, // Le graphe n'admet pas de solution
        RESULTAT_OK

    }

    /**
     * Cherche une solution au TSP pour le graphe <code>g</code> dans la limite
     * de
     * <code>tpsLimite</code> millisecondes
     * Attention : la solution calculee commence necessairement par le sommet 0
     *
     * @param tpsLimite (int) Le temps limite de la recherche de solution en ms
     * @param g         (Graphe) Le graphe dans lequelle la solution est cherche
     */
    public ResultatCalculTSP chercherSolution(int tpsLimite,
            GrapheDepandance g);

    /**
     * @param i (int)
     * @return le ieme sommet visite dans la solution calculee par
     *         <code>chercherSolution</code> (-1 si
     *         <code>chercherSolution</code>
     *         n'a
     *         pas encore ete appele, ou si i {@literal <} 0 ou i
     *         {@literal >}= g.obtenirNombreSommets())
     */
    public Integer obtenirSommetSolution(int i);

    /**
     * @return la somme des couts des arcs de la solution calculee par
     *         <code>chercherSolution</code> (-1 si
     *         <code>chercherSolution</code>
     *         n'a
     *         pas encore ete appele).
     */
    public int obtenirCoutSolution();

    /**
     * @return la duree d'exectuion en ms de <code>chercherSolution</code> de
     *        TSP
     *         (-1 si <code>chercherSolution</code> n'a pas encore ete appele)
     */
    public long obtenirDureeCalcul();
}
