package fr.insa.optimod.view;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

import fr.insa.optimod.controller.MapScreen;
import fr.insa.optimod.model.Demande;
import fr.insa.optimod.model.Livraison;
import fr.insa.optimod.model.Observable;
import fr.insa.optimod.model.Observateur;
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
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 * Page principale : boutons d'import, de calcul et de modification des
 * demandes, carte du plan, liste des demandes et liste de la tournée calculée.
 *
 * <p>La vue observe les demandes affichées (design pattern Observer) : quand
 * elles changent, la liste et la carte se redessinent sans que le contrôleur
 * ait à le demander.
 */
final class MainView extends BorderPane implements MapScreen, Observateur {

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

    private final Button boutonAjouter = new Button("Ajouter une demande");
    private final Button boutonSupprimer = new Button("Supprimer la demande");
    private final Button boutonAnnuler = new Button("Annuler");
    private final Button boutonRefaire = new Button("Rétablir");

    /** Consigne pour l'utilisateur (ex. pendant l'ajout d'une demande). */
    private final Label consigne = new Label();

    /** Vrai si l'état du contrôleur permet de supprimer une demande. */
    private boolean suppressionPermise;

    /** Indicateur d'attente affiché pendant le calcul. */
    private final ProgressIndicator waitingIndicator = new ProgressIndicator();

    /** Carte du plan. */
    private final MapCanvas map = new MapCanvas();

    /** Liste textuelle de la tournée. */
    private final ListView<String> tourList = new ListView<>();

    /** Liste textuelle des demandes importées. */
    private final ListView<String> demandesList = new ListView<>();

    /** Demandes affichées et observées, ou {@code null}. */
    private Livraison livraisonObservee;

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
        boutonAnnuler.setTooltip(new Tooltip("Ctrl+Z"));
        boutonRefaire.setTooltip(new Tooltip("Ctrl+Y"));
        HBox toolBar = new HBox(8, importButton, boutonImporterLivraison,
                calculateButton, waitingIndicator, boutonAjouter,
                boutonAnnuler, boutonRefaire, consigne);
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
                (observable, ancien, nouveau) -> {
                    map.mettreEnEvidence(nouveau.intValue());
                    actualiserBoutonSupprimer();
                });
        VBox.setVgrow(demandesList, Priority.ALWAYS);
        boutonSupprimer.setMaxWidth(Double.MAX_VALUE);
        VBox panneauDemandes = new VBox(4, demandesList, boutonSupprimer);
        panneauDemandes.visibleProperty().bind(demandesList.visibleProperty());
        panneauDemandes.managedProperty().bind(demandesList.managedProperty());
        clearDemandes();
        sceneProperty().addListener((observable, ancienne, scene) -> {
            if (scene != null) {
                scene.getAccelerators().put(KeyCombination.keyCombination(
                        "Shortcut+Z"), boutonAnnuler::fire);
                scene.getAccelerators().put(KeyCombination.keyCombination(
                        "Shortcut+Y"), boutonRefaire::fire);
                scene.getAccelerators().put(KeyCombination.keyCombination(
                        "Shortcut+Shift+Z"), boutonRefaire::fire);
            }
        });
        setLeft(panneauDemandes);
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
    public void activerActions(Set<Action> actions) {
        importButton.setDisable(!actions.contains(Action.IMPORTER_PLAN));
        boutonImporterLivraison.setDisable(
                !actions.contains(Action.IMPORTER_LIVRAISON));
        calculateButton.setDisable(!actions.contains(Action.CALCULER));
        boutonAjouter.setDisable(!actions.contains(Action.AJOUTER_DEMANDE));
        boutonAnnuler.setDisable(!actions.contains(Action.ANNULER));
        boutonRefaire.setDisable(!actions.contains(Action.REFAIRE));
        suppressionPermise = actions.contains(Action.SUPPRIMER_DEMANDE);
        actualiserBoutonSupprimer();
    }

    /** Supprimer demande une action permise et une demande sélectionnée. */
    private void actualiserBoutonSupprimer() {
        boutonSupprimer.setDisable(!suppressionPermise
                || demandesList.getSelectionModel().getSelectedIndex() < 0);
    }

    @Override
    public void definirActionAjouterDemande(Runnable action) {
        boutonAjouter.setOnAction(event -> action.run());
    }

    @Override
    public void definirActionSupprimerDemande(IntConsumer action) {
        boutonSupprimer.setOnAction(event -> action.accept(
                demandesList.getSelectionModel().getSelectedIndex()));
    }

    @Override
    public void definirActionAnnuler(Runnable action) {
        boutonAnnuler.setOnAction(event -> action.run());
    }

    @Override
    public void definirActionRefaire(Runnable action) {
        boutonRefaire.setOnAction(event -> action.run());
    }

    @Override
    public void definirActionClicNoeud(LongConsumer action) {
        map.definirSurClicNoeud(action);
    }

    @Override
    public void afficherConsigne(String texte) {
        consigne.setText(texte);
    }

    @Override
    public void effacerTournee() {
        clearTour();
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
        observer(livraison);
    }

    /** Les demandes observées ont changé : redessine la liste et la carte. */
    @Override
    public void mettreAJour(Observable source) {
        dessinerDemandes();
    }

    /**
     * Change les demandes observées et les dessine.
     *
     * @param livraison demandes à observer, ou {@code null} pour aucune
     */
    private void observer(Livraison livraison) {
        if (livraisonObservee != null) {
            livraisonObservee.retirerObservateur(this);
        }
        livraisonObservee = livraison;
        if (livraison != null) {
            livraison.ajouterObservateur(this);
        }
        dessinerDemandes();
    }

    private void dessinerDemandes() {
        map.afficherLivraison(livraisonObservee);
        demandesList.getItems().clear();
        boolean visible = livraisonObservee != null;
        demandesList.setVisible(visible);
        demandesList.setManaged(visible);
        if (!visible) {
            return;
        }
        int rang = 1;
        for (Demande demande : livraisonObservee.obtenirDemandes()) {
            demandesList.getItems().add(
                    LibellesDemandes.decrire(rang++, demande));
        }
        messageLivraison.setText("livraison chargée : "
                + livraisonObservee.obtenirDemandes().size() + " demande(s)");
    }

    private void clearDemandes() {
        observer(null);
    }

    private void clearTour() {
        tourList.getItems().clear();
        tourList.setVisible(false);
        tourList.setManaged(false);
    }
}
