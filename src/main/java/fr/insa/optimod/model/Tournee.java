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
     * @param debutPlage début de la plage annoncée, inclus
     * @param finPlage fin de la plage annoncée, incluse
     */
    public record Arret(Noeud noeud, LocalTime arrivee, LocalTime depart,
            LocalTime debutPlage, LocalTime finPlage) {

        /**
         * Fixe une plage annoncée d'une heure de part et d'autre de l'arrivée.
         * @param noeud adresse
         * @param arrivee heure d'arrivée
         * @param depart heure de départ
         */
        public Arret(Noeud noeud, LocalTime arrivee, LocalTime depart) {
            this(noeud, arrivee, depart, arrivee.minusHours(1),
                    arrivee.plusHours(1));
        }

        /**
         * Renvoie vrai si l'arrivée sort de la plage annoncée.
         * @return vrai si l'arrivée sort de la plage annoncée
         */
        public boolean estHorsPlage() {
            if (debutPlage.isAfter(finPlage)) {
                return arrivee.isBefore(debutPlage)
                        && arrivee.isAfter(finPlage);
            }
            return arrivee.isBefore(debutPlage) || arrivee.isAfter(finPlage);
        }

        /**
         * Mesure le décalage depuis l'arrivée initialement annoncée.
         * @return secondes de décalage, positives pour un retard
         */
        public long obtenirDecalageSecondes() {
            return java.time.Duration.between(debutPlage.plusHours(1), arrivee)
                    .getSeconds();
        }
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

    /**
     * Renvoie heure de retour à l'entrepôt (arrivée au dernier arrêt).
     * @return heure de retour à l'entrepôt (arrivée au dernier arrêt)
     */
    public LocalTime obtenirHeureRetour() {
        return arrets.get(arrets.size() - 1).arrivee();
    }
}
