package fr.insa.optimod.model;

/**
 * Entrepôt d'où partent et où reviennent les livreurs.
 */
public final class Entrepot {
    private final long id;
    private final String heure;

    /**
     * Crée un entrepôt.
     *
     * @param id id du noeud où se trouve l'entrepôt
     * @param heure heure de départ
     */
    public Entrepot(long id, String heure) {
        this.id = id;
        this.heure = heure;
    }

    /** @return id du noeud où se trouve l'entrepôt */
    public long getId() {
        return id;
    }

    /** @return heure de départ */
    public String getHeure() {
        return heure;
    }
}
