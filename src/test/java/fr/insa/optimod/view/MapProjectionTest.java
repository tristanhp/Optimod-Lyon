package fr.insa.optimod.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import fr.insa.optimod.model.Noeud;

public class MapProjectionTest {

    private static final double W = 800;
    private static final double H = 600;
    private static final double PAD = 20;

    /** Noeud à partir de (latitude, longitude), dans l'ordre de lecture. */
    private static Noeud noeud(long id, double latitude, double longitude) {
        return new Noeud(id, longitude, latitude);
    }

    @Test
    public void lNordEstEnHautEtLEstADroite() {
        Noeud sudOuest = noeud(1, 45.70, 4.80);
        Noeud nordEst = noeud(2, 45.80, 4.90);
        MapProjection p = new MapProjection(List.of(sudOuest, nordEst), W, H, PAD);
        assertTrue(p.toScreenY(45.80) < p.toScreenY(45.70));
        assertTrue(p.toScreenX(4.90) > p.toScreenX(4.80));
    }

    @Test
    public void toutLePlanResteDansLaZone() {
        Noeud a = noeud(1, 45.70, 4.80);
        Noeud b = noeud(2, 45.80, 4.95);
        MapProjection p = new MapProjection(List.of(a, b), W, H, PAD);
        for (Noeud i : List.of(a, b)) {
            double x = p.toScreenX(i.getLongitude());
            double y = p.toScreenY(i.getLatitude());
            assertTrue(x >= PAD - 1e-6 && x <= W - PAD + 1e-6);
            assertTrue(y >= PAD - 1e-6 && y <= H - PAD + 1e-6);
        }
    }

    @Test
    public void leDessinEstCentre() {
        Noeud a = noeud(1, 45.70, 4.80);
        Noeud b = noeud(2, 45.80, 4.82);
        MapProjection p = new MapProjection(List.of(a, b), W, H, PAD);
        double gauche = p.toScreenX(4.80);
        double droite = p.toScreenX(4.82);
        assertEquals(W - droite, gauche, 1e-6);
    }

    @Test
    public void unSeulPointNeFaitPasPlanter() {
        Noeud a = noeud(1, 45.75, 4.85);
        MapProjection p = new MapProjection(List.of(a), W, H, PAD);
        assertTrue(Double.isFinite(p.toScreenX(4.85)));
        assertTrue(Double.isFinite(p.toScreenY(45.75)));
    }
}
