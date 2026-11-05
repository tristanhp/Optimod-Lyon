package fr.insa.optimod.view;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import fr.insa.optimod.model.ProgrammeLivraisons;
import javafx.scene.paint.Color;

/** Tests de la palette des livreurs de la carte (revue de la PR 57). */
public class CouleursLivreursTest {

    /** Rouge des arrêts après 13 h sur la carte. */
    private static final Color ROUGE_APRES_13H = Color.web("#d93025");

    /** Distance euclidienne entre deux couleurs, composantes dans [0, 1]. */
    private static double distance(Color a, Color b) {
        double dr = a.getRed() - b.getRed();
        double dv = a.getGreen() - b.getGreen();
        double db = a.getBlue() - b.getBlue();
        return Math.sqrt(dr * dr + dv * dv + db * db);
    }

    /** Deux livreurs différents doivent avoir deux couleurs différentes. */
    @Test
    public void chaqueLivreurAUneCouleurDistincte() {
        Set<Color> couleurs = new HashSet<>();
        for (int livreur = 1; livreur
                <= ProgrammeLivraisons.MAXIMUM_LIVREURS; livreur++) {
            couleurs.add(MapCanvas.couleurLivreur(livreur - 1));
        }
        assertTrue("Couleurs distinctes : " + couleurs.size() + " pour "
                + ProgrammeLivraisons.MAXIMUM_LIVREURS + " livreurs",
                couleurs.size() == ProgrammeLivraisons.MAXIMUM_LIVREURS);
    }

    /** Le livreur 7 ne doit pas reprendre la couleur du livreur 1. */
    @Test
    public void livreurSeptDifferentDuLivreurUn() {
        assertNotEquals(MapCanvas.couleurLivreur(0), MapCanvas.couleurLivreur(6));
    }

    /** Aucun livreur ne doit avoir une couleur proche du rouge d'alerte. */
    @Test
    public void aucunLivreurNeRessembleAuRougeDAlerte() {
        for (int livreur = 1; livreur <= 6; livreur++) {
            double ecart = distance(MapCanvas.couleurLivreur(livreur - 1),
                    ROUGE_APRES_13H);
            assertTrue("Livreur " + livreur + " trop proche du rouge "
                    + "d'alerte (distance " + ecart + ")", ecart > 0.15);
        }
    }
}
