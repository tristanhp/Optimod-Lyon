package fr.insa.optimod.view;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Filet de sécurité de l'application : reçoit toute exception que personne
 * n'a attrapée, sur le thread de l'interface comme sur les autres. Elle est
 * journalisée, puis l'utilisateur est prévenu par une alerte en français ;
 * l'application n'est pas fermée et le travail en cours est conservé.
 *
 * <p>
 * Une seule alerte est ouverte à la fois : une erreur qui se répète (par
 * exemple à chaque image dessinée) n'ouvre pas des dizaines de fenêtres,
 * elle est seulement journalisée tant que l'alerte est ouverte.
 */
final class GestionnaireErreursInattendues
        implements Thread.UncaughtExceptionHandler {

    /** En-tête de l'alerte. */
    static final String MESSAGE = "Une erreur inattendue s'est produite. "
            + "Votre travail est conservé.";

    /** Journal des erreurs inattendues. */
    private static final Logger JOURNAL = Logger.getLogger(
            GestionnaireErreursInattendues.class.getName());

    /** Exécute l'affichage sur le thread de l'interface. */
    private final Executor interfaceUtilisateur;

    /** Affiche l'alerte ; reçoit le détail technique de l'erreur. */
    private final Consumer<String> afficher;

    /** Vrai tant qu'une alerte est ouverte ou sur le point de l'être. */
    private final AtomicBoolean alerteOuverte = new AtomicBoolean();

    /**
     * Crée le gestionnaire.
     *
     * @param interfaceUtilisateur exécute l'affichage sur le thread de
     *                             l'interface ({@code Platform::runLater})
     * @param afficher             ouvre l'alerte, reçoit le détail technique
     */
    GestionnaireErreursInattendues(Executor interfaceUtilisateur,
            Consumer<String> afficher) {
        this.interfaceUtilisateur = interfaceUtilisateur;
        this.afficher = afficher;
    }

    /**
     * Journalise l'erreur et ouvre l'alerte si aucune ne l'est déjà.
     *
     * @param fil    thread où l'erreur s'est produite
     * @param erreur erreur non attrapée
     */
    @Override
    public void uncaughtException(Thread fil, Throwable erreur) {
        JOURNAL.log(Level.SEVERE, "Erreur inattendue (" + fil.getName() + ")",
                erreur);
        if (!alerteOuverte.compareAndSet(false, true)) {
            return;
        }
        String detail = decrire(erreur);
        try {
            interfaceUtilisateur.execute(() -> {
                try {
                    afficher.accept(detail);
                } catch (RuntimeException echecAffichage) {
                    JOURNAL.log(Level.SEVERE, "Alerte impossible",
                            echecAffichage);
                } finally {
                    alerteOuverte.set(false);
                }
            });
        } catch (RuntimeException echecAffichage) {
            // Interface déjà fermée : l'erreur reste dans le journal.
            alerteOuverte.set(false);
            JOURNAL.log(Level.SEVERE, "Alerte impossible", echecAffichage);
        }
    }

    /**
     * Détail technique d'une erreur : son message, ou le nom de son type
     * s'il n'y en a pas.
     *
     * @param erreur erreur à décrire
     * @return le détail, jamais vide
     */
    static String decrire(Throwable erreur) {
        String message = erreur.getMessage();
        return message == null || message.isBlank()
                ? erreur.getClass().getSimpleName()
                : message;
    }
}
