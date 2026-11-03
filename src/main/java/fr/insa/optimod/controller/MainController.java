package fr.insa.optimod.controller;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.xml.sax.SAXException;

import fr.insa.optimod.model.ProgrammeLivraisons;
import fr.insa.optimod.model.ProgrammeLivraisons.Demande;
import fr.insa.optimod.model.Tournee;
import fr.insa.optimod.model.Plan;

/**
 * Contrôleur de la page principale : importe un plan puis un programme de
 * livraisons (fichiers XML) dans le modèle, demande à la vue de les afficher,
 * puis lance le calcul de la tournée quand l'utilisateur clique sur
 * « Calculer ».
 *
 * <p>Les clics sont délégués à l'état courant ({@link Etat}, design pattern
 * State) : c'est lui qui décide si l'action est permise et quel est l'état
 * suivant. Le contrôleur fournit aux états les opérations élémentaires (lire
 * un fichier, lancer le calcul, mettre à jour l'écran).
 *
 * <p>Les modifications des demandes passent par des commandes annulables
 * ({@link Commande}, design pattern Command). Les tournées déjà calculées sont
 * gardées en cache par liste de demandes : annuler ou rétablir une
 * modification réaffiche la tournée correspondante sans la recalculer.
 */
public final class MainController {

    /** Journal des échecs du recalcul local, pour les retrouver après coup. */
    private static final Logger JOURNAL =
            Logger.getLogger(MainController.class.getName());

    /**
     * Consigne affichée quand la tournée ne vient pas du voyageur de commerce :
     * recalcul local après une suppression, ou ordre changé à la main.
     */
    static final String CONSIGNE_ORDRE_GARDE = "Ordre non optimisé : cliquez "
            + "sur Calculer pour chercher un meilleur ordre.";

    /** Consigne affichée quand les arrêts d'une demande sont introuvables. */
    static final String CONSIGNE_ORDRE_NON_GARDE = "Tournée retirée : les "
            + "arrêts de la demande n'ont pas pu être identifiés. Cliquez sur "
            + "Calculer pour recalculer la tournée.";

    /**
     * Tournée gardée en cache, avec la façon dont elle a été obtenue : seule
     * une tournée du voyageur de commerce est le meilleur ordre connu ; une
     * tournée issue d'un recalcul local ou d'un changement d'ordre à la main
     * garde l'ordre d'avant et peut s'en éloigner.
     *
     * @param tournee tournée calculée
     * @param calculComplet vrai si elle vient du voyageur de commerce, faux si
     *        elle vient d'un recalcul local ou d'un changement d'ordre
     */
    private record TourneeEnCache(Tournee tournee, boolean calculComplet) {
    }

    /** Issue de la lecture d'un fichier choisi par l'utilisateur. */
    enum Lecture {
        /** L'utilisateur a annulé le choix du fichier. */
        ANNULEE,
        /** Le fichier a été lu et le modèle mis à jour. */
        REUSSIE,
        /** Le fichier est invalide ; l'erreur a été affichée. */
        ECHOUEE
    }

    /** Écran piloté. */
    private final MapScreen ecran;

    /** Calcul de la tournée ({@link Plan#calculerTournee} hors tests). */
    private final BiFunction<Plan, ProgrammeLivraisons, Tournee> calculTournee;

    /** Exécute le calcul hors du thread de l'interface. */
    private final Executor arrierePlan;

    /** Exécute les mises à jour de la vue sur le thread de l'interface. */
    private final Executor interfaceUtilisateur;

    /** État courant. */
    private Etat etat;

    /** Plan actuellement chargé, ou {@code null}. */
    private Plan plan;

    /** Programme de livraisons actuellement chargé, ou {@code null}. */
    private ProgrammeLivraisons programme;

    /** Modifications des demandes, pour annuler et rétablir. */
    private final ListeDeCommandes historique = new ListeDeCommandes();

    /**
     * Tournées déjà calculées pour le plan et les demandes chargés, indexées
     * par la liste des demandes (dans l'ordre).
     */
    private final Map<List<Demande>, TourneeEnCache> tourneesCalculees =
            new HashMap<>();

    /**
     * Crée un contrôleur qui calcule la tournée sur le thread appelant.
     *
     * @param ecran écran à piloter
     */
    public MainController(MapScreen ecran) {
        this(ecran, Plan::calculerTournee, Runnable::run, Runnable::run);
    }

    /**
     * Crée le contrôleur, le branche aux boutons de l'écran et le place dans
     * l'état initial (seul l'import d'un plan est possible).
     *
     * @param ecran écran à piloter
     * @param calculTournee calcul de la tournée d'un programme sur un plan
     *        ({@code Plan::calculerTournee} ; remplaçable dans les tests)
     * @param arrierePlan exécute le calcul hors du thread de l'interface
     * @param interfaceUtilisateur exécute les mises à jour de la vue sur le
     *        thread de l'interface
     */
    public MainController(MapScreen ecran,
            BiFunction<Plan, ProgrammeLivraisons, Tournee> calculTournee,
            Executor arrierePlan, Executor interfaceUtilisateur) {
        this.ecran = ecran;
        this.calculTournee = calculTournee;
        this.arrierePlan = arrierePlan;
        this.interfaceUtilisateur = interfaceUtilisateur;
        ecran.definirActionImportPlan(() -> etat.importerPlan(this));
        ecran.definirActionImportProgramme(() -> etat.importerProgramme(this));
        ecran.definirActionCalculer(() -> etat.calculer(this));
        ecran.definirActionAjouterDemande(() -> etat.ajouterDemande(this));
        ecran.definirActionSupprimerDemande(
                position -> etat.supprimerDemande(this, position));
        ecran.definirActionClicNoeud(
                idNoeud -> etat.cliquerNoeud(this, idNoeud));
        ecran.definirActionMonterArret(
                rang -> etat.changerOrdre(this, rang, -1));
        ecran.definirActionDescendreArret(
                rang -> etat.changerOrdre(this, rang, 1));
        ecran.definirActionAnnuler(() -> etat.annuler(this));
        ecran.definirActionRefaire(() -> etat.refaire(this));
        changerEtat(new EtatInitial());
    }

    /** @return plan chargé, ou {@code null} si aucun plan n'est chargé */
    public Plan obtenirPlan() {
        return plan;
    }

    /** @return programme de livraisons chargé, ou {@code null} */
    public ProgrammeLivraisons obtenirProgramme() {
        return programme;
    }

    /** @return état courant (pour les tests) */
    Etat obtenirEtat() {
        return etat;
    }

    /**
     * Passe dans un nouvel état et met à jour les boutons de l'écran.
     *
     * @param nouvelEtat état suivant
     */
    void changerEtat(Etat nouvelEtat) {
        etat = nouvelEtat;
        ecran.activerActions(etat.actionsPermises(this));
    }

    /**
     * Fait choisir un fichier de plan et l'affiche. Un nouveau plan rend le
     * programme précédent caduc ; un plan invalide ouvre la fenêtre d'erreur
     * et vide la page.
     *
     * @return l'issue de la lecture
     */
    Lecture lirePlan() {
        File fichier = ecran.choisirFichierPlan().orElse(null);
        if (fichier == null) {
            return Lecture.ANNULEE;
        }
        programme = null;
        oublierModifications();
        try {
            plan = new Plan(fichier.getPath());
            ecran.afficherPlan(plan);
            return Lecture.REUSSIE;
        } catch (IOException | SAXException | IllegalArgumentException e) {
            plan = null;
            ecran.afficherErreurXml(e.getMessage());
            ecran.reinitialiser();
            return Lecture.ECHOUEE;
        }
    }

    /**
     * Fait choisir un fichier de demandes et affiche le programme, sans
     * modifier le plan. Un fichier invalide ou une adresse absente du plan
     * ouvre la fenêtre d'erreur ; le programme précédent est conservé.
     *
     * @return l'issue de la lecture
     */
    Lecture lireProgramme() {
        File fichier = ecran.choisirFichierDemandes().orElse(null);
        if (fichier == null) {
            return Lecture.ANNULEE;
        }
        try {
            programme = new ProgrammeLivraisons(fichier.getPath(), plan);
            oublierModifications();
            ecran.afficherProgramme(programme);
            return Lecture.REUSSIE;
        } catch (IOException | SAXException e) {
            ecran.afficherErreurXml(e.getMessage());
            return Lecture.ECHOUEE;
        }
    }

    /**
     * Affiche l'attente et lance le calcul de la tournée hors du thread de
     * l'interface. Le résultat est transmis à l'état courant
     * ({@link Etat#finirCalcul}) sur le thread de l'interface.
     */
    void lancerCalcul() {
        Plan planCalcule = plan;
        ProgrammeLivraisons programmeCalcule = programme;
        List<Demande> demandesCalculees = List.copyOf(
                programme.obtenirDemandes());
        ecran.afficherConsigne(""); // efface la consigne d'un recalcul local
        ecran.afficherAttente(true);
        arrierePlan.execute(() -> {
            try {
                Tournee tournee = calculTournee.apply(planCalcule,
                        programmeCalcule);
                interfaceUtilisateur.execute(() -> {
                    tourneesCalculees.put(demandesCalculees,
                            new TourneeEnCache(tournee, true));
                    etat.finirCalcul(this, tournee, null);
                });
            } catch (RuntimeException e) {
                String detail = e.getMessage() != null ? e.getMessage()
                        : e.getClass().getSimpleName();
                interfaceUtilisateur.execute(
                        () -> etat.finirCalcul(this, null, detail));
            }
        });
    }

    /** Retire l'indicateur d'attente du calcul. */
    void arreterAttente() {
        ecran.afficherAttente(false);
    }

    /** @param tournee tournée calculée à afficher */
    void afficherTournee(Tournee tournee) {
        ecran.afficherTournee(tournee);
    }

    /** @param detail explication de l'échec du calcul */
    void afficherErreurCalcul(String detail) {
        ecran.afficherErreurCalcul(detail);
    }

    /** @param consigne consigne à afficher, vide pour l'effacer */
    void afficherConsigne(String consigne) {
        ecran.afficherConsigne(consigne);
    }

    /**
     * Exécute une modification des demandes et l'ajoute à l'historique.
     *
     * @param commande modification à exécuter
     */
    void executer(Commande commande) {
        historique.executer(commande);
        retrouverEtatProgramme();
    }

    /** Annule la dernière modification des demandes. */
    void annulerCommande() {
        historique.annuler();
        retrouverEtatProgramme();
    }

    /** Rétablit la dernière modification annulée. */
    void refaireCommande() {
        historique.refaire();
        retrouverEtatProgramme();
    }

    /** @return vrai s'il reste une modification à annuler */
    boolean peutAnnuler() {
        return historique.peutAnnuler();
    }

    /** @return vrai s'il reste une modification annulée à rétablir */
    boolean peutRefaire() {
        return historique.peutRefaire();
    }

    /**
     * Revient à l'état qui correspond au programme actuel : si leur tournée
     * a déjà été calculée, elle est réaffichée depuis le cache
     * ({@link EtatTourneeCalculee}) ; sinon la tournée est retirée
     * ({@link EtatProgrammeCharge}).
     */
    void retrouverEtatProgramme() {
        ecran.afficherConsigne("");
        TourneeEnCache enCache = tourneesCalculees.get(
                List.copyOf(programme.obtenirDemandes()));
        if (enCache != null) {
            changerEtat(new EtatTourneeCalculee());
            ecran.afficherTournee(enCache.tournee());
            if (!enCache.calculComplet()) {
                // L'ordre n'est pas celui du voyageur de commerce : on le dit.
                ecran.afficherConsigne(CONSIGNE_ORDRE_GARDE);
            }
        } else {
            changerEtat(new EtatProgrammeCharge());
            ecran.effacerTournee();
        }
    }

    /**
     * Supprime une demande quand sa tournée est affichée, en gardant l'ordre
     * des autres arrêts et sans relancer le voyageur de commerce. La tournée
     * affichée (elle est dans le cache) est recalculée localement
     * ({@link Plan#calculerTourneeSansDemande}) hors du thread de l'interface,
     * avec l'attente affichée et toutes les actions bloquées comme pendant le
     * calcul complet ({@link EtatCalculEnCours}). Le résultat est ensuite
     * traité sur le thread de l'interface ({@link #finirSuppression}).
     *
     * <p>Si la tournée des demandes restantes est déjà connue par un calcul
     * complet (voyageur de commerce), il n'y a rien à recalculer : la
     * suppression est exécutée et cette tournée est réaffichée depuis le
     * cache.
     *
     * @param position rang dans le programme de la demande à supprimer ; une
     *        position invalide est ignorée
     */
    void supprimerDemandeEnGardantLOrdre(int position) {
        List<Demande> avant = List.copyOf(programme.obtenirDemandes());
        if (position < 0 || position >= avant.size()) {
            return;
        }
        List<Demande> apres = new ArrayList<>(avant);
        Demande retiree = apres.remove(position);
        TourneeEnCache connue = tourneesCalculees.get(apres);
        if (connue != null && connue.calculComplet()) {
            executer(new CommandeSupprimerDemande(programme, position));
            return;
        }
        Plan planCalcule = plan;
        Tournee actuelle = tourneesCalculees.get(avant).tournee();
        changerEtat(new EtatCalculEnCours());
        ecran.afficherAttente(true);
        arrierePlan.execute(() -> {
            try {
                Tournee locale = planCalcule.calculerTourneeSansDemande(
                        actuelle, retiree);
                interfaceUtilisateur.execute(
                        () -> finirSuppression(position, apres, locale, null));
            } catch (RuntimeException e) {
                interfaceUtilisateur.execute(
                        () -> finirSuppression(position, apres, null, e));
            }
        });
    }

    /**
     * Fin du recalcul local, sur le thread de l'interface : range la tournée
     * recalculée dans le cache (sauf en cas d'échec), puis exécute la
     * suppression. La tournée doit être dans le cache <b>avant</b> la commande,
     * car {@link #executer} réaffiche l'état du programme en la lisant ; en cas
     * d'échec, la tournée est simplement retirée de l'écran et l'utilisateur en
     * est informé.
     *
     * @param position rang de la demande à supprimer
     * @param apres demandes restantes, qui servent de clé dans le cache
     * @param locale tournée recalculée, ou {@code null} en cas d'échec
     * @param echec cause de l'échec, ou {@code null}
     */
    private void finirSuppression(int position, List<Demande> apres,
            Tournee locale, RuntimeException echec) {
        ecran.afficherAttente(false);
        if (locale != null) {
            tourneesCalculees.put(apres, new TourneeEnCache(locale, false));
        }
        executer(new CommandeSupprimerDemande(programme, position));
        if (echec != null) {
            signalerEchecRecalculLocal(echec);
        }
    }

    /**
     * Distingue les échecs du recalcul local au lieu de les ignorer : le cas
     * attendu (arrêts de la demande ambigus ou introuvables dans la tournée)
     * est expliqué par une consigne ; une tournée impossible (retour après
     * 13 h, adresse inaccessible) et une erreur inattendue sont signalées à
     * l'utilisateur et journalisées.
     */
    private void signalerEchecRecalculLocal(RuntimeException echec) {
        if (echec instanceof IllegalArgumentException) {
            JOURNAL.log(Level.INFO, "Recalcul local impossible : {0}",
                    echec.getMessage());
            ecran.afficherConsigne(CONSIGNE_ORDRE_NON_GARDE);
        } else if (echec instanceof IllegalStateException) {
            JOURNAL.log(Level.WARNING, "Recalcul local impossible", echec);
            ecran.afficherErreurCalcul(echec.getMessage());
        } else {
            JOURNAL.log(Level.SEVERE, "Erreur inattendue du recalcul local",
                    echec);
            ecran.afficherErreurCalcul(echec.getMessage() != null
                    ? echec.getMessage() : echec.getClass().getSimpleName());
        }
    }

    /**
     * Change l'ordre d'un arrêt de la tournée affichée (« Monter » ou
     * « Descendre ») par une commande annulable. Comme pour la suppression
     * d'une demande, le nouvel ordre est calculé localement
     * ({@link Plan#calculerTourneeOrdreChange}) hors du thread de l'interface,
     * avec l'attente affichée et toutes les actions bloquées
     * ({@link EtatCalculEnCours}). Le résultat est traité sur le thread de
     * l'interface ({@link #finirChangementOrdre}). La tournée affichée est dans
     * le cache.
     *
     * @param rang rang de l'arrêt sélectionné dans la tournée
     * @param decalage {@code -1} pour monter l'arrêt, {@code 1} pour le
     *        descendre
     */
    void changerOrdreTournee(int rang, int decalage) {
        List<Demande> demandes = List.copyOf(programme.obtenirDemandes());
        TourneeEnCache avant = tourneesCalculees.get(demandes);
        Plan planCalcule = plan;
        changerEtat(new EtatCalculEnCours());
        ecran.afficherAttente(true);
        arrierePlan.execute(() -> {
            try {
                Tournee apres = planCalcule.calculerTourneeOrdreChange(
                        avant.tournee(), demandes, rang, decalage);
                interfaceUtilisateur.execute(() -> finirChangementOrdre(
                        demandes, avant, apres, rang + decalage, null));
            } catch (RuntimeException e) {
                interfaceUtilisateur.execute(() -> finirChangementOrdre(
                        demandes, avant, null, rang + decalage, e));
            }
        });
    }

    /**
     * Fin du changement d'ordre, sur le thread de l'interface. S'il est refusé
     * (contrainte d'ordre, retour après 13 h, arrêt qui ne peut pas bouger),
     * l'écran affiche la raison et rien n'est modifié, pas même l'historique.
     * Sinon la commande range la nouvelle tournée dans le cache, à la place de
     * l'ancienne, et l'arrêt déplacé reste sélectionné.
     *
     * @param demandes demandes de la tournée, clé dans le cache
     * @param avant entrée du cache avant le changement
     * @param apres tournée avec le nouvel ordre, ou {@code null} en cas de
     *        refus
     * @param nouveauRang rang de l'arrêt déplacé dans la nouvelle tournée
     * @param echec cause du refus, ou {@code null}
     */
    private void finirChangementOrdre(List<Demande> demandes,
            TourneeEnCache avant, Tournee apres, int nouveauRang,
            RuntimeException echec) {
        ecran.afficherAttente(false);
        if (echec != null) {
            changerEtat(new EtatTourneeCalculee());
            signalerRefusOrdre(echec);
            return;
        }
        TourneeEnCache nouvelle = new TourneeEnCache(apres, false);
        executer(new CommandeChangerOrdre(
                () -> remplacerSiAffichee(demandes, avant, nouvelle),
                () -> remplacerSiAffichee(demandes, nouvelle, avant)));
        // executer réaffiche la tournée, ce qui efface la sélection de la
        // liste : on la remet sur l'arrêt déplacé pour pouvoir le redéplacer.
        ecran.selectionnerArretTournee(nouveauRang);
    }

    /**
     * Remplace l'entrée du cache de ces demandes, mais seulement si c'est
     * encore {@code attendue}. Si un nouveau calcul a rangé une autre tournée
     * entre-temps, annuler ou rétablir le changement d'ordre ne l'écrase pas.
     */
    private void remplacerSiAffichee(List<Demande> demandes,
            TourneeEnCache attendue, TourneeEnCache nouvelle) {
        if (tourneesCalculees.get(demandes) == attendue) {
            tourneesCalculees.put(demandes, nouvelle);
        }
    }

    /**
     * Distingue les refus du changement d'ordre : une raison attendue
     * (contrainte d'ordre, entrepôt, premier ou dernier arrêt, retour après
     * 13 h) est expliquée à l'utilisateur ; une erreur inattendue est signalée
     * et journalisée, au lieu de laisser les actions bloquées.
     */
    private void signalerRefusOrdre(RuntimeException echec) {
        if (echec instanceof IllegalArgumentException
                || echec instanceof IllegalStateException) {
            ecran.afficherRefusOrdre(echec.getMessage());
        } else {
            JOURNAL.log(Level.SEVERE, "Erreur inattendue du changement "
                    + "d'ordre", echec);
            ecran.afficherErreurCalcul(echec.getMessage() != null
                    ? echec.getMessage() : echec.getClass().getSimpleName());
        }
    }

    /**
     * Oublie l'historique, le cache et la consigne : le plan ou les demandes
     * changent.
     */
    private void oublierModifications() {
        historique.vider();
        tourneesCalculees.clear();
        ecran.afficherConsigne("");
    }
}
