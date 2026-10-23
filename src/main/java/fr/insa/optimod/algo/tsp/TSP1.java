package fr.insa.optimod.algo.tsp;

import java.util.Collection;
import java.util.Iterator;

public class TSP1 extends TemplateTSP {

    @Override
    protected int calculerBorneInferieure(Integer sommetCourant, Collection<Integer> nonVus) {
        return 0;
    }

    @Override
    protected Iterator<Integer> creerIterateurSuccesseurs(Integer sommetCrt, Collection<Integer> nonVus, Graphe g) {
        return new IteratorSeq(nonVus, sommetCrt, g);
    }

    @Override
    protected Boolean croisementArc(Integer sommetCrt, Integer sommetVisite, Collection<Integer> nonVus,
            GrapheDepandance g) {
        return false;
    }

    @Override
    protected Collection<Integer> trieParDistance(Integer sommetCrt, Collection<Integer> nonVus, GrapheDepandance g) {
        return nonVus;
    }
}
