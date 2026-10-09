package fr.insa.optimod.view;

import javafx.application.Application;

/**
 * Point d'entrée de l'interface graphique.
 *
 * <p>JavaFX refuse de démarrer quand la classe principale hérite de
 * {@link Application} et que ses bibliothèques sont sur le classpath ; cette
 * classe intermédiaire contourne le problème (VS Code, jar exécutable).
 */
public final class AppLauncher {

    private AppLauncher() {
    }

    /**
     * Lance l'interface graphique.
     *
     * @param args arguments de la ligne de commande
     */
    public static void main(String[] args) {
        Application.launch(MainApp.class, args);
    }
}
