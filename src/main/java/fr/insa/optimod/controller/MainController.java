package fr.insa.optimod.controller;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.xml.sax.SAXException;

import fr.insa.optimod.model.FeuilleDeRoute;
import fr.insa.optimod.model.Noeud;
import fr.insa.optimod.model.Plan;
import fr.insa.optimod.model.ProgrammeLivraisons;
import fr.insa.optimod.model.ProgrammeLivraisons.Demande;
import fr.insa.optimod.model.SauvegardeTournee;
import fr.insa.optimod.model.Tournee;

/**
 * Contrôleur de la page principale : importe un plan puis un programme de
 * livraisons (fichiers XML) dans le modèle, demande à la vue de les afficher,
 * puis lance le calcul de la tournée quand l'utilisateur clique sur
 * « Calculer ».
 *
 * <p>
 * Les clics sont délégués à l'état courant ({@link Etat}, design pattern
 * State) : c'est lui qui décide si l'action est permise et quel est l'état
 * suivant. Le contrôleur fournit aux états les opérations élémentaires (lire
 * un fichier, lancer le calcul, mettre à jour l'écran).
 *
 * <p>
 * Les tournées calculées sont rangées dans le programme de livraisons, une
 * par livreur ; la vue les observe. Les modifications des demandes passent
 * par des commandes annulables ({@link Commande}, design pattern Command),
 * qui gardent les tournées d'avant et d'après : annuler ou rétablir une
 * modification remet les tournées correspondantes sans les recalculer.
 *
 * <p>
 * Supprimer une demande quand les tournées sont calculées ne relance pas le
 * voyageur de commerce : seule la tournée du livreur de la demande est
 * recalculée localement, en gardant l'ordre de ses autres arrêts
 * ({@link #supprimerDemandeEnGardantLOrdre}).
 */
public final class MainController {

    /** Journal des échecs du recalcul local, pour les retrouver après coup. */
    private static final Logger JOURNAL = Logger.getLogger(MainController.class.getName());

    /**
     * Consigne affichée quand la tournée ne vient pas du voyageur de commerce
     * (recalcul local après une suppression, ou ordre changé à la main) et
     * qu'il n'y a qu'un livreur. Elle ne conseille pas « Calculer », qui
     * remplacerait aussi les ordres choisis à la main.
     */
    static final String CONSIGNE_ORDRE_GARDE = "Ordre non optimisé : il "
            + "n'est peut-être plus le plus court.";

    /**
     * Même consigne avec plusieurs livreurs : {@code %d} est le livreur dont la
     * tournée vient d'être recalculée ou modifiée, les autres tournées ne
     * changent pas.
     */
    static final String CONSIGNE_ORDRE_GARDE_LIVREUR = "Livreur %d : ordre non "
            + "optimisé, il n'est peut-être plus le plus court.";

    /**
     * Consigne après un changement de livreur : les deux tournées touchées
     * (ancien livreur, puis nouveau) ne viennent plus du voyageur de commerce.
     */
    static final String CONSIGNE_ORDRE_GARDE_LIVREURS = "Livreurs %d et %d : "
            + "ordre non optimisé, il n'est peut-être plus le plus court.";

    /** Consigne quand aucun point de collecte ou de livraison est choisi. */
    static final String CONSIGNE_AUCUN_POINT = "Sélectionnez d'abord, dans la "
            + "liste de la tournée, le point de collecte ou de livraison à "
            + "déplacer.";

    /** Début de consigne quand la nouvelle adresse est déjà celle du couple. */
    static final String CONSIGNE_MEME_ADRESSE = "La collecte et la livraison "
            + "d'une demande doivent être à des adresses différentes. ";

    /** Consigne affichée quand les arrêts d'une demande sont introuvables. */
    static final String CONSIGNE_ORDRE_NON_GARDE = "Tournées retirées : les "
            + "arrêts de la demande n'ont pas pu être identifiés. Cliquez sur "
            + "Calculer pour recalculer les tournées.";

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

    /** Calcul des tournées ({@link Plan#calculerTournees} hors tests). */
    private final BiFunction<Plan, ProgrammeLivraisons, Map<Integer, Tournee>> calculTournees;

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
     * Crée un contrôleur qui calcule la tournée sur le thread appelant.
     *
     * @param ecran écran à piloter
     */
    public MainController(MapScreen ecran) {
        this(ecran, Plan::calculerTournees, Runnable::run, Runnable::run);
    }

    /**
     * Crée le contrôleur, le branche aux boutons de l'écran et le place dans
     * l'état initial (seul l'import d'un plan est possible).
     *
     * @param ecran                écran à piloter
     * @param calculTournees       calcul des tournées d'un programme sur un plan
     *                             ({@code Plan::calculerTournees} ; remplaçable
     *                             dans les tests)
     * @param arrierePlan          exécute le calcul hors du thread de l'interface
     * @param interfaceUtilisateur exécute les mises à jour de la vue sur le
     *                             thread de l'interface
     */
    public MainController(MapScreen ecran,
            BiFunction<Plan, ProgrammeLivraisons, Map<Integer, Tournee>> calculTournees,
            Executor arrierePlan, Executor interfaceUtilisateur) {
        this.ecran = ecran;
        this.calculTournees = calculTournees;
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
        ecran.definirActionMonterArret((livreur, rang) -> etat.changerOrdre(this, livreur, rang, -1));
        ecran.definirActionDescendreArret((livreur, rang) -> etat.changerOrdre(this, livreur, rang, 1));
        ecran.definirActionDeplacerPoint((livreur, rang) -> etat.deplacerPoint(this, livreur, rang));
        ecran.definirActionChangerLivreur(
                position -> etat.changerLivreur(this, position));
        ecran.definirActionAnnuler(() -> etat.annuler(this));
        ecran.definirActionRefaire(() -> etat.refaire(this));
        ecran.definirActionSauvegarder(() -> etat.sauvegarder(this));
        ecran.definirActionRestaurer(() -> etat.restaurer(this));
        ecran.definirActionErreurDessin(
                detail -> etat.erreurDessin(this, detail));
        ecran.definirActionGenererFeuillesDeRoute(
                () -> etat.genererFeuillesDeRoute(this));
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
     * Passe dans un nouvel état, met à jour les boutons de l'écran et affiche
     * la consigne de cet état.
     *
     * @param nouvelEtat état suivant
     */
    void changerEtat(Etat nouvelEtat) {
        etat = nouvelEtat;
        ecran.activerActions(etat.actionsPermises(this));
        ecran.afficherConsigne(etat.consigne(this));
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
            Plan nouveauPlan = new Plan(fichier.getPath());
            if (nouveauPlan.obtenirNoeuds().isEmpty()
                    && nouveauPlan.obtenirTroncons().isEmpty()) {
                throw new SAXException("Le plan ne contient ni nœud ni "
                        + "tronçon : il n'y a rien à afficher.");
            }
            plan = nouveauPlan;
            fichierPlan = fichier.toPath();
            ecran.afficherPlan(plan);
            ecran.afficherConfirmation("Plan chargé.");
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
            ecran.signalerArretsHorsPlage(List.of());
            int nombre = programme.obtenirDemandes().size();
            ecran.afficherConfirmation(nombre == 1 ? "1 demande chargée."
                    : nombre + " demandes chargées.");
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
        ecran.afficherConfirmation("");
        ecran.afficherAttente(true);
        arrierePlan.execute(() -> {
            try {
                Map<Integer, Tournee> tournees = calculTournees.apply(
                        planCalcule, programmeCalcule);
                interfaceUtilisateur.execute(
                        () -> etat.finirCalcul(this, tournees, null));
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
     * Range les tournées calculées dans le programme (la vue les observe) et
     * les fait retenir par l'historique, pour annuler et rétablir.
     *
     * @param tournees tournées par numéro de livreur
     */
    void rangerTournees(Map<Integer, Tournee> tournees) {
        programme.definirTournees(tournees);
        historique.memoriserTournees(programme.obtenirTournees());
    }

    /**
     * Renvoie la saisie des durées et du livreur, ou vide après annulation.
     *
     * @return saisie de la demande
     */
    Optional<MapScreen.SaisieDemande> saisirDemande() {
        return ecran.saisirDemande();
    }

    /**
     * Affiche les points choisis sans modifier le programme.
     *
     * @param collecte  point de collecte, ou null pour effacer l'aperçu
     * @param livraison point de livraison, ou null avant le second clic
     */
    void afficherPointsProvisoires(Noeud collecte, Noeud livraison) {
        ecran.afficherPointsProvisoires(collecte, livraison);
    }

    /**
     * Ajoute une demande. Si les tournées sont calculées, l'utilisateur choisit
     * où l'insérer dans la tournée du livreur de la demande : elle est mise à
     * jour localement ({@link Plan#insererDemande}) hors du thread de
     * l'interface, sans relancer le voyageur de commerce, et les tournées des
     * autres livreurs ne changent pas. Sinon la demande est simplement ajoutée.
     * Comme pour la suppression, le calcul se fait avant la commande : annuler
     * et rétablir ne recalculent rien.
     *
     * @param demande demande validée
     */
    void ajouterDemande(Demande demande) {
        Map<Integer, Tournee> avant = programme.obtenirTournees();
        Commande commande = new CommandeAjouterDemande(programme, demande);
        Tournee tourneeLivreur = avant.get(demande.livreur());
        if (tourneeLivreur == null) {
            executer(commande);
            return;
        }
        Integer position = ecran.choisirPositionInsertion(tourneeLivreur)
                .orElse(null);
        if (position == null) {
            annulerAjout();
            return;
        }
        Plan planCalcule = plan;
        changerEtat(new EtatCalculEnCours());
        ecran.afficherAttente(true);
        arrierePlan.execute(() -> {
            try {
                Tournee apres = planCalcule.insererDemande(tourneeLivreur,
                        demande, positionEffective(planCalcule,
                                tourneeLivreur, demande, position));
                interfaceUtilisateur.execute(() -> {
                    ecran.afficherAttente(false);
                    Map<Integer, Tournee> tournees = new TreeMap<>(avant);
                    tournees.put(demande.livreur(), apres);
                    commande.memoriserTourneesAvant(avant);
                    commande.memoriserTourneesApres(
                            Collections.unmodifiableMap(tournees));
                    executer(commande);
                    afficherConsigneOrdreNonOptimise(demande.livreur());
                });
            } catch (RuntimeException erreur) {
                interfaceUtilisateur.execute(() -> {
                    ecran.afficherAttente(false);
                    retrouverEtatProgramme();
                    ecran.afficherErreurCalcul(erreur.getMessage());
                });
            }
        });
    }

    /**
     * Rang de collecte où insérer une demande : celui choisi par
     * l'administrateur, ou, s'il a choisi l'entrée « Moindre coût », celui que
     * calcule {@link Plan#trouverPositionMoindreCout}. À appeler hors du thread
     * de l'interface, car la recherche lance des plus courts chemins.
     */
    private static int positionEffective(Plan plan, Tournee tournee,
            Demande demande, int choix) {
        return choix == MapScreen.POSITION_MOINDRE_COUT
                ? plan.trouverPositionMoindreCout(tournee, demande)
                : choix;
    }

    /**
     * Confie une demande à un autre livreur, par une commande annulable.
     * L'administrateur choisit le livreur, puis la position dans la tournée de
     * ce livreur (ou « Moindre coût »). Sans choix, rien ne change. Ensuite,
     * hors du thread de l'interface et actions bloquées
     * ({@link EtatCalculEnCours}), la demande est retirée de la tournée de
     * l'ancien livreur ({@link Plan#calculerTourneeSansDemande}) puis insérée
     * dans celle du nouveau ({@link Plan#insererDemande}), sans relancer le
     * voyageur de commerce ; les tournées des autres livreurs ne changent pas.
     * Le résultat est traité sur le thread de l'interface
     * ({@link #finirChangementLivreur}).
     *
     * @param position rang dans le programme de la demande ; une position
     *                 invalide est ignorée, de même que tout appel avec un seul
     *                 livreur
     */
    void changerLivreurDemande(int position) {
        List<Demande> demandes = programme.obtenirDemandes();
        int nombreDeLivreurs = programme.obtenirNombreLivreurs();
        if (position < 0 || position >= demandes.size()
                || nombreDeLivreurs < 2) {
            return;
        }
        Demande ancienne = demandes.get(position);
        int livreur = ecran.choisirLivreur(ancienne.livreur(),
                nombreDeLivreurs).orElse(ancienne.livreur());
        if (livreur == ancienne.livreur() || livreur < 1
                || livreur > nombreDeLivreurs) {
            return;
        }
        Map<Integer, Tournee> avant = programme.obtenirTournees();
        Tournee cible = avant.get(livreur);
        Integer choix = ecran.choisirPositionInsertion(cible).orElse(null);
        if (choix == null) {
            return;
        }
        Demande nouvelle = ancienne.avecLivreur(livreur);
        Plan planCalcule = plan;
        changerEtat(new EtatCalculEnCours());
        ecran.afficherAttente(true);
        arrierePlan.execute(() -> {
            try {
                Tournee sans = planCalcule.calculerTourneeSansDemande(
                        avant.get(ancienne.livreur()), ancienne);
                Tournee avec = planCalcule.insererDemande(cible, nouvelle,
                        positionEffective(planCalcule, cible, nouvelle,
                                choix));
                Map<Integer, Tournee> apres = new TreeMap<>(avant);
                apres.put(ancienne.livreur(), sans);
                apres.put(livreur, avec);
                interfaceUtilisateur.execute(() -> finirChangementLivreur(
                        position, ancienne, nouvelle, avant,
                        Collections.unmodifiableMap(apres), null));
            } catch (RuntimeException e) {
                interfaceUtilisateur.execute(() -> finirChangementLivreur(
                        position, ancienne, nouvelle, avant, null, e));
            }
        });
    }

    /**
     * Fin du changement de livreur, sur le thread de l'interface. S'il est
     * refusé (adresse inaccessible, ou retour à l'entrepôt du nouveau livreur
     * rendu impossible après 13 h), l'écran affiche la raison et rien n'est
     * modifié, pas même l'historique. Sinon la commande remplace la demande par
     * la même confiée au nouveau livreur et range les tournées recalculées.
     *
     * <p>
     * Un livreur qui rentrait déjà après 13 h peut recevoir une demande de
     * plus : seul un changement qui rend impossible une tournée possible est
     * refusé ({@link Plan#insererDemande}).
     *
     * @param position rang de la demande dans le programme
     * @param ancienne la demande chez son ancien livreur
     * @param nouvelle la demande chez son nouveau livreur
     * @param avant    tournées d'avant le changement
     * @param apres    tournées d'après, ou {@code null} en cas de refus
     * @param echec    cause du refus, ou {@code null}
     */
    private void finirChangementLivreur(int position, Demande ancienne,
            Demande nouvelle, Map<Integer, Tournee> avant,
            Map<Integer, Tournee> apres, RuntimeException echec) {
        ecran.afficherAttente(false);
        if (echec != null) {
            retrouverEtatProgramme();
            signalerRefus(echec, ecran::afficherRefusChangementLivreur,
                    "du changement de livreur");
            return;
        }
        executer(new CommandeChangerLivreur(programme, position, nouvelle,
                avant, apres));
        garderOrdre(CONSIGNE_ORDRE_GARDE_LIVREURS.formatted(
                ancienne.livreur(), nouvelle.livreur()));
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
        ecran.afficherConfirmation(commande.confirmation());
    }

    /** Annule la dernière modification des demandes. */
    void annulerCommande() {
        Commande annulee = historique.annuler();
        retrouverEtatProgramme();
        if (annulee != null) {
            ecran.afficherConfirmation(
                    "Annulé : " + annulee.description() + ".");
        }
    }

    /** Rétablit la dernière modification annulée. */
    void refaireCommande() {
        Commande retablie = historique.refaire();
        retrouverEtatProgramme();
        if (retablie != null) {
            ecran.afficherConfirmation(
                    "Rétabli : " + retablie.description() + ".");
        }
    }

    /** @return vrai s'il reste une modification à annuler */
    boolean peutAnnuler() {
        return historique.peutAnnuler();
    }

    /** @return vrai s'il reste une modification annulée à rétablir */
    boolean peutRefaire() {
        return historique.peutRefaire();
    }

    /** Abandonne l'ajout d'une demande et le dit à l'utilisateur. */
    void annulerAjout() {
        retrouverEtatProgramme();
        ecran.afficherConfirmation("Ajout annulé.");
    }

    /** Abandonne le déplacement d'un point et le dit à l'utilisateur. */
    void annulerDeplacement() {
        retrouverEtatProgramme();
        ecran.afficherConfirmation("Déplacement annulé.");
    }

    /**
     * Confirme que les tournées sont calculées, en nommant le nombre de
     * livreurs s'il y en a plusieurs.
     *
     * @param tournees tournées calculées, par numéro de livreur
     */
    void confirmerCalcul(Map<Integer, Tournee> tournees) {
        ecran.afficherConfirmation(tournees.size() > 1
                ? "Tournées calculées pour " + tournees.size() + " livreurs."
                : "Tournée calculée.");
    }

    /**
     * Revient à l'état qui correspond au programme actuel :
     * {@link EtatTourneeCalculee} si ses tournées sont calculées, sinon
     * {@link EtatProgrammeCharge}.
     */
    void retrouverEtatProgramme() {
        ecran.afficherPointsProvisoires(null, null);
        if (programme.obtenirTournees().isEmpty()) {
            changerEtat(new EtatProgrammeCharge());
        } else {
            changerEtat(new EtatTourneeCalculee());
        }
        ecran.signalerArretsHorsPlage(programme.obtenirTournees().values()
                .stream().flatMap(tournee -> tournee.arrets().stream())
                .filter(Tournee.Arret::estHorsPlage).toList());
    }

    /** Sauvegarde les tournées calculées dans le fichier choisi. */
    void sauvegarderTournee() {
        ecran.choisirFichierSauvegarde().ifPresent(fichier -> {
            try {
                SauvegardeTournee.sauvegarder(fichier.toPath(), fichierPlan,
                        programme);
                ecran.afficherConfirmation("Tournée sauvegardée.");
            } catch (IOException erreur) {
                ecran.afficherConfirmation("");
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
                    SauvegardeTournee sauvegarde = SauvegardeTournee.restaurer(fichier.toPath());
                    interfaceUtilisateur.execute(() -> {
                        plan = sauvegarde.plan();
                        fichierPlan = sauvegarde.fichierPlan();
                        programme = sauvegarde.programme();
                        oublierModifications();
                        ecran.afficherPlan(plan);
                        ecran.afficherProgramme(programme);
                        rangerTournees(Map.of(1, sauvegarde.tournee()));
                        ecran.afficherAttente(false);
                        retrouverEtatProgramme();
                        ecran.afficherConfirmation("Tournée restaurée.");
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

    /**
     * Supprime une demande quand les tournées sont calculées, en gardant
     * l'ordre des autres arrêts du livreur et sans relancer le voyageur de
     * commerce. La tournée de ce livreur est recalculée localement
     * ({@link Plan#calculerTourneeSansDemande}) hors du thread de l'interface,
     * avec l'attente affichée et toutes les actions bloquées comme pendant le
     * calcul complet ({@link EtatCalculEnCours}) ; les tournées des autres
     * livreurs ne changent pas. Le résultat est ensuite traité sur le thread
     * de l'interface ({@link #finirSuppression}).
     *
     * @param position rang dans le programme de la demande à supprimer ; une
     *                 position invalide est ignorée
     */
    void supprimerDemandeEnGardantLOrdre(int position) {
        List<Demande> demandes = programme.obtenirDemandes();
        if (position < 0 || position >= demandes.size()) {
            return;
        }
        Demande retiree = demandes.get(position);
        Map<Integer, Tournee> avant = programme.obtenirTournees();
        Plan planCalcule = plan;
        changerEtat(new EtatCalculEnCours());
        ecran.afficherAttente(true);
        arrierePlan.execute(() -> {
            try {
                Tournee locale = planCalcule.calculerTourneeSansDemande(
                        avant.get(retiree.livreur()), retiree);
                interfaceUtilisateur.execute(() -> finirSuppression(position,
                        retiree.livreur(), avant, locale, null));
            } catch (RuntimeException e) {
                interfaceUtilisateur.execute(() -> finirSuppression(position,
                        retiree.livreur(), avant, null, e));
            }
        });
    }

    /**
     * Fin du recalcul local, sur le thread de l'interface : exécute la
     * suppression avec, comme tournées d'après, celles d'avant dont la tournée
     * du livreur est remplacée par la tournée recalculée ; annuler remet celles
     * d'avant. En cas d'échec, la suppression est exécutée sans tournées (elles
     * sont retirées de l'écran) et l'utilisateur en est informé.
     *
     * @param position rang de la demande à supprimer
     * @param livreur  numéro du livreur de la demande supprimée
     * @param avant    tournées d'avant la suppression
     * @param locale   tournée recalculée du livreur, ou {@code null} en cas
     *                 d'échec
     * @param echec    cause de l'échec, ou {@code null}
     */
    private void finirSuppression(int position, int livreur,
            Map<Integer, Tournee> avant, Tournee locale,
            RuntimeException echec) {
        ecran.afficherAttente(false);
        Commande suppression = new CommandeSupprimerDemande(programme,
                position);
        if (locale != null) {
            Map<Integer, Tournee> apres = new TreeMap<>(avant);
            apres.put(livreur, locale);
            suppression.memoriserTourneesAvant(avant);
            suppression.memoriserTourneesApres(
                    Collections.unmodifiableMap(apres));
        }
        executer(suppression);
        if (locale != null) {
            afficherConsigneOrdreNonOptimise(livreur);
        } else {
            signalerEchecRecalculLocal(echec);
        }
    }

    /**
     * Prévient que la tournée d'un livreur ne vient plus du voyageur de
     * commerce (recalcul local ou ordre changé à la main). Le livreur n'est
     * nommé que s'il y en a plusieurs.
     *
     * @param livreur numéro du livreur dont la tournée a changé
     */
    private void afficherConsigneOrdreNonOptimise(int livreur) {
        garderOrdre(programme.obtenirNombreLivreurs() > 1
                ? CONSIGNE_ORDRE_GARDE_LIVREUR.formatted(livreur)
                : CONSIGNE_ORDRE_GARDE);
    }

    /**
     * Affiche la consigne « ordre non optimisé » et la retient pour l'état
     * actuel de l'historique : Annuler, Rétablir ou un refus la retrouveront
     * tant que les tournées affichées gardent cet ordre.
     *
     * @param consigne consigne à afficher
     */
    private void garderOrdre(String consigne) {
        historique.definirConsigneOrdre(consigne);
        ecran.afficherConsigne(consigne);
    }

    /**
     * @return consigne « ordre non optimisé » des tournées affichées, vide si
     *         elles viennent du voyageur de commerce
     */
    String obtenirConsigneOrdreGarde() {
        return historique.consigneOrdre();
    }

    /**
     * Distingue les échecs du recalcul local au lieu de les ignorer : le cas
     * attendu (arrêts de la demande introuvables dans la tournée) est expliqué
     * par une consigne ; une adresse inaccessible et une erreur inattendue sont
     * signalées à l'utilisateur et journalisées.
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
                    ? echec.getMessage()
                    : echec.getClass().getSimpleName());
        }
    }

    /**
     * Change l'ordre d'un arrêt de la tournée d'un livreur (« Monter » ou
     * « Descendre ») par une commande annulable. Comme pour la suppression
     * d'une demande, le nouvel ordre est calculé localement
     * ({@link Plan#calculerTourneeOrdreChange}) hors du thread de l'interface,
     * avec l'attente affichée et toutes les actions bloquées
     * ({@link EtatCalculEnCours}). Le résultat est traité sur le thread de
     * l'interface ({@link #finirChangementOrdre}). Les tournées des autres
     * livreurs ne changent pas.
     *
     * @param livreur  numéro du livreur de l'arrêt sélectionné
     * @param rang     rang de l'arrêt sélectionné dans la tournée de ce livreur
     * @param decalage {@code -1} pour monter l'arrêt, {@code 1} pour le
     *                 descendre
     */
    void changerOrdreTournee(int livreur, int rang, int decalage) {
        Map<Integer, Tournee> avant = programme.obtenirTournees();
        Plan planCalcule = plan;
        changerEtat(new EtatCalculEnCours());
        ecran.afficherAttente(true);
        arrierePlan.execute(() -> {
            try {
                Tournee apres = planCalcule.calculerTourneeOrdreChange(
                        avant.get(livreur), rang, decalage);
                interfaceUtilisateur.execute(() -> finirChangementOrdre(
                        livreur, avant, apres, rang + decalage, null));
            } catch (RuntimeException e) {
                interfaceUtilisateur.execute(() -> finirChangementOrdre(
                        livreur, avant, null, rang + decalage, e));
            }
        });
    }

    /**
     * Fin du changement d'ordre, sur le thread de l'interface. S'il est refusé
     * (contrainte d'ordre, arrêt qui ne peut pas bouger, ou retour à
     * l'entrepôt rendu impossible après 13 h), l'écran affiche la raison et
     * rien n'est modifié, pas même l'historique. Sinon la commande range, à la
     * place des tournées d'avant, les mêmes avec la nouvelle tournée du
     * livreur, et l'arrêt déplacé reste sélectionné.
     *
     * <p>
     * Un changement qui ne fait que garder une tournée déjà en retard est
     * accepté : seul un changement qui rend possible une tournée impossible est
     * refusé.
     *
     * @param livreur     numéro du livreur de la tournée
     * @param avant       tournées d'avant le changement
     * @param apres       tournée du livreur avec le nouvel ordre, ou {@code null}
     *                    en
     *                    cas de refus
     * @param nouveauRang rang de l'arrêt déplacé dans la nouvelle tournée
     * @param echec       cause du refus, ou {@code null}
     */
    private void finirChangementOrdre(int livreur,
            Map<Integer, Tournee> avant, Tournee apres, int nouveauRang,
            RuntimeException echec) {
        ecran.afficherAttente(false);
        RuntimeException refus = echec;
        if (refus == null && !apres.estPossible()
                && avant.get(livreur).estPossible()) {
            refus = new IllegalStateException("Changement d'ordre refusé : le "
                    + "retour à l'entrepôt serait à "
                    + apres.obtenirHeureRetour() + ", après "
                    + Plan.HEURE_LIMITE_RETOUR + ".");
        }
        if (refus != null) {
            changerEtat(new EtatTourneeCalculee());
            signalerRefus(refus, ecran::afficherRefusOrdre,
                    "du changement d'ordre");
            return;
        }
        Map<Integer, Tournee> tournees = new TreeMap<>(avant);
        tournees.put(livreur, apres);
        executer(new CommandeChangerOrdre(programme, livreur, avant,
                Collections.unmodifiableMap(tournees)));
        afficherConsigneOrdreNonOptimise(livreur);
        // executer réaffiche les tournées, ce qui efface la sélection de la
        // liste : on la remet sur l'arrêt déplacé pour pouvoir le redéplacer.
        ecran.selectionnerArretTournee(livreur, nouveauRang);
    }

    /**
     * Distingue les refus d'une modification de la tournée : une raison
     * attendue (par exemple contrainte d'ordre, retour après 13 h) est
     * expliquée à l'utilisateur ; une erreur inattendue est signalée et
     * journalisée, au lieu de laisser les actions bloquées.
     *
     * @param echec        cause du refus
     * @param afficher     affiche la raison attendue
     * @param modification ce qui était demandé, pour le journal
     */
    private void signalerRefus(RuntimeException echec,
            Consumer<String> afficher, String modification) {
        ecran.afficherConfirmation("");
        if (echec instanceof IllegalArgumentException
                || echec instanceof IllegalStateException) {
            afficher.accept(echec.getMessage());
        } else {
            JOURNAL.log(Level.SEVERE, "Erreur inattendue lors "
                    + modification, echec);
            ecran.afficherErreurCalcul(echec.getMessage() != null
                    ? echec.getMessage()
                    : echec.getClass().getSimpleName());
        }
    }

    /**
     * Commence le déplacement du point de collecte ou de livraison sélectionné
     * dans la liste des tournées : l'écran attend alors un clic sur sa
     * nouvelle adresse ({@link EtatDeplacementPoint}). Sans arrêt sélectionné,
     * ou si l'arrêt est le départ ou le retour à l'entrepôt, une consigne
     * l'explique.
     *
     * @param livreur numéro du livreur de l'arrêt sélectionné, ou {@code -1}
     *                si aucun arrêt n'est sélectionné
     * @param rang    rang de l'arrêt dans la tournée de ce livreur, ou
     *                {@code -1} si aucun arrêt n'est sélectionné
     */
    void choisirPointADeplacer(int livreur, int rang) {
        Tournee tournee = programme.obtenirTournees().get(livreur);
        if (tournee == null || rang < 1
                || rang >= tournee.arrets().size() - 1) {
            ecran.afficherConsigne(CONSIGNE_AUCUN_POINT);
        } else {
            changerEtat(new EtatDeplacementPoint(livreur, rang));
        }
    }

    /**
     * Déplace un point de la tournée d'un livreur vers le nœud cliqué, par une
     * commande annulable. La tournée du livreur est recalculée localement
     * ({@link Plan#calculerTourneeAvecPointDeplace}) hors du thread de
     * l'interface, actions bloquées ({@link EtatCalculEnCours}) ; l'ordre de
     * passage est gardé et les tournées des autres livreurs ne changent pas.
     * Le résultat est traité sur le thread de l'interface
     * ({@link #finirDeplacement}).
     *
     * @param livreur numéro du livreur de l'arrêt à déplacer
     * @param rang    rang de l'arrêt à déplacer dans la tournée de ce livreur
     * @param idNoeud nœud cliqué, nouvelle adresse du point
     */
    void deplacerPoint(int livreur, int rang, long idNoeud) {
        Noeud nouveau = plan.obtenirNoeud(idNoeud);
        Map<Integer, Tournee> avant = programme.obtenirTournees();
        Tournee actuelle = avant.get(livreur);
        if (nouveau == null || actuelle == null) {
            return; // clic hors du plan : ignoré
        }
        Demande demande = actuelle.arrets().get(rang).demande();
        if (nouveau.equals(demande.collecte())
                || nouveau.equals(demande.livraison())) {
            ecran.afficherConsigne(CONSIGNE_MEME_ADRESSE
                    + EtatDeplacementPoint.CONSIGNE);
            return;
        }
        Plan planCalcule = plan;
        changerEtat(new EtatCalculEnCours());
        ecran.afficherAttente(true);
        arrierePlan.execute(() -> {
            try {
                Tournee apres = planCalcule.calculerTourneeAvecPointDeplace(
                        actuelle, rang, nouveau);
                interfaceUtilisateur.execute(() -> finirDeplacement(livreur,
                        rang, demande, avant, apres, null));
            } catch (RuntimeException e) {
                interfaceUtilisateur.execute(() -> finirDeplacement(livreur,
                        rang, demande, avant, null, e));
            }
        });
    }

    /**
     * Fin du déplacement, sur le thread de l'interface. S'il est refusé
     * (adresse inaccessible, ou retour à l'entrepôt rendu impossible après
     * 13 h), l'écran affiche la raison et rien n'est modifié, pas même
     * l'historique. Sinon la commande remplace la demande par celle dont le
     * point a changé de nœud, range à la place des tournées d'avant les mêmes
     * avec la nouvelle tournée du livreur, et l'arrêt déplacé reste
     * sélectionné.
     *
     * @param livreur  numéro du livreur de la tournée
     * @param rang     rang de l'arrêt déplacé dans la tournée de ce livreur
     * @param ancienne demande dont un point est déplacé
     * @param avant    tournées d'avant le déplacement
     * @param apres    tournée du livreur avec le point déplacé, ou {@code null}
     *                 en cas de refus
     * @param echec    cause du refus, ou {@code null}
     */
    private void finirDeplacement(int livreur, int rang, Demande ancienne,
            Map<Integer, Tournee> avant, Tournee apres,
            RuntimeException echec) {
        ecran.afficherAttente(false);
        RuntimeException refus = echec;
        if (refus == null && !apres.estPossible()
                && avant.get(livreur).estPossible()) {
            refus = new IllegalStateException("Déplacement refusé : le retour "
                    + "à l'entrepôt serait à " + apres.obtenirHeureRetour()
                    + ", après " + Plan.HEURE_LIMITE_RETOUR + ".");
        }
        if (refus != null) {
            retrouverEtatProgramme();
            signalerRefus(refus, ecran::afficherRefusDeplacement,
                    "du déplacement d'un point");
            return;
        }
        Map<Integer, Tournee> tournees = new TreeMap<>(avant);
        tournees.put(livreur, apres);
        executer(new CommandeDeplacerPoint(programme,
                programme.obtenirDemandes().indexOf(ancienne),
                apres.arrets().get(rang).demande(), avant,
                Collections.unmodifiableMap(tournees)));
        afficherConsigneOrdreNonOptimise(livreur);
        // executer réaffiche les tournées, ce qui efface la sélection de la
        // liste : on la remet sur l'arrêt déplacé.
        ecran.selectionnerArretTournee(livreur, rang);
    }

    /**
     * Abandonne l'affichage après une exception pendant le dessin : oublie le
     * plan, le programme et l'historique, vide la page, affiche le message à
     * la place de la carte et revient à {@link EtatInitial} (comme le cas 1).
     *
     * @param detail explication de l'erreur
     */
    void abandonnerAffichage(String detail) {
        plan = null;
        programme = null;
        oublierModifications();
        ecran.reinitialiser();
        ecran.afficherErreurCarte(detail);
        changerEtat(new EtatInitial());
    }

    /** Oublie l'historique : le plan ou les demandes changent. */
    private void oublierModifications() {
        historique.vider();
    }

    /**
     * Écrit une feuille de route HTML par livreur dans le dossier choisi par
     * l'utilisateur ({@link FeuilleDeRoute}). Ne fait rien si le choix du
     * dossier est annulé.
     */
    void genererFeuillesDeRoute() {
        ecran.choisirDossierFeuillesDeRoute().ifPresent(dossier -> {
            try {
                Map<Integer, Tournee> tournees = programme.obtenirTournees();
                for (Map.Entry<Integer, Tournee> tournee
                        : tournees.entrySet()) {
                    Files.writeString(dossier.toPath().resolve(
                            FeuilleDeRoute.nomFichier(tournee.getKey())),
                            FeuilleDeRoute.generer(plan, tournee.getKey(),
                                    tournee.getValue()),
                            StandardCharsets.UTF_8);
                }
                ecran.afficherConfirmation(tournees.size() == 1
                        ? "Feuille de route générée dans " + dossier + "."
                        : tournees.size() + " feuilles de route générées dans "
                                + dossier + ".");
            } catch (IOException | IllegalArgumentException erreur) {
                ecran.afficherErreurXml(erreur.getMessage());
            }
        });
    }
}
