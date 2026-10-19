package fr.insa.optimod.algo.tsp;

import java.util.Collection;
import java.util.Iterator;

public class TSP1 extends TemplateTSP {

    @Override
    protected int calculerBorneInferieure(Integer sommetCourant, Collection<Integer> sommetsNonVisites) {
        return 0;
    }

    @Override
    protected Iterator<Integer> creerIterateurSuccesseurs(Integer sommetCourant, Collection<Integer> sommetsNonVisites, Graphe graphe) {
        return new IteratorSeq(sommetsNonVisites, sommetCourant, graphe);
    }

}
