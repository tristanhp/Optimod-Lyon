package fr.insa.optimod.algo;

import java.util.ArrayList;
import java.util.List;

import fr.insa.optimod.algo.tsp.TSP;
import fr.insa.optimod.algo.tsp.TSP2;
import fr.insa.optimod.model.Chemin;
import fr.insa.optimod.model.Etape;
import fr.insa.optimod.model.Livraison;
import fr.insa.optimod.model.Plan;

/**
 * Calcul de la tournée d'un lot de demandes sur un plan (Cas 5). Le contrôleur
 * s'appuie sur cette interface pour lancer le calcul sans connaître
 * l'algorithme (plus courts chemins, TSP).
 */
enum TSPResult {
    RESULT_OK,
    EXECUTION_DEPASSE,
    HORAIRE_LIMITE_DEPASSE,
    PAS_DE_SOLUTION
}

public class CalculateurTournee {
    private final int TEMPS_LIMITE_MS = 10000;
    public static final double VITESSE_KMH = 15;
    GrapheChemins grapheChemins;
    ArrayList<Chemin> listeChemin;
    ArrayList<Etape> listeEtape;

    /**
     * Calcule la tournée.
     *
     * @param plan      plan de la ville
     * @param livraison demandes à livrer et entrepôt de départ
     * @return les points de la tournée dans l'ordre de visite, avec leurs
     *         heures d'arrivée et de départ
     * @throws IllegalStateException si aucune tournée n'est possible
     */
    public TSPResult calculer(Plan plan, Livraison livraison) {
        grapheChemins = new GrapheChemins(plan, livraison);
        TSP tsp = new TSP2();
        tsp.chercheSolution(TEMPS_LIMITE_MS, grapheChemins);
        listeEtape.add(new Etape(livraison.obtenirEntrepot().obtenirIdentifiant(), 0, 0));
        int heure = 0;
        for (int i = 1; i < grapheChemins.obtenirNombreSommets(); i++) {
            Chemin chemin = grapheChemins.getChemin(tsp.obtenirSommetSolution(i - 1), tsp.obtenirSommetSolution(i));
            listeChemin.add(chemin);
            heure += Math.round(chemin.longueur() / VITESSE_KMH * 1000 / 3600);
            listeEtape.add(new Etape(grapheChemins.obtenirIdNoeuds(i), 100, heure));
            heure += 100;
        }
        return TSPResult.RESULT_OK;
    }

    public List<Chemin> obtenirListeChemin() {
        return listeChemin;
    }

    public List<Etape> obtenirListeEtape() {
        return listeEtape;
    }
}
