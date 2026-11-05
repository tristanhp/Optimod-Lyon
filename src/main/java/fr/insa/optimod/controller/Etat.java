package fr.insa.optimod.controller;

import java.util.Map;
import java.util.Set;

import fr.insa.optimod.model.Tournee;

/**
 * État du contrôleur (design pattern State). {@link MainController} délègue
 * chaque action de l'utilisateur à son état courant, qui décide de ce qui est
 * permis et de l'état suivant.
 *
 * <p>
 * Les méthodes par défaut donnent la règle générale : importer un plan ou
 * des demandes est possible, les autres actions sont ignorées. Chaque état ne
 * redéfinit que ses exceptions.
 */
interface Etat {

    /**
     * Dit à l'utilisateur quoi faire dans cet état. Le contrôleur l'affiche
     * à chaque entrée dans l'état ({@link MainController#changerEtat}), il
     * n'y a donc pas de consigne à poser ailleurs.
     *
     * @param controleur contrôleur (pour les états dont la consigne dépend du
     *        programme chargé)
     * @return consigne en français, jamais vide
     */
    String consigne(MainController controleur);

    /** @param controleur contrôleur ; sauvegarde ignorée par défaut */
    default void sauvegarder(MainController controleur) {
    }

    /** @param controleur contrôleur ; restauration ignorée par défaut */
    default void restaurer(MainController controleur) {
    }

    /**
     * Clic sur « Importer le plan » : le fichier choisi est lu hors du thread
     * de l'interface ({@link EtatLectureFichier}). Un plan valide mène à
     * {@link EtatPlanCharge} ; un plan invalide ou une annulation ne change
     * rien : le plan, le programme et l'historique courants sont gardés.
     *
     * @param controleur contrôleur à piloter
     */
    default void importerPlan(MainController controleur) {
        controleur.lirePlan();
    }

    /**
     * Clic sur « Importer les demandes » : le fichier choisi est lu hors du
     * thread de l'interface ({@link EtatLectureFichier}). Un programme valide
     * mène à {@link EtatProgrammeCharge} ; sinon rien ne change.
     *
     * @param controleur contrôleur à piloter
     */
    default void importerProgramme(MainController controleur) {
        controleur.lireProgramme();
    }

    /**
     * Clic sur « Calculer ». Ignoré par défaut.
     *
     * @param controleur contrôleur à piloter
     */
    default void calculer(MainController controleur) {
    }

    /**
     * Fin du calcul des tournées, sur le thread de l'interface. Ignorée par
     * défaut.
     *
     * @param controleur contrôleur à piloter
     * @param tournees   tournées par numéro de livreur, ou {@code null} en cas
     *                   d'erreur
     * @param erreur     explication de l'erreur, ou {@code null}
     */
    default void finirCalcul(MainController controleur,
            Map<Integer, Tournee> tournees, String erreur) {
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
     * @param position   rang de la demande sélectionnée
     */
    default void supprimerDemande(MainController controleur, int position) {
    }

    /**
     * Clic sur « Monter » ou « Descendre » : change l'ordre d'un arrêt de la
     * tournée d'un livreur. Ignoré par défaut.
     *
     * @param controleur contrôleur à piloter
     * @param livreur    numéro du livreur de l'arrêt sélectionné
     * @param rang       rang de l'arrêt sélectionné dans la tournée de ce livreur
     * @param decalage   {@code -1} pour monter l'arrêt, {@code 1} pour le
     *                   descendre
     */
    default void changerOrdre(MainController controleur, int livreur,
            int rang, int decalage) {
    }

    /**
     * Clic sur « Déplacer » : commence le déplacement d'un point de la tournée
     * affichée, ou l'annule s'il est en cours. Ignoré par défaut.
     *
     * @param controleur contrôleur à piloter
     * @param livreur    numéro du livreur de l'arrêt sélectionné, ou {@code -1}
     *                   si aucun arrêt n'est sélectionné
     * @param rang       rang de l'arrêt sélectionné dans la tournée de ce livreur,
     *                   ou {@code -1} si aucun arrêt n'est sélectionné
     */
    default void deplacerPoint(MainController controleur, int livreur,
            int rang) {
    }

    /**
     * Clic sur « Changer de livreur » : confie une demande à un autre livreur.
     * Ignoré par défaut.
     *
     * @param controleur contrôleur à piloter
     * @param position   rang de la demande sélectionnée dans le programme
     */
    default void changerLivreur(MainController controleur, int position) {
    }

    /**
     * Réglage du champ « Livreurs » : change le nombre de livreurs. Ignoré par
     * défaut.
     *
     * @param controleur contrôleur à piloter
     * @param nombre nombre de livreurs demandé
     */
    default void changerNombreLivreurs(MainController controleur,
            int nombre) {
    }

    /**
     * Clic sur une intersection de la carte. Ignoré par défaut.
     *
     * @param controleur contrôleur à piloter
     * @param idNoeud    identifiant du noeud cliqué
     */
    default void cliquerNoeud(MainController controleur, long idNoeud) {
    }

    /**
     * Exception pendant le dessin de la carte (cas 7) : la page est vidée, un
     * message remplace la carte et le contrôleur revient à
     * {@link EtatInitial}. Valable dans tous les états où une carte est
     * affichée ; {@link EtatInitial}, sans carte, l'ignore.
     *
     * @param controleur contrôleur à piloter
     * @param detail     explication de l'erreur
     */
    default void erreurDessin(MainController controleur, String detail) {
        controleur.abandonnerAffichage(detail);
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
     * Clic sur « Générer les feuilles de route ». Ignoré par défaut.
     *
     * @param controleur contrôleur à piloter
     */
    default void genererFeuillesDeRoute(MainController controleur) {
    }

    /**
     * Donne les actions permises dans cet état, pour activer les boutons.
     *
     * @param controleur contrôleur (pour savoir s'il reste des commandes à
     *                   annuler ou à rétablir)
     * @return actions permises
     */
    Set<MapScreen.Action> actionsPermises(MainController controleur);
}
