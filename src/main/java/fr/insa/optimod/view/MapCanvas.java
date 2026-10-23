package fr.insa.optimod.view;

import fr.insa.optimod.model.Noeud;
import fr.insa.optimod.model.Plan;
import fr.insa.optimod.model.Troncon;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

/**
 * Carte : dessine les tronçons et les noeuds d'un plan.
 * Le dessin s'adapte à la taille du panneau.
 */
final class MapCanvas extends Pane {

    /** Marge autour de la carte, en pixels. */
    private static final double MARGE_INTERIEURE = 24;

    /** Rayon d'une intersection, en pixels. */
    private static final double RAYON_INTERSECTION = 1.6;

    private static final Color COULEUR_FOND = Color.web("#f8f9fa");
    private static final Color COULEUR_RUE = Color.web("#9aa0a6");
    private static final Color COULEUR_INTERSECTION = Color.web("#3c4043");

    /** Surface de dessin. */
    private final Canvas canevas = new Canvas();

    /** Plan affiché, ou {@code null}. */
    private Plan plan;

    /** Crée une carte vide. */
    MapCanvas() {
        getChildren().add(canevas);
    }

    /**
     * Affiche un plan.
     *
     * @param nouveauPlan plan à dessiner
     */
    void afficherPlan(Plan nouveauPlan) {
        plan = nouveauPlan;
        dessinerCarte();
    }

    /** Efface la carte. */
    void effacerCarte() {
        plan = null;
        dessinerCarte();
    }

    @Override
    protected void layoutChildren() {
        canevas.setWidth(getWidth());
        canevas.setHeight(getHeight());
        dessinerCarte();
    }

    private void dessinerCarte() {
        GraphicsContext contexteGraphique = canevas.getGraphicsContext2D();
        contexteGraphique.setFill(COULEUR_FOND);
        contexteGraphique.fillRect(0, 0, canevas.getWidth(), canevas.getHeight());
        if (plan == null || plan.obtenirNoeuds().isEmpty()) {
            return;
        }
        MapProjection projection = new MapProjection(plan.obtenirNoeuds(),
                canevas.getWidth(), canevas.getHeight(), MARGE_INTERIEURE);
        contexteGraphique.setStroke(COULEUR_RUE);
        contexteGraphique.setLineWidth(1);
        contexteGraphique.beginPath();
        for (Troncon troncon : plan.obtenirTroncons()) {
            contexteGraphique.moveTo(projection.calculerAbscisseEcran(troncon.obtenirNoeudOrigine().obtenirLongitude()),
                    projection.calculerOrdonneeEcran(troncon.obtenirNoeudOrigine().obtenirLatitude()));
            contexteGraphique.lineTo(projection.calculerAbscisseEcran(troncon.obtenirNoeudDestination().obtenirLongitude()),
                    projection.calculerOrdonneeEcran(troncon.obtenirNoeudDestination().obtenirLatitude()));
        }
        contexteGraphique.stroke();
        contexteGraphique.setFill(COULEUR_INTERSECTION);
        for (Noeud noeud : plan.obtenirNoeuds()) {
            contexteGraphique.fillOval(projection.calculerAbscisseEcran(noeud.obtenirLongitude())
                    - RAYON_INTERSECTION,
                    projection.calculerOrdonneeEcran(noeud.obtenirLatitude()) - RAYON_INTERSECTION,
                    2 * RAYON_INTERSECTION, 2 * RAYON_INTERSECTION);
        }
    }
}
