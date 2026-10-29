package fr.insa.optimod.view;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

import fr.insa.optimod.controller.MapScreen;
import fr.insa.optimod.model.ProgrammeLivraisons.Demande;
import fr.insa.optimod.model.ProgrammeLivraisons;
import fr.insa.optimod.model.Observable;
import fr.insa.optimod.model.Observateur;
import fr.insa.optimod.model.Tournee;
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
    private static final double LARGEUR_DEMANDES = 330;

    /** Largeur de la liste de la tournée. */
    private static final double LARGEUR_TOURNEE = 280;

    /** Bouton de chargement du fichier XML. */
    private final Button boutonImporterPlan = new Button("Importer le plan");

    /** Bouton de calcul de la tournée. */
    private final Button boutonImporterDemandes = new Button("Importer les demandes");
    private final Label message = new Label();

    private final Button boutonCalculer = new Button("Calculer");

    private final Button boutonAjouter = new Button("Ajouter une demande");
    private final Button boutonSupprimer = new Button("Supprimer la demande");
    private final Button boutonMonter = new Button("Monter");
    private final Button boutonDescendre = new Button("Descendre");
    private final Button boutonAnnuler = new Button("Annuler");
    private final Button boutonRefaire = new Button("Rétablir");

    /** Consigne pour l'utilisateur (ex. pendant l'ajout d'une demande). */
    private final Label consigne = new Label();

    /** Vrai si l'état du contrôleur permet de supprimer une demande. */
    private boolean suppressionPermise;

    /** Vrai si l'état du contrôleur permet de changer l'ordre des arrêts. */
    private boolean changementOrdrePermis;

    /** Indicateur d'attente affiché pendant le calcul. */
    private final ProgressIndicator indicateurAttente = new ProgressIndicator();

    /** Carte du plan. */
    private final MapCanvas carte = new MapCanvas();

    /** Liste textuelle de la tournée. */
    private final ListView<String> listeTournee = new ListView<>();

    /** Liste textuelle des demandes importées. */
    private final ListView<String> listeDemandes = new ListView<>();

    /** Demandes affichées et observées, ou {@code null}. */
    private ProgrammeLivraisons programmeObserve;

    /** Dernier dossier ouvert dans le sélecteur de fichier. */
    private File dernierDossier;

    /** Construit la page vide. */
    MainView() {
        boutonImporterPlan.getStyleClass().add("accent-button");
        boutonCalculer.getStyleClass().add("accent-button");
        indicateurAttente.setPrefSize(24, 24);
        indicateurAttente.setVisible(false);
        boutonImporterDemandes.getStyleClass().add("accent-button");
        message.setPadding(new Insets(12));
        setBottom(message);
        boutonAnnuler.setTooltip(new Tooltip("Ctrl+Z"));
        boutonRefaire.setTooltip(new Tooltip("Ctrl+Y"));
        HBox barreOutils = new HBox(8, boutonImporterPlan, boutonImporterDemandes,
                boutonCalculer, indicateurAttente, boutonAjouter,
                boutonAnnuler, boutonRefaire, consigne);
        barreOutils.getStyleClass().add("tool-bar-box");
        listeTournee.setPrefWidth(LARGEUR_TOURNEE);
        listeTournee.setVisible(false);
        listeTournee.setManaged(false);
        listeDemandes.setPrefWidth(LARGEUR_DEMANDES);
        listeDemandes.setCellFactory(liste -> new ListCell<>() {
            @Override
            protected void updateItem(String texte, boolean vide) {
                super.updateItem(texte, vide);
                setText(vide ? null : texte);
                setGraphic(vide ? null : new Rectangle(10, 10,
                        MapCanvas.couleurDemande(getIndex())));
            }
        });
        carte.definirSurSurvol(indice -> {
            if (indice >= 0) {
                listeDemandes.getSelectionModel().select(indice);
                listeDemandes.scrollTo(indice);
            } else {
                listeDemandes.getSelectionModel().clearSelection();
            }
        });
        listeDemandes.getSelectionModel().selectedIndexProperty().addListener(
                (observable, ancien, nouveau) -> {
                    carte.mettreEnEvidence(nouveau.intValue());
                    actualiserBoutonSupprimer();
                });
        listeTournee.getSelectionModel().selectedIndexProperty().addListener(
                (observable, ancien, nouveau) -> actualiserBoutonsOrdre());
        boutonMonter.setMaxWidth(Double.MAX_VALUE);
        boutonDescendre.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(boutonMonter, Priority.ALWAYS);
        HBox.setHgrow(boutonDescendre, Priority.ALWAYS);
        VBox.setVgrow(listeTournee, Priority.ALWAYS);
        VBox panneauTournee = new VBox(4, listeTournee,
                new HBox(4, boutonMonter, boutonDescendre));
        panneauTournee.visibleProperty().bind(listeTournee.visibleProperty());
        panneauTournee.managedProperty().bind(listeTournee.managedProperty());
        actualiserBoutonsOrdre();
        VBox.setVgrow(listeDemandes, Priority.ALWAYS);
        boutonSupprimer.setMaxWidth(Double.MAX_VALUE);
        VBox panneauDemandes = new VBox(4, listeDemandes, boutonSupprimer);
        panneauDemandes.visibleProperty().bind(listeDemandes.visibleProperty());
        panneauDemandes.managedProperty().bind(listeDemandes.managedProperty());
        effacerDemandes();
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
        setTop(barreOutils);
        setCenter(carte);
        setRight(panneauTournee);
    }

    @Override
    public void definirActionImportPlan(Runnable action) {
        boutonImporterPlan.setOnAction(event -> action.run());
    }

    @Override
    public void definirActionCalculer(Runnable action) {
        boutonCalculer.setOnAction(event -> action.run());
    }

    @Override
    public void activerActions(Set<Action> actions) {
        boutonImporterPlan.setDisable(!actions.contains(Action.IMPORTER_PLAN));
        boutonImporterDemandes.setDisable(
                !actions.contains(Action.IMPORTER_PROGRAMME));
        boutonCalculer.setDisable(!actions.contains(Action.CALCULER));
        boutonAjouter.setDisable(!actions.contains(Action.AJOUTER_DEMANDE));
        boutonAnnuler.setDisable(!actions.contains(Action.ANNULER));
        boutonRefaire.setDisable(!actions.contains(Action.REFAIRE));
        suppressionPermise = actions.contains(Action.SUPPRIMER_DEMANDE);
        actualiserBoutonSupprimer();
        changementOrdrePermis = actions.contains(Action.CHANGER_ORDRE);
        actualiserBoutonsOrdre();
    }

    /**
     * Monter et Descendre : action permise et arrêt sélectionné. Le départ et
     * le retour de l'entrepôt (premier et dernier de la liste) ne se déplacent
     * pas.
     */
    private void actualiserBoutonsOrdre() {
        int rang = listeTournee.getSelectionModel().getSelectedIndex();
        boolean arretSelectionne = rang >= 1
                && rang < listeTournee.getItems().size() - 1;
        boutonMonter.setDisable(!changementOrdrePermis || !arretSelectionne);
        boutonDescendre.setDisable(!changementOrdrePermis
                || !arretSelectionne);
    }

    /** Supprimer demande une action permise et une demande sélectionnée. */
    private void actualiserBoutonSupprimer() {
        boutonSupprimer.setDisable(!suppressionPermise
                || listeDemandes.getSelectionModel().getSelectedIndex() < 0);
    }

    @Override
    public void definirActionAjouterDemande(Runnable action) {
        boutonAjouter.setOnAction(event -> action.run());
    }

    @Override
    public void definirActionSupprimerDemande(IntConsumer action) {
        boutonSupprimer.setOnAction(event -> action.accept(
                listeDemandes.getSelectionModel().getSelectedIndex()));
    }

    @Override
    public void definirActionMonterArret(IntConsumer action) {
        boutonMonter.setOnAction(event -> action.accept(
                listeTournee.getSelectionModel().getSelectedIndex()));
    }

    @Override
    public void definirActionDescendreArret(IntConsumer action) {
        boutonDescendre.setOnAction(event -> action.accept(
                listeTournee.getSelectionModel().getSelectedIndex()));
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
        carte.definirSurClicNoeud(action);
    }

    @Override
    public void afficherConsigne(String texte) {
        consigne.setText(texte);
    }

    @Override
    public void effacerTournee() {
        effacerListeTournee();
    }

    @Override
    public void afficherAttente(boolean enAttente) {
        indicateurAttente.setVisible(enAttente);
    }

    @Override
    public Optional<File> choisirFichierPlan() {
        return choisirFichierXml("Importer un plan XML");
    }

    private Optional<File> choisirFichierXml(String titre) {
        FileChooser selecteur = new FileChooser();
        selecteur.setTitle(titre);
        selecteur.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers XML", "*.xml"));
        if (dernierDossier != null && dernierDossier.isDirectory()) {
            selecteur.setInitialDirectory(dernierDossier);
        }
        File fichier = selecteur.showOpenDialog(getScene().getWindow());
        if (fichier != null) {
            dernierDossier = fichier.getParentFile();
        }
        return Optional.ofNullable(fichier);
    }

    @Override
    public void afficherPlan(Plan plan) {
        effacerListeTournee();
        effacerDemandes();
        carte.afficherPlan(plan);
        message.setText("");
    }

    @Override
    public void afficherTournee(Tournee tournee) {
        listeTournee.getItems().clear();
        for (Tournee.Arret arret : tournee.arrets()) {
            listeTournee.getItems().add(HEURE.format(arret.arrivee()) + " → "
                    + HEURE.format(arret.depart()) + "   nœud "
                    + arret.noeud().identifiant());
        }
        listeTournee.setVisible(true);
        listeTournee.setManaged(true);
    }

    @Override
    public void afficherErreurCalcul(String detail) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Optimod'Lyon");
        alert.setHeaderText("Calcul de la tournée impossible");
        alert.setContentText(detail);
        alert.initOwner(getScene().getWindow());
        alert.showAndWait();
    }

    @Override
    public void afficherRefusOrdre(String detail) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Optimod'Lyon");
        alert.setHeaderText("Changement d'ordre impossible");
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
        effacerListeTournee();
        effacerDemandes();
        carte.effacerCarte();
        message.setText("");
    }

    @Override
    public void definirActionImportProgramme(Runnable action) {
        boutonImporterDemandes.setOnAction(event -> action.run());
    }

    @Override
    public Optional<File> choisirFichierDemandes() {
        return choisirFichierXml("Importer des demandes de livraison XML");
    }

    @Override
    public void afficherProgramme(ProgrammeLivraisons programme) {
        effacerListeTournee();
        observer(programme);
    }

    /** Les demandes observées ont changé : redessine la liste et la carte. */
    @Override
    public void mettreAJour(Observable source) {
        dessinerDemandes();
    }

    /**
     * Change les demandes observées et les dessine.
     *
     * @param programme programme à observer, ou {@code null} pour aucun
     */
    private void observer(ProgrammeLivraisons programme) {
        if (programmeObserve != null) {
            programmeObserve.retirerObservateur(this);
        }
        programmeObserve = programme;
        if (programme != null) {
            programme.ajouterObservateur(this);
        }
        dessinerDemandes();
    }

    private void dessinerDemandes() {
        carte.afficherProgramme(programmeObserve);
        listeDemandes.getItems().clear();
        boolean visible = programmeObserve != null;
        listeDemandes.setVisible(visible);
        listeDemandes.setManaged(visible);
        if (!visible) {
            return;
        }
        int rang = 1;
        for (Demande demande : programmeObserve.obtenirDemandes()) {
            listeDemandes.getItems().add(
                    LibellesDemandes.decrire(rang++, demande));
        }
        message.setText("Programme chargé : "
                + programmeObserve.obtenirDemandes().size() + " demande(s)");
    }

    private void effacerDemandes() {
        observer(null);
    }

    private void effacerListeTournee() {
        listeTournee.getItems().clear();
        listeTournee.setVisible(false);
        listeTournee.setManaged(false);
    }
}
