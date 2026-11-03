package fr.insa.optimod.algo.tsp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class TSP4 extends TSP3 {

    /**
     * Verifie s'il existe un croisement d'arc dans la solution actuelle du TSP.
     * S'il existe un croisement, alors cette solution est non-optimale et peut donc
     * etre abandonnee.
     * Cette fontion verifie aussi que ce croisement n'est pas due a des dependances
     *
     * @param sommetCrt
     * @param sommetVisite
     * @param vus
     * @param g
     * @return <code>true</code>s'il y a un croisement qui n'inverserait pas
     *         l'ordre de dependances,
     *         <code>false</code>sinon
     */
    @Override
    protected Boolean croisementArc(Integer sommetCrt, Integer sommetVisite, Collection<Integer> vus,
            GrapheDepandance g) {

        Iterator<Integer> it = new IteratorSeq(vus, sommetCrt, g);
        if (!it.hasNext())
            return false;
        Integer sommetprecedent = it.next();
        Integer sommet;
        while (it.hasNext()) {
            sommet = it.next();
            if (sommet.equals(sommetCrt)) {
                break;
            }
            if (g.getDepandance(sommet) == sommetCrt) {
                continue;
            }
            if (cross(g, sommetprecedent, sommet, sommetCrt, sommetVisite)) {
                if (pasdeDependanceDansLeCroisement(sommet, sommetCrt, vus, g)) {
                    return true;
                }
            }
            sommetprecedent = sommet;
        }
        return false;
    }

    /**
     * Test s'il y a un croisement d'arc entre les arcs
     * <code>(i, j)</code> et <code>(k, l)</code>
     *
     * @return <code>true</code>s'il y a un croisement,
     *         <code>false</code>sinon
     */
    private boolean cross(Graphe g, int i, int j, int k, int l) { // arc (i, j) et arc (k, l)
        return (g.obtenirCoutArc(i, j) + g.obtenirCoutArc(k, l))
                * 0.7 > (g.obtenirCoutArc(i, k) + g.obtenirCoutArc(l, j));
    }

    /**
     * Verifie qu'aucune dependance (precedence collecte / livraison) n'est inversee
     * lors de l'inversion du sous-chemin compris entre sommetDebut et sommetFin.
     *
     * @return <code>true</code>s'il y a une inversion de l'ordre de dependance,
     *         <code>false</code>sinon
     */
    private Boolean pasdeDependanceDansLeCroisement(Integer sommetDebut, Integer sommetFin, Collection<Integer> vus,
            GrapheDepandance g) {
        ArrayList<Integer> vusListe = new ArrayList<>(vus);
        int indiceDebut = vusListe.indexOf(sommetDebut);
        int indiceFin = vusListe.indexOf(sommetFin);

        if (indiceDebut == -1 || indiceFin == -1 || indiceDebut >= indiceFin) {
            return true;
        }

        for (int i = indiceDebut; i < indiceFin; ++i) {
            for (int j = i + 0; j <= indiceFin; ++j) {
                // Si l'element i depend de l'element j (ou inversement), l'inversion casserait
                // l'ordre des dependances
                if ((vusListe.get(i)).equals(g.getDepandance(vusListe.get(j)))) {
                    return false; // Une dependance serait violee/inversee
                }
            }
        }
        return true;
    }
}
