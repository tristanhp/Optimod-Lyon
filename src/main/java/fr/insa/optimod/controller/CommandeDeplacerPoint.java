package fr.insa.optimod.controller;

import fr.insa.optimod.model.ProgrammeLivraisons;
import fr.insa.optimod.model.ProgrammeLivraisons.Demande;

/**
 * Déplace un point de collecte ou de livraison : remplace la demande par la
 * même demande dont un point a changé de nœud ; l'annulation remet l'ancienne.
 * La tournée est retrouvée dans le cache du contrôleur par la liste des
 * demandes : annuler ou rétablir ne recalcule rien.
 */
final class CommandeDeplacerPoint implements Commande {

    private final ProgrammeLivraisons programme;
    private final int position;
    private final Demande apres;

    /** Demande avant le déplacement, gardée pour l'annulation. */
    private Demande avant;

    /**
     * @param programme demandes à modifier
     * @param position rang de la demande dont un point est déplacé
     * @param apres la demande avec le point à son nouveau nœud
     */
    CommandeDeplacerPoint(ProgrammeLivraisons programme, int position,
            Demande apres) {
        this.programme = programme;
        this.position = position;
        this.apres = apres;
    }

    @Override
    public void executer() {
        avant = programme.remplacerDemande(position, apres);
    }

    @Override
    public void annuler() {
        programme.remplacerDemande(position, avant);
    }
}
