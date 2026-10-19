package fr.insa.optimod.algo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import fr.insa.optimod.model.Etape;
import fr.insa.optimod.model.Passage;

public class CalculHorairesTest {

    /** Entrepôt (noeud 1), collecte de 5 min (noeud 2), livraison de 2 min. */
    private static final List<Etape> TOURNEE = List.of(
            new Etape(1, 0), new Etape(2, 300), new Etape(3, 120));

    /** 1 500 m entre les noeuds 1 et 2, 750 m entre 2 et 3. */
    private static double distance(long origine, long destination) {
        if (origine == 1 && destination == 2) {
            return 1500;
        }
        if (origine == 2 && destination == 3) {
            return 750;
        }
        throw new AssertionError("Trajet inattendu " + origine + "->"
                + destination);
    }

    /** Un cas calculé à la main : 1 500 m à 15 km/h = 6 min, 750 m = 3 min. */
    @Test
    public void horairesCalculesALaMain() {
        List<Passage> passages = CalculHoraires.calculer(TOURNEE,
                CalculHorairesTest::distance);
        assertEquals(3, passages.size());
        assertPassage(passages.get(0), TOURNEE.get(0), "08:00", "08:00");
        assertPassage(passages.get(1), TOURNEE.get(1), "08:06", "08:11");
        assertPassage(passages.get(2), TOURNEE.get(2), "08:14", "08:16");
    }

    /** Tournée vide : aucun passage et aucun calcul de distance. */
    @Test
    public void tourneeVideSansPassage() {
        List<Passage> passages = CalculHoraires.calculer(List.of(),
                (a, b) -> {
                    throw new AssertionError("Distance demandée");
                });
        assertTrue(passages.isEmpty());
    }

    /** Un seul point : arrivée à 8h, départ après la durée sur place. */
    @Test
    public void unSeulPointArriveADepartHeure() {
        List<Passage> passages = CalculHoraires.calculer(
                List.of(new Etape(1, 90)), (a, b) -> {
                    throw new AssertionError("Distance demandée");
                });
        assertEquals(1, passages.size());
        assertPassage(passages.get(0), new Etape(1, 90), "08:00", "08:01:30");
    }

    /** Durée nulle : le livreur repart à l'heure où il arrive. */
    @Test
    public void dureeNulleArriveeEgaleDepart() {
        List<Etape> ordre = List.of(new Etape(1, 0), new Etape(2, 0));
        List<Passage> passages = CalculHoraires.calculer(ordre,
                (a, b) -> 1500);
        assertEquals(passages.get(1).arrivee(), passages.get(1).depart());
        assertEquals(LocalTime.of(8, 6), passages.get(1).arrivee());
    }

    /** Distance nulle : arrivée à l'heure du départ précédent. */
    @Test
    public void distanceNulleSansTempsDeTrajet() {
        List<Etape> ordre = List.of(new Etape(1, 60), new Etape(1, 60));
        List<Passage> passages = CalculHoraires.calculer(ordre,
                (a, b) -> 0);
        assertEquals(LocalTime.of(8, 1), passages.get(1).arrivee());
        assertEquals(LocalTime.of(8, 2), passages.get(1).depart());
    }

    /** Le temps de trajet est arrondi à la seconde la plus proche. */
    @Test
    public void trajetArrondiALaSeconde() {
        List<Etape> ordre = List.of(new Etape(1, 0), new Etape(2, 0));
        // 100 m à 15 km/h = 24 s ; 102 m = 24,48 s -> 24 s ; 103 m -> 25 s
        assertEquals(LocalTime.of(8, 0, 24), CalculHoraires.calculer(ordre,
                (a, b) -> 100).get(1).arrivee());
        assertEquals(LocalTime.of(8, 0, 24), CalculHoraires.calculer(ordre,
                (a, b) -> 102).get(1).arrivee());
        assertEquals(LocalTime.of(8, 0, 25), CalculHoraires.calculer(ordre,
                (a, b) -> 103).get(1).arrivee());
    }

    /** Les distances sont demandées entre points consécutifs, dans l'ordre. */
    @Test
    public void distancesDemandeesEntrePointsConsecutifs() {
        List<String> appels = new ArrayList<>();
        CalculHoraires.calculer(TOURNEE, (a, b) -> {
            appels.add(a + "->" + b);
            return 0;
        });
        assertEquals(List.of("1->2", "2->3"), appels);
    }

    /** Une durée négative est refusée dès la création de l'étape. */
    @Test(expected = IllegalArgumentException.class)
    public void dureeNegativeRefusee() {
        new Etape(1, -1);
    }

    private static void assertPassage(Passage passage, Etape etape,
            String arrivee, String depart) {
        assertEquals(etape, passage.etape());
        assertEquals(LocalTime.parse(arrivee), passage.arrivee());
        assertEquals(LocalTime.parse(depart), passage.depart());
    }
}
