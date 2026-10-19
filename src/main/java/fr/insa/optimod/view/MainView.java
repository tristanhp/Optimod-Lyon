package fr.insa.optimod.view;

import java.io.File;
import java.util.Optional;

import fr.insa.optimod.controller.MapScreen;
import fr.insa.optimod.model.Plan;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.geometry.Insets;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;

/**
 * Page principale : import du plan et des demandes, carte et confirmation.
 */
final class MainView extends BorderPane implements MapScreen {

    /** Bouton de chargement du fichier XML. */
    private final Button boutonImporterPlan = new Button("Importer le Plan");
    private final Button boutonImporterLivraison = new Button("Importer la Livraison");
    private final Label messageLivraison = new Label();

    /** Carte du plan. */
    private final MapCanvas carte = new MapCanvas();

    /** Dernier dossier ouvert dans le sélecteur de fichier. */
    private File dernierDossier;

    /** Construit la page vide. */
    MainView() {
        boutonImporterPlan.getStyleClass().add("accent-button");
        boutonImporterLivraison.getStyleClass().add("accent-button");
        boutonImporterLivraison.setDisable(true);
        HBox barreOutils = new HBox(boutonImporterPlan, boutonImporterLivraison);
        barreOutils.getStyleClass().add("tool-bar-box");
        setTop(barreOutils);
        setCenter(carte);
        messageLivraison.setPadding(new Insets(12));
        setBottom(messageLivraison);
    }

    @Override
    public void definirActionImportPlan(Runnable action) {
        boutonImporterPlan.setOnAction(evenement -> action.run());
    }

    @Override
    public void definirActionImportLivraison(Runnable action) {
        boutonImporterLivraison.setOnAction(evenement -> action.run());
    }

    @Override
    public Optional<File> choisirFichierLivraison() {
        return choisirFichierXml("Importer des demandes de livraison XML");
    }

    @Override
    public Optional<File> choisirFichierPlan() {
        return choisirFichierXml("Importer un plan XML");
    }

    private Optional<File> choisirFichierXml(String titre) {
        FileChooser selecteurFichier = new FileChooser();
        selecteurFichier.setTitle(titre);
        selecteurFichier.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers XML", "*.xml"));
        if (dernierDossier != null && dernierDossier.isDirectory()) {
            selecteurFichier.setInitialDirectory(dernierDossier);
        }
        File fichier = selecteurFichier.showOpenDialog(getScene().getWindow());
        if (fichier != null) {
            dernierDossier = fichier.getParentFile();
        }
        return Optional.ofNullable(fichier);
    }

    @Override
    public void afficherPlan(Plan plan) {
        carte.afficherPlan(plan);
        messageLivraison.setText("");
        boutonImporterLivraison.setDisable(false);
    }

    @Override
    public void afficherLivraisonChargee() {
        messageLivraison.setText("livraison chargée");
    }

    @Override
    public void afficherErreurXml(String detail) {
        Alert alerte = new Alert(Alert.AlertType.INFORMATION);
        alerte.setTitle("Optimod'Lyon");
        alerte.setHeaderText("Fichier XML invalide");
        alerte.setContentText(detail
                + "\n\nVeuillez réessayer avec un autre fichier.");
        alerte.initOwner(getScene().getWindow());
        alerte.showAndWait();
    }

    @Override
    public void reinitialiser() {
        carte.effacerCarte();
        messageLivraison.setText("");
        boutonImporterLivraison.setDisable(true);
    }
}
