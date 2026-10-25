package fr.insa.optimod.controller;

import static org.junit.Assert.assertEquals;

import java.time.LocalTime;
import java.util.List;

import org.junit.Test;

import fr.insa.optimod.model.ProgrammeLivraisons.Demande;
import fr.insa.optimod.model.Noeud;
import fr.insa.optimod.model.ProgrammeLivraisons;

/**
 * Tests de {@link CommandeAjouterDemande} et {@link CommandeSupprimerDemande}.
 */
public class CommandesDemandesTest {

    private static Noeud noeud(long identifiant) {
        return new Noeud(identifiant, 0, 0);
    }

    private final Demande a = new Demande(noeud(1), noeud(2), 60, 60);
    private final Demande b = new Demande(noeud(3), noeud(4), 60, 60);
    private final Demande c = new Demande(noeud(5), noeud(6), 60, 60);
    private final ProgrammeLivraisons programme = new ProgrammeLivraisons(
            noeud(0), LocalTime.of(8, 0), List.of(a, b));

    /** Ajouter met la demande à la fin ; annuler la retire. */
    @Test
    public void ajouterPuisAnnuler() {
        Commande commande = new CommandeAjouterDemande(programme, c);
        commande.executer();
        assertEquals(List.of(a, b, c), programme.obtenirDemandes());
        commande.annuler();
        assertEquals(List.of(a, b), programme.obtenirDemandes());
    }

    /** Supprimer retire la demande ; annuler la remet à son rang. */
    @Test
    public void supprimerPuisAnnulerRemetAuMemeRang() {
        Commande commande = new CommandeSupprimerDemande(programme, 0);
        commande.executer();
        assertEquals(List.of(b), programme.obtenirDemandes());
        commande.annuler();
        assertEquals(List.of(a, b), programme.obtenirDemandes());
    }

    /** Une commande peut être rejouée après annulation (rétablir). */
    @Test
    public void rejouerApresAnnulation() {
        ListeDeCommandes historique = new ListeDeCommandes();
        historique.executer(new CommandeSupprimerDemande(programme, 1));
        historique.executer(new CommandeAjouterDemande(programme, c));
        historique.annuler();
        historique.annuler();
        assertEquals(List.of(a, b), programme.obtenirDemandes());
        historique.refaire();
        historique.refaire();
        assertEquals(List.of(a, c), programme.obtenirDemandes());
    }
}
