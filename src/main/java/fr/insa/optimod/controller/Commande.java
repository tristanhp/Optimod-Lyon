package fr.insa.optimod.controller;

import java.util.Map;

import fr.insa.optimod.model.Tournee;

/**
 * Modification réversible du modèle (design pattern Command). Les commandes
 * exécutées sont gardées par {@link ListeDeCommandes} pour pouvoir les annuler
 * puis les rétablir.
 *
 * <p>
 * Une commande garde les tournées d'avant et d'après la modification :
 * {@link #annuler()} remet celles d'avant, et rétablir (un nouvel
 * {@link #executer()}) remet celles d'après, sans recalcul.
 */
interface Commande {

    /**
     * Applique la modification. Au premier appel, les tournées d'après sont
     * calculées ; aux appels suivants (rétablir), elles sont remises.
     */
    void executer();

    /** Défait la modification, en remettant le modèle dans l'état d'avant. */
    void annuler();

    /**
     * Retient les tournées calculées après coup (« Calculer ») pour l'état
     * d'avant la commande : {@link #annuler()} les remettra.
     *
     * @param tournees tournées par numéro de livreur, éventuellement vide
     */
    void memoriserTourneesAvant(Map<Integer, Tournee> tournees);

    /**
     * Retient les tournées calculées après coup (« Calculer ») pour l'état
     * d'après la commande : rétablir les remettra.
     *
     * @param tournees tournées par numéro de livreur, éventuellement vide
     */
    void memoriserTourneesApres(Map<Integer, Tournee> tournees);
}
