package fr.insa.optimod.controller;

import java.util.function.Consumer;

import fr.insa.optimod.model.Tournee;

/**
 * Change l'ordre des arrêts de la tournée affichée ; l'annulation remet l'ordre
 * précédent. Les deux tournées (avant et après) sont calculées par
 * {@code Plan.calculerTourneeOrdreChange} avant la commande : exécuter ou
 * annuler ne fait que ranger l'une ou l'autre à la place de la tournée
 * affichée, sans aucun calcul.
 */
final class CommandeChangerOrdre implements Commande {

    /** Range une tournée à la place de la tournée affichée. */
    private final Consumer<Tournee> remplacer;

    /** Tournée avant le changement d'ordre, pour l'annulation. */
    private final Tournee avant;

    /** Tournée avec le nouvel ordre. */
    private final Tournee apres;

    /**
     * @param remplacer range une tournée à la place de la tournée affichée
     * @param avant tournée avant le changement d'ordre
     * @param apres tournée avec le nouvel ordre
     */
    CommandeChangerOrdre(Consumer<Tournee> remplacer, Tournee avant,
            Tournee apres) {
        this.remplacer = remplacer;
        this.avant = avant;
        this.apres = apres;
    }

    @Override
    public void executer() {
        remplacer.accept(apres);
    }

    @Override
    public void annuler() {
        remplacer.accept(avant);
    }
}
