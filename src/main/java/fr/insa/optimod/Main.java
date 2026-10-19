package fr.insa.optimod;

/**
 * Point d'entrée de l'application Optimod'Lyon.
 */
public final class Main {

    /** Nom affiché de l'application. */
    private static final String NOM_APPLICATION = "Optimod'Lyon";

    private Main() {
    }

    /**
     * Retourne le message de démarrage de l'application.
     *
     * @return le message de démarrage
     */
    public static String obtenirMessageDemarrage() {
        return NOM_APPLICATION + " - optimisation de tournées de livraison à vélo";
    }

    /**
     * Démarre l'application.
     *
     * @param arguments arguments de la ligne de commande (non utilisés)
     */
    public static void main(String[] arguments) {
        System.out.println(obtenirMessageDemarrage());
    }
}
