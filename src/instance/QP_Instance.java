/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package instance;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import mosek.Env;
import mosek.uplo;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.QRDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

/**
 * A QP instance.
 *
 * @author AGomez
 */
public class QP_Instance {

    //--------------------------------------------------------------------------
    // Constants
    //--------------------------------------------------------------------------
 

    //--------------------------------------------------------------------------
    // Attributes
    //--------------------------------------------------------------------------
    /**
     * Dimensions.
     */
    public int n, k;
    
    public int numC;
    
    public int bandwidth, truncation;

    /**
     * Double constant
     */
    public double constant;

    /**
     * Cost coefficients
     */
    public double[] a, c;

    /**
     * Model matrix, mxn dimensional matrix.
     */
    public double[][] Q, A;

    /**
     * Diagonal quadratic term.
     */
    public double[] diagonal;
    
    public double gamma, C,condNumber;

    /**
     * Mosek environment. <br>
     */
    Env env;
    
    String[] dates, varNames;
    
    double[][] data; // rows=asset, column=time period

    //--------------------------------------------------------------------------
    // Constructor
    //--------------------------------------------------------------------------
// rows=asset, column=time period

    //--------------------------------------------------------------------------
    // Constructor
    //--------------------------------------------------------------------------
 
   
    
    /**
     * Constructs a linear regression model from a file.<br>
     *
     * @param fileName The path to the file. <br>
     * @param periods Number of time periods to use. <br>
     * @param kernel Width of the kernel. <br>
     * @param L0regularization L0 regularization to use. <br>
     * @param L2regularization L2 regularization to use. <br>
     * @param numC numC Minimum number of consecutive ones (0= no constraint). <br>
     * @param seed Seed to use. 
     */
    public QP_Instance(String fileName, int periods, int kernel, double L0regularization, double L2regularization, int numC, int seed) {
        env = new Env();
//        buildTimeSeries(fileName, kernel, L2regularization);
        loadFinanceTimeSeries(fileName, periods);
        Random random= new Random(seed);
        int index=random.nextInt(data.length);
        System.out.println("Loading data from "+varNames[index]);
        buildFinanceModel(index, kernel, L2regularization);
        diagonal = new double[n];
        c = new double[n];
        bandwidth=kernel;
        for (int i = 0; i < n; i++) {
            c[i] = L0regularization;
            diagonal[i] = 1;
        }
        this.numC=numC;

    }
    //--------------------------------------------------------------------------
    // Methods
    //--------------------------------------------------------------------------

     /**
     * Builds the model from data. <br>
     *
     * @param fileName File to read from.
     */
    private void buildFinanceModel(int index, int k, double reg) {
        double[] y = data[index];
        int m = y.length; // Number of rows
        n = m;
         // Response variable


        double mean = 0;
        for (int i = 0; i < m; i++) {
            mean += y[i];
        }
        mean /= (double) m;
        for (int i = 0; i < m; i++) {
            y[i] -= mean;
        }

        // 1 norm
        double var = 0;
        for (int i = 0; i < m; i++) {
            var += y[i] * y[i];
        }
        for (int i = 0; i < m; i++) {
            y[i] /= Math.sqrt(var);
//            y[i] /= Math.sqrt(var/(double)(m));
        }

        // Creates cost parameter
        for (int i = 0; i < m; i++) {
//            System.out.println("y: "+y[i]*y[i]);
            constant += y[i] * y[i];

        }

        a = new double[n];
        Q = new double[n][n];
        int d;
        for (int i = 0; i < n; i++) {
            Q[i][i] += reg;
            a[i] = -2 * y[i];
            d = Math.min(k, i);
            for (int j = i - d; j < i; j++) {
                Q[i][j] -= reg / (double) d;
                Q[j][i] -= reg / (double) d;
                Q[j][j] += reg / (double) (d * d);
                for (int l = j + 1; l < i; l++) {
                    Q[j][l] += reg / (double) (d * d);
                    Q[l][j] += reg / (double) (d * d);
                }
            }

        }

    }
    
    /**
     * Processes the data file. <br>
     *
     * @param fileName File to read from. <br>
     * @param timePeriods Number of desired time periods.
     */
    private void loadFinanceTimeSeries(String fileName, int timePeriods) {
        BufferedReader br = null;
        String cvsSplitBy = ",";
        String line;
        String[] row;
        

        try {
            br = new BufferedReader(new FileReader(fileName));
            row = br.readLine().split(cvsSplitBy); // First line contains headers.
            String[] datesFull = new String[row.length - 2];
            for (int i = 2; i < row.length; i++) {
                datesFull[i - 2] = row[i];
            }
            List<String> varsList = new ArrayList<>();
            List<double[]> series = new ArrayList<>();
            int counter = 0;
//            while ((line = br.readLine()) != null && counter<2) {
            while ((line = br.readLine()) != null) {
                row = line.split(cvsSplitBy);
                String var = row[1];
                double[] serie = new double[row.length - 2];
                for (int i = 2; i < row.length; i++) {
                    serie[i - 2] = Double.parseDouble(row[i]);
                }
                varsList.add(var);
                series.add(serie);
                counter++;
            }
            int numObservations=datesFull.length/timePeriods;
            dates= new String[timePeriods];

            data = new double[series.size()][timePeriods];

            for (int i = 0; i < series.size(); i++) {
                for (int j = 0; j < timePeriods; j++) {
                    if(i==0)
                    {
                        dates[j]=datesFull[j*numObservations+numObservations/2];
                    }
                    for (int l = 0; l < numObservations; l++) {
                        data[i][j] += series.get(i)[j*numObservations+l]/((double)numObservations);
                    }             
                }
            }

            varNames = new String[varsList.size()];
            for (int i = 0; i < varNames.length; i++) {
                varNames[i] = varsList.get(i);

            }

        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                }
            }
        }
    }
    /**
     * Computes the eigendecomposition of the matrix Q.<br>
     *
     * @param Q Matrix Q to compute the eigendecomposition of.
     * @return Matrix F with eigenvectors*eigenvalues.
     */
    public double[][] computeEigendecomposition(double[][] Q) {
        double[] matrix = new double[n * n];
        double Qmax=0;
        for (int i = 0; i < n; i++) {

            for (int j = 0; j < n; j++) {
                matrix[i * n + j] = Q[i][j];
                if (i == j) {
//                    matrix[i * n + j] += diagonal[i];
                }
                Qmax=Math.max(Qmax, Q[i][j]);
            }
        }
        double[] eValues = new double[n];
        env.syevd(uplo.lo, n, matrix, eValues);
        System.out.println(eValues[0]);
        System.out.println(eValues[n-1]);
        condNumber=eValues[n-1]/eValues[0];
        
        gamma=(Math.sqrt(condNumber)-1)/(Math.sqrt(condNumber)+1);
        double denom=1-Math.pow(gamma,1/((double)bandwidth));
        double C0=Math.max(1, (Math.sqrt(condNumber)+1)*(Math.sqrt(condNumber)+1)/(2*condNumber))/eValues[0];
        double C1=Qmax*C0*C0/(denom*denom);
        C=C0*Qmax*(2*C0+2*C1+C0*C1*Qmax)*Math.pow(gamma,1/((double)bandwidth))/(2*denom*denom);
        double maxA=0;
        for (double d : a) {
            maxA=Math.max(d, maxA);
        }
        truncation=(int)Math.ceil(-bandwidth*Math.log(C*maxA*maxA*n*1e5)/Math.log(gamma));
        System.out.println("gamma= "+gamma);
//        System.out.println("C0= "+C0);
//        System.out.println("C1= "+C1);
//        System.out.println("C= "+ C);
        System.out.println("truncation= "+truncation);

        double[][] F = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                F[j][i] = matrix[(n - 1 - i) * n + j] * Math.sqrt(eValues[(n - 1 - i)]);
            }
        }

//        for (int i = 0; i < n; i++) {
//            Q[i][i]+=diagonal[i];
//        }

        return F;
    }
    
    

    /**
     * Given a vector of binary variables z, computes the best objective. <br>
     * @param z Vector of binary variables. <br>
     * @param method Method used to solve the problem (might require adding back diagonal elements). <br>
     * @return The best objective value. 
     */
    public double computeObj(double[] z, int method) {
        int nonzeros = 0;
        double fixedCost = 0;
        for (int i = 0; i < z.length; i++) {
            if (z[i] > 0.5) {
                nonzeros += 1;
                fixedCost += c[i];
            }
        }
        double[][] reducedMatrix = new double[nonzeros][nonzeros];
        double[] reducedVector = new double[nonzeros];
        int rowIndex = 0, colIndex;
        for (int i = 0; i < Q.length; i++) {
            if (z[i] > 0.5) {
                reducedMatrix[rowIndex][rowIndex] = Q[i][i]+(method==0?diagonal[i]:0);
                reducedVector[rowIndex] = a[i];
                colIndex = rowIndex + 1;
                for (int j = i + 1; j < n; j++) {
                    if (z[j] >0.5) {
                        reducedMatrix[rowIndex][colIndex] = Q[i][j];
                        reducedMatrix[colIndex][rowIndex] = Q[i][j];
                        colIndex++;
                    }
                }
                rowIndex++;
            }
        }
         
        RealMatrix matrix = new Array2DRowRealMatrix(reducedMatrix);
        RealVector vector= new ArrayRealVector(reducedVector);
        RealMatrix inverse = MatrixUtils.inverse(matrix);
        double product=inverse.operate(vector).dotProduct(vector);
        return constant+fixedCost-0.25*product;

    }

}
