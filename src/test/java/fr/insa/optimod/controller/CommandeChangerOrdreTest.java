package fr.insa.optimod.controller;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * Tests de {@link CommandeChangerOrdre}.
 */
public class CommandeChangerOrdreTest {

    /** Exécuter fait le rangement, annuler fait la remise en place. */
    @Test
    public void executerPuisAnnulerRemetLOrdrePrecedent() {
        List<String> appels = new ArrayList<>();
        Commande commande = new CommandeChangerOrdre(
                () -> appels.add("ranger"), () -> appels.add("remettre"));
        commande.executer();
        assertEquals(List.of("ranger"), appels);
        commande.annuler();
        assertEquals(List.of("ranger", "remettre"), appels);
    }

    /** Une commande annulée peut être rétablie (exécutée de nouveau). */
    @Test
    public void rejouerApresAnnulationRangeDeNouveau() {
        List<String> appels = new ArrayList<>();
        Commande commande = new CommandeChangerOrdre(
                () -> appels.add("ranger"), () -> appels.add("remettre"));
        commande.executer();
        commande.annuler();
        commande.executer();
        assertEquals(List.of("ranger", "remettre", "ranger"), appels);
    }
}
