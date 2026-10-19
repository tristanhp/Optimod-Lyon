package fr.insa.optimod;

import java.util.Arrays;

import fr.insa.optimod.view.AppLauncher;

/**
 * Point d'entrée de l'application Optimod'Lyon (classe principale du jar).
 */
public final class Main {

    /** Nom affiché de l'application. */
    private static final String APP_NAME = "Optimod'Lyon";

    /** Option qui affiche le message de démarrage sans ouvrir l'interface. */
    public static final String VERSION_OPTION = "--version";

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
     * Indique si l'interface graphique doit être ouverte.
     *
     * @param args arguments de la ligne de commande
     * @return {@code false} si {@link #VERSION_OPTION} est présent
     */
    static boolean shouldLaunchGui(String[] args) {
        return !Arrays.asList(args).contains(VERSION_OPTION);
    }

    /**
     * Démarre l'application : affiche le message de démarrage puis ouvre
     * l'interface graphique, sauf avec {@link #VERSION_OPTION}.
     *
     * @param args arguments de la ligne de commande
     */
    public static void main(String[] args) {
        System.out.println(banner());
        if (shouldLaunchGui(args)) {
            AppLauncher.main(args);
        }
    }
}
