package fr.insa.optimod.algo.tsp;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

public class GrapheDepandance implements Graphe {
    private int nbSommets;
    private int[][] cout;
    private HashMap<Integer, Integer> depandance;
    private HashMap<Integer, Integer> depandanceInverser;

    public GrapheDepandance(int nbSommets, int[][] cout, HashMap<Integer, Integer> depandance) {
        this.nbSommets = nbSommets;
        this.cout = cout.clone();
        this.depandance = new HashMap<>(depandance);
        this.depandanceInverser = new HashMap<Integer, Integer>();
        Iterator<Entry<Integer, Integer>> it = depandance.entrySet().iterator();
        while (it.hasNext()) {
            Entry<Integer, Integer> temp = it.next();
            this.depandanceInverser.put(temp.getValue(), temp.getKey());
        }
    }

    @Override
    public int obtenirNombreSommets() {
        return nbSommets;
    }

    @Override
    public int obtenirCoutArc(int i, int j) {
        if (i < 0 || i >= nbSommets || j < 0 || j >= nbSommets)
            return -1;
        return cout[i][j];
    }

    @Override
    public boolean estArc(int i, int j) {
        if (i < 0 || i >= nbSommets || j < 0 || j >= nbSommets)
            return false;
        return i != j;
    }

    /**
     * Retourne la depandance d'un sommet
     *
     * @param indice
     * @return la depandance si indice a une depandance, -1 sinon
     */
    public int getDepandance(int indice) {
        return (this.depandance.getOrDefault(indice, -1));
    }

    /**
     * Retourne le sommet qui depent du sommet en entre
     *
     * @param indice
     * @return le sommet si indice est une depandance, -1 sinon
     */
    public int obtenirSousDepandance(int indice) {
        return (this.depandanceInverser.getOrDefault(indice, -1));
    }

}
