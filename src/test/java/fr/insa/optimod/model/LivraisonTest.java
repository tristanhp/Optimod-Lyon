package fr.insa.optimod.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * Tests de {@link Livraison}.
 */
public class LivraisonTest {

    /** La livraison garde ses demandes et son entrepôt, sans dépendre de la liste d'origine. */
    @Test
    public void demandesEtEntrepot() {
        Demande demande = new Demande(1, 2, 180, 240);
        Entrepot entrepot = new Entrepot(3, "8:0:0");
        List<Demande> demandes = new ArrayList<>(List.of(demande));
        Livraison livraison = new Livraison(demandes, entrepot);
        demandes.clear();

        assertEquals(List.of(demande), livraison.getDemandes());
        assertSame(entrepot, livraison.getEntrepot());
        assertEquals(1, demande.getIdCollecte());
        assertEquals(2, demande.getIdLivraison());
        assertEquals(180, demande.getTempsCollecte());
        assertEquals(240, demande.getTempsLivraison());
        assertEquals(3, entrepot.getId());
        assertEquals("8:0:0", entrepot.getHeure());
    }

    /** La liste des demandes n'est pas modifiable. */
    @Test(expected = UnsupportedOperationException.class)
    public void demandesNonModifiables() {
        new Livraison(List.of(), new Entrepot(1, "8:0:0")).getDemandes().clear();
    }
}
