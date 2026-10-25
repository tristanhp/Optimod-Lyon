package fr.insa.optimod.model;

import java.time.LocalTime;
import java.util.List;

/**
 * Tournée d'un livreur : les arrêts dans l'ordre de passage, avec leurs heures
 * de passage, et les chemins parcourus entre deux arrêts consécutifs. Le
 * premier arrêt est le départ de l'entrepôt, le dernier le retour à
 * l'entrepôt.
 *
 * @param arrets arrêts dans l'ordre de passage
 * @param chemins chemin entre l'arrêt {@code i} et l'arrêt {@code i + 1}
 */
public record Tournee(List<Arret> arrets, List<Chemin> chemins) {

    /**
     * Arrêt de la tournée et ses heures de passage.
     *
     * @param noeud adresse de l'arrêt
     * @param arrivee heure d'arrivée
     * @param depart heure de départ (arrivée plus durée sur place)
     */
    public record Arret(Noeud noeud, LocalTime arrivee, LocalTime depart) {
    }

    /**
     * Crée une tournée en copiant les listes.
     *
     * @param arrets arrêts dans l'ordre de passage
     * @param chemins chemins entre arrêts consécutifs
     */
    public Tournee {
        arrets = List.copyOf(arrets);
        chemins = List.copyOf(chemins);
    }

    /** @return heure de retour à l'entrepôt (arrivée au dernier arrêt) */
    public LocalTime obtenirHeureRetour() {
        return arrets.get(arrets.size() - 1).arrivee();
    }
}
