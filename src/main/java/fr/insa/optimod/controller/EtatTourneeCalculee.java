package fr.insa.optimod.controller;

/**
 * La tournée est calculée et affichée : on peut la recalculer ou importer un
 * autre plan ou d'autres demandes.
 */
final class EtatTourneeCalculee implements Etat {

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
