package fr.insa.optimod.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * Tests de {@link Observable}.
 */
public class ObservableTest {

    /** Observable minimal pour les tests. */
    private static final class Compteur extends Observable {
        void changer() {
            notifierObservateurs();
        }
    }

    private final Compteur observable = new Compteur();
    private final List<String> appels = new ArrayList<>();

    /** Les observateurs sont prévenus dans l'ordre d'inscription, avec la source. */
    @Test
    public void previentLesObservateursDansLOrdre() {
        observable.ajouterObservateur(source -> {
            assertSame(observable, source);
            appels.add("a");
        });
        observable.ajouterObservateur(source -> appels.add("b"));
        observable.changer();
        assertEquals(List.of("a", "b"), appels);
    }

    /** Un observateur inscrit deux fois n'est prévenu qu'une fois. */
    @Test
    public void inscriptionEnDoubleIgnoree() {
        Observateur observateur = source -> appels.add("a");
        observable.ajouterObservateur(observateur);
        observable.ajouterObservateur(observateur);
        observable.changer();
        assertEquals(List.of("a"), appels);
    }

    /** Un observateur retiré n'est plus prévenu. */
    @Test
    public void observateurRetireNonPrevenu() {
        Observateur observateur = source -> appels.add("a");
        observable.ajouterObservateur(observateur);
        observable.retirerObservateur(observateur);
        observable.retirerObservateur(observateur);
        observable.changer();
        assertTrue(appels.isEmpty());
    }

    /** Un observateur peut se désinscrire pendant la notification. */
    @Test
    public void desinscriptionPendantLaNotification() {
        observable.ajouterObservateur(new Observateur() {
            @Override
            public void mettreAJour(Observable source) {
                appels.add("a");
                source.retirerObservateur(this);
            }
        });
        observable.ajouterObservateur(source -> appels.add("b"));
        observable.changer();
        observable.changer();
        assertEquals(List.of("a", "b", "b"), appels);
    }
}
