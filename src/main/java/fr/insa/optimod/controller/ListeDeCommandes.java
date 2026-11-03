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

    /**
     * Transmet des tournées calculées après coup (« Calculer ») aux commandes
     * voisines de l'état actuel : la dernière commande appliquée les remettra
     * en rétablissant, la prochaine à rétablir en annulant.
     *
     * @param tournees tournées calculées pour l'état actuel
     */
    void memoriserTournees(Map<Integer, Tournee> tournees) {
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
    }
}
