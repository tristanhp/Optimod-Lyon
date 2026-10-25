package fr.insa.optimod;

import java.util.Arrays;

import fr.insa.optimod.view.AppLauncher;

/**
 * Point d'entrée de l'application Optimod'Lyon (classe principale du jar).
 */
public final class Main {

    /** Nom affiché de l'application. */
    private static final String NOM_APPLICATION = "Optimod'Lyon";

    /** Option qui affiche le message de démarrage sans ouvrir l'interface. */
    public static final String OPTION_VERSION = "--version";

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
     * Indique si l'interface graphique doit être ouverte.
     *
     * @param args arguments de la ligne de commande
     * @return {@code false} si {@link #OPTION_VERSION} est présent
     */
    static boolean doitOuvrirInterface(String[] args) {
        return !Arrays.asList(args).contains(OPTION_VERSION);
    }

    /**
     * Démarre l'application : affiche le message de démarrage puis ouvre
     * l'interface graphique, sauf avec {@link #OPTION_VERSION}.
     *
     * @param args arguments de la ligne de commande
     */
    public static void main(String[] args) {
        System.out.println(obtenirMessageDemarrage());
        if (doitOuvrirInterface(args)) {
            AppLauncher.main(args);
        }
    }
}
