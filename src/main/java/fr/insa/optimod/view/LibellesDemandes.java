package fr.insa.optimod.view;

import fr.insa.optimod.model.Demande;

/**
 * Textes de la liste des demandes importées.
 */
final class LibellesDemandes {

    private LibellesDemandes() {
    }

    /**
     * Décrit une demande pour la liste : son numéro et les durées de service.
     *
     * @param rang numéro de la demande, à partir de 1
     * @param demande demande à décrire
     * @return texte de la ligne
     */
    static String decrire(int rang, Demande demande) {
        return "Demande " + rang + " : collecte " + formaterDuree(demande.obtenirDureeCollecte())
                + ", livraison " + formaterDuree(demande.obtenirDureeLivraison());
    }

    /** @return durée en minutes, avec les secondes restantes s'il y en a */
    static String formaterDuree(int secondes) {
        int minutes = secondes / 60;
        int reste = secondes % 60;
        if (reste == 0) {
            return minutes + " min";
        }
        return minutes == 0 ? reste + " s" : minutes + " min " + reste + " s";
    }
}
