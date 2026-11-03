package fr.insa.optimod.algo.tsp;

import java.util.Collection;

public class TSP5 extends TSP3 {

    /**
     * Cette fonction d’évaluation calcule une borne inférieure de la solution
     * actuelle.
     * Elle calcule l'arbre couvrant minimal des sommets non vus grace à
     * l'algorithme de Prim
     * La borne inferieur est le cout de cette arbre couvrant minimal
     *
     * @return le cout de l'arbre couvrant minimal
     */
    @Override
    protected int calculerBorneInferieure(Integer sommetCourant, Collection<Integer> nonVus) {
        // Si tous les sommets ont été visités, on calcule uniquement le coût du retour
        // au dépôt (sommet 0)
        if (nonVus.isEmpty()) {
            int coutRetour = g.obtenirCoutArc(sommetCourant, 0);
            return (coutRetour != -1) ? coutRetour : 0;
        }

        int borne = 0;
        java.util.List<Integer> sommetsRestants = new java.util.ArrayList<>(nonVus);
        int n = g.obtenirNombreSommets();

        // Tableaux pour l'algorithme de Prim sur les sommets non vus
        boolean[] dansArbre = new boolean[n];
        int[] minCoutVersSommet = new int[n];

        for (int sommet : sommetsRestants) {
            minCoutVersSommet[sommet] = Integer.MAX_VALUE;
        }

        // On initialise l'arbre de Prim avec le premier sommet de nonVus
        int premierSommet = sommetsRestants.get(0);
        dansArbre[premierSommet] = true;

        for (int sommet : sommetsRestants) {
            if (sommet != premierSommet && g.estArc(premierSommet, sommet)) {
                int cout = g.obtenirCoutArc(premierSommet, sommet);
                if (cout != -1) {
                    minCoutVersSommet[sommet] = cout;
                }
            }
        }

        int nbSommetsTraites = 1;
        while (nbSommetsTraites < sommetsRestants.size()) {
            int prochainSommet = -1;
            int minCout = Integer.MAX_VALUE;

            // Recherche du sommet non visité le plus proche de l'arbre en construction
            for (int sommet : sommetsRestants) {
                if (!dansArbre[sommet] && minCoutVersSommet[sommet] < minCout) {
                    minCout = minCoutVersSommet[sommet];
                    prochainSommet = sommet;
                }
            }

            if (prochainSommet == -1 || minCout == Integer.MAX_VALUE) {
                break; // Graphe non connexe ou arête manquante
            }

            dansArbre[prochainSommet] = true;
            borne += minCout;
            nbSommetsTraites++;

            // Mise à jour des coûts par rapport au nouveau sommet ajouté
            for (int sommet : sommetsRestants) {
                if (!dansArbre[sommet] && g.estArc(prochainSommet, sommet)) {
                    int cout = g.obtenirCoutArc(prochainSommet, sommet);
                    if (cout != -1 && cout < minCoutVersSommet[sommet]) {
                        minCoutVersSommet[sommet] = cout;
                    }
                }
            }
        }

        // 2. Ajout de l'arête minimale sortante du sommet courant vers l'ensemble des
        // sommets non vus
        int minCoutCourant = Integer.MAX_VALUE;
        for (int sommet : sommetsRestants) {
            if (g.estArc(sommetCourant, sommet)) {
                int cout = g.obtenirCoutArc(sommetCourant, sommet);
                if (cout != -1 && cout < minCoutCourant) {
                    minCoutCourant = cout;
                }
            }
        }
        if (minCoutCourant != Integer.MAX_VALUE) {
            borne += minCoutCourant;
        }

        // 3. Ajout de l'arête minimale entrante vers le dépôt (sommet 0)
        int minCoutRetour = Integer.MAX_VALUE;
        for (int sommet : sommetsRestants) {
            if (g.estArc(sommet, 0)) {
                int cout = g.obtenirCoutArc(sommet, 0);
                if (cout != -1 && cout < minCoutRetour) {
                    minCoutRetour = cout;
                }
            }
        }
        if (minCoutRetour == Integer.MAX_VALUE && g.estArc(sommetCourant, 0)) {
            minCoutRetour = g.obtenirCoutArc(sommetCourant, 0);
        }
        if (minCoutRetour != Integer.MAX_VALUE) {
            borne += minCoutRetour;
        }

        return borne;
    }
}
