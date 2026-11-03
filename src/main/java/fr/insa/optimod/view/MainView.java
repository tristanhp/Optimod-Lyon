package fr.insa.optimod.view;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

import fr.insa.optimod.controller.MapScreen;
import fr.insa.optimod.model.ProgrammeLivraisons.Demande;
import fr.insa.optimod.model.ProgrammeLivraisons;
import fr.insa.optimod.model.Observable;
import fr.insa.optimod.model.Observateur;
import fr.insa.optimod.model.Tournee;
import fr.insa.optimod.model.Plan;
import fr.insa.optimod.model.Noeud;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ChoiceDialog;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.shape.Rectangle;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
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
    private final Button boutonImporterDemandes =
            new Button("Importer les demandes");
    private final Label message = new Label();

    private final Button boutonCalculer = new Button("Calculer");
    private final Button boutonSauvegarder = new Button("Sauvegarder");
    private final Button boutonRestaurer = new Button("Restaurer");

    private final Button boutonAjouter = new Button("Ajouter une demande");
    private final Button boutonSupprimer = new Button("Supprimer la demande");
    private final Button boutonMonter = new Button("Monter");
    private final Button boutonDescendre = new Button("Descendre");
    private final Button boutonDeplacer = new Button("Déplacer");
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

    /** Liste textuelle des tournées. */
    private final ListView<String> listeTournee = new ListView<>();

    /**
     * Ce que représente chaque ligne de la liste des tournées : un titre de
     * livreur ({@code rang} vaut -1) ou l'un de ses arrêts. Refaite avec la
     * liste, pour retrouver le livreur et le rang de la ligne sélectionnée.
     *
     * @param livreur numéro du livreur de la ligne
     * @param rang rang de l'arrêt dans la tournée du livreur (0 est le départ
     *        de l'entrepôt), ou -1 pour un titre
     * @param dernierRang rang du dernier arrêt (le retour à l'entrepôt)
     * @param horsPlage vrai si l'arrivée sort de la plage annoncée
     */
    private record LigneTournee(int livreur, int rang, int dernierRang,
            boolean horsPlage) {
    }

    /** Lignes de {@link #listeTournee}, dans le même ordre. */
    private final List<LigneTournee> lignesTournee = new ArrayList<>();

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
        FlowPane barreOutils = new FlowPane(8, 8,
                boutonImporterPlan, boutonImporterDemandes,
                boutonCalculer, indicateurAttente, boutonAjouter,
                boutonAnnuler, boutonRefaire, boutonSauvegarder,
                boutonRestaurer, consigne);
        barreOutils.getStyleClass().add("tool-bar-box");
        listeTournee.setPrefWidth(LARGEUR_TOURNEE);
        listeTournee.setVisible(false);
        listeTournee.setManaged(false);
        listeTournee.setCellFactory(liste -> new ListCell<>() {
            @Override
            protected void updateItem(String texte, boolean vide) {
                super.updateItem(texte, vide);
                setText(vide ? null : texte);
                boolean horsPlage = !vide && getIndex() >= 0
                        && getIndex() < lignesTournee.size()
                        && lignesTournee.get(getIndex()).horsPlage();
                setStyle(horsPlage ? "-fx-text-fill: #c62828;" : "");
            }
        });
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
        boutonDeplacer.setMaxWidth(Double.MAX_VALUE);
        boutonDeplacer.setTooltip(new Tooltip("Sélectionnez un arrêt dans la "
                + "liste, puis cliquez ici et sur sa nouvelle adresse."));
        HBox.setHgrow(boutonMonter, Priority.ALWAYS);
        HBox.setHgrow(boutonDescendre, Priority.ALWAYS);
        VBox.setVgrow(listeTournee, Priority.ALWAYS);
        VBox panneauTournee = new VBox(4, listeTournee,
                new HBox(4, boutonMonter, boutonDescendre), boutonDeplacer);
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
        boutonSauvegarder.setDisable(!actions.contains(Action.SAUVEGARDER));
        boutonRestaurer.setDisable(!actions.contains(Action.RESTAURER));
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
        boutonDeplacer.setDisable(!actions.contains(Action.DEPLACER_POINT));
    }

    /**
     * Monter et Descendre : action permise et arrêt sélectionné. Un titre de
     * livreur, le départ et le retour de l'entrepôt de chaque tournée ne se
     * déplacent pas.
     */
    private void actualiserBoutonsOrdre() {
        LigneTournee ligne = ligneSelectionnee();
        boolean arretSelectionne = ligne != null && ligne.rang() >= 1
                && ligne.rang() < ligne.dernierRang();
        boutonMonter.setDisable(!changementOrdrePermis || !arretSelectionne);
        boutonDescendre.setDisable(!changementOrdrePermis
                || !arretSelectionne);
    }

    /** @return la ligne sélectionnée de la liste des tournées, ou null */
    private LigneTournee ligneSelectionnee() {
        int indice = listeTournee.getSelectionModel().getSelectedIndex();
        return indice >= 0 && indice < lignesTournee.size()
                ? lignesTournee.get(indice) : null;
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
    public void definirActionMonterArret(BiConsumer<Integer, Integer> action) {
        boutonMonter.setOnAction(event -> deplacerArretSelectionne(action));
    }

    @Override
    public void definirActionDescendreArret(
            BiConsumer<Integer, Integer> action) {
        boutonDescendre.setOnAction(event -> deplacerArretSelectionne(action));
    }

    /** Transmet le livreur et le rang de l'arrêt sélectionné. */
    private void deplacerArretSelectionne(BiConsumer<Integer, Integer> action) {
        LigneTournee ligne = ligneSelectionnee();
        if (ligne != null && ligne.rang() >= 0) {
            action.accept(ligne.livreur(), ligne.rang());
        }
    }

    @Override
    public void definirActionDeplacerPoint(
            BiConsumer<Integer, Integer> action) {
        boutonDeplacer.setOnAction(event -> {
            LigneTournee ligne = ligneSelectionnee();
            boolean arret = ligne != null && ligne.rang() >= 0;
            action.accept(arret ? ligne.livreur() : -1,
                    arret ? ligne.rang() : -1);
        });
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
    public void afficherPointsProvisoires(Noeud collecte, Noeud livraison) {
        carte.afficherPointsProvisoires(collecte, livraison);
    }

    @Override
    public Optional<SaisieDemande> saisirDemande() {
        Dialog<SaisieDemande> dialogue = new Dialog<>();
        dialogue.setTitle("Ajouter une demande");
        dialogue.initOwner(getScene().getWindow());
        TextField collecte = new TextField("5");
        TextField livraison = new TextField("5");
        ComboBox<Integer> livreurs = new ComboBox<>();
        for (int livreur = 1; livreur <= programmeObserve
                .obtenirNombreLivreurs(); livreur++) {
            livreurs.getItems().add(livreur);
        }
        livreurs.setValue(1);
        Label erreur = new Label();
        dialogue.getDialogPane().setContent(new VBox(8,
                new Label("Durée de collecte (minutes)"), collecte,
                new Label("Durée de livraison (minutes)"), livraison,
                new Label("Livreur"), livreurs, erreur));
        dialogue.getDialogPane().getButtonTypes().addAll(
                ButtonType.OK, ButtonType.CANCEL);
        dialogue.getDialogPane().lookupButton(ButtonType.OK)
                .addEventFilter(ActionEvent.ACTION, evenement -> {
                    try {
                        SaisieDemande.convertirDuree(collecte.getText());
                        SaisieDemande.convertirDuree(livraison.getText());
                    } catch (IllegalArgumentException exception) {
                        erreur.setText(exception.getMessage());
                        evenement.consume();
                    }
                });
        dialogue.setResultConverter(bouton -> bouton == ButtonType.OK
                ? new SaisieDemande(collecte.getText(), livraison.getText(),
                        livreurs.getValue()) : null);
        return dialogue.showAndWait();
    }

    @Override
    public void signalerArretsHorsPlage(List<Tournee.Arret> arrets) {
        message.setText(arrets.isEmpty() ? "" : arrets.size()
                + " arrêt(s) hors plage annoncée. Annuler reste possible.");
    }

    @Override
    public Optional<Integer> choisirPositionInsertion(Tournee tournee) {
        List<String> choix = new java.util.ArrayList<>();
        int nombre = Math.max(1, tournee.arrets().size() - 1);
        for (int rang = 0; rang < nombre; rang++) {
            Tournee.Arret avant = tournee.arrets().get(rang);
            Tournee.Arret apres = tournee.arrets().get(
                    Math.min(rang + 1, tournee.arrets().size() - 1));
            choix.add((rang + 1) + " : après l'arrêt " + rang + " (nœud "
                    + avant.noeud().identifiant() + ", "
                    + HEURE.format(avant.depart()) + "), avant le nœud "
                    + apres.noeud().identifiant());
        }
        ChoiceDialog<String> dialogue = new ChoiceDialog<>(choix.get(0), choix);
        dialogue.setTitle("Position de la demande");
        dialogue.setHeaderText("Insérer la collecte puis la livraison");
        dialogue.setContentText("Entre quels arrêts ?");
        dialogue.initOwner(getScene().getWindow());
        return dialogue.showAndWait().map(valeur -> choix.indexOf(valeur) + 1);
    }

    public void afficherAttente(boolean enAttente) {
        indicateurAttente.setVisible(enAttente);
    }

    @Override
    public Optional<File> choisirFichierPlan() {
        return choisirFichierXml("Importer un plan XML");
    }

    @Override
    public void definirActionSauvegarder(Runnable action) {
        boutonSauvegarder.setOnAction(e -> action.run());
    }

    @Override
    public void definirActionRestaurer(Runnable action) {
        boutonRestaurer.setOnAction(e -> action.run());
    }

    @Override
    public Optional<File> choisirFichierSauvegarde() {
        return choisirFichierXml("Sauvegarder la tournée", true);
    }

    @Override
    public Optional<File> choisirFichierRestauration() {
        return choisirFichierXml("Restaurer une tournée");
    }

    private Optional<File> choisirFichierXml(String titre) {
        return choisirFichierXml(titre, false);
    }

    private Optional<File> choisirFichierXml(String titre, boolean sauvegarde) {
        FileChooser selecteur = new FileChooser();
        selecteur.setTitle(titre);
        selecteur.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers XML", "*.xml"));
        if (dernierDossier != null && dernierDossier.isDirectory()) {
            selecteur.setInitialDirectory(dernierDossier);
        }
        File fichier = sauvegarde
                ? selecteur.showSaveDialog(getScene().getWindow())
                : selecteur.showOpenDialog(getScene().getWindow());
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
    public void afficherErreurCalcul(String detail) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Optimod'Lyon");
        alert.setHeaderText("Calcul de la tournée impossible");
        alert.setContentText(detail);
        alert.initOwner(getScene().getWindow());
        alert.showAndWait();
    }

    @Override
    public void selectionnerArretTournee(int livreur, int rang) {
        for (int indice = 0; indice < lignesTournee.size(); indice++) {
            LigneTournee ligne = lignesTournee.get(indice);
            if (ligne.livreur() == livreur && ligne.rang() == rang) {
                listeTournee.getSelectionModel().select(indice);
                return;
            }
        }
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
    public void afficherRefusDeplacement(String detail) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Optimod'Lyon");
        alert.setHeaderText("Déplacement du point impossible");
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

    /**
     * Le programme observé a changé (demandes ou tournées) : redessine la
     * liste des demandes, la carte et la liste des tournées.
     */
    @Override
    public void mettreAJour(Observable source) {
        dessinerDemandes();
        dessinerTournees();
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
        dessinerTournees();
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

    /**
     * Liste les arrêts des tournées rangées dans le programme, avec leurs
     * heures de passage ; un titre par livreur s'il y en a plusieurs. La liste
     * est cachée tant qu'aucune tournée n'est calculée.
     */
    private void dessinerTournees() {
        Map<Integer, Tournee> tournees = programmeObserve == null ? Map.of()
                : programmeObserve.obtenirTournees();
        if (tournees.isEmpty()) {
            effacerListeTournee();
            return;
        }
        listeTournee.getItems().clear();
        lignesTournee.clear();
        for (Map.Entry<Integer, Tournee> entree : tournees.entrySet()) {
            int livreur = entree.getKey();
            int dernierRang = entree.getValue().arrets().size() - 1;
            if (tournees.size() > 1) {
                listeTournee.getItems().add("Livreur " + livreur);
                lignesTournee.add(new LigneTournee(livreur, -1, dernierRang,
                        false));
            }
            int rang = 0;
            for (Tournee.Arret arret : entree.getValue().arrets()) {
                listeTournee.getItems().add(decrireArret(arret));
                lignesTournee.add(new LigneTournee(livreur, rang++,
                        dernierRang, arret.estHorsPlage()));
            }
        }
        carte.afficherArrets(tournees.values().stream()
                .flatMap(tournee -> tournee.arrets().stream()).toList());
        listeTournee.setVisible(true);
        listeTournee.setManaged(true);
    }

    /** Heures, plage annoncée et décalage d'un arrêt. */
    private static String decrireArret(Tournee.Arret arret) {
        long decalage = arret.obtenirDecalageSecondes();
        String signe = decalage >= 0 ? "+" : "−";
        long secondes = Math.abs(decalage);
        return HEURE.format(arret.arrivee()) + " → "
                + HEURE.format(arret.depart()) + "   nœud "
                + arret.noeud().identifiant() + "\nPlage : "
                + HEURE.format(arret.debutPlage()) + " – "
                + HEURE.format(arret.finPlage()) + "\nDécalage : " + signe
                + secondes / 60 + " min " + secondes % 60 + " s"
                + (arret.estHorsPlage() ? " — Hors plage" : "");
    }

    private void effacerDemandes() {
        observer(null);
    }

    private void effacerListeTournee() {
        carte.afficherArrets(List.of());
        listeTournee.getItems().clear();
        lignesTournee.clear();
        listeTournee.setVisible(false);
        listeTournee.setManaged(false);
    }
}
