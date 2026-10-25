package fr.insa.optimod.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Objet du modèle qui prévient ses {@link Observateur observateurs} quand il
 * change (design pattern Observer). Remplace {@code java.util.Observable},
 * obsolète depuis Java 9.
 */
public abstract class Observable {

    /** Observateurs, prévenus dans l'ordre d'inscription. */
    private final List<Observateur> observateurs = new ArrayList<>();

    /**
     * Inscrit un observateur. Inscrire deux fois le même observateur est sans
     * effet.
     *
     * @param observateur objet à prévenir des changements
     */
    public void ajouterObservateur(Observateur observateur) {
        if (!observateurs.contains(observateur)) {
            observateurs.add(observateur);
        }
    }

    /**
     * Désinscrit un observateur ; sans effet s'il n'était pas inscrit.
     *
     * @param observateur objet à ne plus prévenir
     */
    public void retirerObservateur(Observateur observateur) {
        observateurs.remove(observateur);
    }

    /** Prévient tous les observateurs que l'objet a changé. */
    protected void notifierObservateurs() {
        for (Observateur observateur : List.copyOf(observateurs)) {
            observateur.mettreAJour(this);
        }
    }
}
