package fr.insa.optimod.model;

/**
 * Objet prévenu quand un {@link Observable} du modèle change (design pattern
 * Observer). La vue observe le modèle pour se redessiner sans que le modèle
 * la connaisse.
 */
@FunctionalInterface
public interface Observateur {

    /**
     * Appelée après chaque modification de l'objet observé.
     *
     * @param source objet du modèle qui a changé
     */
    void mettreAJour(Observable source);
}
