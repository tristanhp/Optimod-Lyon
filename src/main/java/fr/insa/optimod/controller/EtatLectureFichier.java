package fr.insa.optimod.controller;

import java.util.EnumSet;
import java.util.Set;

import fr.insa.optimod.controller.MapScreen.Action;

/**
 * Lecture d'un fichier de plan ou de demandes en cours, hors du thread de
 * l'interface : un très gros plan peut prendre plusieurs secondes. Toutes
 * les actions sont bloquées jusqu'au résultat, qui mène à
 * {@link EtatPlanCharge} ou {@link EtatProgrammeCharge} ; un fichier
 * invalide ramène à l'état d'avant la lecture, sans rien perdre
 * ({@link MainController#lirePlan}, {@link MainController#lireProgramme}).
 */
final class EtatLectureFichier implements Etat {
    /** Consigne affichée en entrant dans cet état. */
    static final String CONSIGNE = "Lecture du fichier en cours… patientez.";

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
    public Set<Action> actionsPermises(MainController controleur) {
        return EnumSet.noneOf(Action.class);
    }
}
