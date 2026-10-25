package fr.insa.optimod.controller;

/**
 * Un plan est chargé, sans demandes : on peut importer un autre plan ou des
 * demandes de livraison.
 */
final class EtatPlanCharge implements Etat {

    @Override
    public void activerActions(MapScreen ecran) {
        ecran.activerActions(true, true, false);
    }
}
