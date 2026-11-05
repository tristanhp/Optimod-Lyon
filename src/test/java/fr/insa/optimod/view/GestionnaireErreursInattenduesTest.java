package fr.insa.optimod.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests du filet de sécurité, sans fenêtre : l'alerte est remplacée par une
 * liste des détails affichés.
 */
public class GestionnaireErreursInattenduesTest {

    /** Détails passés à l'alerte, dans l'ordre. */
    private final List<String> alertes = new ArrayList<>();

    /** Affichages en attente du thread de l'interface. */
    private final Queue<Runnable> interfaceUtilisateur = new ArrayDeque<>();

    private final GestionnaireErreursInattendues gestionnaire =
            new GestionnaireErreursInattendues(interfaceUtilisateur::add,
                    alertes::add);

    /** Niveau du journal avant le test. */
    private Level niveauAvant;

    /** Coupe le journal : les erreurs simulées ne polluent pas la sortie. */
    @Before
    public void couperLeJournal() {
        Logger journal = Logger.getLogger(
                GestionnaireErreursInattendues.class.getName());
        niveauAvant = journal.getLevel();
        journal.setLevel(Level.OFF);
    }

    /** Rétablit le journal. */
    @After
    public void retablirLeJournal() {
        Logger.getLogger(GestionnaireErreursInattendues.class.getName())
                .setLevel(niveauAvant);
    }

    private void afficherEnAttente() {
        while (!interfaceUtilisateur.isEmpty()) {
            interfaceUtilisateur.remove().run();
        }
    }

    /** Une erreur non attrapée ouvre l'alerte, sur le thread de l'interface. */
    @Test
    public void erreurAfficheeSurLeThreadDeLInterface() {
        gestionnaire.uncaughtException(Thread.currentThread(),
                new IllegalStateException("panne"));
        assertTrue(alertes.isEmpty());
        afficherEnAttente();
        assertEquals(List.of("panne"), alertes);
    }

    /** Une erreur répétée n'ouvre qu'une alerte à la fois. */
    @Test
    public void uneSeuleAlerteALaFois() {
        gestionnaire.uncaughtException(Thread.currentThread(),
                new IllegalStateException("première"));
        gestionnaire.uncaughtException(Thread.currentThread(),
                new IllegalStateException("seconde"));
        afficherEnAttente();
        assertEquals(List.of("première"), alertes);
        gestionnaire.uncaughtException(Thread.currentThread(),
                new IllegalStateException("plus tard"));
        afficherEnAttente();
        assertEquals(List.of("première", "plus tard"), alertes);
    }

    /** Sans message, l'erreur est décrite par son type. */
    @Test
    public void erreurSansMessageDecriteParSonType() {
        assertEquals("NullPointerException",
                GestionnaireErreursInattendues.decrire(
                        new NullPointerException()));
    }

    /** Une alerte qui échoue ne bloque pas les alertes suivantes. */
    @Test
    public void alerteEnEchecNeBloquePasLaSuivante() {
        List<String> affichees = new ArrayList<>();
        GestionnaireErreursInattendues fragile =
                new GestionnaireErreursInattendues(Runnable::run, detail -> {
                    if (affichees.isEmpty()) {
                        affichees.add("échec");
                        throw new IllegalStateException("fenêtre fermée");
                    }
                    affichees.add(detail);
                });
        fragile.uncaughtException(Thread.currentThread(),
                new IllegalStateException("une"));
        fragile.uncaughtException(Thread.currentThread(),
                new IllegalStateException("deux"));
        assertEquals(List.of("échec", "deux"), affichees);
    }

    /** Le message de l'alerte est en français et rassure l'utilisateur. */
    @Test
    public void messageRassurant() {
        assertTrue(GestionnaireErreursInattendues.MESSAGE.contains(
                "Votre travail est conservé"));
    }
}
