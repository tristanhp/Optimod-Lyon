package fr.insa.optimod.algo.tsp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class TSP3 extends TSP2 {

    /**
     * Creer un iterateur sur les successeurs du sommet courant avec un ordre base
     * sur la distance du sommet courant aux sommets non vus
     *
     * @param sommetCrt
     * @param nonVus
     * @param g
     * @return un iterateur de <code>nonVus</code> trie par ordre croissant de la
     *         distance de <code>sommetCrt</code>
     */
    @Override
    protected Iterator<Integer> creerIterateurSuccesseurs(Integer sommetCrt, Collection<Integer> nonVus,
            GrapheDepandance g) {
        // On trie la liste restante en fonction de la distance des sommets au sommet
        // courant
        ((ArrayList<Integer>) nonVus).sort((a, b) -> {
            return g.obtenirCoutArc(sommetCrt, b) - g.obtenirCoutArc(sommetCrt, a);
        });
        return new IteratorSeq(nonVus, sommetCrt, g);
    }
}
