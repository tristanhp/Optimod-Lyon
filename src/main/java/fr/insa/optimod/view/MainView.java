package fr.insa.optimod.view;

import java.io.File;
import java.util.Optional;

import fr.insa.optimod.controller.MapScreen;
import fr.insa.optimod.model.Plan;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;

/**
 * Page principale : un bouton « Importer » et la carte du plan.
 */
final class MainView extends BorderPane implements MapScreen {

    /** Bouton de chargement du fichier XML. */
    private final Button importButton = new Button("Importer");

    /** Carte du plan. */
    private final MapCanvas map = new MapCanvas();

    /** Dernier dossier ouvert dans le sélecteur de fichier. */
    private File lastDirectory;

    /** Construit la page vide. */
    MainView() {
        importButton.getStyleClass().add("accent-button");
        HBox toolBar = new HBox(importButton);
        toolBar.getStyleClass().add("tool-bar-box");
        setTop(toolBar);
        setCenter(map);
    }

    @Override
    public void setOnImport(Runnable action) {
        importButton.setOnAction(event -> action.run());
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
        map.showPlan(plan);
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
        map.clear();
    }
}
