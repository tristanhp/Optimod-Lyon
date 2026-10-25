package fr.insa.optimod.view;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import fr.insa.optimod.model.Plan;
import fr.insa.optimod.controller.MainController;
import javafx.application.Application;
import javafx.application.Platform;
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
    private static final double LARGEUR = 1100;

    /** Hauteur initiale de la fenêtre. */
    private static final double HAUTEUR = 760;

    /** Exécute le calcul de la tournée hors du thread de l'interface. */
    private ExecutorService calcul;

    /** Contrôleur, gardé pour qu'il vive autant que la fenêtre. */
    private MainController controleur;

    @Override
    public void start(Stage stage) {
        MainView vue = new MainView();
        calcul = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "calcul-tournee");
            t.setDaemon(true);
            return t;
        });
        controleur = new MainController(vue, Plan::calculerTournee, calcul,
                Platform::runLater);
        Scene scene = new Scene(vue, LARGEUR, HAUTEUR);
        scene.getStylesheets().add(Objects.requireNonNull(
                getClass().getResource("style.css")).toExternalForm());
        stage.setTitle("Optimod'Lyon");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        calcul.shutdownNow();
    }
}
