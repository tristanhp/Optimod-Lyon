package fr.insa.optimod.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import fr.insa.optimod.model.ProgrammeLivraisons.Demande;
import fr.insa.optimod.model.Tournee.Arret;
import fr.insa.optimod.model.Tournee.TypeArret;

/**
 * Tests de {@link Tournee}.
 */
public class TourneeTest {

    private static final Noeud ENTREPOT = new Noeud(1, 4.85, 45.75);
    private static final Noeud A = new Noeud(2, 4.86, 45.76);
    private static final Noeud B = new Noeud(3, 4.87, 45.77);
    private static final Demande DEMANDE = new Demande(A, B, 60, 60);

    /** Tournée entrepôt, collecte, livraison, entrepôt, retour à l'heure. */
    private static Tournee tourneeRevenantA(LocalTime retour) {
        LocalTime depart = LocalTime.of(8, 0);
        return new Tournee(List.of(
                new Arret(ENTREPOT, depart, depart, TypeArret.ENTREPOT, null),
                new Arret(A, depart, depart, TypeArret.COLLECTE, DEMANDE),
                new Arret(B, depart, depart, TypeArret.LIVRAISON, DEMANDE),
                new Arret(ENTREPOT, retour, retour, TypeArret.ENTREPOT,
                        null)),
                List.of(), true);
    }

    /** Une tournée vide part de l'entrepôt et n'a aucun chemin. */
    @Test
    public void tourneeVide() {
        Tournee tournee = Tournee.vide(ENTREPOT, LocalTime.of(8, 0));
        assertEquals(1, tournee.arrets().size());
        Arret depart = tournee.arrets().get(0);
        assertEquals(ENTREPOT, depart.noeud());
        assertEquals(TypeArret.ENTREPOT, depart.type());
        assertNull(depart.demande());
        assertTrue(tournee.chemins().isEmpty());
        assertEquals(LocalTime.of(8, 0), tournee.obtenirHeureRetour());
        assertTrue(tournee.estPossible());
    }

    /** Un retour à 13 h pile est encore possible. */
    @Test
    public void retourA13hPossible() {
        assertTrue(tourneeRevenantA(LocalTime.of(13, 0)).estPossible());
    }

    /** Un retour après 13 h rend la tournée impossible. */
    @Test
    public void retourApres13hImpossible() {
        assertFalse(tourneeRevenantA(LocalTime.of(13, 0, 1)).estPossible());
    }

    /** Un arrêt à 13 h pile est à l'heure, une seconde plus tard non. */
    @Test
    public void arretApresHeureLimiteAu13hPres() {
        LocalTime treize = Plan.HEURE_LIMITE_RETOUR;
        assertFalse(new Arret(A, treize, treize, TypeArret.COLLECTE, DEMANDE)
                .estApresHeureLimite());
        assertTrue(new Arret(A, treize.plusSeconds(1), treize.plusSeconds(1),
                TypeArret.COLLECTE, DEMANDE).estApresHeureLimite());
    }

    /**
     * Seuls les arrêts atteints après 13 h sont marqués : ici la livraison à
     * 13:10 et le retour à 13:30, pas la collecte à 12:50 ni le départ.
     */
    @Test
    public void seulsLesArretsTardifsSontMarques() {
        LocalTime depart = LocalTime.of(8, 0);
        Tournee tournee = new Tournee(List.of(
                new Arret(ENTREPOT, depart, depart, TypeArret.ENTREPOT, null),
                new Arret(A, LocalTime.of(12, 50), LocalTime.of(12, 55),
                        TypeArret.COLLECTE, DEMANDE),
                new Arret(B, LocalTime.of(13, 10), LocalTime.of(13, 15),
                        TypeArret.LIVRAISON, DEMANDE),
                new Arret(ENTREPOT, LocalTime.of(13, 30),
                        LocalTime.of(13, 30), TypeArret.ENTREPOT, null)),
                List.of(), true);
        List<Boolean> marques = tournee.arrets().stream()
                .map(Arret::estApresHeureLimite).toList();
        assertEquals(List.of(false, false, true, true), marques);
        assertFalse(tournee.estPossible());
    }

    /** L'entrepôt n'a pas de demande, une collecte ou une livraison si. */
    @Test(expected = IllegalArgumentException.class)
    public void entrepotAvecDemandeRefuse() {
        new Arret(ENTREPOT, LocalTime.of(8, 0), LocalTime.of(8, 0),
                TypeArret.ENTREPOT, DEMANDE);
    }

    /** Une collecte sans demande est refusée. */
    @Test(expected = IllegalArgumentException.class)
    public void collecteSansDemandeRefusee() {
        new Arret(A, LocalTime.of(8, 0), LocalTime.of(8, 0),
                TypeArret.COLLECTE, null);
    }

    /** Les bornes sont inclusives et restent fixes lorsque l'arrivée change. */
    @Test
    public void plageUneHeureEtDepassement() {
        Arret arret = new Arret(A, LocalTime.of(9, 47), LocalTime.of(9, 52),
                TypeArret.COLLECTE, DEMANDE);
        assertEquals(LocalTime.of(8, 47), arret.debutPlage());
        assertEquals(LocalTime.of(10, 47), arret.finPlage());
        assertFalse(arret.estHorsPlage());
        assertFalse(new Arret(A, LocalTime.of(10, 47), LocalTime.of(10, 52),
                TypeArret.COLLECTE, DEMANDE, arret.debutPlage(),
                arret.finPlage()).estHorsPlage());
        assertTrue(new Arret(A, LocalTime.of(10, 48), LocalTime.of(10, 53),
                TypeArret.COLLECTE, DEMANDE, arret.debutPlage(),
                arret.finPlage()).estHorsPlage());
    }

    /** Une plage autour de minuit conserve son sens sur les deux jours. */
    @Test
    public void plageAutourDeMinuit() {
        Arret arret = new Arret(A, LocalTime.of(0, 30), LocalTime.of(0, 35),
                TypeArret.COLLECTE, DEMANDE);
        assertFalse(arret.estHorsPlage());
        assertFalse(new Arret(A, LocalTime.of(23, 45), LocalTime.of(23, 50),
                TypeArret.COLLECTE, DEMANDE, arret.debutPlage(),
                arret.finPlage()).estHorsPlage());
        assertTrue(new Arret(A, LocalTime.of(12, 0), LocalTime.of(12, 5),
                TypeArret.COLLECTE, DEMANDE, arret.debutPlage(),
                arret.finPlage()).estHorsPlage());
    }

    /** Un tronçon repris dans deux chemins est compté deux fois. */
    @Test
    public void tronconReprisDeuxFois() {
        Troncon allerAB = new Troncon(A, B, 100, "rue A");
        Troncon allerBE = new Troncon(B, ENTREPOT, 100, "rue B");
        Tournee tournee = new Tournee(tourneeRevenantA(
                LocalTime.of(9, 0)).arrets(), List.of(
                new Chemin(200, List.of(allerAB, allerBE)),
                new Chemin(200, List.of(allerBE, allerAB))), true);
        Map<Set<Noeud>, Integer> passages =
                tournee.compterPassagesParTroncon();
        assertEquals(Integer.valueOf(2), passages.get(Set.of(A, B)));
        assertEquals(Integer.valueOf(2), passages.get(Set.of(B, ENTREPOT)));
        assertEquals(2, passages.size());
    }

    /** Un aller-retour sur une rue compte pour deux passages, sens ignoré. */
    @Test
    public void allerRetourCompteDeuxPassages() {
        Troncon allerAB = new Troncon(A, B, 100, "rue A");
        Troncon retourBA = new Troncon(B, A, 100, "rue A");
        Tournee tournee = new Tournee(tourneeRevenantA(
                LocalTime.of(9, 0)).arrets(), List.of(
                new Chemin(100, List.of(allerAB)),
                new Chemin(100, List.of(retourBA))), true);
        Map<Set<Noeud>, Integer> passages =
                tournee.compterPassagesParTroncon();
        assertEquals(Integer.valueOf(2), passages.get(Set.of(A, B)));
        assertEquals(Integer.valueOf(2), passages.get(Set.of(B, A)));
        assertEquals(1, passages.size());
    }

    /** Deux tronçons différents ne sont pas confondus. */
    @Test
    public void tronconsDifferentsComptesSeparement() {
        Troncon allerAB = new Troncon(A, B, 100, "rue A");
        Troncon allerBE = new Troncon(B, ENTREPOT, 100, "rue B");
        Tournee tournee = new Tournee(tourneeRevenantA(
                LocalTime.of(9, 0)).arrets(), List.of(
                new Chemin(200, List.of(allerAB, allerBE))), true);
        Map<Set<Noeud>, Integer> passages =
                tournee.compterPassagesParTroncon();
        assertEquals(Integer.valueOf(1), passages.get(Set.of(A, B)));
        assertEquals(Integer.valueOf(1), passages.get(Set.of(B, ENTREPOT)));
        assertEquals(2, passages.size());
    }

    /** Une tournée sans chemin n'a aucun tronçon compté. */
    @Test
    public void tourneeSansCheminSansPassage() {
        assertTrue(Tournee.vide(ENTREPOT, LocalTime.of(8, 0))
                .compterPassagesParTroncon().isEmpty());
    }
}
