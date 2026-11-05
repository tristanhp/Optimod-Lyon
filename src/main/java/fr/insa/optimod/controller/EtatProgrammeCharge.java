package fr.insa.optimod.controller;

import java.util.EnumSet;
import java.util.Set;

import fr.insa.optimod.controller.MapScreen.Action;

/**
 * Un plan et des demandes sont chargés : on peut calculer la tournée, modifier
 * les demandes (ajout, suppression), régler le nombre de livreurs et annuler ou
 * rétablir ces modifications.
 */
class EtatProgrammeCharge implements Etat {
    /** Consigne affichée en entrant dans cet état. */
    static final String CONSIGNE =
            "Choisissez le nombre de livreurs (1 par défaut) puis cliquez "
            + "sur « Calculer ».";

    @Override
    public String consigne(MainController controleur) {
        return CONSIGNE;
    }


    @Override
    public void restaurer(MainController controleur) {
        controleur.restaurerSauvegarde();
    }

    @Override
    public void calculer(MainController controleur) {
        controleur.changerEtat(new EtatCalculEnCours());
        controleur.lancerCalcul();
    }

    @Override
    public void ajouterDemande(MainController controleur) {
        controleur.changerEtat(new EtatAjoutCollecte());
    }

    @Override
    public void supprimerDemande(MainController controleur, int position) {
        if (position >= 0 && position < controleur.obtenirProgramme().obtenirDemandes().size()) {
            controleur.executer(new CommandeSupprimerDemande(
                    controleur.obtenirProgramme(), position));
        }
    }

    /**
     * Règle le nombre de livreurs : le contrôleur vérifie le réglage, puis
     * exécute la commande ; un réglage refusé ne change rien.
     */
    @Override
    public void changerNombreLivreurs(MainController controleur, int nombre) {
        controleur.changerNombreLivreurs(nombre);
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
                Action.AJOUTER_DEMANDE, Action.SUPPRIMER_DEMANDE,
                Action.CHANGER_NOMBRE_LIVREURS, Action.RESTAURER);
        if (controleur.peutAnnuler()) {
            actions.add(Action.ANNULER);
        }
        if (controleur.peutRefaire()) {
            actions.add(Action.REFAIRE);
        }
        return actions;
    }
}
