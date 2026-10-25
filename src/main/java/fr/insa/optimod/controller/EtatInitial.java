package fr.insa.optimod.controller;

/**
 * Aucun plan chargé : seul l'import d'un plan est possible.
 */
final class EtatInitial implements Etat {

    @Override
    public void importerLivraison(MainController controleur) {
    }

    @Override
    public void activerActions(MapScreen ecran) {
        ecran.activerActions(true, false, false);
    }
}
