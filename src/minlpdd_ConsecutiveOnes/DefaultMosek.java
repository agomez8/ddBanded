package minlpdd_ConsecutiveOnes;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
import mosek.*;
import java.util.HashMap;
import java.util.Map;
import static mosek.solsta.dual_infeas_cer;
import static mosek.solsta.integer_optimal;
import static mosek.solsta.optimal;
import static mosek.solsta.prim_infeas_cer;
import static mosek.solsta.unknown;

/**
 *
 * @author agome
 */
public class DefaultMosek {

    /**
     * Mosek environment. <br>
     */
    Env env;

    /**
     * Mosek task. <br>
     */
    Task task;

    /**
     * Array with position of the variables
     */
    int[] x, z, s, persp;

    /**
     * Index of last variable and constraint added to the model
     */
    int varIndex, conIndex;

    /**
     * 1 to solve the integer problem, 0 to solve the relaxation.
     */
    int integer;

    //--------------------------------------------------------------------------
    // Constructor
    //--------------------------------------------------------------------------
    /**
     * Constructor.<br>
     *
     * @param constant Constant in the objective.  <br>
     * @param contCost Costs of the continuous variables. <br>
     * @param indCost Costs of the discrete variables. <br>
     * @param diagram Diagram to build linear relaxation (if null, then builds
     * perspective). <br>
     * @param integer Whether to solve the discrete problem or its relaxation.
     */
    public DefaultMosek(double constant, double[] contCost, double[] indCost,
            DecisionDiagram diagram, int integer) {
        env = new Env();
        task = new Task(env, 0, 0);
        task.putintparam(mosek.iparam.num_threads, 1);
        task.putdouparam(dparam.mio_max_time, 1800);
//        integer=0;
        this.integer = integer;
        buildInstanceQP(constant, contCost, indCost, diagram);

        if (integer == 1) {
            task.set_Stream(streamtype.err, new mosek.Stream() {
                @Override
                public void stream(String string) {
                    System.out.print(string);

                }
            });
        }

    }

    /**
     * Constructor.<br>
     *
     * @param constant Constant in the objective.  <br>
     * @param contCost Costs of the continuous variables. <br>
     * @param indCost Costs of the discrete variables. <br>
     * @param diagram Diagram to build linear relaxation (if null, then builds
     * perspective). <br>
     * @param integer Whether to solve the discrete problem or its relaxation.
     * <br>
     * @param pos
     * @param diag Array with the diagonal elements of the Q matrix (for
     * perspective relaxation). <br>
     * @param factorization Matrix F such that Q=FF'. <br>
     * @param consecutive Number of consecutive ones.
     */
    public DefaultMosek(double constant, double[] contCost, double[] indCost, double[] diag, double[][] factorization, int pos,
            DecisionDiagram diagram, int integer, int consecutive) {
        env = new Env();
        task = new Task(env, 0, 0);
        task.putintparam(mosek.iparam.num_threads, 1);
        task.putdouparam(dparam.mio_max_time, 1800);
//        integer=0;
        this.integer = integer;
        buildInstanceQP(constant, contCost, indCost, diag, factorization, pos, diagram, consecutive);

        if (integer == 1) {
            task.set_Stream(streamtype.err, new mosek.Stream() {
                @Override
                public void stream(String string) {
                    System.out.print(string);

                }
            });
        }

    }
    //--------------------------------------------------------------------------
    // Methods
    //--------------------------------------------------------------------------

    /**
     * Builds a (MI)SOCP formulation of a MIQP. <br>
     *
     * @param constant Constant in the objective. <br>
     * @param contCost Linear coefficients for the continuous variables. <br>
     * @param indCost Linear coefficients for the discrete variables. <br>
     * @param diag Diagonal quadratic term. <br>
     * @param factorization Factorization of the matrix. <br>
     * @param pos Position for the strengthening. <br>
     * @param diagram Decision diagram to use in the formulation.
     */
    private void buildInstanceQP(double constant, double[] contCost, double[] indCost, double[] diag, double[][] factorization, int pos,
            DecisionDiagram diagram, int consecutive) {
        int n = contCost.length;
        varIndex = 0;
        x = new int[n];
        z = new int[n];
        task.appendvars(2 * n); // Continuous + discrete variables
//        task.appendcons(2 * n); // Big M constraints
        task.putcfix(constant);

        // Adds variables
        for (int i = 0; i < n; i++) {
            x[i] = varIndex;
            task.putvarbound(x[i], boundkey.fr, -0.0, +0.0);
            task.putvarname(x[i], "x" + i);
            task.putcj(x[i], contCost[i]);
            varIndex++;

            z[i] = varIndex;
            task.putvarbound(z[i], boundkey.ra, 0.0, 1.0);
            if (integer == 1) {
                task.putvartype(z[i], mosek.variabletype.type_int);

            }
            task.putvarname(z[i], "z" + i);
            task.putcj(z[i], indCost[i]);
            varIndex++;
        }

        if (consecutive > 1) {
            int w; // Index of the additional variables
            // First index
            task.appendvars(1);
            w = varIndex;
            task.putvarbound(w, boundkey.ra, 0, 1);
            task.putvartype(w, mosek.variabletype.type_int);
            task.putvarname(w, "w" + 0);
            varIndex++;
            task.appendcons(1);
            task.putarow(conIndex, new int[]{z[0], w}, new double[]{1, -1});
            task.putconbound(conIndex, boundkey.up, -0.0, 0);
            task.putconname(conIndex, "Start_spike" + 0);
            conIndex++;

            task.appendcons(consecutive - 1);
            for (int j = 1; j < 1 + consecutive - 1; j++) {
                task.putarow(conIndex, new int[]{z[j], w}, new double[]{-1, 1});
                task.putconbound(conIndex, boundkey.up, -0.0, 0);
                task.putconname(conIndex, "Keep_spike" + 0 + "," + j);
                conIndex++;
            }

            // Middle indexes
            for (int i = 1; i + consecutive <= n; i++) {
                task.appendvars(1);
                w = varIndex;
                task.putvarbound(w, boundkey.ra, 0, 1);
                task.putvartype(w, mosek.variabletype.type_int);
                task.putvarname(w, "w" + i);
                varIndex++;
                task.appendcons(1);
                task.putarow(conIndex, new int[]{z[i], z[i - 1], w}, new double[]{1, -1, -1});
                task.putconbound(conIndex, boundkey.up, -0.0, 0);
                task.putconname(conIndex, "Start_spike" + i);
                conIndex++;

                task.appendcons(consecutive - 1);
                for (int j = i + 1; j < i + consecutive; j++) {
                    task.putarow(conIndex, new int[]{z[j], w}, new double[]{-1, 1});
                    task.putconbound(conIndex, boundkey.up, -0.0, 0);
                    task.putconname(conIndex, "Keep_spike" + i + "," + j);
                    conIndex++;
                }
            }

            // Last indexes
            for (int i = n - consecutive + 1; i < n; i++) {

                task.appendcons(1);
                task.putarow(conIndex, new int[]{z[i], z[i - 1]}, new double[]{1, -1});
                task.putconbound(conIndex, boundkey.up, -0.0, 0);
                task.putconname(conIndex, "No_spike" + i);
                conIndex++;

            }

        }

        s = new int[factorization.length];
        for (int i = pos; i < factorization.length; i++) {
            task.appendvars(3);

            // epigraph variable
            s[i] = varIndex;
            task.putvarbound(varIndex, boundkey.lo, 0.0, +0.0);
            task.putvarname(varIndex, "s" + i);
            task.putcj(varIndex, 1);

            // constant variable
            task.putvarbound(varIndex + 1, boundkey.fx, 0.5, 0.5);
            task.putvarname(varIndex + 1, "k" + i);

            // linear term variable
            task.putvarbound(varIndex + 2, boundkey.fr, -0.0, +0.0);
            task.putvarname(varIndex + 2, "r" + i);
            task.appendcons(1);
            for (int j = 0; j < n; j++) {
                task.putaij(conIndex, x[j], factorization[j][i]);
            }
            task.putaij(conIndex, varIndex + 2, -1);
            task.putconbound(conIndex, boundkey.fx, 0, 0);
            conIndex++;

            task.appendcone(conetype.rquad, 0, new int[]{varIndex, varIndex + 1, varIndex + 2});
            varIndex += 3;
        }
        if (diag[0] > 0) {
            persp = new int[n];
        }

        for (int i = 0; i < n; i++) {
            if (diag[i] > 0) {

                task.appendvars(1);
                // epigraph variable
                persp[i] = varIndex;
                task.putvarbound(varIndex, boundkey.lo, 0.0, +0.0);
                task.putcj(varIndex, 2 * diag[i]);
                task.putvarname(varIndex, "p" + i);

                task.appendcone(conetype.rquad, 0, new int[]{varIndex, z[i], x[i]});
                varIndex++;
            }
//            else {
//
//                task.appendcons(2);
//                task.putarow(conIndex, new int[]{x[i], z[i]}, new double[]{1, -1000});
//                task.putconbound(conIndex, boundkey.up, 0, 0);
//                task.putarow(conIndex + 1, new int[]{x[i], z[i]}, new double[]{1, 1000});
//                task.putconbound(conIndex + 1, boundkey.lo, 0, 0);
//                conIndex += 2;
//            }
        }

        if (diagram != null) {

            int[] r = new int[pos];
            for (int i = 0; i < pos; i++) {
                task.appendvars(1);

                // epigraph variable
//                task.putvarbound(varIndex, boundkey.lo, 0.0, +0.0);
//                task.putvarname(varIndex, "s" + i);
//                task.putcj(varIndex, 1);
                // constant variable
//                task.putvarbound(varIndex + 1, boundkey.fx, 0.5, 0.5);
//                task.putvarname(varIndex + 1, "k" + i);
                // linear term variable
                r[i] = varIndex;
                task.putvarbound(r[i], boundkey.fr, -0.0, +0.0);
                task.putvarname(r[i], "rho" + i);
                task.appendcons(1);
                for (int j = 0; j < n; j++) {
                    task.putaij(conIndex, x[j], factorization[j][i]);
                }
                task.putaij(conIndex, r[i], -1);
                task.putconbound(conIndex, boundkey.fx, 0, 0);
                conIndex++;

//                task.appendcone(conetype.rquad, 0, new int[]{varIndex, varIndex + 1, varIndex + 2});
                varIndex++;
            }

            // Adds flow conservation constraints
            task.appendcons(diagram.nodes.size());
            Map<Integer, Integer> nodeMap = new HashMap<>();
            for (NodeLight node : diagram.nodes) {
                if (node.ID == diagram.sourceId) {
                    task.putconbound(conIndex, boundkey.fx, 1, 1);
                } else if (node.ID == diagram.sinkId) {
                    task.putconbound(conIndex, boundkey.fx, -1, -1);
                } else {
                    task.putconbound(conIndex, boundkey.fx, 0, 0);
                }
                nodeMap.put(node.ID, conIndex);
                conIndex++;
            }

            // Adds constraints linking arcs and continuous variables
            task.appendcons(3 * n);
            int[] rConstr = new int[r.length];
            int[] zConstrLb = new int[n];
            int[] zConstrUb = new int[n];

            for (int i = 0; i < r.length; i++) {
                rConstr[i] = conIndex;
                task.putconbound(rConstr[i], boundkey.fx, 0, 0);
                task.putaij(rConstr[i], r[i], -1);
                conIndex++;
            }

            for (int i = 0; i < n; i++) {

                zConstrLb[i] = conIndex;
                task.putconbound(zConstrLb[i], boundkey.lo, 1, +0.0);
                task.putaij(zConstrLb[i], z[i], 1);
                conIndex++;

                zConstrUb[i] = conIndex;
                task.putconbound(zConstrUb[i], boundkey.up, 0.0, 0);
                task.putaij(zConstrUb[i], z[i], 1);
                conIndex++;
            }

            // Builds the formulation
            int tau, y, rank;
            int[] w;

            for (Arc arc : diagram.arcs) {

//                System.out.print(arc.tail + "," + arc.head + "="+arc.arcVal+":\t");
//                if (arc.vars == null) {
//                    System.out.println(arc.varIndex+".");
//                } else {
//                    for (Integer var : arc.vars) {
//                        System.out.print(var + " ");
//                    }
//                    System.out.println("");
//                }
                task.appendvars(2);
                tau = varIndex;
                task.putvarbound(tau, boundkey.lo, 0.0, +0.0);
                task.putcj(tau, 2);
                task.putvarname(tau, "t" + arc.tail + "," + arc.head);
                varIndex++;

                y = varIndex;
                task.putvarbound(y, boundkey.ra, 0.0, +1.0);
                task.putaij(nodeMap.get(arc.head), y, -1);
                task.putaij(nodeMap.get(arc.tail), y, 1);

                task.putvarname(tau, "t" + arc.tail + "," + arc.head);
                varIndex++;
                if (arc.uVector != null) {
                    rank = arc.uVector.length;
                    w = new int[rank + 2];
                    task.appendvars(rank);
                    w[0] = tau;
                    w[1] = y;
                    for (int i = 0; i < rank; i++) {
                        w[i + 2] = varIndex;
                        task.putvarbound(w[i + 2], boundkey.fr, -0.0, 0.0);
                        for (int j = 0; j < r.length; j++) {
//                            System.out.println(j + " " + i);
                            task.putaij(rConstr[j], w[i + 2], arc.uVector[i][j]);
                        }
                        varIndex++;
                    }

                    task.appendcone(conetype.rquad, 0, w);
                }

                //Link path and discrete variables
                if (arc.arcVal == 0) {
                    task.putaij(zConstrLb[arc.varIndex], y, 1);
                } else if (arc.arcVal == 1) {
//                        task.putcj(y, indCost[arc.varIndex]);
                    task.putaij(zConstrUb[arc.varIndex], y, -1);
                }

            }
        }

    }

    /**
     * Builds a (MI)SOCP formulation of a MIQP. <br>
     *
     * @param constant Constant in the objective. <br>
     * @param contCost Linear coefficients for the continuous variables. <br>
     * @param indCost Linear coefficients for the discrete variables. <br>
     * @param diag Diagonal quadratic term. <br>
     * @param factorization Factorization of the matrix. <br>
     * @param pos Position for the strengthening. <br>
     * @param diagram Decision diagram to use in the formulation.
     */
    private void buildInstanceQP(double constant, double[] contCost, double[] indCost, DecisionDiagram diagram) {
        int n = contCost.length;
        varIndex = 0;
        x = new int[n];
        z = new int[n];
        task.appendvars(2 * n); // Continuous + discrete variables
//        task.appendcons(2 * n); // Big M constraints
        task.putcfix(constant);

        // Adds variables
        for (int i = 0; i < n; i++) {
            x[i] = varIndex;
            task.putvarbound(x[i], boundkey.fr, -0.0, +0.0);
            task.putvarname(x[i], "x" + i);
            task.putcj(x[i], contCost[i]);
            varIndex++;

            z[i] = varIndex;
            task.putvarbound(z[i], boundkey.ra, 0.0, 1.0);
//            if (i<=5 || i== 7 || i==8)
//            {
//                 task.putvarbound(z[i], boundkey.fx, 1.0, 1.0);
//            }
            if (integer == 1) {
                task.putvartype(z[i], mosek.variabletype.type_int);

            }

            task.putvarname(z[i], "z" + i);
            task.putcj(z[i], indCost[i]);
            varIndex++;
        }

        if (diagram != null) {

            // Adds flow conservation constraints
            task.appendcons(diagram.nodes.size());
            Map<Integer, Integer> nodeMap = new HashMap<>();
            for (NodeLight node : diagram.nodes) {
                if (node.ID == diagram.sourceId) {
                    task.putconbound(conIndex, boundkey.fx, 1, 1);
                } else if (node.ID == diagram.sinkId) {
                    task.putconbound(conIndex, boundkey.fx, -1, -1);
                } else {
                    task.putconbound(conIndex, boundkey.fx, 0, 0);
                }
                nodeMap.put(node.ID, conIndex);
                task.putconname(conIndex, "Flow conservation " + node.ID);
                conIndex++;
            }

            // Adds constraints linking arcs and continuous variables
            task.appendcons(2 * n);
            int[] xConstr = new int[x.length];
            int[] zConstrEq = new int[n];

            for (int i = 0; i < x.length; i++) {
                xConstr[i] = conIndex;
                task.putconbound(xConstr[i], boundkey.fx, 0, 0);
                task.putaij(xConstr[i], x[i], -1);
                task.putconname(xConstr[i], "XeqConstr" + i);
                conIndex++;
            }

            for (int i = 0; i < n; i++) {

//                zConstrLb[i] = conIndex;
//                task.putconbound(zConstrLb[i], boundkey.lo, 1, +0.0);
//                task.putaij(zConstrLb[i], z[i], 1);
//                conIndex++;
                zConstrEq[i] = conIndex;
                task.putconbound(zConstrEq[i], boundkey.fx, 0.0, 0);
                task.putaij(zConstrEq[i], z[i], 1);
                task.putconname(zConstrEq[i], "ZLinkingConstr" + i);
                conIndex++;
            }

            // Builds the formulation
            int tau, y, rank;
            int[] w;

            for (Arc arc : diagram.arcs) {
                task.appendvars(2);
                tau = varIndex;
                task.putvarbound(tau, boundkey.lo, 0.0, +0.0);
                task.putcj(tau, 2);
                task.putvarname(tau, "t" + arc.tail + "," + arc.head);
                varIndex++;

                y = varIndex;
                task.putvarbound(y, boundkey.lo, 0.0, +1.0);
                task.putaij(nodeMap.get(arc.head), y, -1);
                task.putaij(nodeMap.get(arc.tail), y, 1);
                task.putvarname(y, "y" + arc.tail + "," + arc.head);
                varIndex++;
                if (arc.uVector != null) {
                    rank = arc.uVector.length;
                    w = new int[rank + 2];
                    task.appendvars(rank);
                    w[0] = tau;
                    w[1] = y;
                    for (int i = 0; i < rank; i++) {
                        w[i + 2] = varIndex;
                        task.putvarbound(w[i + 2], boundkey.fr, -0.0, 0.0);
                        task.putvarname(w[i + 2], "w" + arc.tail + "," + arc.head + "," + i);
                        for (int j = 0; j < x.length; j++) {
//                            System.out.println(j + " " + i);
                            task.putaij(xConstr[j], w[i + 2], arc.uVector[i][j]);
                        }
                        varIndex++;
                    }

                    task.appendcone(conetype.rquad, 0, w);
                }

                //Link path and discrete variables
                if (arc.arcVal == 1) {
//                        task.putcj(y, indCost[arc.varIndex]);
                    task.putaij(zConstrEq[arc.varIndex], y, -1);
                }

            }
        }

    }

    public double solve(double[] xSol, double[] zSol) {

        task.optimize();
        task.getdouinf(mosek.dinfitem.optimizer_time);

        mosek.solsta[] solsta = new mosek.solsta[1];

        task.getsolsta(integer == 1 ? mosek.soltype.itg : mosek.soltype.itr, solsta);
        System.out.println("Status: " + solsta[0]);
        switch (solsta[0]) {
            case optimal, integer_optimal -> {
                //case near_integer_optimal:
                double[] solOpt = new double[varIndex];
//                    double[] R = new double[n*(n+1)/2];

                task.getxxslice(integer == 1 ? mosek.soltype.itg : mosek.soltype.itr, 0, varIndex, solOpt);
//                    task.getbarxj(mosek.soltype.itr, /* Request the interior solution. */
//                            0,
//                            R);
                for (int i = 0; i < xSol.length; i++) {
                    xSol[i] = solOpt[x[i]];
                    zSol[i] = solOpt[z[i]];
                }
//                for (int i = 0; i < solOpt.length; i++) {
//                    if(Math.abs(solOpt[i])>1e-5)
//                    System.out.println(task.getvarname(i)+" "+solOpt[i]);
//                            
//                }

                return task.getprimalobj(integer == 1 ? mosek.soltype.itg : mosek.soltype.itr);
            }
            case dual_infeas_cer, prim_infeas_cer -> {
                //case near_dual_infeas_cer:
                //case near_prim_infeas_cer:
                System.out.println("Primal or dual infeasibility certificate found.");
                return -1;
            }
            case unknown -> {
                System.out.println("The status of the solution could not be determined.");
                return -1;
            }
            default -> {
                System.out.println("Other solution status.");
                return -1;
            }
        }
        //case near_optimal:
    }

}
