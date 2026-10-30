package fr.insa.optimod.algo.tsp;

public interface TSP {

    enum ResultatCalculTSP {
        CALCUL_TROP_LONG, // Le temps de calcul depasse le temps maximum autorise
        CALCUL_TROP_LONG_SANS_RESULTAT, // Le temps de calcul depasse le temps maximum autorise et aucun resultat non
                                        // optimal n'a ete trouve
        TEMPS_LIMITE_NEGATIF, // Le temps de calcul maximum autorise est negatif
        COUT_TROP_ELEVE, // Le cout de la meilleur solution est superieur au cout limite
                         // => La meilleur solution a une arrive a l'entrepot apres 13h
        PAS_DE_SOLUTION, // Le graphe n'admet pas de solution
        RESULTAT_OK

    }

    /**
     * Cherche une solution au TSP pour le graphe <code>g</code> dans la limite de
     * <code>tpsLimite</code> millisecondes
     * Attention : la solution calculee commence necessairement par le sommet 0
     *
     * @param tpsLimite  (int) Le temps limite de la recherche de solution en ms
     * @param g          (Graphe) Le graphe dans lequelle la solution est cherche
     * @param coutLimite (int) Le cout limite de la solution a ne pas depasse. Si la
     *                   solution depasse ce cout,
     *                   <code> ResultatCalculTSP.COUT_TROP_ELEVE </code> est
     *                   renvoye. -1 pour ne pas en definir
     */
    public ResultatCalculTSP chercherSolution(int tpsLimite, GrapheDepandance g, int coutLimite);

    /**
     * @param i (int)
     * @return le ieme sommet visite dans la solution calculee par
     *         <code>chercherSolution</code> (-1 si <code>chercherSolution</code>
     *         n'a
     *         pas encore ete appele, ou si i {@literal <} 0 ou i
     *         {@literal >}= g.obtenirNombreSommets())
     */
    public Integer obtenirSommetSolution(int i);

    /**
     * @return la somme des couts des arcs de la solution calculee par
     *         <code>chercherSolution</code> (-1 si <code>chercherSolution</code>
     *         n'a
     *         pas encore ete appele).
     */
    public int obtenirCoutSolution();

    /**
     * @return la duree d'exectuion en ms de <code>chercherSolution</code> de TSP
     *         (-1 si <code>chercherSolution</code> n'a pas encore ete appele)
     */
    public long obtenirDureeCalcul();
}
