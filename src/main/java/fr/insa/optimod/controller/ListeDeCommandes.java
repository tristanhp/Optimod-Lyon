package fr.insa.optimod.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import fr.insa.optimod.model.Tournee;

/**
 * Historique des commandes exécutées, pour annuler et rétablir (design pattern
 * Command). Exécuter une nouvelle commande oublie les commandes annulées qui
 * n'ont pas été rétablies.
 */
final class ListeDeCommandes {

    /** Commandes, de la plus ancienne à la plus récente. */
    private final List<Commande> commandes = new ArrayList<>();

    /** Nombre de commandes actuellement appliquées (début de la liste). */
    private int nombreAppliquees;

    /**
     * Consigne « ordre non optimisé » de chaque état de l'historique : l'indice
     * est le nombre de commandes appliquées. Vide quand les tournées de cet
     * état viennent du voyageur de commerce. Annuler et rétablir retrouvent
     * ainsi la consigne qui correspond aux tournées remises.
     */
    private final List<String> consignesOrdre = new ArrayList<>(List.of(""));

    /**
     * Exécute une commande et l'ajoute à l'historique.
     *
     * @param commande commande à exécuter
     */
    void executer(Commande commande) {
        commandes.subList(nombreAppliquees, commandes.size()).clear();
        consignesOrdre.subList(nombreAppliquees + 1, consignesOrdre.size())
                .clear();
        commande.executer();
        commandes.add(commande);
        nombreAppliquees++;
        consignesOrdre.add("");
    }

    /**
     * Retient la consigne « ordre non optimisé » de l'état actuel : les
     * tournées affichées gardent un ordre qui ne vient pas du voyageur de
     * commerce.
     *
     * @param consigne consigne affichée, vide si l'ordre est optimisé
     */
    void definirConsigneOrdre(String consigne) {
        consignesOrdre.set(nombreAppliquees, consigne);
    }

    /** @return consigne « ordre non optimisé » de l'état actuel, ou vide */
    String consigneOrdre() {
        return consignesOrdre.get(nombreAppliquees);
    }

    /** @return vrai s'il reste une commande à annuler */
    boolean peutAnnuler() {
        return nombreAppliquees > 0;
    }

    /** @return vrai s'il reste une commande annulée à rétablir */
    boolean peutRefaire() {
        return nombreAppliquees < commandes.size();
    }

    /**
     * Annule la dernière commande appliquée ; sans effet s'il n'y en a pas.
     *
     * @return la commande annulée, ou {@code null} s'il n'y en avait pas
     */
    Commande annuler() {
        if (!peutAnnuler()) {
            return null;
        }
        nombreAppliquees--;
        Commande commande = commandes.get(nombreAppliquees);
        commande.annuler();
        return commande;
    }

    /**
     * Rétablit la dernière commande annulée ; sans effet s'il n'y en a pas.
     *
     * @return la commande rétablie, ou {@code null} s'il n'y en avait pas
     */
    Commande refaire() {
        if (!peutRefaire()) {
            return null;
        }
        Commande commande = commandes.get(nombreAppliquees);
        commande.executer();
        nombreAppliquees++;
        return commande;
    }

    /**
     * Transmet des tournées calculées après coup (« Calculer ») aux commandes
     * voisines de l'état actuel : la dernière commande appliquée les remettra
     * en rétablissant, la prochaine à rétablir en annulant.
     *
     * @param tournees tournées calculées pour l'état actuel
     */
    void memoriserTournees(Map<Integer, Tournee> tournees) {
        consignesOrdre.set(nombreAppliquees, "");
        if (peutAnnuler()) {
            commandes.get(nombreAppliquees - 1)
                    .memoriserTourneesApres(tournees);
        }
        if (peutRefaire()) {
            commandes.get(nombreAppliquees).memoriserTourneesAvant(tournees);
        }
    }

    /** Oublie tout l'historique (nouveau plan ou nouvelles demandes). */
    void vider() {
        commandes.clear();
        nombreAppliquees = 0;
        consignesOrdre.clear();
        consignesOrdre.add("");
    }
}
