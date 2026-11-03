package fr.insa.optimod.controller;

import java.util.Set;

import fr.insa.optimod.model.Tournee;

/**
 * État du contrôleur (design pattern State). {@link MainController} délègue
 * chaque action de l'utilisateur à son état courant, qui décide de ce qui est
 * permis et de l'état suivant.
 *
 * <p>Les méthodes par défaut donnent la règle générale : importer un plan ou
 * des demandes est possible, les autres actions sont ignorées. Chaque état ne
 * redéfinit que ses exceptions.
 */
interface Etat {

    /**
     * Clic sur « Importer le plan » : un plan valide mène à
     * {@link EtatPlanCharge}, un plan invalide vide la page
     * ({@link EtatInitial}), une annulation ne change rien.
     *
     * @param controleur contrôleur à piloter
     */
    default void importerPlan(MainController controleur) {
        switch (controleur.lirePlan()) {
            case REUSSIE -> controleur.changerEtat(new EtatPlanCharge());
            case ECHOUEE -> controleur.changerEtat(new EtatInitial());
            case ANNULEE -> { }
        }
    }

    /**
     * Clic sur « Importer les demandes » : un programme valide mène à
     * {@link EtatProgrammeCharge} ; sinon l'état ne change pas.
     *
     * @param controleur contrôleur à piloter
     */
    default void importerProgramme(MainController controleur) {
        if (controleur.lireProgramme() == MainController.Lecture.REUSSIE) {
            controleur.changerEtat(new EtatProgrammeCharge());
        }
    }

    /**
     * Clic sur « Calculer ». Ignoré par défaut.
     *
     * @param controleur contrôleur à piloter
     */
    default void calculer(MainController controleur) {
    }

    /**
     * Fin du calcul de la tournée, sur le thread de l'interface. Ignorée par
     * défaut.
     *
     * @param controleur contrôleur à piloter
     * @param tournee tournée calculée, ou {@code null} en cas d'erreur
     * @param erreur explication de l'erreur, ou {@code null}
     */
    default void finirCalcul(MainController controleur, Tournee tournee,
            String erreur) {
    }

    /**
     * Clic sur « Ajouter une demande ». Ignoré par défaut.
     *
     * @param controleur contrôleur à piloter
     */
    default void ajouterDemande(MainController controleur) {
    }

    /**
     * Clic sur « Supprimer la demande ». Ignoré par défaut.
     *
     * @param controleur contrôleur à piloter
     * @param position rang de la demande sélectionnée
     */
    default void supprimerDemande(MainController controleur, int position) {
    }

    /**
     * Clic sur « Monter » ou « Descendre » : change l'ordre d'un arrêt de la
     * tournée affichée. Ignoré par défaut.
     *
     * @param controleur contrôleur à piloter
     * @param rang rang de l'arrêt sélectionné dans la tournée
     * @param decalage {@code -1} pour monter l'arrêt, {@code 1} pour le
     *        descendre
     */
    default void changerOrdre(MainController controleur, int rang,
            int decalage) {
    }

    /**
     * Clic sur une intersection de la carte. Ignoré par défaut.
     *
     * @param controleur contrôleur à piloter
     * @param idNoeud identifiant du noeud cliqué
     */
    default void cliquerNoeud(MainController controleur, long idNoeud) {
    }

    /**
     * « Annuler » (Ctrl+Z). Ignoré par défaut.
     *
     * @param controleur contrôleur à piloter
     */
    default void annuler(MainController controleur) {
    }

    /**
     * « Rétablir » (Ctrl+Y). Ignoré par défaut.
     *
     * @param controleur contrôleur à piloter
     */
    default void refaire(MainController controleur) {
    }

    /**
     * Donne les actions permises dans cet état, pour activer les boutons.
     *
     * @param controleur contrôleur (pour savoir s'il reste des commandes à
     *        annuler ou à rétablir)
     * @return actions permises
     */
    Set<MapScreen.Action> actionsPermises(MainController controleur);
}
