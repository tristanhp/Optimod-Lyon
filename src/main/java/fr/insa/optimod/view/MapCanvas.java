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
    private static final double PADDING = 24;

    /** Rayon d'une intersection, en pixels. */
    private static final double INTERSECTION_RADIUS = 1.6;

    private static final Color BACKGROUND = Color.web("#f8f9fa");
    private static final Color ROAD = Color.web("#9aa0a6");
    private static final Color INTERSECTION = Color.web("#3c4043");

    /** Surface de dessin. */
    private final Canvas canvas = new Canvas();

    /** Plan affiché, ou {@code null}. */
    private Plan plan;

    /** Crée une carte vide. */
    MapCanvas() {
        getChildren().add(canvas);
    }

    /**
     * Affiche un plan.
     *
     * @param newPlan plan à dessiner
     */
    void showPlan(Plan newPlan) {
        plan = newPlan;
        draw();
    }

    /** Efface la carte. */
    void clear() {
        plan = null;
        draw();
    }

    @Override
    protected void layoutChildren() {
        canvas.setWidth(getWidth());
        canvas.setHeight(getHeight());
        draw();
    }

    private void draw() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setFill(BACKGROUND);
        g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        if (plan == null || plan.getNoeuds().isEmpty()) {
            return;
        }
        MapProjection projection = new MapProjection(plan.getNoeuds(),
                canvas.getWidth(), canvas.getHeight(), PADDING);
        g.setStroke(ROAD);
        g.setLineWidth(1);
        g.beginPath();
        for (Troncon t : plan.getTroncons()) {
            g.moveTo(projection.toScreenX(t.getNoeudOrigine().getLongitude()),
                    projection.toScreenY(t.getNoeudOrigine().getLatitude()));
            g.lineTo(projection.toScreenX(t.getNoeudDestination().getLongitude()),
                    projection.toScreenY(t.getNoeudDestination().getLatitude()));
        }
        g.stroke();
        g.setFill(INTERSECTION);
        for (Noeud n : plan.getNoeuds()) {
            g.fillOval(projection.toScreenX(n.getLongitude())
                    - INTERSECTION_RADIUS,
                    projection.toScreenY(n.getLatitude()) - INTERSECTION_RADIUS,
                    2 * INTERSECTION_RADIUS, 2 * INTERSECTION_RADIUS);
        }
    }
}
