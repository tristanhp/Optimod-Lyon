package fr.insa.optimod.controller;

import java.util.List;

import fr.insa.optimod.model.Passage;

/**
 * Calcul de la tournée en cours : toutes les actions sont bloquées jusqu'au
 * résultat, pour que le plan et les demandes ne changent pas pendant le
 * calcul.
 */
final class EtatCalculEnCours implements Etat {

    @Override
    public void importerPlan(MainController controleur) {
    }

    @Override
    public void importerLivraison(MainController controleur) {
    }

    /**
     * Affiche la tournée ({@link EtatTourneeCalculee}) ou l'erreur (retour à
     * {@link EtatLivraisonChargee}).
     */
    @Override
    public void finirCalcul(MainController controleur, List<Passage> tournee,
            String erreur) {
        controleur.arreterAttente();
        if (tournee != null) {
            controleur.changerEtat(new EtatTourneeCalculee());
            controleur.afficherTournee(tournee);
        } else {
            controleur.changerEtat(new EtatLivraisonChargee());
            controleur.afficherErreurCalcul(erreur);
        }
    }

    @Override
    public void activerActions(MapScreen ecran) {
        ecran.activerActions(false, false, false);
    }
}
