package fr.insa.optimod.controller;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import fr.insa.optimod.controller.MapScreen.Action;
import fr.insa.optimod.model.Tournee;

/**
 * Calcul de la tournée en cours : toutes les actions sont bloquées jusqu'au
 * résultat, pour que le plan et les demandes ne changent pas pendant le
 * calcul. Cet état sert aussi pendant le recalcul local qui suit la
 * suppression d'une demande
 * ({@link MainController#supprimerDemandeEnGardantLOrdre}).
 */
final class EtatCalculEnCours implements Etat {
    /** Consigne affichée en entrant dans cet état. */
    static final String CONSIGNE =
            "Calcul en cours… la tournée s'affiche "
            + "dès qu'elle est prête.";

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

    /**
     * Range les tournées dans le programme ({@link EtatTourneeCalculee}), même
     * si un livreur revient après {@code Plan.HEURE_LIMITE_RETOUR} : ce n'est
     * pas une erreur de calcul. Sinon affiche l'erreur (retour à
     * {@link EtatProgrammeCharge}).
     */
    @Override
    public void finirCalcul(MainController controleur,
            Map<Integer, Tournee> tournees, String erreur) {
        controleur.arreterAttente();
        if (tournees != null) {
            controleur.rangerTournees(tournees);
            controleur.changerEtat(new EtatTourneeCalculee());
            controleur.confirmerCalcul(tournees);
        } else {
            controleur.changerEtat(new EtatProgrammeCharge());
            controleur.afficherErreurCalcul(erreur);
        }
    }

    @Override
    public Set<Action> actionsPermises(MainController controleur) {
        return EnumSet.noneOf(Action.class);
    }
}
