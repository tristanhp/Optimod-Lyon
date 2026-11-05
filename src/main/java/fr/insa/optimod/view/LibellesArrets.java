package fr.insa.optimod.view;

import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.TreeSet;

import fr.insa.optimod.model.Noeud;
import fr.insa.optimod.model.Plan;
import fr.insa.optimod.model.Tournee.Arret;
import fr.insa.optimod.model.Tournee.TypeArret;
import fr.insa.optimod.model.Troncon;

/**
 * Textes d'un arrêt, communs à la liste de la tournée et au popup de la carte.
 */
final class LibellesArrets {

    /** Format des heures de passage. */
    private static final DateTimeFormatter HEURE = DateTimeFormatter.ofPattern("HH:mm");

    private LibellesArrets() {
    }

    /**
     * Donne le type d'un arrêt en toutes lettres.
     *
     * @param type type de l'arrêt
     * @return « Entrepôt », « Point de collecte » ou « Point de livraison »
     */
    static String typeEnToutesLettres(TypeArret type) {
        return switch (type) {
            case ENTREPOT -> "Entrepôt";
            case COLLECTE -> "Point de collecte";
            case LIVRAISON -> "Point de livraison";
        };
    }

    /**
     * Donne l'adresse d'un nœud : les noms des rues des tronçons qui y
     * arrivent ou en partent, sans doublon, dans l'ordre alphabétique.
     *
     * @param plan  plan du nœud, ou {@code null}
     * @param noeud adresse à décrire
     * @return noms des rues séparés par des virgules, ou « nœud N » si aucune
     *         rue n'a de nom
     */
    static String adresse(Plan plan, Noeud noeud) {
        Set<String> rues = new TreeSet<>();
        if (plan != null) {
            for (Troncon troncon : plan.obtenirTroncons()) {
                if ((troncon.origine().equals(noeud)
                        || troncon.destination().equals(noeud))
                        && !troncon.nomRue().isBlank()) {
                    rues.add(troncon.nomRue());
                }
            }
        }
        return rues.isEmpty() ? "nœud " + noeud.identifiant()
                : String.join(", ", rues);
    }

    /**
     * Décrit un arrêt : type et adresse, heures de passage, plage annoncée et
     * décalage.
     *
     * @param plan  plan de l'arrêt, ou {@code null}
     * @param arret arrêt à décrire
     * @return texte sur plusieurs lignes
     */
    static String decrire(Plan plan, Arret arret) {
        long decalage = arret.obtenirDecalageSecondes();
        String signe = decalage >= 0 ? "+" : "−";
        long secondes = Math.abs(decalage);
        return typeEnToutesLettres(arret.type()) + " — "
                + adresse(plan, arret.noeud())
                + "\nArrivée " + HEURE.format(arret.arrivee())
                + " → départ " + HEURE.format(arret.depart())
                + "\nPlage : " + HEURE.format(arret.debutPlage()) + " – "
                + HEURE.format(arret.finPlage())
                + "\nDécalage : " + signe + secondes / 60 + " min "
                + secondes % 60 + " s"
                + (arret.estHorsPlage() ? " — Hors plage" : "")
                + (arret.estApresHeureLimite() ? " — Après 13 h" : "");
    }
}
