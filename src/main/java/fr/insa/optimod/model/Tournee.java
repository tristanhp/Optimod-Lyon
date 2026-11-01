package fr.insa.optimod.model;

import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

import fr.insa.optimod.model.ProgrammeLivraisons.Demande;

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

    /** Ce que le livreur fait à un arrêt. */
    public enum TypeArret {
        /** Départ ou retour à l'entrepôt. */
        ENTREPOT,
        /** Point de collecte d'une demande. */
        COLLECTE,
        /** Point de livraison d'une demande. */
        LIVRAISON
    }

    /**
     * Arrêt de la tournée et ses heures de passage.
     *
     * @param noeud adresse de l'arrêt
     * @param arrivee heure d'arrivée
     * @param depart heure de départ (arrivée plus durée sur place)
     * @param type entrepôt, point de collecte ou point de livraison
     * @param demande demande servie à cet arrêt, {@code null} à l'entrepôt
     */
    public record Arret(Noeud noeud, LocalTime arrivee, LocalTime depart,
            TypeArret type, Demande demande) {

        /**
         * Crée un arrêt.
         *
         * @param noeud adresse de l'arrêt
         * @param arrivee heure d'arrivée
         * @param depart heure de départ
         * @param type entrepôt, point de collecte ou point de livraison
         * @param demande demande servie, {@code null} à l'entrepôt
         * @throws IllegalArgumentException si l'entrepôt a une demande, ou
         *         si une collecte ou une livraison n'en a pas
         */
        public Arret {
            Objects.requireNonNull(noeud, "noeud");
            Objects.requireNonNull(arrivee, "arrivee");
            Objects.requireNonNull(depart, "depart");
            Objects.requireNonNull(type, "type");
            if ((type == TypeArret.ENTREPOT) != (demande == null)) {
                throw new IllegalArgumentException("Seuls les arrêts de "
                        + "collecte et de livraison ont une demande.");
            }
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
     * Tournée d'un livreur sans demande : un seul arrêt, le départ de
     * l'entrepôt, et aucun chemin. Visible dans le package (programme de
     * livraisons, plan, tests).
     *
     * @param entrepot adresse de l'entrepôt
     * @param heureDepart heure de départ de l'entrepôt
     * @return la tournée vide
     */
    static Tournee vide(Noeud entrepot, LocalTime heureDepart) {
        return new Tournee(List.of(new Arret(entrepot, heureDepart,
                heureDepart, TypeArret.ENTREPOT, null)), List.of());
    }

    /** @return heure de retour à l'entrepôt (arrivée au dernier arrêt) */
    public LocalTime obtenirHeureRetour() {
        return arrets.get(arrets.size() - 1).arrivee();
    }

    /**
     * Indique si la tournée est possible : le livreur revient à l'entrepôt
     * au plus tard à {@link Plan#HEURE_LIMITE_RETOUR}.
     *
     * @return vrai si le retour à l'entrepôt respecte l'heure limite
     */
    public boolean estPossible() {
        return !obtenirHeureRetour().isAfter(Plan.HEURE_LIMITE_RETOUR);
    }
}
