package fr.insa.optimod.algo.tsp;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class IteratorSeq implements Iterator<Integer> {

    private Integer[] candidats;
    private int nombreCandidats;

    /**
     * Cree un iterateur pour iterer sur l'ensemble des sommets de sommetsNonVisites
     * qui sont
     * successeurs de sommetCourant dans le graphe graphe,
     * dans l'odre d'apparition dans <code>sommetsNonVisites</code>
     *
     * @param sommetsNonVisites
     * @param sommetCourant
     * @param graphe
     */
    public IteratorSeq(Collection<Integer> sommetsNonVisites, int sommetCourant, Graphe graphe) {
        this.candidats = new Integer[sommetsNonVisites.size()];
        Iterator<Integer> iterateurSommets = sommetsNonVisites.iterator();
        while (iterateurSommets.hasNext()) {
            Integer sommetCandidat = iterateurSommets.next();
            if (graphe.estArc(sommetCourant, sommetCandidat))
                candidats[nombreCandidats++] = sommetCandidat;
        }
    }

    @Override
    public boolean hasNext() {
        return nombreCandidats > 0;
    }

    @Override
    public Integer next() {
        if (!hasNext()) {
            throw new NoSuchElementException("No more elements available");
        }
        nombreCandidats--;
        return candidats[nombreCandidats];
    }

    @Override
    public void remove() {
    }
}
