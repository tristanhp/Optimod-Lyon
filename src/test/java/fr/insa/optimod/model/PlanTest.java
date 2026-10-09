package fr.insa.optimod.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class PlanTest {

    private final Intersection a = new Intersection(1, 45.75, 4.85);
    private final Intersection b = new Intersection(2, 45.76, 4.86);

    @Test
    public void conserveIntersectionsTronconsEtEntrepot() {
        Troncon t = new Troncon(a, b, "Rue A", 120.5);
        Plan plan = new Plan(List.of(a, b), List.of(t), a);
        assertEquals(List.of(a, b), plan.getIntersections());
        assertEquals(List.of(t), plan.getTroncons());
        assertSame(a, plan.getEntrepot().orElseThrow());
    }

    @Test
    public void entrepotFacultatif() {
        Plan plan = new Plan(List.of(a, b), List.of(), null);
        assertFalse(plan.getEntrepot().isPresent());
        assertTrue(plan.toString().contains("aucun"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void refuseUnTronconHorsPlan() {
        new Plan(List.of(a), List.of(new Troncon(a, b, "Rue A", 1)), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void refuseUnEntrepotHorsPlan() {
        new Plan(List.of(a), List.of(), b);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void listesNonModifiables() {
        new Plan(List.of(a), List.of(), null).getIntersections().clear();
    }
}
