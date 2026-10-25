package fr.insa.optimod.model;

/**
 * Nœud du plan : un croisement, identifié par son identifiant dans le fichier
 * de plan. Une adresse est un nœud du plan où l'on passe.
 *
 * @param identifiant identifiant du nœud dans le fichier de plan
 * @param longitude longitude en degrés
 * @param latitude latitude en degrés
 */
public record Noeud(long identifiant, double longitude, double latitude) {
}
