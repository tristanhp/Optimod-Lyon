package fr.insa.optimod.algo.tsp;

import java.util.HashMap;

public class GrapheDepandance implements Graphe {
    private int nbSommets;
    private int[][] cout;
    private HashMap<Integer, Integer> depandance;

    public GrapheDepandance(int nbSommets, int[][] cout, HashMap<Integer, Integer> depandance) {
        this.nbSommets = nbSommets;
        this.cout = cout.clone();
        this.depandance = new HashMap<>(depandance);
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

    public int getDepandance(int indice) {
        return (this.depandance.getOrDefault(indice, -1));
    }
}
