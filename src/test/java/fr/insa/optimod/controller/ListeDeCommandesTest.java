package fr.insa.optimod.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * Tests de {@link ListeDeCommandes}.
 */
public class ListeDeCommandesTest {

    private final List<String> journal = new ArrayList<>();
    private final ListeDeCommandes historique = new ListeDeCommandes();

    /** Commande qui note ses exécutions et annulations dans le journal. */
    private Commande commande(String nom) {
        return new Commande() {
            @Override
            public void executer() {
                journal.add("+" + nom);
            }

            @Override
            public void annuler() {
                journal.add("-" + nom);
            }
        };
    }

    /** Une liste vide n'a rien à annuler ni à rétablir. */
    @Test
    public void listeVide() {
        assertFalse(historique.peutAnnuler());
        assertFalse(historique.peutRefaire());
        historique.annuler();
        historique.refaire();
        assertTrue(journal.isEmpty());
    }

    /** Annuler défait les commandes de la plus récente à la plus ancienne. */
    @Test
    public void annulerDansLOrdreInverse() {
        historique.executer(commande("a"));
        historique.executer(commande("b"));
        historique.annuler();
        historique.annuler();
        assertEquals(List.of("+a", "+b", "-b", "-a"), journal);
        assertFalse(historique.peutAnnuler());
        assertTrue(historique.peutRefaire());
    }

    /** Rétablir réapplique les commandes annulées, dans l'ordre. */
    @Test
    public void refaireDansLOrdre() {
        historique.executer(commande("a"));
        historique.executer(commande("b"));
        historique.annuler();
        historique.annuler();
        journal.clear();
        historique.refaire();
        historique.refaire();
        assertEquals(List.of("+a", "+b"), journal);
        assertFalse(historique.peutRefaire());
    }

    /** Une nouvelle commande oublie les commandes annulées. */
    @Test
    public void nouvelleCommandeOublieLesAnnulees() {
        historique.executer(commande("a"));
        historique.executer(commande("b"));
        historique.annuler();
        historique.executer(commande("c"));
        assertFalse(historique.peutRefaire());
        historique.annuler();
        historique.annuler();
        assertEquals(List.of("+a", "+b", "-b", "+c", "-c", "-a"), journal);
    }

    /** Vider oublie tout l'historique. */
    @Test
    public void viderOublieTout() {
        historique.executer(commande("a"));
        historique.annuler();
        historique.executer(commande("b"));
        historique.vider();
        assertFalse(historique.peutAnnuler());
        assertFalse(historique.peutRefaire());
    }
}
