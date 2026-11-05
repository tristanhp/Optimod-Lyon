package fr.insa.optimod.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

import javafx.scene.control.ButtonBar;
import org.junit.Test;

/** Tests de la confirmation du recalcul, sans ouvrir de fenêtre. */
public class MainViewTest {

    /** Seul « Oui » autorise le recalcul. */
    @Test
    public void ouiAccepteLeRecalcul() {
        assertTrue(MainView.recalculAccepte(Optional.of(MainView.OUI)));
    }

    /** « Non » refuse le recalcul. */
    @Test
    public void nonRefuseLeRecalcul() {
        assertFalse(MainView.recalculAccepte(Optional.of(MainView.NON)));
    }

    /** Fermer la fenêtre (croix, Échap) ne vaut pas accord. */
    @Test
    public void fenetreFermeeRefuseLeRecalcul() {
        assertFalse(MainView.recalculAccepte(Optional.empty()));
    }

    /** « Non » est le bouton d'annulation, « Oui » ne l'est pas. */
    @Test
    public void nonEstLeBoutonDAnnulation() {
        assertEquals(ButtonBar.ButtonData.CANCEL_CLOSE,
                MainView.NON.getButtonData());
        assertFalse(MainView.OUI.getButtonData().isCancelButton());
    }
}
