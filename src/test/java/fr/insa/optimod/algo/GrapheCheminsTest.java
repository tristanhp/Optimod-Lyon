package fr.insa.optimod.algo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import fr.insa.optimod.model.Demande;
import fr.insa.optimod.model.Entrepot;
import fr.insa.optimod.model.Livraison;
import fr.insa.optimod.model.Plan;

/**
 * Tests de {@link GrapheChemins}.
 */
public class GrapheCheminsTest {

    private Plan plan;

    /** Entrepôt en 1, une demande collectée en 3 et livrée en 2. */
    private GrapheChemins graphe;

    /**
     * Charge le petit réseau et construit le graphe de référence avant chaque
     * test.
     *
     * @throws Exception si le XML du réseau est invalide
     */
    @Before
    public void creer() throws Exception {
        plan = PlansPourTests.petitReseau();
        graphe = graphe(1, new Demande(3, 2, 60, 60));
    }

    /**
     * Construit le graphe d'une livraison sur le petit réseau.
     *
     * @param entrepot id du noeud de l'entrepôt
     * @param demandes demandes de la livraison
     * @return le graphe des plus courts chemins
     */
    private GrapheChemins graphe(long entrepot, Demande... demandes) {
        return new GrapheChemins(plan, new Livraison(List.of(demandes),
                new Entrepot(entrepot, "8:0:0")));
    }

    /** n demandes donnent 2n+1 sommets. */
    @Test
    public void unSommetParPoint() {
        GrapheChemins deuxDemandes = graphe(1, new Demande(3, 2, 60, 60),
                new Demande(2, 3, 60, 60));
        assertEquals(5, deuxDemandes.getNbSommets());
    }

    /**
     * Le coût est la longueur orientée du plus court chemin, arrondie au
     * mètre. Les valeurs confirment aussi la numérotation : 0 entrepôt,
     * 1 collecte, 2 livraison.
     */
    @Test
    public void coutsOrientesEtArrondis() {
        assertEquals(100, graphe.getCout(0, 1));
        assertEquals(450, graphe.getCout(1, 0));
        assertEquals(150, graphe.getCout(1, 2));
        assertEquals(250, graphe.getCout(0, 2));
        assertEquals(300, graphe.getCout(2, 0));
        assertEquals(249.6, graphe.getChemin(0, 2).longueur(), 1e-9);
    }

    /** Pas d'arc d'un sommet vers lui-même. */
    @Test
    public void pasDeBoucle() {
        for (int i = 0; i < graphe.getNbSommets(); i++) {
            assertFalse(graphe.estArc(i, i));
            assertEquals(-1, graphe.getCout(i, i));
        }
    }

    /** Un point inaccessible n'a pas d'arc. */
    @Test
    public void pointInaccessibleSansArc() {
        GrapheChemins avecIsole = graphe(1, new Demande(4, 2, 60, 60));
        assertFalse(avecIsole.estArc(0, 1));
        assertEquals(-1, avecIsole.getCout(0, 1));
        assertTrue(avecIsole.estArc(0, 2));
    }

    /** Un point absent du plan est refusé. */
    @Test(expected = IllegalArgumentException.class)
    public void pointAbsentDuPlanRefuse() {
        graphe(1, new Demande(99, 2, 60, 60));
    }
}
