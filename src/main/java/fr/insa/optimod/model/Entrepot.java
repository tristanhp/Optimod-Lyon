package fr.insa.optimod.model;

/**
 * Entrepôt d'où partent et où reviennent les livreurs.
 */
public final class Entrepot {
    private final long identifiant;
    private final String heureDepart;

    /**
     * Crée un entrepôt.
     *
     * @param identifiant identifiant du noeud où se trouve l'entrepôt
     * @param heureDepart heureDepart de départ
     */
    public Entrepot(long identifiant, String heureDepart) {
        this.identifiant = identifiant;
        this.heureDepart = heureDepart;
    }

    /** @return identifiant du noeud où se trouve l'entrepôt */
    public long obtenirIdentifiant() {
        return identifiant;
    }

    /** @return heureDepart de départ */
    public String obtenirHeureDepart() {
        return heureDepart;
    }
}
