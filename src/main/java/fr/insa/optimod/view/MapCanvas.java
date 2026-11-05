package fr.insa.optimod.view;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

import fr.insa.optimod.model.Chemin;
import fr.insa.optimod.model.Noeud;
import fr.insa.optimod.model.Plan;
import fr.insa.optimod.model.ProgrammeLivraisons;
import fr.insa.optimod.model.ProgrammeLivraisons.Demande;
import fr.insa.optimod.model.Tournee;
import fr.insa.optimod.model.Troncon;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.TextAlignment;

/**
 * Carte : dessine les tronçons et les noeuds d'un plan.
 * Le dessin s'adapte à la taille du panneau.
 */
final class MapCanvas extends Pane {

    /** Marge autour de la carte, en pixels. */
    private static final double MARGE_INTERIEURE = 24;

    /** Rayon d'une intersection, en pixels. */
    private static final double RAYON_INTERSECTION = 1.6;

    /** Demi-côté d'un marqueur de collecte ou de livraison, en pixels. */
    private static final double TAILLE_MARQUEUR = 5;

    private static final Color COULEUR_ENTREPOT = Color.web("#202124");

    /** Demi-diagonale du losange de l'entrepôt, en pixels. */
    private static final double TAILLE_ENTREPOT = 9;

    /** Agrandissement d'un marqueur mis en évidence. */
    private static final double TAILLE_EN_EVIDENCE = 1.6;

    private static final Color COULEUR_CONTOUR_EVIDENCE = Color.BLACK;

    /** Zoom maximal (1 = plan entier visible). */
    private static final double ZOOM_MAXIMAL = 40;

    /** Facteur appliqué par un cran de molette. */
    private static final double FACTEUR_MOLETTE = 1.2;

    /** Marge de tolérance autour d'un marqueur pour le survol, en pixels. */
    private static final double TOLERANCE_SURVOL = 3;

    /** Distance maximale entre un clic et l'intersection choisie, en pixels. */
    private static final double TOLERANCE_CLIC = 10;

    /** Une couleur par demande, réutilisées en boucle. */
    private static final Color[] COULEURS_DEMANDES = {
            Color.web("#d93025"), Color.web("#1a73e8"), Color.web("#188038"),
            Color.web("#e37400"), Color.web("#9334e6"), Color.web("#129eaf"),
            Color.web("#c2185b"), Color.web("#5f6368")
    };

    /**
     * Renvoie la couleur commune à la carte et à la liste pour une demande.
     *
     * @param indice rang de la demande
     * @return couleur de la demande
     */
    static Color couleurDemande(int indice) {
        return COULEURS_DEMANDES[indice % COULEURS_DEMANDES.length];
    }

    /** Couleur de la tournée au départ de l'entrepôt (clair). */
    private static final Color COULEUR_DEBUT_TOURNEE = Color.web("#8ab4f8");

    /** Couleur de la tournée au retour à l'entrepôt (foncé). */
    private static final Color COULEUR_FIN_TOURNEE = Color.web("#0b2a6b");

    /** Largeur du trait de la tournée, en pixels. */
    private static final double LARGEUR_TOURNEE = 5;

    /** Demi-longueur d'une flèche de sens, en pixels. */
    private static final double TAILLE_FLECHE = 5;

    /** Longueur d'écran en dessous de laquelle un tronçon n'a pas de flèche. */
    private static final double LONGUEUR_MINIMALE_FLECHE = 14;

    /** Rayon du disque qui porte le numéro d'un arrêt, en pixels. */
    private static final double RAYON_NUMERO = 8;

    private static final Color COULEUR_FOND = Color.web("#f8f9fa");
    private static final Color COULEUR_RUE = Color.web("#9aa0a6");
    private static final Color COULEUR_INTERSECTION = Color.web("#3c4043");

    /** Surface de dessin. */
    private final Canvas canevas = new Canvas();

    /** Tournées dessinées, par numéro de livreur. */
    private Map<Integer, Tournee> tournees = Map.of();

    /** Popup du type, de l'adresse et des heures de l'arrêt cliqué. */
    private final Tooltip detailsArret = new Tooltip();

    /** Message affiché à la place de la carte, quand le dessin échoue. */
    private final Label messageErreur = new Label();

    /** Livreur de l'arrêt sélectionné, ou -1. */
    private int livreurSelectionne = -1;

    /** Rang dans sa tournée de l'arrêt sélectionné, ou -1. */
    private int rangSelectionne = -1;

    /** Plan affiché, ou {@code null}. */
    private Plan plan;

    /** Demandes affichées, ou {@code null}. */
    private ProgrammeLivraisons programme;

    /** Points de la demande en cours, indépendants du programme validé. */
    private Noeud collecteProvisoire;
    private Noeud livraisonProvisoire;

    /**
     * Redessine les points choisis avant validation de la demande.
     *
     * @param collecte  point de collecte, ou null pour effacer l'aperçu
     * @param livraison point de livraison, ou null avant le second clic
     */
    void afficherPointsProvisoires(Noeud collecte, Noeud livraison) {
        collecteProvisoire = collecte;
        livraisonProvisoire = livraison;
        dessinerCarte();
    }

    /** Zoom courant ; 1 affiche le plan entier. */
    private double zoom = 1;

    /** Décalage du plan zoomé, en pixels. */
    private double decalageX;
    private double decalageY;

    /** Position du dernier événement de glissement. */
    private double dernierX;
    private double dernierY;

    /** Projection du dernier dessin, utilisée pour repérer le survol. */
    private MapProjection projection;

    /** Indice de la demande mise en évidence, ou -1. */
    private int demandeEnEvidence = -1;

    /** Prévenu de l'indice de la demande survolée (-1 hors d'un point). */
    private IntConsumer surSurvol = indice -> {
    };

    /** Prévenu de l'identifiant de l'intersection cliquée. */
    private LongConsumer surClicNoeud = idNoeud -> {
    };

    /** Prévenu du livreur et du rang de l'arrêt cliqué. */
    private BiConsumer<Integer, Integer> surClicArret = (livreur, rang) -> {
    };

    /** Prévenu de l'explication quand le dessin lève une exception. */
    private Consumer<String> surErreurDessin = detail -> {
    };

    /** Crée une carte vide. */
    MapCanvas() {
        messageErreur.setWrapText(true);
        messageErreur.setAlignment(Pos.CENTER);
        messageErreur.setStyle("-fx-font-size: 14px; -fx-text-fill: #c62828;");
        messageErreur.setVisible(false);
        detailsArret.setAutoHide(true);
        getChildren().addAll(canevas, messageErreur);
        setOnScroll(evenement -> {
            if (evenement.getDeltaY() != 0) {
                zoomer(evenement.getDeltaY() > 0 ? FACTEUR_MOLETTE
                        : 1
                                / FACTEUR_MOLETTE,
                        evenement.getX(), evenement.getY());
            }
        });
        setOnMousePressed(evenement -> {
            dernierX = evenement.getX();
            dernierY = evenement.getY();
        });
        setOnMouseDragged(evenement -> {
            if (evenement.getButton() == MouseButton.PRIMARY && zoom > 1) {
                decalageX += evenement.getX() - dernierX;
                decalageY += evenement.getY() - dernierY;
                dernierX = evenement.getX();
                dernierY = evenement.getY();
                contenirDansLaCarte();
                dessinerCarte();
            }
        });
        setOnMouseClicked(evenement -> {
            if (evenement.getClickCount() == 2) {
                reinitialiserZoom();
            } else if (evenement.getButton() == MouseButton.PRIMARY
                    && evenement.isStillSincePress()) {
                Noeud noeud = trouverNoeud(evenement.getX(), evenement.getY());
                if (noeud != null) {
                    surClicNoeud.accept(noeud.identifiant());
                }
                montrerArret(noeud, evenement.getScreenX(),
                        evenement.getScreenY());
            }
        });
        setOnMouseMoved(evenement -> signalerSurvol(
                trouverDemande(evenement.getX(), evenement.getY())));
        setOnMouseExited(evenement -> signalerSurvol(-1));
    }

    /**
     * Dessine les tournées : tronçons en dégradé, flèches de sens, numéros des
     * arrêts.
     *
     * @param nouvellesTournees tournées par numéro de livreur, vide pour les
     *                          effacer
     */
    void afficherTournees(Map<Integer, Tournee> nouvellesTournees) {
        tournees = Map.copyOf(nouvellesTournees);
        detailsArret.hide();
        livreurSelectionne = -1;
        rangSelectionne = -1;
        dessinerCarte();
    }

    /**
     * Met en évidence un arrêt, sans prévenir personne (la liste l'a choisi).
     *
     * @param livreur numéro du livreur, ou -1 pour aucun
     * @param rang    rang de l'arrêt dans sa tournée, ou -1 pour aucun
     */
    void selectionnerArret(int livreur, int rang) {
        if (livreur != livreurSelectionne || rang != rangSelectionne) {
            livreurSelectionne = livreur;
            rangSelectionne = rang;
            dessinerCarte();
        }
    }

    /**
     * Définit l'action déclenchée par un clic sur un arrêt de la tournée.
     *
     * @param action reçoit le livreur puis le rang de l'arrêt
     */
    void definirSurClicArret(BiConsumer<Integer, Integer> action) {
        surClicArret = action;
    }

    /**
     * Définit l'action déclenchée quand le dessin lève une exception.
     *
     * @param action reçoit l'explication de l'erreur
     */
    void definirSurErreurDessin(Consumer<String> action) {
        surErreurDessin = action;
    }

    /**
     * Affiche un message à la place de la carte.
     *
     * @param detail explication de l'erreur
     */
    void afficherMessageErreur(String detail) {
        messageErreur.setText("La carte ne peut pas être affichée.\n" + detail);
        messageErreur.setVisible(true);
        requestLayout();
    }

    /**
     * Sélectionne et décrit l'arrêt qui se trouve au nœud cliqué ; sans arrêt,
     * ferme le popup.
     */
    private void montrerArret(Noeud noeud, double ecranXSouris,
            double ecranYSouris) {
        detailsArret.hide();
        if (noeud == null) {
            return;
        }
        for (Map.Entry<Integer, Tournee> entree : tournees.entrySet()) {
            List<Tournee.Arret> arrets = entree.getValue().arrets();
            for (int rang = 0; rang < arrets.size(); rang++) {
                Tournee.Arret arret = arrets.get(rang);
                if (arret.noeud().equals(noeud)) {
                    selectionnerArret(entree.getKey(), rang);
                    surClicArret.accept(entree.getKey(), rang);
                    detailsArret.setText(
                            LibellesArrets.decrire(plan, arret));
                    detailsArret.show(this, ecranXSouris + 12,
                            ecranYSouris + 12);
                    return;
                }
            }
        }
    }

    /**
     * Définit l'action déclenchée quand la souris passe sur une demande.
     *
     * @param action reçoit l'indice de la demande, ou -1 en dehors
     */
    void definirSurSurvol(IntConsumer action) {
        surSurvol = action;
    }

    /**
     * Définit l'action déclenchée par un clic sur une intersection.
     *
     * @param action reçoit l'identifiant du noeud le plus proche du clic
     */
    void definirSurClicNoeud(LongConsumer action) {
        surClicNoeud = action;
    }

    /**
     * Met en évidence la collecte et la livraison d'une demande.
     *
     * @param indice indice de la demande, ou -1 pour aucune
     */
    void mettreEnEvidence(int indice) {
        if (indice != demandeEnEvidence) {
            demandeEnEvidence = indice;
            dessinerCarte();
        }
    }

    private void signalerSurvol(int indice) {
        mettreEnEvidence(indice);
        surSurvol.accept(indice);
    }

    /** Revient à la vue d'ensemble du plan. */
    void reinitialiserZoom() {
        zoom = 1;
        decalageX = 0;
        decalageY = 0;
        dessinerCarte();
    }

    /** Zoome autour d'un point de l'écran, de façon à ce qu'il reste fixe. */
    private void zoomer(double facteur, double x, double y) {
        double nouveauZoom = Math.max(1, Math.min(ZOOM_MAXIMAL, zoom
                * facteur));
        double rapport = nouveauZoom / zoom;
        decalageX = x - (x - decalageX) * rapport;
        decalageY = y - (y - decalageY) * rapport;
        zoom = nouveauZoom;
        contenirDansLaCarte();
        dessinerCarte();
    }

    /** Empêche de faire glisser le plan hors de la zone visible. */
    private void contenirDansLaCarte() {
        decalageX = Math.min(0, Math.max(decalageX, getWidth() * (1 - zoom)));
        decalageY = Math.min(0, Math.max(decalageY, getHeight() * (1 - zoom)));
    }

    private double ecranX(double longitude) {
        return projection.calculerAbscisseEcran(longitude) * zoom + decalageX;
    }

    private double ecranY(double latitude) {
        return projection.calculerOrdonneeEcran(latitude) * zoom + decalageY;
    }

    /** Indice de la demande dont un point est sous la souris, ou -1. */
    private int trouverDemande(double x, double y) {
        if (plan == null || programme == null || projection == null) {
            return -1;
        }
        double portee = TAILLE_MARQUEUR + TOLERANCE_SURVOL;
        List<Demande> demandes = programme.obtenirDemandes();
        for (int indice = demandes.size() - 1; indice >= 0; indice--) {
            Demande demande = demandes.get(indice);
            if (estProche(demande.collecte(), x, y, portee)
                    || estProche(demande.livraison(), x, y, portee)) {
                return indice;
            }
        }
        return -1;
    }

    /**
     * Intersection la plus proche d'un point de l'écran, ou null si trop loin.
     */
    private Noeud trouverNoeud(double x, double y) {
        if (plan == null || projection == null) {
            return null;
        }
        Noeud plusProche = null;
        double meilleureDistance = TOLERANCE_CLIC * TOLERANCE_CLIC;
        for (Noeud noeud : plan.obtenirNoeuds()) {
            double dx = ecranX(noeud.longitude()) - x;
            double dy = ecranY(noeud.latitude()) - y;
            double distance = dx * dx + dy * dy;
            if (distance <= meilleureDistance) {
                meilleureDistance = distance;
                plusProche = noeud;
            }
        }
        return plusProche;
    }

    private boolean estProche(Noeud noeud, double x, double y, double portee) {
        return noeud != null
                && Math.abs(ecranX(noeud.longitude()) - x) <= portee
                && Math.abs(ecranY(noeud.latitude()) - y) <= portee;
    }

    /**
     * Affiche un plan.
     *
     * @param nouveauPlan plan à dessiner
     */
    void afficherPlan(Plan nouveauPlan) {
        collecteProvisoire = null;
        livraisonProvisoire = null;
        plan = nouveauPlan;
        programme = null;
        tournees = Map.of();
        messageErreur.setVisible(false);
        demandeEnEvidence = -1;
        zoom = 1;
        decalageX = 0;
        decalageY = 0;
        dessinerCarte();
    }

    /**
     * Superpose les demandes au plan : collectes en carré, livraisons en rond.
     *
     * @param nouveauProgramme demandes à dessiner, ou {@code null} pour les
     *                         retirer
     */
    void afficherProgramme(ProgrammeLivraisons nouveauProgramme) {
        programme = nouveauProgramme;
        demandeEnEvidence = -1;
        dessinerCarte();
    }

    /** Efface la carte. */
    void effacerCarte() {
        collecteProvisoire = null;
        livraisonProvisoire = null;
        plan = null;
        programme = null;
        tournees = Map.of();
        messageErreur.setVisible(false);
        detailsArret.hide();
        demandeEnEvidence = -1;
        zoom = 1;
        decalageX = 0;
        decalageY = 0;
        dessinerCarte();
    }

    @Override
    protected void layoutChildren() {
        canevas.setWidth(getWidth());
        canevas.setHeight(getHeight());
        messageErreur.resizeRelocate(MARGE_INTERIEURE, MARGE_INTERIEURE,
                Math.max(0, getWidth() - 2 * MARGE_INTERIEURE),
                Math.max(0, getHeight() - 2 * MARGE_INTERIEURE));
        contenirDansLaCarte();
        dessinerCarte();
    }

    /** Dessine la carte ; une exception remplace la carte par un message. */
    private void dessinerCarte() {
        try {
            dessinerCarteComplete();
        } catch (RuntimeException erreur) {
            String detail = erreur.getMessage() != null ? erreur.getMessage()
                    : erreur.getClass().getSimpleName();
            surErreurDessin.accept(detail);
        }
    }

    private void dessinerCarteComplete() {
        GraphicsContext contexteGraphique = canevas.getGraphicsContext2D();
        contexteGraphique.setFill(COULEUR_FOND);
        contexteGraphique.fillRect(0, 0, canevas.getWidth(),
                canevas.getHeight());
        if (plan == null || plan.obtenirNoeuds().isEmpty()) {
            projection = null;
            return;
        }
        projection = new MapProjection(plan.obtenirNoeuds(),
                canevas.getWidth(), canevas.getHeight(), MARGE_INTERIEURE);
        contexteGraphique.setStroke(COULEUR_RUE);
        contexteGraphique.setLineWidth(1);
        contexteGraphique.beginPath();
        for (Troncon troncon : plan.obtenirTroncons()) {
            contexteGraphique.moveTo(ecranX(troncon.origine().longitude()),
                    ecranY(troncon.origine().latitude()));
            contexteGraphique.lineTo(ecranX(troncon.destination().longitude()),
                    ecranY(troncon.destination().latitude()));
        }
        contexteGraphique.stroke();
        contexteGraphique.setFill(COULEUR_INTERSECTION);
        for (Noeud noeud : plan.obtenirNoeuds()) {
            contexteGraphique.fillOval(ecranX(noeud.longitude())
                    - RAYON_INTERSECTION,
                    ecranY(noeud.latitude()) - RAYON_INTERSECTION,
                    2 * RAYON_INTERSECTION, 2 * RAYON_INTERSECTION);
        }
        dessinerTournees(contexteGraphique);
        if (programme != null) {
            dessinerDemandes(contexteGraphique);
        }
        dessinerNumerosArrets(contexteGraphique);
        dessinerPointsProvisoires(contexteGraphique);
    }

    /**
     * Trace les tronçons de chaque chemin d'un trait épais, du plus clair (au
     * départ) au plus foncé (au retour), avec une flèche au milieu de chaque
     * tronçon pour montrer le sens de parcours.
     */
    private void dessinerTournees(GraphicsContext contexte) {
        contexte.setLineCap(StrokeLineCap.ROUND);
        for (Tournee tournee : tournees.values()) {
            int total = tournee.chemins().stream()
                    .mapToInt(chemin -> chemin.troncons().size()).sum();
            int fait = 0;
            for (Chemin chemin : tournee.chemins()) {
                for (Troncon troncon : chemin.troncons()) {
                    verifierDansLePlan(troncon.origine());
                    verifierDansLePlan(troncon.destination());
                    double avancement = total <= 1 ? 0
                            : (double) fait / (total - 1);
                    dessinerTroncon(contexte, troncon,
                            COULEUR_DEBUT_TOURNEE.interpolate(
                                    COULEUR_FIN_TOURNEE, avancement));
                    fait++;
                }
            }
        }
        contexte.setLineCap(StrokeLineCap.BUTT);
    }

    /** Signale une adresse de la tournée qui n'existe pas dans le plan. */
    private void verifierDansLePlan(Noeud noeud) {
        if (plan.obtenirNoeud(noeud.identifiant()) == null) {
            throw new IllegalStateException("Adresse " + noeud.identifiant()
                    + " absente du plan.");
        }
    }

    private void dessinerTroncon(GraphicsContext contexte, Troncon troncon,
            Color couleur) {
        double x1 = ecranX(troncon.origine().longitude());
        double y1 = ecranY(troncon.origine().latitude());
        double x2 = ecranX(troncon.destination().longitude());
        double y2 = ecranY(troncon.destination().latitude());
        contexte.setStroke(couleur);
        contexte.setLineWidth(LARGEUR_TOURNEE);
        contexte.strokeLine(x1, y1, x2, y2);
        double longueur = Math.hypot(x2 - x1, y2 - y1);
        if (longueur < LONGUEUR_MINIMALE_FLECHE) {
            return;
        }
        double ux = (x2 - x1) / longueur;
        double uy = (y2 - y1) / longueur;
        double mx = (x1 + x2) / 2;
        double my = (y1 + y2) / 2;
        double[] abscisses = { mx + ux * TAILLE_FLECHE,
                mx - ux * TAILLE_FLECHE * 0.6 - uy * TAILLE_FLECHE * 0.8,
                mx - ux * TAILLE_FLECHE * 0.6 + uy * TAILLE_FLECHE * 0.8 };
        double[] ordonnees = { my + uy * TAILLE_FLECHE,
                my - uy * TAILLE_FLECHE * 0.6 + ux * TAILLE_FLECHE * 0.8,
                my - uy * TAILLE_FLECHE * 0.6 - ux * TAILLE_FLECHE * 0.8 };
        contexte.setFill(Color.WHITE);
        contexte.fillPolygon(abscisses, ordonnees, 3);
    }

    /**
     * Numérote les arrêts dans l'ordre de passage (le départ et le retour de
     * l'entrepôt gardent le losange) et entoure l'arrêt sélectionné.
     */
    private void dessinerNumerosArrets(GraphicsContext contexte) {
        contexte.setTextAlign(TextAlignment.CENTER);
        contexte.setTextBaseline(VPos.CENTER);
        for (Map.Entry<Integer, Tournee> entree : tournees.entrySet()) {
            List<Tournee.Arret> arrets = entree.getValue().arrets();
            for (int rang = 1; rang < arrets.size() - 1; rang++) {
                Noeud noeud = arrets.get(rang).noeud();
                verifierDansLePlan(noeud);
                double x = ecranX(noeud.longitude());
                double y = ecranY(noeud.latitude());
                boolean selectionne = entree.getKey() == livreurSelectionne
                        && rang == rangSelectionne;
                double rayon = selectionne ? RAYON_NUMERO * 1.3 : RAYON_NUMERO;
                contexte.setFill(selectionne ? Color.web("#fbbc04")
                        : Color.WHITE);
                contexte.setStroke(COULEUR_FIN_TOURNEE);
                contexte.setLineWidth(selectionne ? 3 : 2);
                contexte.fillOval(x - rayon, y - rayon, 2 * rayon, 2 * rayon);
                contexte.strokeOval(x - rayon, y - rayon, 2 * rayon, 2 * rayon);
                contexte.setFill(Color.BLACK);
                contexte.fillText(String.valueOf(rang), x, y);
            }
        }
        contexte.setTextBaseline(VPos.BASELINE);
    }

    /** Dessine l'aperçu au-dessus des points déjà validés. */
    private void dessinerPointsProvisoires(GraphicsContext contexte) {
        int indice = programme == null ? 0 : programme.obtenirDemandes().size();
        contexte.setFill(couleurDemande(indice));
        contexte.setStroke(COULEUR_CONTOUR_EVIDENCE);
        contexte.setLineWidth(2);
        double taille = TAILLE_MARQUEUR * TAILLE_EN_EVIDENCE;
        if (collecteProvisoire != null) {
            double x = ecranX(collecteProvisoire.longitude()) - taille;
            double y = ecranY(collecteProvisoire.latitude()) - taille;
            contexte.fillRect(x, y, 2 * taille, 2 * taille);
            contexte.strokeRect(x, y, 2 * taille, 2 * taille);
        }
        if (livraisonProvisoire != null) {
            double x = ecranX(livraisonProvisoire.longitude()) - taille;
            double y = ecranY(livraisonProvisoire.latitude()) - taille;
            contexte.fillOval(x, y, 2 * taille, 2 * taille);
            contexte.strokeOval(x, y, 2 * taille, 2 * taille);
        }
    }

    private void dessinerDemandes(GraphicsContext contexte) {
        List<Demande> demandes = programme.obtenirDemandes();
        for (int indice = 0; indice < demandes.size(); indice++) {
            if (indice != demandeEnEvidence) {
                dessinerDemande(contexte, indice, false);
            }
        }
        if (demandeEnEvidence >= 0 && demandeEnEvidence < demandes.size()) {
            dessinerDemande(contexte, demandeEnEvidence, true);
        }
        dessinerEntrepot(contexte);
    }

    /** Dessine l'entrepôt par-dessus le reste : losange étiqueté. */
    private void dessinerEntrepot(GraphicsContext contexte) {
        Noeud depot = programme.obtenirEntrepot();
        if (depot == null) {
            return;
        }
        double x = ecranX(depot.longitude());
        double y = ecranY(depot.latitude());
        double[] abscisses = { x, x + TAILLE_ENTREPOT, x, x - TAILLE_ENTREPOT };
        double[] ordonnees = { y - TAILLE_ENTREPOT, y, y + TAILLE_ENTREPOT, y };
        contexte.setFill(COULEUR_ENTREPOT);
        contexte.setStroke(COULEUR_FOND);
        contexte.setLineWidth(2);
        contexte.fillPolygon(abscisses, ordonnees, 4);
        contexte.strokePolygon(abscisses, ordonnees, 4);
        contexte.setTextAlign(TextAlignment.CENTER);
        contexte.setLineWidth(3);
        contexte.strokeText("Entrepôt", x, y - TAILLE_ENTREPOT - 4);
        contexte.fillText("Entrepôt", x, y - TAILLE_ENTREPOT - 4);
    }

    /** Dessine la collecte (carré) et la livraison (rond) d'une demande. */
    private void dessinerDemande(GraphicsContext contexte, int indice,
            boolean enEvidence) {
        Demande demande = programme.obtenirDemandes().get(indice);
        double taille = enEvidence ? TAILLE_MARQUEUR * TAILLE_EN_EVIDENCE
                : TAILLE_MARQUEUR;
        contexte.setFill(couleurDemande(indice));
        contexte.setStroke(COULEUR_CONTOUR_EVIDENCE);
        contexte.setLineWidth(2);
        Noeud collecte = demande.collecte();
        if (collecte != null) {
            double x = ecranX(collecte.longitude()) - taille;
            double y = ecranY(collecte.latitude()) - taille;
            contexte.fillRect(x, y, 2 * taille, 2 * taille);
            if (enEvidence) {
                contexte.strokeRect(x, y, 2 * taille, 2 * taille);
            }
        }
        Noeud destination = demande.livraison();
        if (destination != null) {
            double x = ecranX(destination.longitude()) - taille;
            double y = ecranY(destination.latitude()) - taille;
            contexte.fillOval(x, y, 2 * taille, 2 * taille);
            if (enEvidence) {
                contexte.strokeOval(x, y, 2 * taille, 2 * taille);
            }
        }
    }
}
