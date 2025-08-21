package minlpdd_ConsecutiveOnes;

import instance.QP_Instance;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 *
 * @author lozanolo
 */
public class MINLPDDOnline {

    /**
     * @param args the command line arguments. args[0]: Path to the file. <br>
     * args[1]: Number of time periods. <br>
     * args[2]: L0 regularization parameter. <br>
     * args[3]: L2 regularization parameter. <br>
     * args[4]: kernel. <br>
     * args[5]: Number of consecutive ones numC. <br>
     * args[6]: Horizon. <br>
     * args[7]: Seed. <br>
     * args[8]: Method: 0-mosek, 1-dd. <br>
     * @throws java.io.IOException
     *
     */
    public static void main(String[] args) throws IOException {
// Create alg and read data
        String path = args[0];
        int timePeriods = Integer.parseInt(args[1]);
        double L0regularization = Double.parseDouble(args[2]);
        double L2regularization = Double.parseDouble(args[3]);
        int method = Integer.parseInt(args[8]);
//        int rank = Integer.parseInt(args[4]);
        int kernel = Integer.parseInt(args[4]);
        int numC = Integer.parseInt(args[5]);
        int seed = Integer.parseInt(args[7]);
        int horizon = Integer.parseInt(args[6]);
        QP_Instance instance = new QP_Instance(path, horizon, kernel, L0regularization,
                L2regularization, numC, seed);
        QP_Instance instanceFull = new QP_Instance(path, timePeriods, kernel, L0regularization,
                L2regularization, numC, seed);

        // Solves continuous relaxation
//        DefaultMosek mosek ;
        double[] xSol = new double[instance.n], zSol = new double[instance.n];
        //Orders variables in natural order
        for (int i = 0; i < zSol.length; i++) {
            zSol[i] = -i;
        }
//        double[][] Ffull = instance.computeEigendecomposition(instance.Q);

        double time;
        double[] output = new double[5];
        if (method == 0) {
            System.out.println("Not supported");
        } else if (method >= 1) {

            double[] atilde = instance.a;
            double[] beta = new double[instance.n];

            for (int i = 0; i < instance.n; i++) {
                beta[i] = L0regularization;
                instance.Q[i][i] += instance.diagonal[i];
            }

            AlgHandler alg = new AlgHandler();
            Node.improvement = method - 1;

            alg.regressionData(instance.n, atilde.length, atilde, beta, instance.Q, instance.constant, instance.numC);

            // Build DD
            time = System.currentTimeMillis();

            alg.buildDD(zSol);
            time = (System.currentTimeMillis() - time) / 1000.0;
            output[0] = alg.DD.nodes.size();
            output[1] = alg.DD.arcs.size();
            output[2] = time;
            System.out.println("Time offline = " + time);
            double[] yFull = instanceFull.a;
            int[] sol = new int[horizon];
            double timeTotal = 0;
            System.out.println("Online...");
            for (int i = 0; i + horizon <= timePeriods; i++) {

                double[] aSmall = new double[horizon];
                System.arraycopy(yFull, i, aSmall, 0, horizon);
                time = System.currentTimeMillis();

                alg.solveSP(sol, aSmall, beta);
                timeTotal += (System.currentTimeMillis() - time) / 1000.0;
            }

            output[3] = timeTotal;
            System.out.println("Time online = " + timeTotal);

//            System.out.println(opt);
//            solver = new DefaultMosek(instance.constant, instance.a, instance.c, instance.diagonal, Ffull, rank,
//                    alg.DD, integer);
//            solver = new DefaultMosek(instance.constant, instance.a, instance.c, instance.diagonal, Ffull, 0,
//                    null, integer);

        }

//        xSol = new double[instance.n];
//        zSol = new double[instance.n];
//        System.out.println("Solving relaxation");
//        time = System.currentTimeMillis();
//
//        double opt = solver.solve(xSol, zSol);
//        time = (System.currentTimeMillis() - time) / 1000.0;
//        output[4] = time;
//
//        output[7] = opt;
//
//        System.out.println("Obj Val: " + opt);
//        System.out.println("xSol:");
//
//        for (double d : xSol) {
//            System.out.print(format.format(d) + " ");
//        }
//        System.out.println("");
//        System.out.println("zSol:");
//        int count = 0;
//        for (double d : zSol) {
//            count += d > 0.5 ? 1 : 0;
//            System.out.print(format.format(d) + " ");
//        }
//        System.out.println("");
//        if (count >= 1) {
//            output[8] = instance.computeObj(zSol, method);
//        } else {
//            output[8] = instance.constant;
//        }
//        if (method == 0) {
//            output[9] = solver.task.getintinf(mosek.iinfitem.mio_num_branch);
//            output[10] = solver.task.getdouinf(dinfitem.mio_obj_rel_gap);
//        }
        exportSolution(args, output);

//        System.out.println("Verification Mosek: " + instance.computeObj(zSol, method));
    }

    static void exportSolution(String[] params, double[] optResults) throws IOException {

        try (FileWriter out = new FileWriter(new File("./results/resultsOnline.csv"), true)) {
            for (String param : params) {
                out.write(param + ", ");
            }

            for (double v : optResults) {
                out.write(v + ", ");
            }

            out.write("\n");

        }
    }

}
