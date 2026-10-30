package fr.insa.optimod.algo.tsp;

import java.util.Collection;
import java.util.Iterator;

public class TSP2 extends TSP1 {

    @Override
    protected int calculerBorneInferieure(Integer sommetCourant, Collection<Integer> nonVus) {
        /*
         * Cette fonction d’évaluation calcule une borne inférieure de la solution
         * actuelle.
         * Pour le sommet courant, elle prend le chemin le plus court vers un autre
         * sommet non vus (ou 0, s'il reste que 0) et pour tous les autres sommets non
         * vus, elle prends le chemin le plus court vers un autre sommet non vus ou 0.
         * Tous ces cout sont additionés pour obtenir une borne inferieur.
         */
        if (nonVus.size() == 1) {
            return g.obtenirCoutArc(sommetCourant, 0);
        }
        int res = coutMin(sommetCourant, nonVus);
        Iterator<Integer> it = new IteratorSeq(nonVus, sommetCourant, g);
        while (it.hasNext()) {
            int next = it.next();
            int temp = coutMin(next, nonVus);
            if (temp < g.obtenirCoutArc(next, 0)) {
                res += temp;
            } else {
                res += g.obtenirCoutArc(next, 0);
            }
        }
        return res;
    }

    /**
     * Calcul le cout minimal d'un sommet vers une liste de sommet
     *
     * @param sommet
     * @param listeSommet
     * @return le minimum des couts de <code>sommet</code> vers
     *         <code>listeSommet</code>
     */
    private int coutMin(Integer sommet, Collection<Integer> listeSommet) {
        int min = Integer.MAX_VALUE;
        Iterator<Integer> it = new IteratorSeq(listeSommet, sommet, g);
        while (it.hasNext()) {
            int next = it.next();
            if (g.obtenirCoutArc(sommet, next) < min)
                min = g.obtenirCoutArc(sommet, next);
        }
        return min;
    }
}
