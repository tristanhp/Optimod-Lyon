package fr.insa.optimod.model;

import java.util.List;

/**
 * Ensemble des demandes à livrer depuis un entrepôt.
 */
public final class Livraison {
    private final List<Demande> demandes;
    private final Entrepot entrepot;

    /**
     * Crée une livraison.
     *
     * @param demandes demandes à livrer
     * @param entrepot entrepôt de départ
     */
    public Livraison(List<Demande> demandes, Entrepot entrepot) {
        this.demandes = List.copyOf(demandes);
        this.entrepot = entrepot;
    }

    /** @return demandes à livrer (non modifiable) */
    public List<Demande> getDemandes() {
        return demandes;
    }

    /** @return entrepôt de départ */
    public Entrepot getEntrepot() {
        return entrepot;
    }
}
