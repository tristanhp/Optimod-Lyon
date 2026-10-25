package fr.insa.optimod.controller;

import java.util.List;

import fr.insa.optimod.model.Passage;

/**
 * État du contrôleur (design pattern State). {@link MainController} délègue
 * chaque action de l'utilisateur à son état courant, qui décide de ce qui est
 * permis et de l'état suivant.
 *
 * <p>Les méthodes par défaut donnent la règle générale : importer un plan ou
 * des demandes est possible, calculer ne l'est pas. Chaque état ne redéfinit
 * que ses exceptions.
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
     * Clic sur « Importer la livraison » : des demandes valides mènent à
     * {@link EtatLivraisonChargee} ; sinon l'état ne change pas.
     *
     * @param controleur contrôleur à piloter
     */
    default void importerLivraison(MainController controleur) {
        if (controleur.lireLivraison() == MainController.Lecture.REUSSIE) {
            controleur.changerEtat(new EtatLivraisonChargee());
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
    default void finirCalcul(MainController controleur, List<Passage> tournee,
            String erreur) {
    }

    /**
     * Active les boutons permis dans cet état.
     *
     * @param ecran écran dont on règle les boutons
     */
    void activerActions(MapScreen ecran);
}
