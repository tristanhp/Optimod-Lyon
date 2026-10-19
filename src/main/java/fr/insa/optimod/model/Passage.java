package fr.insa.optimod.model;

import java.time.LocalTime;

/**
 * Passage du livreur sur un point de la tournée, avec ses horaires.
 *
 * @param etape point visité
 * @param arrivee heure d'arrivée sur le point
 * @param depart heure de départ du point (arrivée + durée sur place)
 */
public record Passage(Etape etape, LocalTime arrivee, LocalTime depart) {
}
