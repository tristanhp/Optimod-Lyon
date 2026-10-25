package fr.insa.optimod.view;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import fr.insa.optimod.controller.MapScreen;
import fr.insa.optimod.model.Demande;
import fr.insa.optimod.model.Livraison;
import fr.insa.optimod.model.Passage;
import fr.insa.optimod.model.Plan;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.geometry.Insets;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.shape.Rectangle;
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

    /** Largeur de la liste des demandes. */
    private static final double DEMANDES_WIDTH = 330;

    /** Largeur de la liste de la tournée. */
    private static final double TOUR_WIDTH = 280;

    /** Bouton de chargement du fichier XML. */
    private final Button importButton = new Button("Importer le Plan");

    /** Bouton de calcul de la tournée. */
    private final Button boutonImporterLivraison = new Button("Importer la Livraison");
    private final Label messageLivraison = new Label();

    private final Button calculateButton = new Button("Calculer");

    /** Indicateur d'attente affiché pendant le calcul. */
    private final ProgressIndicator waitingIndicator = new ProgressIndicator();

    /** Carte du plan. */
    private final MapCanvas map = new MapCanvas();

    /** Liste textuelle de la tournée. */
    private final ListView<String> tourList = new ListView<>();

    /** Liste textuelle des demandes importées. */
    private final ListView<String> demandesList = new ListView<>();

    /** Dernier dossier ouvert dans le sélecteur de fichier. */
    private File lastDirectory;

    /** Construit la page vide. */
    MainView() {
        importButton.getStyleClass().add("accent-button");
        calculateButton.getStyleClass().add("accent-button");
        waitingIndicator.setPrefSize(24, 24);
        waitingIndicator.setVisible(false);
        boutonImporterLivraison.getStyleClass().add("accent-button");
        messageLivraison.setPadding(new Insets(12));
        setBottom(messageLivraison);
        HBox toolBar = new HBox(8, importButton, boutonImporterLivraison, calculateButton,
                waitingIndicator);
        toolBar.getStyleClass().add("tool-bar-box");
        tourList.setPrefWidth(TOUR_WIDTH);
        tourList.setVisible(false);
        tourList.setManaged(false);
        demandesList.setPrefWidth(DEMANDES_WIDTH);
        demandesList.setCellFactory(liste -> new ListCell<>() {
            @Override
            protected void updateItem(String texte, boolean vide) {
                super.updateItem(texte, vide);
                setText(vide ? null : texte);
                setGraphic(vide ? null : new Rectangle(10, 10,
                        MapCanvas.couleurDemande(getIndex())));
            }
        });
        map.definirSurSurvol(indice -> {
            if (indice >= 0) {
                demandesList.getSelectionModel().select(indice);
                demandesList.scrollTo(indice);
            } else {
                demandesList.getSelectionModel().clearSelection();
            }
        });
        demandesList.getSelectionModel().selectedIndexProperty().addListener(
                (observable, ancien, nouveau) -> map.mettreEnEvidence(nouveau.intValue()));
        clearDemandes();
        setLeft(demandesList);
        setTop(toolBar);
        setCenter(map);
        setRight(tourList);
    }

    @Override
    public void definirActionImportPlan(Runnable action) {
        importButton.setOnAction(event -> action.run());
    }

    @Override
    public void setOnCalculate(Runnable action) {
        calculateButton.setOnAction(event -> action.run());
    }

    @Override
    public void activerActions(boolean importPlan, boolean importLivraison,
            boolean calcul) {
        importButton.setDisable(!importPlan);
        boutonImporterLivraison.setDisable(!importLivraison);
        calculateButton.setDisable(!calcul);
    }

    @Override
    public void showWaiting(boolean isWaiting) {
        waitingIndicator.setVisible(isWaiting);
    }

    @Override
    public Optional<File> choisirFichierPlan() {
        return choisirFichierXml("Importer un plan XML");
    }

    private Optional<File> choisirFichierXml(String titre) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(titre);
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
    public void afficherPlan(Plan plan) {
        clearTour();
        clearDemandes();
        map.afficherPlan(plan);
        messageLivraison.setText("");
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
    public void afficherErreurXml(String detail) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Optimod'Lyon");
        alert.setHeaderText("Fichier XML invalide");
        alert.setContentText(detail
                + "\n\nVeuillez réessayer avec un autre fichier.");
        alert.initOwner(getScene().getWindow());
        alert.showAndWait();
    }

    @Override
    public void reinitialiser() {
        clearTour();
        clearDemandes();
        map.effacerCarte();
        messageLivraison.setText("");
    }

    @Override
    public void definirActionImportLivraison(Runnable action) {
        boutonImporterLivraison.setOnAction(event -> action.run());
    }

    @Override
    public Optional<File> choisirFichierLivraison() {
        return choisirFichierXml("Importer des demandes de livraison XML");
    }

    @Override
    public void afficherLivraisonChargee(Livraison livraison) {
        clearTour();
        map.afficherLivraison(livraison);
        demandesList.getItems().clear();
        int rang = 1;
        for (Demande demande : livraison.obtenirDemandes()) {
            demandesList.getItems().add(LibellesDemandes.decrire(rang++, demande));
        }
        demandesList.setVisible(true);
        demandesList.setManaged(true);
        messageLivraison.setText("livraison chargée : "
                + livraison.obtenirDemandes().size() + " demande(s)");
    }

    private void clearDemandes() {
        map.afficherLivraison(null);
        demandesList.getItems().clear();
        demandesList.setVisible(false);
        demandesList.setManaged(false);
    }

    private void clearTour() {
        tourList.getItems().clear();
        tourList.setVisible(false);
        tourList.setManaged(false);
    }
}
