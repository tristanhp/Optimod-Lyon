package fr.insa.optimod.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

/**
 * Tests de la conversion des durées saisies dans le dialogue d'ajout d'une
 * demande ({@link MapScreen.SaisieDemande#convertirDuree}).
 */
public class MapScreenTest {

    /** Une durée en minutes est convertie en secondes. */
    @Test
    public void dureeConvertieEnSecondes() {
        assertEquals(300, MapScreen.SaisieDemande.convertirDuree(" 5 "));
    }

    /**
     * Une durée nulle est acceptée : les fichiers de demandes fournis en
     * contiennent (TIG-107).
     */
    @Test
    public void dureeNulleAcceptee() {
        assertEquals(0, MapScreen.SaisieDemande.convertirDuree("0"));
    }

    /** Négatifs, texte et durées trop grandes sont refusés. */
    @Test
    public void dureesInvalidesRefusees() {
        for (String texte : List.of("", "abc", "-1", "1.5", "2147483647")) {
            IllegalArgumentException erreur = assertThrows(texte,
                    IllegalArgumentException.class,
                    () -> MapScreen.SaisieDemande.convertirDuree(texte));
            assertTrue(erreur.getMessage().contains("positive ou nulle"));
        }
    }
}
