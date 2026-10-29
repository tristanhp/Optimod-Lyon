package fr.insa.optimod.controller;

import static org.junit.Assert.assertSame;

import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import fr.insa.optimod.model.Noeud;
import fr.insa.optimod.model.Tournee;

/**
 * Tests de {@link CommandeChangerOrdre}.
 */
public class CommandeChangerOrdreTest {

    /** Tournée réduite au départ, à l'heure donnée (pour les distinguer). */
    private static Tournee tourneeAuDepart(int heure) {
        return new Tournee(List.of(new Tournee.Arret(
                new Noeud(1, 4.85, 45.75), LocalTime.of(heure, 0),
                LocalTime.of(heure, 0))), List.of());
    }

    /** Exécuter donne la nouvelle tournée, annuler redonne l'ancienne. */
    @Test
    public void executerPuisAnnulerRemetLOrdrePrecedent() {
        Tournee avant = tourneeAuDepart(8);
        Tournee apres = tourneeAuDepart(9);
        AtomicReference<Tournee> rangee = new AtomicReference<>(avant);
        Commande commande = new CommandeChangerOrdre(rangee::set, avant,
                apres);
        commande.executer();
        assertSame(apres, rangee.get());
        commande.annuler();
        assertSame(avant, rangee.get());
    }

    /** Une commande annulée peut être rétablie (exécutée de nouveau). */
    @Test
    public void rejouerApresAnnulationRedonneLaNouvelleTournee() {
        Tournee avant = tourneeAuDepart(8);
        Tournee apres = tourneeAuDepart(9);
        AtomicReference<Tournee> rangee = new AtomicReference<>(avant);
        Commande commande = new CommandeChangerOrdre(rangee::set, avant,
                apres);
        commande.executer();
        commande.annuler();
        commande.executer();
        assertSame(apres, rangee.get());
    }
}
