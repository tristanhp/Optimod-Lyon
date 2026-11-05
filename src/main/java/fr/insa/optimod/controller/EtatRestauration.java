package fr.insa.optimod.controller;

import java.util.Set;

/** Empêche les modifications pendant la lecture et le recalcul des trajets. */
final class EtatRestauration implements Etat {
    /** Consigne affichée en entrant dans cet état. */
    static final String CONSIGNE =
            "Restauration en cours… patientez.";

    @Override
    public String consigne(MainController controleur) {
        return CONSIGNE;
    }

    @Override
    public void importerPlan(MainController controleur) {
    }

    @Override
    public void importerProgramme(MainController controleur) {
    }

    @Override
    public Set<MapScreen.Action> actionsPermises(MainController controleur) {
        return Set.of();
    }
}
