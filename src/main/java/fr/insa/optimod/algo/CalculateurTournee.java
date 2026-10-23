package fr.insa.optimod.algo;

import java.util.ArrayList;

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
        listeEtape.add(new Etape(livraison.getEntrepot(), TEMPS_LIMITE_MS, TEMPS_LIMITE_MS))
        for (int i = 1; i<grapheChemins.getNbSommets(); i++) {
            listeChemin.add(grapheChemins.getChemin(tsp.getSolution(i-1), tsp.getSolution(i)));
            listeEtape.add(new Etape(i, i, i));
        }
        return TSPResult.RESULT_OK;
    }
}
