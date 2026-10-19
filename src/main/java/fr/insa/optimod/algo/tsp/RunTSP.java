package fr.insa.optimod.algo.tsp;

public class RunTSP {

    public static void main(String[] arguments) {
        TSP solveurTsp = new TSP1();
        for (int nombreSommets = 8; nombreSommets <= 16; nombreSommets += 2) {
            System.out.println("Graphes de " + nombreSommets + " sommets :");
            Graphe graphe = new GrapheComplet(nombreSommets);
            long tempsDebut = System.currentTimeMillis();
            solveurTsp.chercherSolution(60000, graphe);
            System.out.print("Solution de longueur " + solveurTsp.obtenirCoutSolution() + " trouvee en "
                    + (System.currentTimeMillis() - tempsDebut) + "ms : ");
            for (int indiceSommet = 0; indiceSommet < nombreSommets; indiceSommet++)
                System.out.print(solveurTsp.obtenirSommetSolution(indiceSommet) + " ");
            System.out.println();
        }
    }
}
