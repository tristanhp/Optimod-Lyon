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
    private static final double LARGEUR_INITIALE = 1100;

    /** Hauteur initiale de la fenêtre. */
    private static final double HAUTEUR_INITIALE = 760;

    /** Contrôleur, gardé pour qu'il vive autant que la fenêtre. */
    private MainController controleur;

    @Override
    public void start(Stage fenetre) {
        MainView vue = new MainView();
        controleur = new MainController(vue);
        Scene scene = new Scene(vue, LARGEUR_INITIALE, HAUTEUR_INITIALE);
        scene.getStylesheets().add(Objects.requireNonNull(
                getClass().getResource("style.css")).toExternalForm());
        fenetre.setTitle("Optimod'Lyon");
        fenetre.setScene(scene);
        fenetre.show();
    }
}
