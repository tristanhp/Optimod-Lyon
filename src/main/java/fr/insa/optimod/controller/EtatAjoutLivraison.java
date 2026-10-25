package fr.insa.optimod.controller;

import java.util.EnumSet;
import java.util.Set;

import fr.insa.optimod.controller.MapScreen.Action;
import fr.insa.optimod.model.Demande;

/**
 * Ajout d'une demande, seconde étape : le point de collecte est choisi,
 * l'utilisateur clique sur le point de livraison. La demande est alors ajoutée
 * par une commande annulable.
 */
final class EtatAjoutLivraison implements Etat {

    /** Consigne affichée en entrant dans cet état. */
    static final String CONSIGNE = "Cliquez sur le point de livraison "
            + "(« Ajouter une demande » pour annuler).";

    /**
     * Durée de collecte et de livraison d'une demande ajoutée à la main, en
     * secondes.
     */
    static final int DUREE_PAR_DEFAUT = 5 * 60;

    /** Noeud de collecte choisi à l'étape précédente. */
    private final long idCollecte;

    /** @param idCollecte noeud de collecte choisi */
    EtatAjoutLivraison(long idCollecte) {
        this.idCollecte = idCollecte;
    }

    @Override
    public void importerPlan(MainController controleur) {
    }

    @Override
    public void importerLivraison(MainController controleur) {
    }

    @Override
    public void ajouterDemande(MainController controleur) {
        controleur.retrouverEtatLivraison();
    }

    @Override
    public void cliquerNoeud(MainController controleur, long idNoeud) {
        if (idNoeud == idCollecte) {
            controleur.afficherConsigne("La livraison doit avoir lieu ailleurs "
                    + "que la collecte. " + CONSIGNE);
            return;
        }
        controleur.executer(new CommandeAjouterDemande(
                controleur.obtenirLivraison(), new Demande(idCollecte, idNoeud,
                        DUREE_PAR_DEFAUT, DUREE_PAR_DEFAUT)));
    }

    @Override
    public Set<Action> actionsPermises(MainController controleur) {
        return EnumSet.of(Action.AJOUTER_DEMANDE);
    }
}
