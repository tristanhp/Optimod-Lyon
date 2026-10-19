package fr.insa.optimod.algo.tsp;

import java.util.Collection;
import java.util.Iterator;

public class TSP2 extends TemplateTSP {

    @Override
    protected int bound(Integer sommetCourant, Collection<Integer> nonVus) {
        /*
         * Cette fonction d’évaluation calcule une borne inférieure de la
         * longueur du plus court chemin allant du dernier sommet visité jusqu’à 0
         * (depart/arrive) en passant par chaque sommet non visité
         * exactement une fois 1.
         */
        if (nonVus.size() == 1) {
            return g.getCout(sommetCourant, 0);
        }
        int res = coutMin(sommetCourant, nonVus);
        Iterator<Integer> it = iterator(sommetCourant, nonVus, g);
        while (it.hasNext()) {
            int next = it.next();
            int temp = coutMin(next, nonVus);
            if (temp < g.getCout(next, 0)) {
                res += temp;
            } else {
                res += g.getCout(next, 0);
            }
        }
        // System.out.println(res);
        return res;
    }

    @Override
    protected Iterator<Integer> iterator(Integer sommetCrt, Collection<Integer> nonVus, Graphe g) {
        return new IteratorSeq(nonVus, sommetCrt, g);
    }

    @Override
    protected Boolean croisementArc(Integer sommetCrt, Integer sommetVisite, Collection<Integer> nonVus,
            GrapheDepandance g) {
        return false;
        /*
         * Iterator<Integer> it = iterator(sommetCrt, nonVus, g);
         * if (!it.hasNext())
         * return false;
         * Integer sommetprecedent = it.next();
         * Integer sommet;
         * while (it.hasNext()) {
         * sommet = it.next();
         * if (g.getDepandance(sommet) == sommetCrt)
         * continue;
         * if (cross(g, sommetprecedent, sommet, sommetCrt, sommetVisite)) {
         * return true;
         * }
         * }
         * return false;
         */
    }

    private boolean cross(Graphe g, int i, int j, int k, int l) { // arc (i, j) et arc (k, l)
        return ((g.getCout(i, j)) + g.getCout(k, l)) > (g.getCout(i, k) + g.getCout(j, l));
    }

    private int coutMin(Integer sommet, Collection<Integer> listeSommet) {
        int min = Integer.MAX_VALUE;
        Iterator<Integer> it = iterator(sommet, listeSommet, g);
        while (it.hasNext()) {
            int next = it.next();
            if (g.getCout(sommet, next) < min)
                min = g.getCout(sommet, next);
        }
        return min;
    }

    @Override
    protected Collection<Integer> trieParDistance(Integer sommetCrt, Collection<Integer> nonVus, GrapheDepandance g) {
        return nonVus;
    }
}
