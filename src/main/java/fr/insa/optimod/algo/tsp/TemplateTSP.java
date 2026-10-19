package fr.insa.optimod.algo.tsp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public abstract class TemplateTSP implements TSP {

    private Integer[] meilleureSolution;
    protected Graphe graphe;
    private int coutMeilleureSolution;
    private int dureeLimiteMillis;
    private long instantDebutMillis;
    private long instantFinMillis;

    public void chercherSolution(int dureeLimiteMillis, Graphe graphe) {
        if (dureeLimiteMillis <= 0)
            return;
        instantDebutMillis = System.currentTimeMillis();
        this.dureeLimiteMillis = dureeLimiteMillis;
        this.graphe = graphe;
        meilleureSolution = new Integer[graphe.obtenirNombreSommets()];
        Collection<Integer> sommetsNonVisites = new ArrayList<Integer>(graphe.obtenirNombreSommets() - 1);
        for (int indiceSommet = 1; indiceSommet < graphe.obtenirNombreSommets(); indiceSommet++)
            sommetsNonVisites.add(indiceSommet);
        Collection<Integer> sommetsVisites = new ArrayList<Integer>(graphe.obtenirNombreSommets());
        sommetsVisites.add(0); // le premier sommet visite est 0
        coutMeilleureSolution = Integer.MAX_VALUE;
        explorerParSeparationEtEvaluation(0, sommetsNonVisites, sommetsVisites, 0);
        instantFinMillis = System.currentTimeMillis();
    }

    public Integer obtenirSommetSolution(int indiceSommet) {
        if (graphe != null && indiceSommet >= 0 && indiceSommet < graphe.obtenirNombreSommets())
            return meilleureSolution[indiceSommet];
        return -1;
    }

    public int obtenirCoutSolution() {
        if (graphe != null)
            return coutMeilleureSolution;
        return -1;
    }

    public long obtenirDureeCalcul() {
        if (graphe != null)
            return instantFinMillis - instantDebutMillis;
        return -1;
    }

    /**
     * Methode devant etre redefinie par les sous-classes de TemplateTSP
     *
     * @param sommetCourant
     * @param sommetsNonVisites
     * @return une borne inferieure du cout des chemins de <code>graphe</code> partant de
     *         <code>sommetCourant</code>, visitant
     *         tous les sommets de <code>sommetsNonVisites</code> exactement une fois, puis
     *         retournant sur le sommet <code>0</code>.
     */
    protected abstract int calculerBorneInferieure(Integer sommetCourant, Collection<Integer> sommetsNonVisites);

    /**
     * Methode devant etre redefinie par les sous-classes de TemplateTSP
     *
     * @param sommetCourant
     * @param sommetsNonVisites
     * @param graphe
     * @return un iterateur permettant d'iterer sur tous les sommets de
     *         <code>sommetsNonVisites</code> qui sont successeurs de
     *         <code>sommetCourant</code>
     */
    protected abstract Iterator<Integer> creerIterateurSuccesseurs(Integer sommetCourant, Collection<Integer> sommetsNonVisites, Graphe graphe);

    /**
     * Methode definissant le patron (template) d'une resolution par separation et
     * evaluation (branch and bound) du TSP pour le graphe <code>graphe</code>.
     *
     * @param sommetCourant le dernier sommet visite
     * @param sommetsNonVisites    la liste des sommets qui n'ont pas encore ete visites
     * @param sommetsVisites       la liste des sommets deja visites (y compris sommetCourant)
     * @param coutParcours   la somme des couts des arcs du chemin passant par tous les
     *                  sommets de sommetsVisites dans l'ordre ou ils ont ete visites
     */
    private void explorerParSeparationEtEvaluation(int sommetCourant, Collection<Integer> sommetsNonVisites, Collection<Integer> sommetsVisites, int coutParcours) {
        if (System.currentTimeMillis() - instantDebutMillis > dureeLimiteMillis)
            return;
        if (sommetsNonVisites.size() == 0) { // tous les sommets ont ete visites
            if (graphe.estArc(sommetCourant, 0)) { // on peut retourner au sommet de depart (0)
                if (coutParcours + graphe.obtenirCoutArc(sommetCourant, 0) < coutMeilleureSolution) { // on a trouve une solution meilleure
                                                                                 // que meilleureSolution
                    sommetsVisites.toArray(meilleureSolution);
                    coutMeilleureSolution = coutParcours + graphe.obtenirCoutArc(sommetCourant, 0);
                }
            }
        } else if (coutParcours + calculerBorneInferieure(sommetCourant, sommetsNonVisites) < coutMeilleureSolution) {
            Iterator<Integer> iterateurSommets = creerIterateurSuccesseurs(sommetCourant, sommetsNonVisites, graphe);
            while (iterateurSommets.hasNext()) {
                Integer prochainSommet = iterateurSommets.next();
                sommetsVisites.add(prochainSommet);
                sommetsNonVisites.remove(prochainSommet);
                explorerParSeparationEtEvaluation(prochainSommet, sommetsNonVisites, sommetsVisites, coutParcours + graphe.obtenirCoutArc(sommetCourant, prochainSommet));
                sommetsVisites.remove(prochainSommet);
                sommetsNonVisites.add(prochainSommet);
            }
        }
    }
}
