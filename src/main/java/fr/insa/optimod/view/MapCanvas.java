package fr.insa.optimod.view;

import fr.insa.optimod.model.Intersection;
import fr.insa.optimod.model.Plan;
import fr.insa.optimod.model.Troncon;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Carte : dessine les tronçons, les intersections et l'entrepôt d'un plan.
 * Le dessin s'adapte à la taille du panneau.
 */
final class MapCanvas extends Pane {

    /** Marge autour de la carte, en pixels. */
    private static final double PADDING = 24;

    /** Rayon d'une intersection, en pixels. */
    private static final double INTERSECTION_RADIUS = 1.6;

    /** Demi-diagonale du losange de l'entrepôt, en pixels. */
    private static final double WAREHOUSE_SIZE = 11;

    private static final Color BACKGROUND = Color.web("#f8f9fa");
    private static final Color ROAD = Color.web("#9aa0a6");
    private static final Color INTERSECTION = Color.web("#3c4043");
    private static final Color WAREHOUSE = Color.web("#188038");

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
        if (plan == null || plan.getIntersections().isEmpty()) {
            return;
        }
        MapProjection projection = new MapProjection(plan, canvas.getWidth(),
                canvas.getHeight(), PADDING);
        g.setStroke(ROAD);
        g.setLineWidth(1);
        g.beginPath();
        for (Troncon t : plan.getTroncons()) {
            g.moveTo(projection.toScreenX(t.origine().longitude()),
                    projection.toScreenY(t.origine().latitude()));
            g.lineTo(projection.toScreenX(t.destination().longitude()),
                    projection.toScreenY(t.destination().latitude()));
        }
        g.stroke();
        g.setFill(INTERSECTION);
        for (Intersection i : plan.getIntersections()) {
            g.fillOval(projection.toScreenX(i.longitude())
                    - INTERSECTION_RADIUS,
                    projection.toScreenY(i.latitude()) - INTERSECTION_RADIUS,
                    2 * INTERSECTION_RADIUS, 2 * INTERSECTION_RADIUS);
        }
        plan.getEntrepot().ifPresent(e -> drawWarehouse(g, projection, e));
    }

    private void drawWarehouse(GraphicsContext g, MapProjection projection,
            Intersection warehouse) {
        double x = projection.toScreenX(warehouse.longitude());
        double y = projection.toScreenY(warehouse.latitude());
        g.setFill(WAREHOUSE);
        g.fillPolygon(
                new double[] {x, x + WAREHOUSE_SIZE, x, x - WAREHOUSE_SIZE},
                new double[] {y - WAREHOUSE_SIZE, y, y + WAREHOUSE_SIZE, y},
                4);
        g.setStroke(Color.WHITE);
        g.setLineWidth(2);
        g.strokePolygon(
                new double[] {x, x + WAREHOUSE_SIZE, x, x - WAREHOUSE_SIZE},
                new double[] {y - WAREHOUSE_SIZE, y, y + WAREHOUSE_SIZE, y},
                4);
        g.setFont(Font.font("System", FontWeight.BOLD, 13));
        g.setFill(WAREHOUSE);
        g.fillText("Entrepôt", x + WAREHOUSE_SIZE + 4, y + 4);
    }
}
