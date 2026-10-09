package fr.insa.optimod.model;

import java.util.List;

public class Livraison {
    private final List<Demande> demandes;
    private final Entrepot entrepot;

    public Livraison(List<Demande> demandes, Entrepot entrepot) {
        this.demandes = demandes;
        this.entrepot = entrepot;
    }

    public List<Demande> getDemandes() {
        return demandes;
    }

    public Entrepot getEntrepot() {
        return entrepot;
    }

    
    
}

