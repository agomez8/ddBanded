/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package minlpdd_ConsecutiveOnes;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 *
 * @author Andres Gomez.
 */
public class FileGeneratorOnline {

    public static void main(String[] args) throws IOException {

        String instance = "java "
                //                + "-Djava.library.path=\"C:/Program Files/IBM/ILOG/CPLEX_Studio1271/cplex/bin/x64_win64" 
                + " -cp ./dist/MINLPDD.jar minlpdd_ConsecutiveOnes.MINLPDDOnline";

        String[] data = new String[]{"./data/daily_data_1990.csv"};

        int[] sizes = new int[]{7022};

        double[] l0s = new double[]{0.001, 0.005, 0.01, 0.02, 0.05, 0.1};
        double[] l2s = new double[]{0.25, 0.5, 1.0, 2.0, 5.0};

        int[] kernels = new int[]{2, 3};
        int[] consecutives = new int[]{0};
        int[] horizons = new int[]{200};
        int[] seeds = new int[]{101, 102, 103, 104, 105};

        int[] methods = new int[]{1};

        try ( FileWriter out = new FileWriter(new File("./runDDOnline.bat"))) {
            for (String dat : data) {
                for (int size : sizes) {
                    for (double l0 : l0s) {
                        for (double l2 : l2s) {
                            for (int kernel : kernels) {
                                for (int consecutive : consecutives) {
                                    for (int horizon : horizons) {
                                        for (int seed : seeds) {
                                            for (int method : methods) {

                                                out.write(instance + " " + dat + " " + size + " "
                                                        + l0 + " " + l2 + " " + kernel + " " + consecutive
                                                        + " " + horizon + " " + seed + " " + method + "\n");
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

            }

        }
    }
}
