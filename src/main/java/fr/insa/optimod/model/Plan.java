package fr.insa.optimod.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Plan de la ville : intersections, tronçons de route et, s'il est indiqué,
 * entrepôt. Un plan est immuable.
 */
public final class Plan {

    /** Intersections du plan. */
    private final List<Intersection> intersections;

    /** Tronçons de route du plan. */
    private final List<Troncon> troncons;

    /** Intersection où se trouve l'entrepôt, ou {@code null}. */
    private final Intersection entrepot;

    /**
     * Construit un plan.
     *
     * @param intersections intersections du plan
     * @param troncons tronçons, dont les extrémités sont des intersections du
     *        plan
     * @param entrepot intersection de l'entrepôt (une intersection du plan),
     *        ou {@code null} si le plan n'en indique pas
     * @throws IllegalArgumentException si un tronçon ou l'entrepôt référence
     *         une intersection absente du plan
     */
    public Plan(Collection<Intersection> intersections,
            Collection<Troncon> troncons, Intersection entrepot) {
        this.intersections = List.copyOf(intersections);
        this.troncons = List.copyOf(troncons);
        this.entrepot = entrepot;
        Set<Intersection> connues = new HashSet<>(this.intersections);
        for (Troncon troncon : this.troncons) {
            if (!connues.contains(troncon.origine())
                    || !connues.contains(troncon.destination())) {
                throw new IllegalArgumentException(
                        "Tronçon « " + troncon.nomRue()
                        + " » : extrémité absente du plan.");
            }
        }
        if (entrepot != null && !connues.contains(entrepot)) {
            throw new IllegalArgumentException(
                    "L'entrepôt n'est pas une intersection du plan.");
        }
    }

    /**
     * Retourne les intersections du plan.
     *
     * @return liste non modifiable des intersections
     */
    public List<Intersection> getIntersections() {
        return intersections;
    }

    /**
     * Retourne les tronçons de route du plan.
     *
     * @return liste non modifiable des tronçons
     */
    public List<Troncon> getTroncons() {
        return troncons;
    }

    /**
     * Retourne l'intersection où se trouve l'entrepôt.
     *
     * @return l'entrepôt, ou vide si le plan n'en indique pas
     */
    public Optional<Intersection> getEntrepot() {
        return Optional.ofNullable(entrepot);
    }

    @Override
    public String toString() {
        return "Plan[" + intersections.size() + " intersections, "
                + troncons.size() + " tronçons, entrepôt="
                + (entrepot == null ? "aucun" : entrepot.id()) + "]";
    }
}
