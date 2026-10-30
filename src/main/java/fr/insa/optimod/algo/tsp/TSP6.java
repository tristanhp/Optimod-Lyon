package fr.insa.optimod.algo.tsp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TSP6 extends TSP3 {

    /**
     * NON-FONCTIONNELLE
     * TODO : A SUPRIMMER LORS DE TIG-98
     *
     * @return la borne inferieur
     */
    @Override
    protected int calculerBorneInferieure(Integer sommetCourant, Collection<Integer> nonVus) {
        List<Integer> sommets = new ArrayList<>(nonVus);
        if (!sommets.contains(sommetCourant)) {
            sommets.add(sommetCourant);
        }
        if (!sommets.contains(0)) {
            sommets.add(0);
        }

        int n = sommets.size();
        if (n <= 1) {
            return 0;
        }
        if (n == 2) {
            int u = sommets.get(0);
            int v = sommets.get(1);
            int cout = g.obtenirCoutArc(u, v);
            return (cout != -1) ? cout : 0;
        }

        // --- 1. Calcul d'un Arbre Couvrant Minimum (MST) via l'algorithme de Prim ---
        boolean[] inMST = new boolean[n];
        int[] minDist = new int[n];
        for (int i = 0; i < n; i++) {
            minDist[i] = Integer.MAX_VALUE;
        }

        minDist[0] = 0;
        int mstCost = 0;

        for (int i = 0; i < n; i++) {
            int u = -1;
            int minVal = Integer.MAX_VALUE;
            for (int j = 0; j < n; j++) {
                if (!inMST[j] && minDist[j] < minVal) {
                    minVal = minDist[j];
                    u = j;
                }
            }

            if (u == -1 || minVal == Integer.MAX_VALUE) {
                break;
            }

            inMST[u] = true;
            mstCost += minVal;

            for (int v = 0; v < n; v++) {
                if (!inMST[v]) {
                    int cout = g.obtenirCoutArc(sommets.get(u), sommets.get(v));
                    if (cout != -1 && cout < minDist[v]) {
                        minDist[v] = cout;
                    }
                }
            }
        }

        // --- 2. Ajout des arêtes minimales incidentes pour respecter la structure du
        // TSP ---
        // On cherche l'arête sortante la moins coûteuse à partir de `sommetCourant`
        int minArcCourant = Integer.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            if (sommets.get(i).equals(sommetCourant))
                continue;
            int cout = g.obtenirCoutArc(sommetCourant, sommets.get(i));
            if (cout != -1 && cout < minArcCourant) {
                minArcCourant = cout;
            }
        }

        // Borne inférieure finale (MST + liaison minimale du sommet courant)
        return mstCost + (minArcCourant == Integer.MAX_VALUE ? 0 : minArcCourant);
    }
}
