package fr.insa.optimod.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import fr.insa.optimod.model.Tournee;

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

            @Override
            public String confirmation() {
                return nom + " fait.";
            }

            @Override
            public String description() {
                return nom;
            }

            @Override
            public void memoriserTourneesAvant(
                    Map<Integer, Tournee> tournees) {
                journal.add("avant:" + nom);
            }

            @Override
            public void memoriserTourneesApres(
                    Map<Integer, Tournee> tournees) {
                journal.add("apres:" + nom);
            }
        };
    }

    /** Annuler et rétablir rendent la commande concernée, pour la nommer. */
    @Test
    public void annulerEtRefaireRendentLaCommande() {
        assertNull(historique.annuler());
        assertNull(historique.refaire());
        historique.executer(commande("a"));
        assertEquals("a", historique.annuler().description());
        assertEquals("a", historique.refaire().description());
    }

    /**
     * La consigne « ordre non optimisé » suit l'état de l'historique : annuler
     * et rétablir la retrouvent, une nouvelle commande oublie celles des
     * états rétablissables, et un calcul (voyageur de commerce) l'efface.
     */
    @Test
    public void consigneOrdreSuitLesEtats() {
        assertEquals("", historique.consigneOrdre());
        historique.executer(commande("a"));
        historique.definirConsigneOrdre("x");
        historique.executer(commande("b"));
        historique.definirConsigneOrdre("y");

        historique.annuler();
        assertEquals("x", historique.consigneOrdre());
        historique.annuler();
        assertEquals("", historique.consigneOrdre());
        historique.refaire();
        assertEquals("x", historique.consigneOrdre());
        historique.refaire();
        assertEquals("y", historique.consigneOrdre());

        historique.annuler();
        historique.executer(commande("c"));
        assertEquals("", historique.consigneOrdre());
        historique.annuler();
        assertEquals("x", historique.consigneOrdre());

        historique.memoriserTournees(Map.of());
        assertEquals("", historique.consigneOrdre());
        historique.refaire();
        historique.vider();
        assertEquals("", historique.consigneOrdre());
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

    /**
     * Des tournées calculées après coup sont retenues par la dernière
     * commande appliquée (pour Rétablir), et le calcul oublie les commandes
     * annulées : Rétablir ne peut plus remettre des tournées calculées avant
     * lui (TIG-107).
     */
    @Test
    public void calculRetenuParLaDerniereCommandeEtOublieLesAnnulees() {
        historique.memoriserTournees(Map.of());
        historique.executer(commande("a"));
        historique.executer(commande("b"));
        historique.annuler();
        journal.clear();
        historique.memoriserTournees(Map.of());
        assertEquals(List.of("apres:a"), journal);
        assertFalse(historique.peutRefaire());
        assertNull(historique.refaire());
        assertTrue(historique.peutAnnuler());
    }
}
