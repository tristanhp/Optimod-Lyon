package fr.insa.optimod.controller;

/**
 * Modification réversible du modèle (design pattern Command). Les commandes
 * exécutées sont gardées par {@link ListeDeCommandes} pour pouvoir les annuler
 * puis les rétablir.
 */
interface Commande {

    /** Applique la modification. */
    void executer();

    /** Défait la modification, en remettant le modèle dans l'état d'avant. */
    void annuler();
}
