package fr.insa.optimod.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import fr.insa.optimod.model.Intersection;
import fr.insa.optimod.model.Plan;

public class MapProjectionTest {

    private static final double W = 800;
    private static final double H = 600;
    private static final double PAD = 20;

    private static Plan plan(Intersection... points) {
        return new Plan(List.of(points), List.of(), null);
    }

    @Test
    public void lNordEstEnHautEtLEstADroite() {
        Intersection sudOuest = new Intersection(1, 45.70, 4.80);
        Intersection nordEst = new Intersection(2, 45.80, 4.90);
        MapProjection p = new MapProjection(plan(sudOuest, nordEst), W, H, PAD);
        assertTrue(p.toScreenY(45.80) < p.toScreenY(45.70));
        assertTrue(p.toScreenX(4.90) > p.toScreenX(4.80));
    }

    @Test
    public void toutLePlanResteDansLaZone() {
        Intersection a = new Intersection(1, 45.70, 4.80);
        Intersection b = new Intersection(2, 45.80, 4.95);
        MapProjection p = new MapProjection(plan(a, b), W, H, PAD);
        for (Intersection i : List.of(a, b)) {
            double x = p.toScreenX(i.longitude());
            double y = p.toScreenY(i.latitude());
            assertTrue(x >= PAD - 1e-6 && x <= W - PAD + 1e-6);
            assertTrue(y >= PAD - 1e-6 && y <= H - PAD + 1e-6);
        }
    }

    @Test
    public void leDessinEstCentre() {
        Intersection a = new Intersection(1, 45.70, 4.80);
        Intersection b = new Intersection(2, 45.80, 4.82);
        MapProjection p = new MapProjection(plan(a, b), W, H, PAD);
        double gauche = p.toScreenX(4.80);
        double droite = p.toScreenX(4.82);
        assertEquals(W - droite, gauche, 1e-6);
    }

    @Test
    public void unSeulPointNeFaitPasPlanter() {
        Intersection a = new Intersection(1, 45.75, 4.85);
        MapProjection p = new MapProjection(plan(a), W, H, PAD);
        assertTrue(Double.isFinite(p.toScreenX(4.85)));
        assertTrue(Double.isFinite(p.toScreenY(45.75)));
    }
}
