package fr.insa.optimod.model;

import java.time.Duration;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects;

import fr.insa.optimod.model.ProgrammeLivraisons.Demande;

/**
 * Tournée d'un livreur : les arrêts dans l'ordre de passage, avec leurs heures
 * de passage, et les chemins parcourus entre deux arrêts consécutifs. Le
 * premier arrêt est le départ de l'entrepôt, le dernier le retour à
 * l'entrepôt.
 *
 * @param arrets  arrêts dans l'ordre de passage
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
     * @param noeud      adresse de l'arrêt
     * @param arrivee    heure d'arrivée
     * @param depart     heure de départ (arrivée plus durée sur place)
     * @param type       entrepôt, point de collecte ou point de livraison
     * @param demande    demande servie à cet arrêt, {@code null} à l'entrepôt
     * @param debutPlage début de la plage annoncée, inclus
     * @param finPlage   fin de la plage annoncée, incluse
     */
    public record Arret(Noeud noeud, LocalTime arrivee, LocalTime depart,
            TypeArret type, Demande demande, LocalTime debutPlage,
            LocalTime finPlage) {

        /**
         * Crée un arrêt.
         *
         * @param noeud      adresse de l'arrêt
         * @param arrivee    heure d'arrivée
         * @param depart     heure de départ
         * @param type       entrepôt, point de collecte ou point de livraison
         * @param demande    demande servie, {@code null} à l'entrepôt
         * @param debutPlage début de la plage annoncée, inclus
         * @param finPlage   fin de la plage annoncée, incluse
         * @throws IllegalArgumentException si l'entrepôt a une demande, ou
         *                                  si une collecte ou une livraison n'en a pas
         */
        public Arret {
            Objects.requireNonNull(noeud, "noeud");
            Objects.requireNonNull(arrivee, "arrivee");
            Objects.requireNonNull(depart, "depart");
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(debutPlage, "debutPlage");
            Objects.requireNonNull(finPlage, "finPlage");
            if ((type == TypeArret.ENTREPOT) != (demande == null)) {
                throw new IllegalArgumentException("Seuls les arrêts de "
                        + "collecte et de livraison ont une demande.");
            }
        }

        /**
         * Crée un arrêt dont la plage annoncée est d'une heure de part et
         * d'autre de l'heure d'arrivée.
         *
         * @param noeud   adresse de l'arrêt
         * @param arrivee heure d'arrivée
         * @param depart  heure de départ
         * @param type    entrepôt, point de collecte ou point de livraison
         * @param demande demande servie, {@code null} à l'entrepôt
         */
        public Arret(Noeud noeud, LocalTime arrivee, LocalTime depart,
                TypeArret type, Demande demande) {
            this(noeud, arrivee, depart, type, demande,
                    arrivee.minusHours(1), arrivee.plusHours(1));
        }

        /**
         * Indique si l'arrivée sort de la plage annoncée.
         *
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
         *
         * @return secondes de décalage, positives pour un retard
         */
        public long obtenirDecalageSecondes() {
            return Duration.between(debutPlage.plusHours(1), arrivee)
                    .getSeconds();
        }

        /**
         * Indique si le livreur arrive à cet arrêt après
         * {@link Plan#HEURE_LIMITE_RETOUR}, pour l'afficher en rouge. À
         * l'heure limite pile, l'arrêt est encore à l'heure.
         *
         * @return vrai si l'heure d'arrivée dépasse l'heure limite
         */
        public boolean estApresHeureLimite() {
            return arrivee.isAfter(Plan.HEURE_LIMITE_RETOUR);
        }
    }

    /**
     * Crée une tournée en copiant les listes.
     *
     * @param arrets  arrêts dans l'ordre de passage
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
     * @param entrepot    adresse de l'entrepôt
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
        return !arrets.get(arrets.size() - 1).estApresHeureLimite();
    }

    /**
     * Compte combien de fois la tournée parcourt chaque tronçon, sans tenir
     * compte du sens : un aller et son retour sur la même rue font deux
     * passages. Un tronçon est désigné par l'ensemble de ses deux extrémités.
     *
     * @return nombre de passages par tronçon parcouru, non modifiable
     */
    public Map<Set<Noeud>, Integer> compterPassagesParTroncon() {
        Map<Set<Noeud>, Integer> passages = new HashMap<>();
        for (Chemin chemin : chemins) {
            for (Troncon troncon : chemin.troncons()) {
                passages.merge(
                        Set.of(troncon.origine(), troncon.destination()), 1,
                        Integer::sum);
            }
        }
        return Map.copyOf(passages);
    }
}
