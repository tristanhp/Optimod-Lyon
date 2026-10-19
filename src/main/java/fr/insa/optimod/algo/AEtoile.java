package fr.insa.optimod.algo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import fr.insa.optimod.model.Noeud;
import fr.insa.optimod.model.Plan;
import fr.insa.optimod.model.Troncon;

/**
 * Calcul du plus court chemin entre deux noeuds d'un plan avec l'algorithme
 * A* : Dijkstra guidé par la distance à vol d'oiseau jusqu'à la cible.
 */
public final class AEtoile {

    /** Rayon moyen de la Terre, en mètres. */
    private static final double RAYON_TERRE = 6_371_000;

    /**
     * Coefficient appliqué à la distance à vol d'oiseau. Les longueurs des
     * XML fournis sont arrondies et descendent jusqu'à 0.91 fois la distance
     * à vol d'oiseau : sans ce coefficient, l'heuristique surestimerait
     * certains chemins et A* ne serait plus exact.
     */
    private static final double COEFFICIENT_HEURISTIQUE = 0.9;

    /** Noeud à explorer, avec sa distance depuis la source et f = g + h. */
    private record Etiquette(Noeud noeud, double distance, double estimation) {
    }

    private final Plan plan;

    /**
     * Prépare les calculs sur un plan.
     *
     * @param plan plan dans lequel chercher les chemins
     */
    public AEtoile(Plan plan) {
        this.plan = plan;
    }

    /**
     * Cherche le plus court chemin d'un noeud à un autre en suivant le sens
     * des tronçons.
     *
     * @param source noeud de départ
     * @param cible noeud d'arrivée
     * @return le plus court chemin, ou {@link Chemin#INACCESSIBLE} si la cible
     *         ne peut pas être atteinte
     */
    public Chemin plusCourtChemin(Noeud source, Noeud cible) {
        // Etape 1: initialiser la recherche depuis la source
        Map<Noeud, Double> distances = new HashMap<>();
        Map<Noeud, Troncon> predecesseurs = new HashMap<>();
        PriorityQueue<Etiquette> file = new PriorityQueue<>(
                Comparator.comparingDouble(Etiquette::estimation));
        distances.put(source, 0.0);
        file.add(new Etiquette(source, 0, heuristique(source, cible)));

        // Etape 2: explorer d'abord le noeud dont f = g + h est le plus petit
        while (!file.isEmpty()) {
            Etiquette courante = file.poll();
            Noeud noeud = courante.noeud();
            if (noeud.equals(cible)) {
                return reconstruireChemin(predecesseurs, source, cible,
                        courante.distance());
            }
            // Etiquette périmée : un chemin plus court a été trouvé depuis
            if (courante.distance() > distances.get(noeud)) {
                continue;
            }
            for (Troncon troncon : plan.getTronconsSortants(noeud)) {
                Noeud voisin = troncon.getNoeudDestination();
                double distance = courante.distance() + troncon.getLongueur();
                if (distance < distances.getOrDefault(voisin,
                        Double.POSITIVE_INFINITY)) {
                    distances.put(voisin, distance);
                    predecesseurs.put(voisin, troncon);
                    file.add(new Etiquette(voisin, distance,
                            distance + heuristique(voisin, cible)));
                }
            }
        }

        // Etape 3: la file est vide sans avoir atteint la cible
        return Chemin.INACCESSIBLE;
    }

    /**
     * Remonte les prédécesseurs de la cible jusqu'à la source.
     *
     * @param predecesseurs tronçon par lequel chaque noeud a été atteint
     * @param source noeud de départ
     * @param cible noeud d'arrivée
     * @param longueur longueur du chemin trouvé
     * @return le chemin de la source à la cible
     */
    private static Chemin reconstruireChemin(
            Map<Noeud, Troncon> predecesseurs, Noeud source, Noeud cible,
            double longueur) {
        List<Troncon> troncons = new ArrayList<>();
        for (Noeud noeud = cible; !noeud.equals(source);) {
            Troncon troncon = predecesseurs.get(noeud);
            troncons.add(troncon);
            noeud = troncon.getNoeudOrigine();
        }
        Collections.reverse(troncons);
        return new Chemin(longueur, troncons);
    }

    /**
     * Estime la longueur restante d'un noeud à la cible, sans jamais la
     * surestimer sur les plans fournis : distance à vol d'oiseau (formule de
     * haversine) multipliée par {@link #COEFFICIENT_HEURISTIQUE}.
     *
     * @param noeud noeud courant
     * @param cible noeud d'arrivée
     * @return une borne inférieure de la longueur restante, en mètres
     */
    static double heuristique(Noeud noeud, Noeud cible) {
        double latitudeA = Math.toRadians(noeud.getLatitude());
        double latitudeB = Math.toRadians(cible.getLatitude());
        double ecartLatitude = latitudeB - latitudeA;
        double ecartLongitude =
                Math.toRadians(cible.getLongitude() - noeud.getLongitude());
        double h = Math.pow(Math.sin(ecartLatitude / 2), 2)
                + Math.cos(latitudeA) * Math.cos(latitudeB)
                * Math.pow(Math.sin(ecartLongitude / 2), 2);
        double volOiseau = 2 * RAYON_TERRE * Math.asin(Math.sqrt(h));
        return COEFFICIENT_HEURISTIQUE * volOiseau;
    }
}
