package fr.insa.optimod.view;

import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

import fr.insa.optimod.model.Demande;
import fr.insa.optimod.model.Livraison;
import fr.insa.optimod.model.Noeud;
import fr.insa.optimod.model.Plan;
import fr.insa.optimod.model.Troncon;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
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

    /** @return couleur de la demande d'indice donné, la même sur la carte et dans la liste */
    static Color couleurDemande(int indice) {
        return COULEURS_DEMANDES[indice % COULEURS_DEMANDES.length];
    }

    private static final Color COULEUR_FOND = Color.web("#f8f9fa");
    private static final Color COULEUR_RUE = Color.web("#9aa0a6");
    private static final Color COULEUR_INTERSECTION = Color.web("#3c4043");

    /** Surface de dessin. */
    private final Canvas canevas = new Canvas();

    /** Plan affiché, ou {@code null}. */
    private Plan plan;

    /** Demandes affichées, ou {@code null}. */
    private Livraison livraison;

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
    private IntConsumer surSurvol = indice -> { };

    /** Prévenu de l'identifiant de l'intersection cliquée. */
    private LongConsumer surClicNoeud = idNoeud -> { };

    /** Crée une carte vide. */
    MapCanvas() {
        getChildren().add(canevas);
        setOnScroll(evenement -> {
            if (evenement.getDeltaY() != 0) {
                zoomer(evenement.getDeltaY() > 0 ? FACTEUR_MOLETTE : 1 / FACTEUR_MOLETTE,
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
                    surClicNoeud.accept(noeud.obtenirIdentifiant());
                }
            }
        });
        setOnMouseMoved(evenement -> signalerSurvol(
                trouverDemande(evenement.getX(), evenement.getY())));
        setOnMouseExited(evenement -> signalerSurvol(-1));
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
        double nouveauZoom = Math.max(1, Math.min(ZOOM_MAXIMAL, zoom * facteur));
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
        if (plan == null || livraison == null || projection == null) {
            return -1;
        }
        double portee = TAILLE_MARQUEUR + TOLERANCE_SURVOL;
        List<Demande> demandes = livraison.obtenirDemandes();
        for (int indice = demandes.size() - 1; indice >= 0; indice--) {
            Demande demande = demandes.get(indice);
            if (estProche(plan.obtenirNoeud(demande.obtenirIdentifiantCollecte()), x, y, portee)
                    || estProche(plan.obtenirNoeud(demande.obtenirIdentifiantLivraison()), x, y, portee)) {
                return indice;
            }
        }
        return -1;
    }

    /** Intersection la plus proche d'un point de l'écran, ou null si trop loin. */
    private Noeud trouverNoeud(double x, double y) {
        if (plan == null || projection == null) {
            return null;
        }
        Noeud plusProche = null;
        double meilleureDistance = TOLERANCE_CLIC * TOLERANCE_CLIC;
        for (Noeud noeud : plan.obtenirNoeuds()) {
            double dx = ecranX(noeud.obtenirLongitude()) - x;
            double dy = ecranY(noeud.obtenirLatitude()) - y;
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
                && Math.abs(ecranX(noeud.obtenirLongitude()) - x) <= portee
                && Math.abs(ecranY(noeud.obtenirLatitude()) - y) <= portee;
    }

    /**
     * Affiche un plan.
     *
     * @param nouveauPlan plan à dessiner
     */
    void afficherPlan(Plan nouveauPlan) {
        plan = nouveauPlan;
        livraison = null;
        demandeEnEvidence = -1;
        zoom = 1;
        decalageX = 0;
        decalageY = 0;
        dessinerCarte();
    }

    /**
     * Superpose les demandes au plan : collectes en carré, livraisons en rond.
     *
     * @param nouvelleLivraison demandes à dessiner, ou {@code null} pour les retirer
     */
    void afficherLivraison(Livraison nouvelleLivraison) {
        livraison = nouvelleLivraison;
        demandeEnEvidence = -1;
        dessinerCarte();
    }

    /** Efface la carte. */
    void effacerCarte() {
        plan = null;
        livraison = null;
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
        contenirDansLaCarte();
        dessinerCarte();
    }

    private void dessinerCarte() {
        GraphicsContext contexteGraphique = canevas.getGraphicsContext2D();
        contexteGraphique.setFill(COULEUR_FOND);
        contexteGraphique.fillRect(0, 0, canevas.getWidth(), canevas.getHeight());
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
            contexteGraphique.moveTo(ecranX(troncon.obtenirNoeudOrigine().obtenirLongitude()),
                    ecranY(troncon.obtenirNoeudOrigine().obtenirLatitude()));
            contexteGraphique.lineTo(ecranX(troncon.obtenirNoeudDestination().obtenirLongitude()),
                    ecranY(troncon.obtenirNoeudDestination().obtenirLatitude()));
        }
        contexteGraphique.stroke();
        contexteGraphique.setFill(COULEUR_INTERSECTION);
        for (Noeud noeud : plan.obtenirNoeuds()) {
            contexteGraphique.fillOval(ecranX(noeud.obtenirLongitude()) - RAYON_INTERSECTION,
                    ecranY(noeud.obtenirLatitude()) - RAYON_INTERSECTION,
                    2 * RAYON_INTERSECTION, 2 * RAYON_INTERSECTION);
        }
        if (livraison != null) {
            dessinerDemandes(contexteGraphique);
        }
    }

    private void dessinerDemandes(GraphicsContext contexte) {
        List<Demande> demandes = livraison.obtenirDemandes();
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
        Noeud depot = plan.obtenirNoeud(livraison.obtenirEntrepot().obtenirIdentifiant());
        if (depot == null) {
            return;
        }
        double x = ecranX(depot.obtenirLongitude());
        double y = ecranY(depot.obtenirLatitude());
        double[] abscisses = {x, x + TAILLE_ENTREPOT, x, x - TAILLE_ENTREPOT};
        double[] ordonnees = {y - TAILLE_ENTREPOT, y, y + TAILLE_ENTREPOT, y};
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
    private void dessinerDemande(GraphicsContext contexte, int indice, boolean enEvidence) {
        Demande demande = livraison.obtenirDemandes().get(indice);
        double taille = enEvidence ? TAILLE_MARQUEUR * TAILLE_EN_EVIDENCE : TAILLE_MARQUEUR;
        contexte.setFill(couleurDemande(indice));
        contexte.setStroke(COULEUR_CONTOUR_EVIDENCE);
        contexte.setLineWidth(2);
        Noeud collecte = plan.obtenirNoeud(demande.obtenirIdentifiantCollecte());
        if (collecte != null) {
            double x = ecranX(collecte.obtenirLongitude()) - taille;
            double y = ecranY(collecte.obtenirLatitude()) - taille;
            contexte.fillRect(x, y, 2 * taille, 2 * taille);
            if (enEvidence) {
                contexte.strokeRect(x, y, 2 * taille, 2 * taille);
            }
        }
        Noeud destination = plan.obtenirNoeud(demande.obtenirIdentifiantLivraison());
        if (destination != null) {
            double x = ecranX(destination.obtenirLongitude()) - taille;
            double y = ecranY(destination.obtenirLatitude()) - taille;
            contexte.fillOval(x, y, 2 * taille, 2 * taille);
            if (enEvidence) {
                contexte.strokeOval(x, y, 2 * taille, 2 * taille);
            }
        }
    }
}
