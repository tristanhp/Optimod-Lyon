package fr.insa.optimod.view;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Tests de la mise en évidence d'une demande sur la carte, sans ouvrir de
 * fenêtre : le survol ne remplace pas la demande choisie dans la liste
 * (TIG-107).
 */
public class MapCanvasTest {

    /** La demande survolée passe avant celle choisie dans la liste. */
    @Test
    public void survolMetEnEvidenceLaDemandeSurvolee() {
        assertEquals(4, MapCanvas.demandeAMettreEnEvidence(4, 2));
        assertEquals(4, MapCanvas.demandeAMettreEnEvidence(4, -1));
    }

    /**
     * Hors d'un point ou hors de la carte, la demande choisie dans la liste
     * revient en évidence : le survol ne l'a pas effacée.
     */
    @Test
    public void horsDUnPointLaDemandeChoisieRevient() {
        assertEquals(2, MapCanvas.demandeAMettreEnEvidence(-1, 2));
    }

    /** Sans survol ni demande choisie, rien n'est mis en évidence. */
    @Test
    public void sansSurvolNiChoixRienEnEvidence() {
        assertEquals(-1, MapCanvas.demandeAMettreEnEvidence(-1, -1));
    }
}
