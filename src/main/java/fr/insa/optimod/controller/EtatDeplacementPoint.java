package fr.insa.optimod.controller;

import java.util.EnumSet;
import java.util.Set;

import fr.insa.optimod.controller.MapScreen.Action;

/**
 * Déplacement d'un point, seconde étape : le point de collecte ou de livraison
 * est choisi, l'utilisateur clique sur sa nouvelle adresse. Le déplacement est
 * alors fait par une commande annulable. Un nouveau clic sur « Déplacer »
 * annule la saisie.
 */
final class EtatDeplacementPoint implements Etat {

    /** Consigne affichée en entrant dans cet état. */
    static final String CONSIGNE = "Cliquez sur la nouvelle adresse du point "
            + "(« Déplacer » pour annuler).";

    /** Livreur de l'arrêt à déplacer. */
    private final int livreur;

    /** Rang, dans la tournée de ce livreur, de l'arrêt à déplacer. */
    private final int rang;

    /**
     * @param livreur numéro du livreur de l'arrêt à déplacer
     * @param rang    rang de l'arrêt à déplacer dans la tournée de ce livreur
     */
    EtatDeplacementPoint(int livreur, int rang) {
        this.livreur = livreur;
        this.rang = rang;
    }

    @Override
    public void importerPlan(MainController controleur) {
    }

    @Override
    public void importerProgramme(MainController controleur) {
    }

    @Override
    public void deplacerPoint(MainController controleur, int livreurSelectionne,
            int rangSelectionne) {
        controleur.retrouverEtatProgramme();
    }

    @Override
    public void cliquerNoeud(MainController controleur, long idNoeud) {
        controleur.deplacerPoint(livreur, rang, idNoeud);
    }

    @Override
    public Set<Action> actionsPermises(MainController controleur) {
        return EnumSet.of(Action.DEPLACER_POINT);
    }
}
