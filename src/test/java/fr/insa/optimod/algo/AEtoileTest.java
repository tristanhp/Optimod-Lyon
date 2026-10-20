package fr.insa.optimod.algo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import fr.insa.optimod.model.Noeud;
import fr.insa.optimod.model.Plan;
import fr.insa.optimod.model.Troncon;

/**
 * Tests de {@link AEtoile}.
 */
public class AEtoileTest {

    private static final double PRECISION = 1e-9;

    private Plan plan;
    private AEtoile aEtoile;

    /**
     * Charge le petit réseau avant chaque test.
     *
     * @throws Exception si le XML du réseau est invalide
     */
    @Before
    public void creer() throws Exception {
        plan = PlansPourTests.petitReseau();
        aEtoile = new AEtoile(plan);
    }

    /**
     * Cherche le plus court chemin entre deux noeuds du petit réseau.
     *
     * @param source id du noeud de départ
     * @param cible id du noeud d'arrivée
     * @return le chemin trouvé par A*
     */
    private Chemin chemin(long source, long cible) {
        return aEtoile.plusCourtChemin(plan.obtenirNoeud(source),
                plan.obtenirNoeud(cible));
    }

    /** Vérifie que le chemin relie la source à la cible sans trou. */
    private static void verifierContigu(Chemin chemin, Noeud source,
            Noeud cible) {
        Noeud courant = source;
        double somme = 0;
        for (Troncon t : chemin.troncons()) {
            assertSame(courant, t.obtenirNoeudOrigine());
            courant = t.obtenirNoeudDestination();
            somme += t.obtenirLongueur();
        }
        assertSame(cible, courant);
        assertEquals(chemin.longueur(), somme, PRECISION);
    }

    /** Le détour plus court est préféré au tronçon direct. */
    @Test
    public void detourPlusCourtChoisi() {
        Chemin chemin = chemin(1, 2);
        assertEquals(249.6, chemin.longueur(), PRECISION);
        assertEquals(2, chemin.troncons().size());
        verifierContigu(chemin, plan.obtenirNoeud(1), plan.obtenirNoeud(2));
    }

    /** Les tronçons sont orientés : le retour peut être plus long. */
    @Test
    public void sensUniqueRespecte() {
        assertEquals(100, chemin(1, 3).longueur(), PRECISION);
        Chemin retour = chemin(3, 1);
        assertEquals(449.6, retour.longueur(), PRECISION);
        verifierContigu(retour, plan.obtenirNoeud(3), plan.obtenirNoeud(1));
    }

    /** Un noeud isolé est inaccessible, dans les deux sens. */
    @Test
    public void noeudIsoleInaccessible() {
        for (Chemin chemin : List.of(chemin(1, 4), chemin(4, 1))) {
            assertFalse(chemin.estAccessible());
            assertTrue(chemin.troncons().isEmpty());
        }
    }

    /** L'heuristique ne surestime jamais un tronçon du plan fourni. */
    @Test
    public void heuristiqueAdmissibleSurPetitPlan() throws Exception {
        for (Troncon t : PlansPourTests.petitPlan().obtenirTroncons()) {
            double estimation = AEtoile.heuristique(t.obtenirNoeudOrigine(),
                    t.obtenirNoeudDestination());
            assertTrue(estimation <= t.obtenirLongueur());
        }
    }
}
