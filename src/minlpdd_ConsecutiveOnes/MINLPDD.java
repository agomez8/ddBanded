package minlpdd_ConsecutiveOnes;

import instance.QP_Instance;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import mosek.dinfitem;

/**
 *
 * @author lozanolo
 */
public class MINLPDD {

    /**
     * @param args the command line arguments. args[0]: Path to the file. <br>
     * args[1]: Number of time periods. <br>
     * args[2]: L0 regularization parameter. <br>
     * args[3]: L2 regularization parameter. <br>
     * args[4]: kernel (bandwidth). <br>
     * args[5]: Number of consecutive ones numC. <br>
     * args[6]: Seed. <br>
     * args[7]: Method: 0-mosek, 1-dd. <br>
     * @throws java.io.IOException 
     *
     */
    public static void main(String[] args) throws IOException {
        // Create alg and read data
        DecimalFormat format = new DecimalFormat("0.00");
        String path = args[0];
        int timePeriods = Integer.parseInt(args[1]);
        double L0regularization = Double.parseDouble(args[2]);
        double L2regularization = Double.parseDouble(args[3]);
        int method = Integer.parseInt(args[7]);
        int kernel = Integer.parseInt(args[4]);
        int numC = Integer.parseInt(args[5]);
        int seed = Integer.parseInt(args[6]);
        QP_Instance instance = new QP_Instance(path, timePeriods, kernel, L0regularization,
                L2regularization, numC, seed);
        

        // Solves continuous relaxation
//        DefaultMosek mosek ;
        double[] xSol = new double[instance.n], zSol = new double[instance.n];
        //Orders variables in natural order
        for (int i = 0; i < zSol.length; i++) {
            zSol[i] = -i;
        }

//        double[][] Ffull = instance.computeEigendecomposition(instance.Q);
        DefaultMosek solver = null;
        double time;
        double[] output = new double[15];
        if (method == 0) {
            double[][] Ffull = instance.computeEigendecomposition(instance.Q);
            System.out.println("here");

            solver = new DefaultMosek(instance.constant, instance.a, instance.c, instance.diagonal, Ffull, 0,
                    null, 1, numC);

            output[0] = 0;
            output[1] = 0;
        } else if (method >= 1) {

            double[] c = new double[instance.n]; // vector of cost for discrete variables

            for (int i = 0; i < instance.n; i++) {
                c[i] = L0regularization;
                instance.Q[i][i] += instance.diagonal[i];
            }
            instance.computeEigendecomposition(instance.Q);
            output[12]=instance.truncation;
            output[13]=Math.pow(2, instance.truncation);
            output[14]=instance.condNumber;

            AlgHandler alg = new AlgHandler();
            Node.improvement = method - 1;

            alg.regressionData(instance.n, instance.a.length, instance.a, c, instance.Q, instance.constant, instance.numC);

            // Build DD
            time = System.currentTimeMillis();

            alg.buildDD(zSol);
            if(alg.reachLimit)
            {
                output[0]=-1;
                output[1]=-1;
                output[2]=AlgHandler.TIME_LIMIT;
                output[3] = AlgHandler.TIME_LIMIT;
                exportSolution(args, output);
                return;
            }
            time = (System.currentTimeMillis() - time) / 1000.0;
            output[0] = alg.DD.nodes.size();
            output[1] = alg.DD.arcs.size();
            output[2] = time;
            output[11] = alg.maxQueue;

            int[] sol = new int[instance.n];
            time = System.currentTimeMillis();
            double opt = alg.solveSP(sol);
//            double opt = alg.solveSP(sol,instance.a,beta);
            time = (System.currentTimeMillis() - time) / 1000.0;
            output[3] = time;
            double solDouble[] = new double[sol.length];
            output[5] = opt;
//            System.out.println("Sol DD");
            int count = 0;
            for (int i = 0; i < sol.length; i++) {
                solDouble[i] = sol[i];
                count += sol[i];
//                System.out.print(format.format(solDouble[i]) + " ");
            }
//            System.out.println("");
            if (count >= 1) {
                output[6] = instance.computeObj(solDouble, method);
            }
            else
            {
                output[6] = instance.constant;
            }

            solver = new DefaultMosek(instance.constant, instance.a, instance.c, alg.DD, 0);


        }

        xSol = new double[instance.n];
        zSol = new double[instance.n];
        System.out.println("Solving relaxation");
        time = System.currentTimeMillis();

        double opt = solver.solve(xSol, zSol);
        time = (System.currentTimeMillis() - time) / 1000.0;
        output[4] = time;

        output[7] = opt;

        System.out.println("Obj Val: " + opt);
        System.out.println("xSol:");

        for (double d : xSol) {
            System.out.print(format.format(d) + " ");
        }
        System.out.println("");
        System.out.println("zSol:");
        int count=0;
        for (double d : zSol) {
            count+=d>0.5?1:0;
            System.out.print(format.format(d) + " ");
        }
        System.out.println("");
        if(count>=1)
            output[8] = instance.computeObj(zSol, method);
        else
            output[8] = instance.constant;
        if (method == 0) {
            output[9] = solver.task.getintinf(mosek.iinfitem.mio_num_branch);
            output[10]=solver.task.getdouinf(dinfitem.mio_obj_rel_gap);
        }

        exportSolution(args, output);

        
//        System.out.println("Verification Mosek: " + instance.computeObj(zSol, method));
    }

    static void exportSolution(String[] params, double[] optResults) throws IOException {

        try ( FileWriter out = new FileWriter(new File("./results/resultsOffline.csv"), true)) {
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
