package fr.insa.optimod.controller;

/**
 * Un plan et des demandes sont chargés : le calcul de la tournée devient
 * possible.
 */
final class EtatLivraisonChargee implements Etat {

    @Override
    public void calculer(MainController controleur) {
        controleur.changerEtat(new EtatCalculEnCours());
        controleur.lancerCalcul();
    }

    @Override
    public void activerActions(MapScreen ecran) {
        ecran.activerActions(true, true, true);
    }
}
