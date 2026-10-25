package fr.insa.optimod.controller;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import fr.insa.optimod.model.Demande;
import fr.insa.optimod.model.Entrepot;
import fr.insa.optimod.model.Livraison;

/**
 * Tests de {@link CommandeAjouterDemande} et {@link CommandeSupprimerDemande}.
 */
public class CommandesDemandesTest {

    private final Demande a = new Demande(1, 2, 60, 60);
    private final Demande b = new Demande(3, 4, 60, 60);
    private final Demande c = new Demande(5, 6, 60, 60);
    private final Livraison livraison = new Livraison(List.of(a, b),
            new Entrepot(0, "8:0:0"));

    /** Ajouter met la demande à la fin ; annuler la retire. */
    @Test
    public void ajouterPuisAnnuler() {
        Commande commande = new CommandeAjouterDemande(livraison, c);
        commande.executer();
        assertEquals(List.of(a, b, c), livraison.obtenirDemandes());
        commande.annuler();
        assertEquals(List.of(a, b), livraison.obtenirDemandes());
    }

    /** Supprimer retire la demande ; annuler la remet à son rang. */
    @Test
    public void supprimerPuisAnnulerRemetAuMemeRang() {
        Commande commande = new CommandeSupprimerDemande(livraison, 0);
        commande.executer();
        assertEquals(List.of(b), livraison.obtenirDemandes());
        commande.annuler();
        assertEquals(List.of(a, b), livraison.obtenirDemandes());
    }

    /** Une commande peut être rejouée après annulation (rétablir). */
    @Test
    public void rejouerApresAnnulation() {
        ListeDeCommandes historique = new ListeDeCommandes();
        historique.executer(new CommandeSupprimerDemande(livraison, 1));
        historique.executer(new CommandeAjouterDemande(livraison, c));
        historique.annuler();
        historique.annuler();
        assertEquals(List.of(a, b), livraison.obtenirDemandes());
        historique.refaire();
        historique.refaire();
        assertEquals(List.of(a, c), livraison.obtenirDemandes());
    }
}
