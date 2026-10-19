package fr.insa.optimod.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import fr.insa.optimod.model.Noeud;

public class MapProjectionTest {

    private static final double LARGEUR = 800;
    private static final double HAUTEUR = 600;
    private static final double MARGE = 20;

    /** Noeud à partir de (latitude, longitude), dans l'ordre de lecture. */
    private static Noeud noeud(long identifiant, double latitude, double longitude) {
        return new Noeud(identifiant, longitude, latitude);
    }

    @Test
    public void lNordEstEnHautEtLEstADroite() {
        Noeud sudOuest = noeud(1, 45.70, 4.80);
        Noeud nordEst = noeud(2, 45.80, 4.90);
        MapProjection projection = new MapProjection(List.of(sudOuest, nordEst), LARGEUR, HAUTEUR, MARGE);
        assertTrue(projection.calculerOrdonneeEcran(45.80) < projection.calculerOrdonneeEcran(45.70));
        assertTrue(projection.calculerAbscisseEcran(4.90) > projection.calculerAbscisseEcran(4.80));
    }

    @Test
    public void toutLePlanResteDansLaZone() {
        Noeud premierNoeud = noeud(1, 45.70, 4.80);
        Noeud secondNoeud = noeud(2, 45.80, 4.95);
        MapProjection projection = new MapProjection(List.of(premierNoeud, secondNoeud), LARGEUR, HAUTEUR, MARGE);
        for (Noeud noeud : List.of(premierNoeud, secondNoeud)) {
            double abscisse = projection.calculerAbscisseEcran(noeud.obtenirLongitude());
            double ordonnee = projection.calculerOrdonneeEcran(noeud.obtenirLatitude());
            assertTrue(abscisse >= MARGE - 1e-6 && abscisse <= LARGEUR - MARGE + 1e-6);
            assertTrue(ordonnee >= MARGE - 1e-6 && ordonnee <= HAUTEUR - MARGE + 1e-6);
        }
    }

    @Test
    public void leDessinEstCentre() {
        Noeud premierNoeud = noeud(1, 45.70, 4.80);
        Noeud secondNoeud = noeud(2, 45.80, 4.82);
        MapProjection projection = new MapProjection(List.of(premierNoeud, secondNoeud), LARGEUR, HAUTEUR, MARGE);
        double gauche = projection.calculerAbscisseEcran(4.80);
        double droite = projection.calculerAbscisseEcran(4.82);
        assertEquals(LARGEUR - droite, gauche, 1e-6);
    }

    @Test
    public void unSeulPointNeFaitPasPlanter() {
        Noeud premierNoeud = noeud(1, 45.75, 4.85);
        MapProjection projection = new MapProjection(List.of(premierNoeud), LARGEUR, HAUTEUR, MARGE);
        assertTrue(Double.isFinite(projection.calculerAbscisseEcran(4.85)));
        assertTrue(Double.isFinite(projection.calculerOrdonneeEcran(45.75)));
    }
}
