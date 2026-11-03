package fr.insa.optimod.controller;

/**
 * Change l'ordre des arrêts de la tournée affichée ; l'annulation remet l'ordre
 * précédent. Les deux tournées (avant et après) sont calculées par
 * {@code Plan.calculerTourneeOrdreChange} avant la commande : exécuter ou
 * annuler ne fait que ranger l'une ou l'autre dans le cache du contrôleur, sans
 * aucun calcul. Chaque rangement est fait par le contrôleur, qui ne remplace
 * que la tournée que cette commande avait rangée : un nouveau calcul fait
 * entre-temps n'est pas écrasé.
 */
final class CommandeChangerOrdre implements Commande {

    /** Range dans le cache la tournée avec le nouvel ordre. */
    private final Runnable ranger;

    /** Remet dans le cache la tournée d'avant le changement d'ordre. */
    private final Runnable remettre;

    /**
     * @param ranger range dans le cache la tournée avec le nouvel ordre
     * @param remettre remet dans le cache la tournée d'avant
     */
    CommandeChangerOrdre(Runnable ranger, Runnable remettre) {
        this.ranger = ranger;
        this.remettre = remettre;
    }

    @Override
    public void executer() {
        ranger.run();
    }

    @Override
    public void annuler() {
        remettre.run();
    }
}
