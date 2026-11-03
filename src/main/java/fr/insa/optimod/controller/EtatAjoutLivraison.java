package fr.insa.optimod.controller;

import java.util.EnumSet;
import java.util.Set;

import fr.insa.optimod.controller.MapScreen.Action;
import fr.insa.optimod.model.Noeud;
import fr.insa.optimod.model.ProgrammeLivraisons.Demande;

/**
 * Ajout d'une demande, seconde étape : le point de collecte est choisi,
 * l'utilisateur clique sur le point de livraison. La demande est alors ajoutée
 * par une commande annulable.
 */
final class EtatAjoutLivraison implements Etat {

    /** Consigne affichée en entrant dans cet état. */
    static final String CONSIGNE = "Cliquez sur le point de livraison "
            + "(« Ajouter une demande » pour annuler).";

    /** Noeud de collecte choisi à l'étape précédente. */
    private final long idCollecte;

    /**
     * Mémorise le point de collecte choisi.
     * @param idCollecte noeud de collecte choisi
     */
    EtatAjoutLivraison(long idCollecte) {
        this.idCollecte = idCollecte;
    }

    @Override
    public void importerPlan(MainController controleur) {
    }

    @Override
    public void importerProgramme(MainController controleur) {
    }

    @Override
    public void ajouterDemande(MainController controleur) {
        controleur.retrouverEtatProgramme();
    }

    @Override
    public void annuler(MainController controleur) {
        controleur.retrouverEtatProgramme();
    }

    @Override
    public void cliquerNoeud(MainController controleur, long idNoeud) {
        if (idNoeud == idCollecte) {
            controleur.afficherConsigne("La livraison doit avoir lieu ailleurs "
                    + "que la collecte. " + CONSIGNE);
            return;
        }
        Noeud collecte = controleur.obtenirPlan().obtenirNoeud(idCollecte);
        Noeud livraison = controleur.obtenirPlan().obtenirNoeud(idNoeud);
        if (collecte == null || livraison == null) {
            return; // clic hors du plan : ignoré
        }
        controleur.afficherPointsProvisoires(collecte, livraison);
        controleur.saisirDemande().ifPresentOrElse(saisie -> {
            try {
                int dureeCollecte = MapScreen.SaisieDemande.convertirDuree(
                        saisie.dureeCollecte());
                int dureeLivraison = MapScreen.SaisieDemande.convertirDuree(
                        saisie.dureeLivraison());
                int nombreLivreurs = controleur.obtenirProgramme()
                        .obtenirNombreLivreurs();
                if (saisie.livreur() < 1 || saisie.livreur() > nombreLivreurs) {
                    throw new IllegalArgumentException("Livreur "
                            + saisie.livreur() + " inconnu : il y a "
                            + nombreLivreurs + " livreur(s).");
                }
                controleur.ajouterDemande(new Demande(collecte, livraison,
                        dureeCollecte, dureeLivraison, saisie.livreur()));
            } catch (IllegalArgumentException erreur) {
                controleur.afficherConsigne(erreur.getMessage());
            }
        }, controleur::retrouverEtatProgramme);
    }

    @Override
    public Set<Action> actionsPermises(MainController controleur) {
        return EnumSet.of(Action.AJOUTER_DEMANDE, Action.ANNULER);
    }
}
