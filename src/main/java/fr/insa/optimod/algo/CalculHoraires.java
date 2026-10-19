package fr.insa.optimod.algo;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleBiFunction;

import fr.insa.optimod.model.Etape;
import fr.insa.optimod.model.Passage;

/**
 * Calcule les heures d'arrivée et de départ de chaque point d'une tournée à
 * partir de l'ordre de visite : départ à 8h, vitesse de 15 km/h.
 */
public final class CalculHoraires {

    /** Heure de départ de la tournée, au premier point. */
    public static final LocalTime HEURE_DEPART = LocalTime.of(8, 0);

    /** Vitesse du livreur, en km/h. */
    public static final double VITESSE_KMH = 15;

    private CalculHoraires() {
    }

    /**
     * Calcule les horaires de la tournée.
     *
     * <p>Le premier point est atteint à l'heure de départ. Ensuite, l'arrivée
     * sur un point est le départ du point précédent plus le temps de trajet
     * (distance / vitesse, arrondi à la seconde), et le départ est l'arrivée
     * plus la durée passée sur le point.
     *
     * @param ordre points dans l'ordre de visite
     * @param distanceMetres distance en mètres du trajet entre deux noeuds
     *        (origine, destination)
     * @return un passage par point, dans le même ordre ; liste vide si la
     *         tournée est vide
     */
    public static List<Passage> calculer(List<Etape> ordre,
            ToDoubleBiFunction<Long, Long> distanceMetres) {
        List<Passage> passages = new ArrayList<>();
        LocalTime depart = HEURE_DEPART;
        Etape precedente = null;
        for (Etape etape : ordre) {
            LocalTime arrivee = depart;
            if (precedente != null) {
                double metres = distanceMetres.applyAsDouble(
                        precedente.idNoeud(), etape.idNoeud());
                arrivee = depart.plusSeconds(secondesDeTrajet(metres));
            }
            depart = arrivee.plusSeconds(etape.dureeSecondes());
            passages.add(new Passage(etape, arrivee, depart));
            precedente = etape;
        }
        return passages;
    }

    private static long secondesDeTrajet(double metres) {
        double metresParSeconde = VITESSE_KMH * 1000 / 3600;
        return Math.round(metres / metresParSeconde);
    }
}
