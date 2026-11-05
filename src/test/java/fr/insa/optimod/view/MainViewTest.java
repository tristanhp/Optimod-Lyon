package fr.insa.optimod.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

import fr.insa.optimod.controller.MapScreen;
import javafx.scene.control.ButtonBar;
import org.junit.Test;

/**
 * Tests des fenêtres de confirmation et de choix, sans ouvrir de fenêtre :
 * ils vérifient comment la réponse de l'administrateur est traduite.
 */
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

    /** « Garder ce livreur » donne le choix de ne pas changer de livreur. */
    @Test
    public void garderDonneLeChoixGarder() {
        assertEquals(Optional.of(MapScreen.GARDER_LIVREUR),
                MainView.traduireChoixLivreur(
                        Optional.of(MainView.GARDER), 3));
    }

    /** « Garder ce livreur » marche aussi quand la liste est vide. */
    @Test
    public void garderSansSelectionDonneLeChoixGarder() {
        assertEquals(Optional.of(MapScreen.GARDER_LIVREUR),
                MainView.traduireChoixLivreur(
                        Optional.of(MainView.GARDER), null));
    }

    /** « Affecter » donne le livreur sélectionné dans la liste. */
    @Test
    public void affecterDonneLeLivreurSelectionne() {
        assertEquals(Optional.of(3), MainView.traduireChoixLivreur(
                Optional.of(MainView.AFFECTER), 3));
    }

    /** « Affecter » sans livreur sélectionné ne choisit personne. */
    @Test
    public void affecterSansSelectionNeChoisitPersonne() {
        assertEquals(Optional.empty(), MainView.traduireChoixLivreur(
                Optional.of(MainView.AFFECTER), null));
    }

    /** « Annuler », Échap et la croix abandonnent l'ajout. */
    @Test
    public void annulerNeDonneAucunChoix() {
        assertEquals(Optional.empty(), MainView.traduireChoixLivreur(
                Optional.of(MainView.ANNULER), 3));
        assertEquals(Optional.empty(),
                MainView.traduireChoixLivreur(Optional.empty(), 3));
    }

    /** « Annuler » est le bouton d'annulation : Échap et la croix l'imitent. */
    @Test
    public void annulerEstLeBoutonDAnnulationDuChoixDeLivreur() {
        assertEquals(ButtonBar.ButtonData.CANCEL_CLOSE,
                MainView.ANNULER.getButtonData());
        assertFalse(MainView.GARDER.getButtonData().isCancelButton());
        assertFalse(MainView.AFFECTER.getButtonData().isCancelButton());
    }
}
