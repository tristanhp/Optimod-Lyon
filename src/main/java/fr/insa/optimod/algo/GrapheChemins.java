package fr.insa.optimod.algo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import fr.insa.optimod.algo.tsp.Graphe;
import fr.insa.optimod.model.Chemin;
import fr.insa.optimod.model.Demande;
import fr.insa.optimod.model.Livraison;
import fr.insa.optimod.model.Noeud;
import fr.insa.optimod.model.Plan;

/**
 * Graphe complet orienté des plus courts chemins entre les points d'une
 * livraison, utilisé par le TSP. Numérotation des sommets : 0 est
 * l'entrepôt, puis pour la demande k, 2k+1 est sa collecte et 2k+2 sa
 * livraison.
 */
public final class GrapheChemins implements Graphe { // extends GrapheDepandance {

    /** Noeud du plan associé à chaque sommet. */
    private final List<Noeud> sommets;

    /** Plus court chemin entre chaque couple de sommets. */
    private final Chemin[][] chemins;

    /** Dictionnaire des depandances du graphe */
    private HashMap<Integer, Integer> depandance;

    /**
     * Construit le graphe en calculant les plus courts chemins entre tous les
     * points de la livraison.
     *
     * @param plan      plan de la ville
     * @param livraison entrepôt et demandes à desservir
     * @throws IllegalArgumentException si un point est absent du plan
     */
    public GrapheChemins(Plan plan, Livraison livraison) {
        sommets = indexerPoints(plan, livraison);
        chemins = calculerChemins(new AEtoile(plan), sommets);
        depandance = creerDepandanceHashMap(obtenirNombreSommets());
    }

    /**
     * Range les points de la livraison dans l'ordre des sommets.
     *
     * @param plan      plan de la ville
     * @param livraison entrepôt et demandes
     * @return les noeuds, rangés par numéro de sommet
     */
    private static List<Noeud> indexerPoints(Plan plan, Livraison livraison) {
        List<Noeud> points = new ArrayList<>();
        points.add(trouverNoeud(plan, livraison.obtenirEntrepot().obtenirIdentifiant()));
        for (Demande demande : livraison.obtenirDemandes()) {
            points.add(trouverNoeud(plan, demande.obtenirIdentifiantCollecte()));
            points.add(trouverNoeud(plan, demande.obtenirIdentifiantLivraison()));
        }
        return points;
    }

    /**
     * Création un dictionnaire des depandances collecte et livraison
     *
     * @param nbNoeuds le nombre de noeuds
     * @return Hashmap des depandances du graphe
     */
    private static HashMap<Integer, Integer> creerDepandanceHashMap(int nbNoeuds) {
        HashMap<Integer, Integer> depandance = new HashMap<>();
        /*
         * Numérotation des sommets : 0 est l'entrepôt, puis pour la demande k, 2k+1 est
         * sa collecte et 2k+2 sa livraison.
         * Il y 2*n + 1 noeud avec n collecte, n livraison et 1 entrepot
         */
        for (int i = 0; i < (int) (nbNoeuds) - 1 / 2; i++) {
            depandance.put(2 * i + 1, 2 * i + 2);
        }
        return depandance;
    }

    /**
     * Renvoie le noeud du plan qui porte un id donné.
     *
     * @param plan plan de la ville
     * @param id   identifiant du noeud
     * @return le noeud
     * @throws IllegalArgumentException si le noeud est absent du plan
     */
    private static Noeud trouverNoeud(Plan plan, long id) {
        Noeud noeud = plan.obtenirNoeud(id);
        if (noeud == null) {
            throw new IllegalArgumentException(
                    "Point de livraison absent du plan : " + id);
        }
        return noeud;
    }

    /**
     * Calcule le plus court chemin entre chaque couple de sommets.
     *
     * @param aEtoile calculateur de plus courts chemins
     * @param sommets noeuds rangés par numéro de sommet
     * @return les chemins, indexés par sommet de départ puis d'arrivée
     */
    private static Chemin[][] calculerChemins(AEtoile aEtoile,
            List<Noeud> sommets) {
        int nbSommets = sommets.size();
        Chemin[][] chemins = new Chemin[nbSommets][nbSommets];
        for (int i = 0; i < nbSommets; i++) {
            for (int j = 0; j < nbSommets; j++) {
                chemins[i][j] = aEtoile.plusCourtChemin(sommets.get(i),
                        sommets.get(j));
            }
        }
        return chemins;
    }

    @Override
    public int obtenirNombreSommets() {
        return sommets.size();
    }

    /**
     * Renvoie la longueur du plus court chemin entre deux sommets.
     *
     * @param i sommet de départ
     * @param j sommet d'arrivée
     * @return la longueur arrondie au mètre si (i,j) est un arc, -1 sinon
     */
    @Override
    public int obtenirCoutArc(int i, int j) {
        if (!estArc(i, j)) {
            return -1;
        }
        return (int) Math.round(chemins[i][j].longueur());
    }

    /**
     * Indique s'il existe un arc entre deux sommets.
     *
     * @param i sommet de départ
     * @param j sommet d'arrivée
     * @return vrai si les sommets sont distincts et si j est accessible
     *         depuis i
     */
    @Override
    public boolean estArc(int i, int j) {
        return i != j && chemins[i][j].estAccessible();
    }

    /**
     * Renvoie le plus court chemin entre deux sommets.
     *
     * @param i sommet de départ
     * @param j sommet d'arrivée
     * @return le chemin, {@link Chemin#INACCESSIBLE} si j est inaccessible
     *         depuis i
     * @throws ArrayIndexOutOfBoundsException si un sommet n'existe pas
     */
    public Chemin getChemin(int i, int j) {
        return chemins[i][j];
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
     * Renvoie le plus court chemin entre deux sommets.
     *
     * @param i sommet de départ
     * @param j sommet d'arrivée
     * @return le chemin, {@link Chemin#INACCESSIBLE} si j est inaccessible
     *         depuis i
     * @throws ArrayIndexOutOfBoundsException si un sommet n'existe pas
     */
    public long obtenirIdNoeuds(int indiceSommet) {
        return sommets.get(indiceSommet).obtenirIdentifiant();
    }
}
