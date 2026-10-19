package fr.insa.optimod.model;

/**
 * Demande de livraison : une collecte puis une livraison.
 */
public final class Demande {
    private final long identifiantCollecte;
    private final long identifiantLivraison;
    private final int dureeCollecte; //secondes
    private final int dureeLivraison; //secondes

    /**
     * Crée une demande.
     *
     * @param identifiantCollecte identifiant du noeud de collecte
     * @param identifiantLivraison identifiant du noeud de livraison
     * @param dureeCollecte durée de la collecte en secondes
     * @param dureeLivraison durée de la livraison en secondes
     */

    public Demande(long identifiantCollecte, long identifiantLivraison, int dureeCollecte, int dureeLivraison) {
        this.identifiantCollecte = identifiantCollecte;
        this.identifiantLivraison = identifiantLivraison;
        this.dureeCollecte = dureeCollecte;
        this.dureeLivraison = dureeLivraison;
    }

    /** @return identifiant du noeud de collecte */
    public long obtenirIdentifiantCollecte() {
        return identifiantCollecte;
    }

    /** @return identifiant du noeud de livraison */
    public long obtenirIdentifiantLivraison() {
        return identifiantLivraison;
    }

    /** @return durée de la collecte en secondes */
    public int obtenirDureeCollecte() {
        return dureeCollecte;
    }

    /** @return durée de la livraison en secondes */
    public int obtenirDureeLivraison() {
        return dureeLivraison;
    }
}
