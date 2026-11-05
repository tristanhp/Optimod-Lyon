package fr.insa.optimod.controller;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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

    /**
     * Consigne après un calcul complet dont la limite de temps a coupé la
     * recherche (un seul livreur) : la tournée affichée est la meilleure
     * trouvée, pas une tournée prouvée la plus courte.
     */
    static final String CONSIGNE_NON_PROUVEE = "Solution non prouvée optimale "
            + "(limite de 10 s atteinte).";

    /**
     * Même consigne avec plusieurs livreurs : {@code %s} liste les livreurs
     * dont la tournée n'est pas prouvée optimale (la limite est partagée
     * entre les livreurs ayant des demandes).
     */
    static final String CONSIGNE_NON_PROUVEE_LIVREURS = "Livreur(s) %s : "
            + "solution non prouvée optimale (limite de temps atteinte).";

    /**
     * Avertissement ajouté à la consigne quand le seul livreur rentre après
     * 13 h : {@code %s} est son heure de retour. Un retour après 13 h n'est
     * jamais refusé, il est signalé ici et par le rouge des arrêts.
     */
    static final String CONSIGNE_RETOUR_APRES_LIMITE = " Retour à l'entrepôt à "
            + "%s, après 13 h.";

    /**
     * Même avertissement avec plusieurs livreurs : {@code %s} nomme chaque
     * livreur qui rentre après 13 h avec son heure de retour.
     */
    static final String CONSIGNE_RETOURS_APRES_LIMITE = " Retour après 13 h : "
            + "%s.";

    /**
     * Détail montré à l'administrateur quand un ajout ferait rentrer un
     * livreur après 13 h : le livreur, son heure de retour, l'heure limite.
     */
    static final String DETAIL_RETOUR_APRES_LIMITE = "Le livreur %d rentrerait "
            + "à %s, après %s.";

    /** Consigne quand aucun point de collecte ou de livraison est choisi. */
    static final String CONSIGNE_AUCUN_POINT = "Sélectionnez d'abord, dans la "
            + "liste de la tournée, le point de collecte ou de livraison à "
            + "déplacer.";

    /**
     * Début de consigne quand la nouvelle adresse est celle de l'autre point
     * de la demande.
     */
    static final String CONSIGNE_MEME_ADRESSE = "La collecte et la livraison "
            + "d'une demande doivent être à des adresses différentes. ";

    /** Début de consigne quand la nouvelle adresse est celle du point. */
    static final String CONSIGNE_DEJA_A_CETTE_ADRESSE = "Ce point est déjà à "
            + "cette adresse. ";

    /** Refus de sauvegarder des tournées de plusieurs livreurs. */
    static final String CONSIGNE_SAUVEGARDE_UN_LIVREUR = "La sauvegarde ne "
            + "gère qu'un livreur pour l'instant : revenez à 1 livreur pour "
            + "sauvegarder.";


    /** Consigne affichée quand les arrêts d'une demande sont introuvables. */
    static final String CONSIGNE_ORDRE_NON_GARDE = "Tournées retirées : les "
            + "arrêts de la demande n'ont pas pu être identifiés. Cliquez sur "
            + "Calculer pour recalculer les tournées.";

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

    /** Fichier du programme courant, ou {@code null} (restauration). */
    private Path fichierProgramme;

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
        ecran.definirActionNombreLivreurs(
                nombre -> etat.changerNombreLivreurs(this, nombre));
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
     * Fait choisir un fichier de plan et le lit hors du thread de
     * l'interface, avec l'attente affichée et les actions bloquées
     * ({@link EtatLectureFichier}). Un plan valide remplace le plan, rend le
     * programme précédent caduc et vide l'historique
     * ({@link EtatPlanCharge}). Un plan invalide ouvre la fenêtre d'erreur et
     * ne change rien : plan, programme, tournées et historique sont gardés.
     * Une annulation ne fait rien.
     */
    void lirePlan() {
        File fichier = ecran.choisirFichierPlan().orElse(null);
        if (fichier == null) {
            return;
        }
        changerEtat(new EtatLectureFichier());
        ecran.afficherAttente(true);
        enArrierePlan(() -> {
            try {
                Plan nouveauPlan = new Plan(fichier.getPath());
                if (nouveauPlan.obtenirNoeuds().isEmpty()) {
                    throw new SAXException("Le plan ne contient aucun nœud : "
                            + "il n'y a rien à afficher.");
                }
                return () -> finirLecturePlan(nouveauPlan, fichier.toPath());
            } catch (IOException | SAXException erreur) {
                return () -> refuserFichier(erreur.getMessage());
            }
        });
    }

    /**
     * Fin de la lecture d'un plan valide, sur le thread de l'interface : le
     * nouveau plan remplace l'ancien, le programme et l'historique sont
     * oubliés.
     */
    private void finirLecturePlan(Plan nouveauPlan, Path fichier) {
        ecran.afficherAttente(false);
        plan = nouveauPlan;
        fichierPlan = fichier;
        programme = null;
        fichierProgramme = null;
        oublierModifications();
        ecran.afficherPlan(plan);
        changerEtat(new EtatPlanCharge());
        ecran.afficherConfirmation("Plan chargé.");
    }

    /**
     * Fait choisir un fichier de demandes et le lit hors du thread de
     * l'interface ({@link EtatLectureFichier}), sans modifier le plan. Un
     * fichier invalide ou une adresse absente du plan ouvre la fenêtre
     * d'erreur ; le programme précédent est conservé.
     */
    void lireProgramme() {
        File fichier = ecran.choisirFichierDemandes().orElse(null);
        if (fichier == null) {
            return;
        }
        Plan planLu = plan;
        changerEtat(new EtatLectureFichier());
        ecran.afficherAttente(true);
        enArrierePlan(() -> {
            try {
                ProgrammeLivraisons lu = new ProgrammeLivraisons(
                        fichier.getPath(), planLu);
                return () -> finirLectureProgramme(lu, fichier.toPath());
            } catch (IOException | SAXException erreur) {
                return () -> refuserFichier(erreur.getMessage());
            }
        });
    }

    /**
     * Fin de la lecture d'un programme valide, sur le thread de l'interface :
     * il remplace le programme précédent et l'historique est oublié.
     */
    private void finirLectureProgramme(ProgrammeLivraisons lu, Path fichier) {
        ecran.afficherAttente(false);
        programme = lu;
        fichierProgramme = fichier;
        oublierModifications();
        ecran.afficherProgramme(programme);
        ecran.signalerArretsHorsPlage(List.of());
        changerEtat(new EtatProgrammeCharge());
        int nombre = programme.obtenirDemandes().size();
        ecran.afficherConfirmation(nombre == 1 ? "1 demande chargée."
                : nombre + " demandes chargées.");
    }

    /**
     * Fichier refusé (plan, demandes ou sauvegarde), sur le thread de
     * l'interface : rien n'a changé, on revient à l'état d'avant la lecture
     * et la fenêtre d'erreur explique pourquoi.
     *
     * @param detail explication en français
     */
    private void refuserFichier(String detail) {
        ecran.afficherAttente(false);
        retrouverEtatStable();
        ecran.afficherErreurXml(detail);
    }

    /**
     * Revient à l'état qui correspond aux données chargées :
     * {@link EtatInitial} sans plan, {@link EtatPlanCharge} sans programme,
     * sinon l'état du programme ({@link #retrouverEtatProgramme}). Sert après
     * un refus ou une erreur pendant un travail en arrière-plan.
     */
    void retrouverEtatStable() {
        if (plan == null) {
            changerEtat(new EtatInitial());
        } else if (programme == null) {
            changerEtat(new EtatPlanCharge());
        } else {
            retrouverEtatProgramme();
        }
    }

    /**
     * Exécute un travail hors du thread de l'interface, puis la suite qu'il
     * renvoie sur le thread de l'interface. Quoi qu'il arrive, l'interface
     * ne reste pas bloquée : une exception ou une erreur imprévue, pendant
     * le travail ou pendant la suite, éteint l'attente, ramène à un état
     * stable et est signalée ({@link #signalerErreurInattendue}). Si l'état a
     * changé entre-temps (page vidée après une erreur de dessin), la suite
     * est abandonnée : elle porterait sur des données oubliées.
     *
     * @param travail calcul à faire en arrière-plan ; renvoie la suite à
     *                exécuter sur le thread de l'interface
     */
    private void enArrierePlan(Supplier<Runnable> travail) {
        Etat etatAuLancement = etat;
        arrierePlan.execute(() -> {
            Runnable suite;
            try {
                suite = travail.get();
            } catch (RuntimeException | Error erreur) {
                // Toute erreur, même une Error (pile ou mémoire épuisée sur
                // un fichier piégé), doit rendre la main à l'interface.
                suite = () -> signalerErreurInattendue(erreur);
            }
            Runnable suiteRetenue = suite;
            interfaceUtilisateur.execute(() -> {
                if (etat != etatAuLancement) {
                    return;
                }
                try {
                    suiteRetenue.run();
                } catch (RuntimeException | StackOverflowError erreur) {
                    signalerErreurInattendue(erreur);
                }
            });
        });
    }

    /**
     * Erreur imprévue (défaut de l'application) : elle est journalisée,
     * l'attente est retirée, l'état revient à celui des données chargées et
     * l'utilisateur est prévenu que son travail est conservé.
     *
     * @param erreur erreur survenue
     */
    private void signalerErreurInattendue(Throwable erreur) {
        JOURNAL.log(Level.SEVERE, "Erreur inattendue", erreur);
        ecran.afficherAttente(false);
        retrouverEtatStable();
        ecran.afficherConfirmation("");
        ecran.afficherErreurInattendue(erreur.getMessage() != null
                ? erreur.getMessage() : erreur.getClass().getSimpleName());
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
        enArrierePlan(() -> {
            try {
                Map<Integer, Tournee> tournees = calculTournees.apply(
                        planCalcule, programmeCalcule);
                return () -> etat.finirCalcul(this, tournees, null);
            } catch (RuntimeException e) {
                String detail = e.getMessage() != null ? e.getMessage()
                        : e.getClass().getSimpleName();
                return () -> etat.finirCalcul(this, null, detail);
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
     * Fait confirmer un nouveau calcul complet par l'administrateur.
     *
     * @return vrai si le calcul peut être relancé
     */
    boolean confirmerRecalcul() {
        return ecran.confirmerRecalcul();
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
     * <p>Si l'insertion fait rentrer le livreur après 13 h, ce n'est pas un
     * refus : l'administrateur est prévenu et se voit proposer les livreurs qui
     * peuvent prendre la demande avant 13 h
     * ({@link Plan#trouverLivreursPossibles}), y compris celui de la demande,
     * puisqu'une autre position peut suffire. Il peut choisir un de ces
     * livreurs, puis la position dans sa tournée comme à l'ajout, garder le
     * livreur malgré le retard, ou annuler : l'ajout est alors abandonné et
     * rien n'est modifié. Seuls une adresse inaccessible, une position invalide
     * ou une tournée qui dépasse minuit refusent l'ajout.
     *
     * @param demande demande validée
     */
    void ajouterDemande(Demande demande) {
        Map<Integer, Tournee> avant = programme.obtenirTournees();
        if (!avant.containsKey(demande.livreur())) {
            executer(new CommandeAjouterDemande(programme, demande));
            return;
        }
        placerDemande(demande, avant);
    }

    /**
     * Fait choisir la position de la demande dans la tournée de son livreur,
     * puis l'insère hors du thread de l'interface. Une insertion qui fait
     * rentrer le livreur après 13 h n'est pas refusée : elle mène à
     * l'avertissement et à la proposition d'autres livreurs
     * ({@link #proposerLivreurs}), qui peut rappeler cette méthode.
     *
     * @param demande demande à insérer chez son livreur
     * @param avant tournées d'avant l'ajout, par livreur
     */
    private void placerDemande(Demande demande, Map<Integer, Tournee> avant) {
        Tournee tourneeLivreur = avant.get(demande.livreur());
        Integer position = ecran.choisirPositionInsertion(tourneeLivreur)
                .orElse(null);
        if (position == null) {
            annulerAjout();
            return;
        }
        Plan planCalcule = plan;
        changerEtat(new EtatCalculEnCours());
        ecran.afficherAttente(true);
        enArrierePlan(() -> {
            try {
                Tournee apres = planCalcule.insererDemande(tourneeLivreur,
                        demande, positionEffective(planCalcule,
                                tourneeLivreur, demande, position));
                if (apres.estPossible()) {
                    return () -> finirAjout(demande, avant, apres);
                }
                List<Integer> possibles =
                        planCalcule.trouverLivreursPossibles(avant, demande);
                return () -> proposerLivreurs(demande, avant, apres,
                        possibles);
            } catch (RuntimeException erreur) {
                return () -> refuserAjout(erreur);
            }
        });
    }

    /**
     * Sur le thread de l'interface, quand l'ajout ferait rentrer le livreur
     * après 13 h : l'administrateur est prévenu de l'heure de retour et
     * choisit parmi les livreurs qui rentrent avant 13 h (la liste peut être
     * vide). Un livreur choisi, il choisit la position chez lui
     * ({@link #placerDemande}). S'il garde le livreur de la demande, elle est
     * ajoutée à la position déjà choisie, sans autre calcul. S'il annule,
     * l'ajout est abandonné : rien n'est modifié, pas même l'historique.
     *
     * @param demande demande à ajouter, chez son livreur actuel
     * @param avant tournées d'avant l'ajout, par livreur
     * @param apres tournée du livreur de la demande avec l'ajout, après 13 h
     * @param possibles livreurs qui peuvent prendre la demande avant 13 h
     */
    private void proposerLivreurs(Demande demande, Map<Integer, Tournee> avant,
            Tournee apres, List<Integer> possibles) {
        ecran.afficherAttente(false);
        String detail = DETAIL_RETOUR_APRES_LIMITE.formatted(
                demande.livreur(), apres.obtenirHeureRetour(),
                Plan.HEURE_LIMITE_RETOUR);
        Integer choix = ecran.choisirLivreurPossible(demande.livreur(),
                possibles, detail).orElse(null);
        if (choix != null && choix == MapScreen.GARDER_LIVREUR) {
            finirAjout(demande, avant, apres);
        } else if (choix != null && possibles.contains(choix)) {
            placerDemande(demande.avecLivreur(choix), avant);
        } else {
            annulerAjout();
        }
    }

    /**
     * Fin de l'ajout, sur le thread de l'interface : la commande range la
     * demande et les tournées recalculées. Les tournées des autres livreurs
     * sont reprises telles quelles.
     *
     * @param demande demande ajoutée, chez son livreur définitif
     * @param avant tournées d'avant l'ajout, par livreur
     * @param apres tournée du livreur de la demande, après l'insertion
     */
    private void finirAjout(Demande demande, Map<Integer, Tournee> avant,
            Tournee apres) {
        ecran.afficherAttente(false);
        Map<Integer, Tournee> tournees = new TreeMap<>(avant);
        tournees.put(demande.livreur(), apres);
        Commande commande = new CommandeAjouterDemande(programme, demande);
        commande.memoriserTourneesAvant(avant);
        commande.memoriserTourneesApres(Collections.unmodifiableMap(tournees));
        executer(commande);
        afficherConsigneOrdreNonOptimise(demande.livreur());
    }

    /**
     * Fin d'un ajout refusé, sur le thread de l'interface : la raison attendue
     * est expliquée, une erreur inattendue est signalée et journalisée
     * ({@link #signalerRefus}). Rien n'est modifié.
     *
     * @param erreur cause du refus
     */
    private void refuserAjout(RuntimeException erreur) {
        ecran.afficherAttente(false);
        retrouverEtatProgramme();
        signalerRefus(erreur, ecran::afficherRefusAjout,
                "de l'ajout d'une demande");
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
        enArrierePlan(() -> {
            try {
                Tournee sans = planCalcule.calculerTourneeSansDemande(
                        avant.get(ancienne.livreur()), ancienne);
                Tournee avec = planCalcule.insererDemande(cible, nouvelle,
                        positionEffective(planCalcule, cible, nouvelle,
                                choix));
                Map<Integer, Tournee> apres = new TreeMap<>(avant);
                apres.put(ancienne.livreur(), sans);
                apres.put(livreur, avec);
                return () -> finirChangementLivreur(position, ancienne,
                        nouvelle, avant, Collections.unmodifiableMap(apres),
                        null);
            } catch (RuntimeException e) {
                return () -> finirChangementLivreur(position, ancienne,
                        nouvelle, avant, null, e);
            }
        });
    }

    /**
     * Fin du changement de livreur, sur le thread de l'interface. S'il est
     * refusé (adresse inaccessible, ou tournée du nouveau livreur qui dépasse
     * minuit), l'écran affiche la raison et rien n'est modifié, pas même
     * l'historique. Sinon la commande remplace la demande par la même confiée
     * au nouveau livreur et range les tournées recalculées.
     *
     * <p>
     * Un retour après 13 h n'est pas un refus : la consigne le signale
     * ({@link #obtenirAvertissementRetour}).
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

    /**
     * Règle le nombre de livreurs. Un réglage refusé (moins d'un livreur, ou
     * livreur retiré qui a encore des demandes) n'exécute pas la commande :
     * demandes, tournées et historique restent intacts, le champ reprend
     * l'ancienne valeur et la consigne explique le refus (la confirmation
     * précédente est effacée). Un réglage accepté est confirmé par la
     * commande. Demander le nombre actuel ne fait rien.
     *
     * @param nombre nombre de livreurs demandé
     */
    void changerNombreLivreurs(int nombre) {
        if (nombre == programme.obtenirNombreLivreurs()) {
            return;
        }
        try {
            programme.verifierNombreLivreurs(nombre);
        } catch (IllegalArgumentException refus) {
            ecran.afficherNombreLivreurs(programme.obtenirNombreLivreurs());
            ecran.afficherConfirmation("");
            ecran.afficherConsigne(refus.getMessage());
            return;
        }
        executer(new CommandeChangerNombreLivreurs(programme, nombre));
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
     * Retient, pour l'état actuel de l'historique, la consigne « solution non
     * prouvée optimale » si la limite de temps a coupé le calcul d'au moins un
     * livreur. L'état qui suit l'affiche en entrant ; Annuler et Rétablir la
     * retrouvent tant que les tournées affichées sont celles du calcul. À
     * appeler après {@link #rangerTournees} et avant le changement d'état.
     *
     * @param tournees tournées qui viennent d'être calculées
     */
    void retenirConsigneNonProuvee(Map<Integer, Tournee> tournees) {
        List<Integer> livreurs = tournees.entrySet().stream()
                .filter(entree -> !entree.getValue().prouveeOptimale())
                .map(Map.Entry::getKey).sorted().toList();
        if (livreurs.isEmpty()) {
            return;
        }
        historique.definirConsigneOrdre(tournees.size() > 1
                ? CONSIGNE_NON_PROUVEE_LIVREURS.formatted(livreurs.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(", ")))
                : CONSIGNE_NON_PROUVEE);
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

    /**
     * Sauvegarde les tournées calculées dans le fichier choisi. Refusée avec
     * plusieurs livreurs (le format n'en gère qu'un) ou si le fichier choisi
     * est celui du plan ou du programme chargé, qu'elle détruirait. Une
     * écriture impossible (dossier protégé, introuvable) est signalée comme
     * telle.
     */
    void sauvegarderTournee() {
        if (programme.obtenirNombreLivreurs() != 1) {
            ecran.afficherConfirmation("");
            ecran.afficherConsigne(CONSIGNE_SAUVEGARDE_UN_LIVREUR);
            return;
        }
        ecran.choisirFichierSauvegarde().ifPresent(fichier -> {
            Path destination = fichier.toPath();
            if (estLeMemeFichier(destination, fichierPlan)
                    || estLeMemeFichier(destination, fichierProgramme)) {
                ecran.afficherConfirmation("");
                ecran.afficherRefusSauvegarde("Le fichier "
                        + fichier.getName() + " est celui du plan ou des "
                        + "demandes chargés : la sauvegarde l'écraserait. "
                        + "Choisissez un autre nom de fichier.");
                return;
            }
            try {
                SauvegardeTournee.sauvegarder(destination, fichierPlan,
                        programme);
                ecran.afficherConfirmation("Tournée sauvegardée.");
            } catch (IOException erreur) {
                ecran.afficherConfirmation("");
                ecran.afficherErreurEcriture(decrireErreurEcriture(erreur,
                        destination));
            }
        });
    }

    /**
     * Vrai si les deux chemins désignent le même fichier, y compris par un
     * autre chemin (lien, « .. »). Faux si l'un est {@code null}.
     */
    private static boolean estLeMemeFichier(Path premier, Path second) {
        if (premier == null || second == null) {
            return false;
        }
        if (premier.toAbsolutePath().normalize().equals(
                second.toAbsolutePath().normalize())) {
            return true;
        }
        try {
            return Files.exists(premier) && Files.exists(second)
                    && Files.isSameFile(premier, second);
        } catch (IOException illisible) {
            return false;
        }
    }

    /**
     * Explique en français pourquoi un fichier n'a pas pu être écrit.
     *
     * @param erreur  erreur d'écriture
     * @param fichier fichier ou dossier visé
     * @return message pour l'utilisateur
     */
    static String decrireErreurEcriture(IOException erreur, Path fichier) {
        if (erreur instanceof AccessDeniedException) {
            return "Accès refusé : l'application n'a pas le droit d'écrire "
                    + "dans " + fichier + ". Choisissez un autre dossier.";
        }
        if (erreur instanceof NoSuchFileException) {
            return "Dossier introuvable : " + fichier + ". Il a peut-être "
                    + "été déplacé ou supprimé.";
        }
        if (erreur instanceof FileSystemException) {
            return "Impossible d'écrire " + fichier + " (fichier protégé, "
                    + "ouvert ailleurs ou disque plein).";
        }
        return "Impossible d'écrire " + fichier + " : "
                + erreur.getMessage();
    }

    /**
     * Fait choisir une sauvegarde, puis la lit et recalcule ses trajets hors
     * du thread de l'interface ({@link EtatRestauration}). Rien n'est
     * remplacé avant que tout soit relu : une sauvegarde invalide, ou toute
     * erreur pendant la lecture, laisse l'état courant intact.
     */
    void restaurerSauvegarde() {
        ecran.choisirFichierRestauration().ifPresent(fichier -> {
            changerEtat(new EtatRestauration());
            ecran.afficherAttente(true);
            enArrierePlan(() -> {
                try {
                    SauvegardeTournee sauvegarde = SauvegardeTournee
                            .restaurer(fichier.toPath());
                    return () -> finirRestauration(sauvegarde);
                } catch (SAXException erreur) {
                    return () -> refuserFichier(erreur.getMessage());
                }
            });
        });
    }

    /** Fin de la restauration, sur le thread de l'interface. */
    private void finirRestauration(SauvegardeTournee sauvegarde) {
        plan = sauvegarde.plan();
        fichierPlan = sauvegarde.fichierPlan();
        programme = sauvegarde.programme();
        fichierProgramme = null;
        oublierModifications();
        ecran.afficherPlan(plan);
        ecran.afficherProgramme(programme);
        rangerTournees(Map.of(1, sauvegarde.tournee()));
        ecran.afficherAttente(false);
        retrouverEtatProgramme();
        ecran.afficherConfirmation("Tournée restaurée.");
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
        enArrierePlan(() -> {
            try {
                Tournee locale = planCalcule.calculerTourneeSansDemande(
                        avant.get(retiree.livreur()), retiree);
                return () -> finirSuppression(position, retiree.livreur(),
                        avant, locale, null);
            } catch (RuntimeException e) {
                return () -> finirSuppression(position, retiree.livreur(),
                        avant, null, e);
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
        ecran.afficherConsigne(consigne + obtenirAvertissementRetour());
    }

    /**
     * @return consigne « ordre non optimisé » des tournées affichées, vide si
     *         elles viennent du voyageur de commerce
     */
    String obtenirConsigneOrdreGarde() {
        return historique.consigneOrdre();
    }

    /**
     * Prévient des livreurs qui rentrent après
     * {@link Plan#HEURE_LIMITE_RETOUR}. L'avertissement est déduit des
     * tournées affichées, sans rien retenir : il suit donc Annuler, Rétablir,
     * un recalcul ou un nouveau réglage du nombre de livreurs. Avec un seul
     * livreur, seule son heure de retour est citée ; avec plusieurs, chaque
     * livreur en retard est nommé, dans l'ordre de leur numéro.
     *
     * @return texte à ajouter à une consigne, commençant par une espace, ou
     *         chaîne vide si tous les livreurs rentrent à l'heure
     */
    String obtenirAvertissementRetour() {
        Map<Integer, Tournee> tournees = programme.obtenirTournees();
        List<String> retards = tournees.entrySet().stream()
                .filter(entree -> !entree.getValue().estPossible())
                .sorted(Map.Entry.comparingByKey())
                .map(entree -> tournees.size() > 1
                        ? "livreur " + entree.getKey() + " à "
                                + entree.getValue().obtenirHeureRetour()
                        : entree.getValue().obtenirHeureRetour().toString())
                .toList();
        if (retards.isEmpty()) {
            return "";
        }
        return (tournees.size() > 1 ? CONSIGNE_RETOURS_APRES_LIMITE
                : CONSIGNE_RETOUR_APRES_LIMITE)
                .formatted(String.join(", ", retards));
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
        enArrierePlan(() -> {
            try {
                Tournee apres = planCalcule.calculerTourneeOrdreChange(
                        avant.get(livreur), rang, decalage);
                return () -> finirChangementOrdre(livreur, avant, apres,
                        rang + decalage, null);
            } catch (RuntimeException e) {
                return () -> finirChangementOrdre(livreur, avant, null,
                        rang + decalage, e);
            }
        });
    }

    /**
     * Fin du changement d'ordre, sur le thread de l'interface. S'il est refusé
     * (contrainte d'ordre, arrêt qui ne peut pas bouger, ou tournée qui
     * dépasse minuit), l'écran affiche la raison et rien n'est modifié, pas
     * même l'historique. Sinon la commande range, à la place des tournées
     * d'avant, les mêmes avec la nouvelle tournée du livreur, et l'arrêt
     * déplacé reste sélectionné.
     *
     * <p>
     * Un retour après 13 h n'est pas un refus : la consigne le signale
     * ({@link #obtenirAvertissementRetour}).
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
        if (echec != null) {
            changerEtat(new EtatTourneeCalculee());
            signalerRefus(echec, ecran::afficherRefusOrdre,
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
     * attendue (par exemple contrainte d'ordre, tournée qui dépasse minuit) est
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
        Tournee.Arret arret = actuelle.arrets().get(rang);
        Demande demande = arret.demande();
        if (nouveau.equals(arret.noeud())) {
            ecran.afficherConsigne(CONSIGNE_DEJA_A_CETTE_ADRESSE
                    + EtatDeplacementPoint.CONSIGNE);
            return;
        }
        if (nouveau.equals(demande.collecte())
                || nouveau.equals(demande.livraison())) {
            ecran.afficherConsigne(CONSIGNE_MEME_ADRESSE
                    + EtatDeplacementPoint.CONSIGNE);
            return;
        }
        Plan planCalcule = plan;
        changerEtat(new EtatCalculEnCours());
        ecran.afficherAttente(true);
        enArrierePlan(() -> {
            try {
                Tournee apres = planCalcule.calculerTourneeAvecPointDeplace(
                        actuelle, rang, nouveau);
                return () -> finirDeplacement(livreur, rang, demande, avant,
                        apres, null);
            } catch (RuntimeException e) {
                return () -> finirDeplacement(livreur, rang, demande, avant,
                        null, e);
            }
        });
    }

    /**
     * Fin du déplacement, sur le thread de l'interface. S'il est refusé
     * (adresse inaccessible, ou tournée qui dépasse minuit), l'écran affiche
     * la raison et rien n'est modifié, pas même l'historique. Sinon la
     * commande remplace la demande par celle dont le point a changé de nœud,
     * range à la place des tournées d'avant les mêmes avec la nouvelle tournée
     * du livreur, et l'arrêt déplacé reste sélectionné. Un retour après 13 h
     * n'est pas un refus : la consigne le signale
     * ({@link #obtenirAvertissementRetour}).
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
        if (echec != null) {
            retrouverEtatProgramme();
            signalerRefus(echec, ecran::afficherRefusDeplacement,
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
        ecran.afficherAttente(false);
        plan = null;
        fichierPlan = null;
        programme = null;
        fichierProgramme = null;
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
            } catch (IOException erreur) {
                ecran.afficherConfirmation("");
                ecran.afficherErreurEcriture(decrireErreurEcriture(erreur,
                        dossier.toPath()));
            } catch (RuntimeException erreur) {
                JOURNAL.log(Level.SEVERE,
                        "Erreur inattendue des feuilles de route", erreur);
                ecran.afficherConfirmation("");
                ecran.afficherErreurInattendue(erreur.getMessage() != null
                        ? erreur.getMessage()
                        : erreur.getClass().getSimpleName());
            }
        });
    }
}
