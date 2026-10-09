package fr.insa.optimod.model;

/**
 * Demande de livraison : une collecte puis une livraison.
 */
public final class Demande {
    private final long idCollecte;
    private final long idLivraison;
    private final int tempsCollecte; //secondes
    private final int tempsLivraison; //secondes

    /**
     * Crée une demande.
     *
     * @param idCollecte id du noeud de collecte
     * @param idLivraison id du noeud de livraison
     * @param tempsCollecte durée de la collecte en secondes
     * @param tempsLivraison durée de la livraison en secondes
     */
    public Demande(long idCollecte, long idLivraison, int tempsCollecte, int tempsLivraison) {
        this.idCollecte = idCollecte;
        this.idLivraison = idLivraison;
        this.tempsCollecte = tempsCollecte;
        this.tempsLivraison = tempsLivraison;
    }

    /** @return id du noeud de collecte */
    public long getIdCollecte() {
        return idCollecte;
    }

    /** @return id du noeud de livraison */
    public long getIdLivraison() {
        return idLivraison;
    }

    /** @return durée de la collecte en secondes */
    public int getTempsCollecte() {
        return tempsCollecte;
    }

    /** @return durée de la livraison en secondes */
    public int getTempsLivraison() {
        return tempsLivraison;
    }
}
