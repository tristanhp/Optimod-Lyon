package fr.insa.optimod.controller;

import java.io.File;
import java.util.Optional;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

import fr.insa.optimod.model.ProgrammeLivraisons;
import fr.insa.optimod.model.Plan;
import fr.insa.optimod.model.Noeud;
import fr.insa.optimod.model.Tournee;

/**
 * Ce que le contrôleur attend de l'écran de la carte. Séparer cette interface
 * de la vue JavaFX permet de tester le contrôleur sans afficher de fenêtre.
 */
public interface MapScreen {

    /**
     * Saisie des durées en minutes et du livreur choisi.
     * @param dureeCollecte minutes de collecte saisies
     * @param dureeLivraison minutes de livraison saisies
     * @param livreur numéro du livreur
     */
    record SaisieDemande(String dureeCollecte, String dureeLivraison,
            int livreur) {

        /**
         * Convertit une durée strictement positive en secondes entières.
         * @param texte minutes saisies
         * @return secondes
         * @throws IllegalArgumentException si la saisie est invalide
         */
        public static int convertirDuree(String texte) {
            try {
                int minutes = Integer.parseInt(texte.trim());
                if (minutes <= 0) {
                    throw new NumberFormatException();
                }
                return Math.multiplyExact(minutes, 60);
            } catch (NumberFormatException | ArithmeticException erreur) {
                throw new IllegalArgumentException("Saisir une durée entière "
                        + "strictement positive en minutes.", erreur);
            }
        }
    }

    /**
     * Renvoie saisie des durées et du livreur, ou vide après annulation.
     * @return saisie des durées et du livreur, ou vide après annulation
     */
    Optional<SaisieDemande> saisirDemande();

    /**
     * Affiche les points choisis avant validation de la demande.
     * @param collecte point de collecte, ou null pour effacer l'aperçu
     * @param livraison point de livraison, ou null avant le second clic
     */
    void afficherPointsProvisoires(Noeud collecte, Noeud livraison);

    /**
     * Fait choisir l'intervalle où insérer collecte puis livraison.
     * @param tournee arrêts existants dans leur ordre de passage
     * @return rang de collecte (à partir de 1), ou vide après annulation
     */
    Optional<Integer> choisirPositionInsertion(Tournee tournee);

    /**
     * Signale les arrêts hors de leur plage sans bloquer l'annulation.
     * @param arrets arrêts concernés, vide pour retirer le signalement
     */
    void signalerArretsHorsPlage(List<Tournee.Arret> arrets);

    /** Actions de l'utilisateur que l'écran peut activer ou désactiver. */
    enum Action {
        /** Bouton « Importer le plan ». */
        IMPORTER_PLAN,
        /** Bouton « Importer les demandes » (programme de livraisons). */
        IMPORTER_PROGRAMME,
        /** Bouton « Calculer ». */
        CALCULER,
        /** Bouton « Ajouter une demande » (ou annuler l'ajout en cours). */
        AJOUTER_DEMANDE,
        /** Bouton « Supprimer la demande » (demande sélectionnée). */
        SUPPRIMER_DEMANDE,
        /** Boutons « Monter » et « Descendre » (arrêt sélectionné). */
        CHANGER_ORDRE,
        /** Bouton « Déplacer » (ou annuler le déplacement en cours). */
        DEPLACER_POINT,
        /** Bouton « Annuler » (Ctrl+Z). */
        ANNULER,
        /** Bouton « Rétablir » (Ctrl+Y). */
        REFAIRE,
        /** Sauvegarder la tournée calculée. */
        SAUVEGARDER,
        /** Restaurer une sauvegarde XML. */
        RESTAURER
    }

    /**
     * Définit l'action d'import des demandes.
     * @param action action au clic
     */
    void definirActionImportProgramme(Runnable action);

    /**
     * Renvoie fichier XML de demandes choisi, ou vide après annulation.
     * @return fichier XML de demandes choisi, ou vide après annulation
     */
    Optional<File> choisirFichierDemandes();

    /**
     * Affiche les demandes chargées sur la carte et dans une liste.
     *
     * @param programme programme de livraisons chargé
     */
    void afficherProgramme(ProgrammeLivraisons programme);


    /**
     * Définit l'action déclenchée par le bouton « Importer ».
     *
     * @param action action à exécuter à chaque clic
     */
    void definirActionImportPlan(Runnable action);

    /**
     * Demande à l'utilisateur de choisir un fichier XML.
     *
     * @return le fichier choisi, ou vide si l'utilisateur annule
     */
    Optional<File> choisirFichierPlan();

    /**
     * Affiche la carte d'un plan.
     *
     * @param plan plan à afficher
     */
    void afficherPlan(Plan plan);

    /**
     * Ouvre la fenêtre d'information « fichier XML invalide ».
     *
     * @param detail explication de l'erreur
     */
    void afficherErreurXml(String detail);

    /**
     * Retire de la page les informations du plan et de la tournée (carte
     * vide).
     */
    void reinitialiser();

    /**
     * Définit l'action déclenchée par le bouton « Calculer ».
     *
     * @param action action à exécuter à chaque clic
     */
    void definirActionCalculer(Runnable action);

    /**
     * Active les boutons des actions permises et désactive les autres. C'est
     * l'état du contrôleur qui décide des actions permises ; la vue se
     * contente de les appliquer.
     *
     * @param actions actions permises
     */
    void activerActions(Set<Action> actions);

    /**
     * Définit l'action du bouton « Ajouter une demande ».
     *
     * @param action action à exécuter à chaque clic
     */
    void definirActionAjouterDemande(Runnable action);

    /**
     * Définit l'action du bouton « Supprimer la demande ».
     *
     * @param action reçoit le rang de la demande sélectionnée
     */
    void definirActionSupprimerDemande(IntConsumer action);

    /**
     * Définit l'action du bouton « Monter » (faire passer plus tôt l'arrêt
     * sélectionné dans la liste des tournées).
     *
     * @param action reçoit le numéro du livreur de l'arrêt sélectionné, puis
     *        son rang dans la tournée de ce livreur (0 est le départ de
     *        l'entrepôt)
     */
    void definirActionMonterArret(BiConsumer<Integer, Integer> action);

    /**
     * Définit l'action du bouton « Descendre » (faire passer plus tard l'arrêt
     * sélectionné dans la liste des tournées).
     *
     * @param action reçoit le numéro du livreur de l'arrêt sélectionné, puis
     *        son rang dans la tournée de ce livreur (0 est le départ de
     *        l'entrepôt)
     */
    void definirActionDescendreArret(BiConsumer<Integer, Integer> action);

    /**
     * Définit l'action du bouton « Déplacer » (déplacer un point de collecte
     * ou de livraison de la tournée).
     *
     * @param action reçoit le numéro du livreur de l'arrêt sélectionné, puis
     *        son rang dans la tournée de ce livreur ; {@code -1} et {@code -1}
     *        si aucun arrêt n'est sélectionné
     */
    void definirActionDeplacerPoint(BiConsumer<Integer, Integer> action);

    /**
     * Définit l'action du bouton « Annuler » et de Ctrl+Z.
     *
     * @param action action à exécuter
     */
    void definirActionAnnuler(Runnable action);

    /**
     * Définit l'action du bouton « Rétablir » et de Ctrl+Y.
     *
     * @param action action à exécuter
     */
    void definirActionRefaire(Runnable action);

    /**
     * Définit l'action d'un clic sur une intersection de la carte.
     *
     * @param action reçoit l'identifiant du noeud cliqué
     */
    void definirActionClicNoeud(LongConsumer action);

    /**
     * Affiche une consigne pour l'utilisateur (ex. « cliquez sur le point de
     * collecte »).
     *
     * @param consigne texte à afficher, vide pour l'effacer
     */
    void afficherConsigne(String consigne);

    /**
     * Affiche ou retire l'état d'attente pendant le calcul.
     *
     * @param waiting {@code true} tant que le calcul est en cours
     */
    void afficherAttente(boolean waiting);

    /**
     * Sélectionne un arrêt dans la liste des tournées. Après un changement
     * d'ordre, la liste est refaite (la vue observe le programme) et la
     * sélection perdue : le contrôleur resélectionne l'arrêt déplacé, à sa
     * nouvelle place.
     *
     * @param livreur numéro du livreur de la tournée
     * @param rang rang de l'arrêt dans sa tournée (0 est le départ de
     *        l'entrepôt)
     */
    void selectionnerArretTournee(int livreur, int rang);

    /**
     * Explique à l'utilisateur pourquoi le changement d'ordre est refusé ; les
     * tournées affichées n'ont pas changé.
     *
     * @param detail explication du refus
     */
    void afficherRefusOrdre(String detail);

    /**
     * Explique à l'utilisateur pourquoi le déplacement du point est refusé
     * (tournée impossible) ; la tournée et les demandes n'ont pas changé.
     *
     * @param detail explication du refus
     */
    void afficherRefusDeplacement(String detail);

    /**
     * Signale que le calcul a échoué.
     *
     * @param detail explication de l'erreur
     */
    void afficherErreurCalcul(String detail);

    /**
     * Définit l'action de sauvegarde de la tournée.
     * @param action action de sauvegarde
     */
    void definirActionSauvegarder(Runnable action);

    /**
     * Définit l'action de restauration de la tournée.
     * @param action action de restauration
     */
    void definirActionRestaurer(Runnable action);

    /**
     * Renvoie destination XML, ou vide après annulation.
     * @return destination XML, ou vide après annulation
     */
    Optional<File> choisirFichierSauvegarde();

    /**
     * Renvoie sauvegarde XML, ou vide après annulation.
     * @return sauvegarde XML, ou vide après annulation
     */
    Optional<File> choisirFichierRestauration();
}
