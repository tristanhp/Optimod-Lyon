package fr.insa.optimod.controller;

import java.util.EnumSet;
import java.util.Set;

import fr.insa.optimod.controller.MapScreen.Action;

/**
 * Un plan et des demandes sont chargés : on peut calculer la tournée, modifier
 * les demandes (ajout, suppression) et annuler ou rétablir ces modifications.
 */
class EtatProgrammeCharge implements Etat {

    @Override
    public void calculer(MainController controleur) {
        controleur.changerEtat(new EtatCalculEnCours());
        controleur.lancerCalcul();
    }

    @Override
    public void ajouterDemande(MainController controleur) {
        controleur.changerEtat(new EtatAjoutCollecte());
        controleur.afficherConsigne(EtatAjoutCollecte.CONSIGNE);
    }

    @Override
    public void supprimerDemande(MainController controleur, int position) {
        if (position >= 0 && position
                < controleur.obtenirProgramme().obtenirDemandes().size()) {
            controleur.executer(new CommandeSupprimerDemande(
                    controleur.obtenirProgramme(), position));
        }
    }

    @Override
    public void annuler(MainController controleur) {
        controleur.annulerCommande();
    }

    @Override
    public void refaire(MainController controleur) {
        controleur.refaireCommande();
    }

    @Override
    public Set<Action> actionsPermises(MainController controleur) {
        Set<Action> actions = EnumSet.of(Action.IMPORTER_PLAN,
                Action.IMPORTER_PROGRAMME, Action.CALCULER,
                Action.AJOUTER_DEMANDE, Action.SUPPRIMER_DEMANDE);
        if (controleur.peutAnnuler()) {
            actions.add(Action.ANNULER);
        }
        if (controleur.peutRefaire()) {
            actions.add(Action.REFAIRE);
        }
        return actions;
    }
}
