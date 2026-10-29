package fr.insa.optimod.controller;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;

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
    private final Map<List<Demande>, Tournee> tourneesCalculees =
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
        ecran.afficherAttente(true);
        arrierePlan.execute(() -> {
            try {
                Tournee tournee = calculTournee.apply(planCalcule,
                        programmeCalcule);
                interfaceUtilisateur.execute(() -> {
                    tourneesCalculees.put(demandesCalculees, tournee);
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
        Tournee tournee = tourneesCalculees.get(
                List.copyOf(programme.obtenirDemandes()));
        if (tournee != null) {
            changerEtat(new EtatTourneeCalculee());
            ecran.afficherTournee(tournee);
        } else {
            changerEtat(new EtatProgrammeCharge());
            ecran.effacerTournee();
        }
    }

    /**
     * Prépare l'affichage de la tournée qui suit la suppression d'une demande,
     * sans relancer le voyageur de commerce. La tournée affichée est celle des
     * demandes actuelles (elle est dans le cache) : on en retire les deux
     * arrêts de la demande ({@link Plan#calculerTourneeSansDemande}), l'ordre
     * des autres arrêts est gardé, et on range le résultat dans le cache sous
     * la liste des demandes restantes. Une fois la suppression exécutée,
     * {@link #retrouverEtatProgramme} la trouvera donc dans le cache.
     *
     * <p>Ne fait rien (la tournée sera simplement retirée de l'écran, comme
     * avant) si la position est invalide, si la tournée des demandes restantes
     * est déjà connue (calculée par le voyageur de commerce : elle est
     * meilleure, on la garde), ou si le recalcul local est impossible (arrêts
     * de la demande ambigus dans la tournée).
     *
     * @param position rang dans le programme de la demande à supprimer
     */
    void memoriserTourneeSansDemande(int position) {
        List<Demande> avant = List.copyOf(programme.obtenirDemandes());
        Tournee actuelle = tourneesCalculees.get(avant);
        if (actuelle == null || position < 0 || position >= avant.size()) {
            return;
        }
        List<Demande> apres = new ArrayList<>(avant);
        apres.remove(position);
        if (tourneesCalculees.containsKey(apres)) {
            return;
        }
        try {
            tourneesCalculees.put(List.copyOf(apres),
                    plan.calculerTourneeSansDemande(actuelle,
                            avant.get(position)));
        } catch (IllegalArgumentException | IllegalStateException e) {
            // Recalcul local impossible : on ne range rien dans le cache, la
            // suppression retirera simplement la tournée affichée.
        }
    }

    /**
     * Change l'ordre d'un arrêt de la tournée affichée (« Monter » ou
     * « Descendre ») par une commande annulable. Le nouvel ordre est d'abord
     * calculé localement ({@link Plan#calculerTourneeOrdreChange}) : s'il est
     * refusé (contrainte d'ordre, retour après 13 h, arrêt qui ne peut pas
     * bouger), l'écran affiche la raison et rien n'est modifié, pas même
     * l'historique. Sinon la commande range la nouvelle tournée dans le cache,
     * à la place de l'ancienne, et {@link #retrouverEtatProgramme} l'affiche.
     *
     * @param rang rang de l'arrêt sélectionné dans la tournée
     * @param decalage {@code -1} pour monter l'arrêt, {@code 1} pour le
     *        descendre
     */
    void changerOrdreTournee(int rang, int decalage) {
        List<Demande> demandes = List.copyOf(programme.obtenirDemandes());
        Tournee avant = tourneesCalculees.get(demandes);
        if (avant == null) {
            return;
        }
        Tournee apres;
        try {
            apres = plan.calculerTourneeOrdreChange(avant, demandes, rang,
                    decalage);
        } catch (IllegalArgumentException | IllegalStateException e) {
            ecran.afficherRefusOrdre(e.getMessage());
            return;
        }
        executer(new CommandeChangerOrdre(
                tournee -> tourneesCalculees.put(demandes, tournee), avant,
                apres));
    }

    /** Oublie l'historique et le cache : le plan ou les demandes changent. */
    private void oublierModifications() {
        historique.vider();
        tourneesCalculees.clear();
    }
}
