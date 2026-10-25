package fr.insa.optimod.model;

/**
 * Tronçon : un bout de rue orienté entre deux nœuds.
 *
 * @param origine nœud de départ
 * @param destination nœud d'arrivée
 * @param longueur longueur en mètres
 * @param nomRue nom de la rue (peut être vide)
 */
public record Troncon(Noeud origine, Noeud destination, double longueur,
        String nomRue) {
}
