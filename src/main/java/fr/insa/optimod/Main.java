package fr.insa.optimod;

/**
 * Point d'entrée de l'application Optimod'Lyon.
 */
public final class Main {

    /** Nom affiché de l'application. */
    private static final String APP_NAME = "Optimod'Lyon";

    private Main() {
    }

    /**
     * Retourne le message de démarrage de l'application.
     *
     * @return le message de démarrage
     */
    public static String banner() {
        return APP_NAME + " - optimisation de tournées de livraison à vélo";
    }

    /**
     * Démarre l'application.
     *
     * @param args arguments de la ligne de commande (non utilisés)
     */
    public static void main(String[] args) {
        System.out.println(banner());
    }
}
