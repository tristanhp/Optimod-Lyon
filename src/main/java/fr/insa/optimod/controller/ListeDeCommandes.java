package fr.insa.optimod.controller;

import java.util.ArrayList;
import java.util.List;

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
     * Exécute une commande et l'ajoute à l'historique.
     *
     * @param commande commande à exécuter
     */
    void executer(Commande commande) {
        commandes.subList(nombreAppliquees, commandes.size()).clear();
        commande.executer();
        commandes.add(commande);
        nombreAppliquees++;
    }

    /** @return vrai s'il reste une commande à annuler */
    boolean peutAnnuler() {
        return nombreAppliquees > 0;
    }

    /** @return vrai s'il reste une commande annulée à rétablir */
    boolean peutRefaire() {
        return nombreAppliquees < commandes.size();
    }

    /** Annule la dernière commande appliquée ; sans effet s'il n'y en a pas. */
    void annuler() {
        if (peutAnnuler()) {
            nombreAppliquees--;
            commandes.get(nombreAppliquees).annuler();
        }
    }

    /** Rétablit la dernière commande annulée ; sans effet s'il n'y en a pas. */
    void refaire() {
        if (peutRefaire()) {
            commandes.get(nombreAppliquees).executer();
            nombreAppliquees++;
        }
    }

    /** Oublie tout l'historique (nouveau plan ou nouvelles demandes). */
    void vider() {
        commandes.clear();
        nombreAppliquees = 0;
    }
}
