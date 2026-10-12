package fr.insa.optimod.view;

import java.util.Objects;

import fr.insa.optimod.controller.MainController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Application JavaFX : crée la fenêtre principale et son contrôleur.
 *
 * <p>Ne pas lancer cette classe directement depuis l'IDE : passer par
 * {@link AppLauncher}.
 */
public final class MainApp extends Application {

    /** Largeur initiale de la fenêtre. */
    private static final double WIDTH = 1100;

    /** Hauteur initiale de la fenêtre. */
    private static final double HEIGHT = 760;

    /** Contrôleur, gardé pour qu'il vive autant que la fenêtre. */
    private MainController controller;

    @Override
    public void start(Stage stage) {
        MainView view = new MainView();
        controller = new MainController(view);
        Scene scene = new Scene(view, WIDTH, HEIGHT);
        scene.getStylesheets().add(Objects.requireNonNull(
                getClass().getResource("style.css")).toExternalForm());
        stage.setTitle("Optimod'Lyon");
        stage.setScene(scene);
        stage.show();
    }
}
