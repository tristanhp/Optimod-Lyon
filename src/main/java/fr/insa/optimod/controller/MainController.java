package fr.insa.optimod.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;

import org.xml.sax.SAXException;

import fr.insa.optimod.model.ProgrammeLivraisons;
import fr.insa.optimod.model.ProgrammeLivraisons.Demande;
import fr.insa.optimod.model.Tournee;
import fr.insa.optimod.model.Plan;
import fr.insa.optimod.model.Noeud;
import fr.insa.optimod.model.SauvegardeTournee;

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

    /** Référence du fichier du plan courant. */
    private Path fichierPlan;

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
        ecran.definirActionAnnuler(() -> etat.annuler(this));
        ecran.definirActionRefaire(() -> etat.refaire(this));
        ecran.definirActionSauvegarder(() -> etat.sauvegarder(this));
        ecran.definirActionRestaurer(() -> etat.restaurer(this));
        changerEtat(new EtatInitial());
    }

    /**
     * Renvoie plan chargé, ou {@code null} si aucun plan n'est chargé.
     * @return plan chargé, ou {@code null} si aucun plan n'est chargé
     */
    public Plan obtenirPlan() {
        return plan;
    }

    /**
     * Renvoie programme de livraisons chargé, ou {@code null}.
     * @return programme de livraisons chargé, ou {@code null}
     */
    public ProgrammeLivraisons obtenirProgramme() {
        return programme;
    }

    /**
     * Renvoie état courant (pour les tests).
     * @return état courant (pour les tests)
     */
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
            fichierPlan = fichier.toPath();
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

    /**
     * Affiche la tournée et signale les dépassements de plage.
     * @param tournee tournée calculée à afficher
     */
    void afficherTournee(Tournee tournee) {
        ecran.afficherTournee(tournee);
        ecran.signalerArretsHorsPlage(tournee.arrets().stream()
                .filter(Tournee.Arret::estHorsPlage).toList());
    }

    /**
     * Renvoie saisie ou absence si l'utilisateur annule.
     * @return saisie ou absence si l'utilisateur annule
     */
    Optional<MapScreen.SaisieDemande> saisirDemande() {
        return ecran.saisirDemande();
    }

    /**
     * Prépare l'insertion hors du thread graphique avant toute modification.
     * @param demande demande validée
     */
    void ajouterDemande(Demande demande) {
        Tournee avant = tourneesCalculees.get(
                List.copyOf(programme.obtenirDemandes()));
        Commande commande = new CommandeAjouterDemande(programme, demande);
        if (avant == null) {
            executer(commande);
            return;
        }
        Integer position = ecran.choisirPositionInsertion(avant).orElse(null);
        if (position == null) {
            retrouverEtatProgramme();
            return;
        }
        if (position < 1 || position >= Math.max(2, avant.arrets().size())) {
            throw new IllegalArgumentException(
                    "Position d'insertion invalide.");
        }
        changerEtat(new EtatCalculEnCours());
        ecran.afficherAttente(true);
        arrierePlan.execute(() -> {
            try {
                Tournee apres = plan.insererDemande(avant, demande, position);
                interfaceUtilisateur.execute(() -> {
                    List<Demande> demandes = new java.util.ArrayList<>(
                            programme.obtenirDemandes());
                    demandes.add(demande);
                    tourneesCalculees.put(List.copyOf(demandes), apres);
                    ecran.afficherAttente(false);
                    executer(commande);
                });
            } catch (IllegalStateException erreur) {
                interfaceUtilisateur.execute(() -> {
                    ecran.afficherAttente(false);
                    retrouverEtatProgramme();
                    ecran.afficherErreurCalcul(erreur.getMessage());
                });
            }
        });
    }

    /**
     * Affiche l'explication d'un calcul impossible.
     * @param detail explication de l'échec du calcul
     */
    void afficherErreurCalcul(String detail) {
        ecran.afficherErreurCalcul(detail);
    }

    /**
     * Affiche une consigne de saisie.
     * @param consigne consigne à afficher, vide pour l'effacer
     */
    void afficherConsigne(String consigne) {
        ecran.afficherConsigne(consigne);
    }

    /**
     * Affiche l'aperçu de la demande sans modifier le programme.
     * @param collecte point de collecte, ou null pour effacer l'aperçu
     * @param livraison point de livraison, ou null avant le second clic
     */
    void afficherPointsProvisoires(Noeud collecte, Noeud livraison) {
        ecran.afficherPointsProvisoires(collecte, livraison);
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

    /**
     * Renvoie vrai s'il reste une modification à annuler.
     * @return vrai s'il reste une modification à annuler
     */
    boolean peutAnnuler() {
        return historique.peutAnnuler();
    }

    /**
     * Renvoie vrai s'il reste une modification annulée à rétablir.
     * @return vrai s'il reste une modification annulée à rétablir
     */
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
        ecran.afficherPointsProvisoires(null, null);
        ecran.afficherConsigne("");
        Tournee tournee = tourneesCalculees.get(
                List.copyOf(programme.obtenirDemandes()));
        if (tournee != null) {
            changerEtat(new EtatTourneeCalculee());
            afficherTournee(tournee);
        } else {
            changerEtat(new EtatProgrammeCharge());
            ecran.effacerTournee();
            ecran.signalerArretsHorsPlage(List.of());
        }
    }

    /** Sauvegarde la tournée courante dans le fichier choisi. */
    void sauvegarderTournee() {
        ecran.choisirFichierSauvegarde().ifPresent(fichier -> {
            try {
                SauvegardeTournee.sauvegarder(fichier.toPath(), fichierPlan,
                        programme, tourneesCalculees.get(
                                List.copyOf(programme.obtenirDemandes())));
            } catch (IOException erreur) {
                ecran.afficherErreurXml(erreur.getMessage());
            }
        });
    }

    /** Restaure intégralement avant de remplacer l'état courant. */
    void restaurerSauvegarde() {
        ecran.choisirFichierRestauration().ifPresent(fichier -> {
            Etat ancienEtat = etat;
            changerEtat(new EtatRestauration());
            ecran.afficherAttente(true);
            arrierePlan.execute(() -> {
                try {
                    SauvegardeTournee sauvegarde =
                            SauvegardeTournee.restaurer(fichier.toPath());
                    interfaceUtilisateur.execute(() -> {
                        plan = sauvegarde.plan();
                        fichierPlan = sauvegarde.fichierPlan();
                        programme = sauvegarde.programme();
                        oublierModifications();
                        tourneesCalculees.put(List.copyOf(
                                programme.obtenirDemandes()),
                                sauvegarde.tournee());
                        ecran.afficherPlan(plan);
                        ecran.afficherProgramme(programme);
                        ecran.afficherConsigne("");
                        afficherTournee(sauvegarde.tournee());
                        ecran.afficherAttente(false);
                        changerEtat(new EtatTourneeCalculee());
                    });
                } catch (SAXException erreur) {
                    interfaceUtilisateur.execute(() -> {
                        ecran.afficherAttente(false);
                        changerEtat(ancienEtat);
                        ecran.afficherErreurXml(erreur.getMessage());
                    });
                }
            });
        });
    }

    /** Oublie l'historique et le cache : le plan ou les demandes changent. */
    private void oublierModifications() {
        historique.vider();
        tourneesCalculees.clear();
    }
}
