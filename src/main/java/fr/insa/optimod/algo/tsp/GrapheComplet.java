package fr.insa.optimod.algo.tsp;

public class GrapheComplet implements Graphe {

    private static final int COUT_MAX = 40;
    private static final int COUT_MIN = 10;
    int nombreSommets;
    int[][] coutsArcs;

    /**
     * Cree un graphe complet dont les aretes ont un coût compris entre COUT_MIN et
     * COUT_MAX
     *
     * @param nombreSommets
     */
    public GrapheComplet(int nombreSommets) {
        this.nombreSommets = nombreSommets;
        int graineAleatoire = 1;
        coutsArcs = new int[nombreSommets][nombreSommets];
        for (int indiceSommet = 0; indiceSommet < nombreSommets; indiceSommet++) {
            for (int indiceDestination = 0; indiceDestination < nombreSommets; indiceDestination++) {
                if (indiceSommet == indiceDestination)
                    coutsArcs[indiceSommet][indiceDestination] = -1;
                else {
                    int valeurAleatoire = 16807 * (graineAleatoire % 127773) - 2836 * (graineAleatoire / 127773);
                    if (valeurAleatoire > 0)
                        graineAleatoire = valeurAleatoire;
                    else
                        graineAleatoire = 2147483647 + valeurAleatoire;
                    coutsArcs[indiceSommet][indiceDestination] = COUT_MIN + graineAleatoire % (COUT_MAX - COUT_MIN + 1);
                }
            }
        }
    }

    @Override
    public int obtenirNombreSommets() {
        return nombreSommets;
    }

    @Override
    public int obtenirCoutArc(int indiceSommet, int indiceDestination) {
        if (indiceSommet < 0 || indiceSommet >= nombreSommets || indiceDestination < 0 || indiceDestination >= nombreSommets)
            return -1;
        return coutsArcs[indiceSommet][indiceDestination];
    }

    @Override
    public boolean estArc(int indiceSommet, int indiceDestination) {
        if (indiceSommet < 0 || indiceSommet >= nombreSommets || indiceDestination < 0 || indiceDestination >= nombreSommets)
            return false;
        return indiceSommet != indiceDestination;
    }

}
