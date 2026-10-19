package fr.insa.optimod.view;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import fr.insa.optimod.controller.MapScreen;
import fr.insa.optimod.model.Passage;
import fr.insa.optimod.model.Plan;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;

/**
 * Page principale : boutons « Importer » et « Calculer », carte du plan et
 * liste de la tournée calculée.
 */
final class MainView extends BorderPane implements MapScreen {

    /** Format des heures dans la liste de la tournée. */
    private static final DateTimeFormatter HEURE =
            DateTimeFormatter.ofPattern("HH:mm");

    /** Largeur de la liste de la tournée. */
    private static final double TOUR_WIDTH = 280;

    /** Bouton de chargement du fichier XML. */
    private final Button importButton = new Button("Importer");

    /** Bouton de calcul de la tournée. */
    private final Button calculateButton = new Button("Calculer");

    /** Indicateur d'attente affiché pendant le calcul. */
    private final ProgressIndicator waitingIndicator = new ProgressIndicator();

    /** Carte du plan. */
    private final MapCanvas map = new MapCanvas();

    /** Liste textuelle de la tournée. */
    private final ListView<String> tourList = new ListView<>();

    /** Dernier dossier ouvert dans le sélecteur de fichier. */
    private File lastDirectory;

    /** Vrai si le calcul est autorisé (lot de requêtes chargé). */
    private boolean calculateEnabled;

    /** Vrai pendant le calcul. */
    private boolean waiting;

    /** Construit la page vide. */
    MainView() {
        importButton.getStyleClass().add("accent-button");
        calculateButton.getStyleClass().add("accent-button");
        waitingIndicator.setPrefSize(24, 24);
        waitingIndicator.setVisible(false);
        HBox toolBar = new HBox(8, importButton, calculateButton,
                waitingIndicator);
        toolBar.getStyleClass().add("tool-bar-box");
        tourList.setPrefWidth(TOUR_WIDTH);
        tourList.setVisible(false);
        tourList.setManaged(false);
        setTop(toolBar);
        setCenter(map);
        setRight(tourList);
        updateCalculateButton();
    }

    @Override
    public void setOnImport(Runnable action) {
        importButton.setOnAction(event -> action.run());
    }

    @Override
    public void setOnCalculate(Runnable action) {
        calculateButton.setOnAction(event -> action.run());
    }

    @Override
    public void setCalculateEnabled(boolean enabled) {
        calculateEnabled = enabled;
        updateCalculateButton();
    }

    @Override
    public void showWaiting(boolean isWaiting) {
        waiting = isWaiting;
        waitingIndicator.setVisible(isWaiting);
        updateCalculateButton();
    }

    @Override
    public Optional<File> chooseXmlFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Importer un plan XML");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers XML", "*.xml"));
        if (lastDirectory != null && lastDirectory.isDirectory()) {
            chooser.setInitialDirectory(lastDirectory);
        }
        File file = chooser.showOpenDialog(getScene().getWindow());
        if (file != null) {
            lastDirectory = file.getParentFile();
        }
        return Optional.ofNullable(file);
    }

    @Override
    public void showPlan(Plan plan) {
        clearTour();
        map.showPlan(plan);
    }

    @Override
    public void showTour(List<Passage> passages) {
        tourList.getItems().clear();
        for (Passage p : passages) {
            tourList.getItems().add(HEURE.format(p.arrivee()) + " → "
                    + HEURE.format(p.depart()) + "   noeud "
                    + p.etape().idNoeud());
        }
        tourList.setVisible(true);
        tourList.setManaged(true);
    }

    @Override
    public void showCalculationError(String detail) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Optimod'Lyon");
        alert.setHeaderText("Calcul de la tournée impossible");
        alert.setContentText(detail);
        alert.initOwner(getScene().getWindow());
        alert.showAndWait();
    }

    @Override
    public void showInvalidXml(String detail) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Optimod'Lyon");
        alert.setHeaderText("Fichier XML invalide");
        alert.setContentText(detail
                + "\n\nVeuillez réessayer avec un autre fichier.");
        alert.initOwner(getScene().getWindow());
        alert.showAndWait();
    }

    @Override
    public void reset() {
        clearTour();
        map.clear();
    }

    private void clearTour() {
        tourList.getItems().clear();
        tourList.setVisible(false);
        tourList.setManaged(false);
    }

    private void updateCalculateButton() {
        calculateButton.setDisable(!calculateEnabled || waiting);
    }
}
