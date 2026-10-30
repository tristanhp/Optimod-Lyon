package fr.insa.optimod.algo.tsp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public abstract class TemplateTSP implements TSP {

    private Integer[] meilleureSolution;
    protected GrapheDepandance g;
    private int coutMeilleureSolution;
    private int coutLimite;
    private int tpsLimite;
    private long tpsDebut;
    private long tpsFin;

    public ResultatCalculTSP chercherSolution(int tpsLimite, GrapheDepandance g, int coutLimite) {
        if (tpsLimite <= 0)
            return ResultatCalculTSP.TEMPS_LIMITE_NEGATIF;
        tpsDebut = System.currentTimeMillis();
        this.tpsLimite = tpsLimite;
        this.g = g;
        this.coutLimite = coutLimite;
        meilleureSolution = new Integer[g.obtenirNombreSommets()];
        Collection<Integer> nonVus = new ArrayList<Integer>(g.obtenirNombreSommets() - 1);
        Collection<Integer> accessible = new ArrayList<Integer>(g.obtenirNombreSommets() - 1);
        for (int i = 1; i < g.obtenirNombreSommets(); i++) {
            if (g.getDepandance(i) == -1) {
                accessible.add(i);
            }
            nonVus.add(i);
        }
        Collection<Integer> vus = new ArrayList<Integer>(g.obtenirNombreSommets());
        vus.add(0); // le premier sommet visite est 0
        coutMeilleureSolution = Integer.MAX_VALUE;
        branchAndBound(0, nonVus, accessible, vus, 0);
        tpsFin = System.currentTimeMillis();

        // Si le temps limite est atteint, la solution peut etre non-optimal
        if (tpsFin - tpsDebut >= tpsLimite) {
            if (coutMeilleureSolution == Integer.MAX_VALUE) {
                return ResultatCalculTSP.CALCUL_TROP_LONG_SANS_RESULTAT;
            }
            return ResultatCalculTSP.CALCUL_TROP_LONG;
        }
        if (coutMeilleureSolution == Integer.MAX_VALUE) {
            return ResultatCalculTSP.PAS_DE_SOLUTION;
        }
        if (coutMeilleureSolution > coutLimite) {
            return ResultatCalculTSP.COUT_TROP_ELEVE;
        }
        // La solution est optimal
        return ResultatCalculTSP.RESULTAT_OK;
    }

    public Integer obtenirSommetSolution(int i) {
        if (g != null && i >= 0 && i < g.obtenirNombreSommets())
            return meilleureSolution[i];
        return -1;
    }

    public int obtenirCoutSolution() {
        if (g != null)
            return coutMeilleureSolution;
        return -1;
    }

    public long obtenirDureeCalcul() {
        if (g != null)
            return tpsFin - tpsDebut;
        return -1;
    }

    /**
     * Methode devant etre redefinie par les sous-classes de TemplateTSP
     *
     * @param sommetCourant
     * @param nonVus
     * @return une borne inferieure du cout des chemins de <code>g</code> partant de
     *         <code>sommetCourant</code>, visitant
     *         tous les sommets de <code>nonVus</code> exactement une fois, puis
     *         retournant sur le sommet <code>0</code>.
     */
    protected abstract int calculerBorneInferieure(Integer sommetCourant, Collection<Integer> nonVus);

    /**
     * Methode devant etre redefinie par les sous-classes de TemplateTSP
     *
     * @param sommetCrt
     * @param nonVus
     * @param g
     * @return un iterateur permettant d'iterer sur tous les sommets de
     *         <code>nonVus</code> qui sont successeurs de
     *         <code>sommetCourant</code>
     */
    protected abstract Iterator<Integer> creerIterateurSuccesseurs(Integer sommetCrt, Collection<Integer> nonVus,
            GrapheDepandance g);

    /**
     * Methode devant etre redefinie par les sous-classes de TemplateTSP
     *
     * @param sommetCrt
     * @param nonVus
     * @param g
     * @return true s'il y a un croisement d'arc sans inversion de dependance, false
     *         sinon
     */
    protected abstract Boolean croisementArc(Integer sommetCrt, Integer sommetVisite, Collection<Integer> nonVus,
            GrapheDepandance g);

    /**
     * Methode definissant le patron (template) d'une resolution par separation et
     * evaluation (branch and calculerBorneInferieure) du TSP pour le graphe
     * <code>g</code>.
     *
     * @param sommetCrt le dernier sommet visite
     * @param nonVus    la liste des sommets qui n'ont pas encore ete visites
     * @param vus       la liste des sommets deja visites (y compris sommetCrt)
     * @param coutVus   la somme des couts des arcs du chemin passant par tous les
     *                  sommets de vus dans l'ordre ou ils ont ete visites
     */
    private void branchAndBound(int sommetCrt, Collection<Integer> nonVus, Collection<Integer> accessible,
            Collection<Integer> vus, int coutVus) {
        if (System.currentTimeMillis() - tpsDebut > tpsLimite)
            return;
        if (nonVus.size() == 0) { // tous les sommets ont ete visites
            if (g.estArc(sommetCrt, 0)) { // on peut retourner au sommet de depart (0)
                if (coutVus + g.obtenirCoutArc(sommetCrt, 0) < coutMeilleureSolution) { // on a trouve une solution
                                                                                        // meilleure
                    // que meilleureSolution
                    vus.toArray(meilleureSolution);
                    coutMeilleureSolution = coutVus + g.obtenirCoutArc(sommetCrt, 0);
                }
            }
        } else if (coutVus + calculerBorneInferieure(sommetCrt, nonVus) < coutMeilleureSolution) {
            // Si le cout limite est depasse, il est inutile de continuer les calculs
            if (coutLimite != -1 && coutVus + calculerBorneInferieure(sommetCrt, nonVus) > coutLimite) {
                return;
            }

            Iterator<Integer> it = creerIterateurSuccesseurs(sommetCrt, accessible, g);
            while (it.hasNext()) {
                Integer prochainSommet = it.next();

                // On verifie qu'il n'y a pas d'arc qui se croise (hors depandance)
                if (croisementArc(sommetCrt, prochainSommet, vus, g)) {
                    continue;
                }

                vus.add(prochainSommet);
                nonVus.remove(prochainSommet);
                accessible.remove(prochainSommet);
                if (g.obtenirSousDepandance(prochainSommet) != -1) {
                    accessible.add(g.obtenirSousDepandance(prochainSommet));
                }
                branchAndBound(prochainSommet, nonVus, accessible, vus,
                        coutVus + g.obtenirCoutArc(sommetCrt, prochainSommet));
                vus.remove(prochainSommet);
                nonVus.add(prochainSommet);
                accessible.add(prochainSommet);
                if (g.obtenirSousDepandance(prochainSommet) != -1) {
                    accessible.remove(g.obtenirSousDepandance(prochainSommet));
                }
            }
        }
    }
}
