package fr.insa.optimod.view;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import fr.insa.optimod.model.Noeud;
import fr.insa.optimod.model.ProgrammeLivraisons.Demande;

public class LibellesDemandesTest {

    @Test
    public void decritLeNumeroEtLesDureesSansAdresse() {
        Demande demande = new Demande(new Noeud(10, 0, 0), new Noeud(20, 0, 0),
                180, 240);
        assertEquals("Demande 3 : collecte 3 min, livraison 4 min",
                LibellesDemandes.decrire(3, demande));
    }

    @Test
    public void formateLesDureesEnMinutesEtSecondes() {
        assertEquals("0 min", LibellesDemandes.formaterDuree(0));
        assertEquals("45 s", LibellesDemandes.formaterDuree(45));
        assertEquals("1 min 30 s", LibellesDemandes.formaterDuree(90));
        assertEquals("7 min", LibellesDemandes.formaterDuree(420));
    }
}
