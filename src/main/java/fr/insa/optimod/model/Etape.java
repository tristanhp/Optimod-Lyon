package fr.insa.optimod.model;

/**
 * Point visité par une tournée : un noeud du plan et le temps passé dessus
 * (collecte ou livraison ; zéro pour l'entrepôt).
 *
 * @param idNoeud       id du noeud du plan où se trouve le point
 * @param dureeSecondes temps passé sur le point, en secondes (positif ou nul)
 * @param heureArrivee
 */
public record Etape(long idNoeud, int dureeSecondes, int heureArrivee) {

    /**
     * Crée une étape.
     *
     * @param idNoeud       id du noeud du plan où se trouve le point
     * @param dureeSecondes temps passé sur le point, en secondes
     * @param heureArrivee  heure d'arrive sur le noeud, en seconde
     * @throws IllegalArgumentException si la durée est négative
     */
    public Etape {
        if (dureeSecondes < 0) {
            throw new IllegalArgumentException(
                    "Durée négative : " + dureeSecondes + " s.");
        }
    }
}
