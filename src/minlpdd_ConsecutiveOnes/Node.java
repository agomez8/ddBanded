package minlpdd_ConsecutiveOnes;

import java.text.DecimalFormat;
import java.util.List;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.OpenMapRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

/**
 *
 * @author lozanolo
 */
public class Node {

    final static double EPSILON = 1e-5;

    static int n, k, numC;

    static double[] b;

    static double minB;

    static RealMatrix Q, QInv;

    static int improvement; // How to evaluate the improvement: 0-closed form, 1-dive right

    String key;                 //String representation
    int ID;
    
    RealMatrix Sigma;  // State variable
    int[] zvalues;              //Current zvalues: 0,1, or 9
    int countC;                 // Number of consecutive 1s. If greater than or equal to numC, we record numC
    double aCost, cCost; // Current costs 
    

    //Constructor for new node
    public Node() {
        key = "";
        ID = 0;
        countC = 0;
    }

    //Constructor for new node as a copy of other node
    Node(Node node) {
        key = "";
 
        zvalues = node.zvalues.clone();
        countC = node.countC;
    }

    /**
     * Initializes the matrices, to be called at the root node.
     */
    public void initRoot() {
        n = AlgHandler.n;
        k = AlgHandler.k;
        Q = AlgHandler.QMatrix;
        numC = AlgHandler.numC;
        
        //QInv = new CholeskyDecomposition(Q).getSolver().getInverse();
        b = AlgHandler.c;
        minB = Double.POSITIVE_INFINITY;
        for (double val : b) {
            minB = Math.min(minB, val);
        }
        AlgHandler.a = new ArrayRealVector(AlgHandler.atilde);

        zvalues = new int[n];
        for (int i = 0; i < n; i++) {
            zvalues[i] = 9; //Because Gasmor loves 9 for some reason...
        }
        double[][] I = new double[k][k];
        for (int i = 0; i < k; i++) {
            I[i][i] = 1;
        }
//        System.out.println(I.length);
        Sigma = new OpenMapRealMatrix(n, n);

        aCost = 0;
        cCost = 0;

    }

    /**
     * Creates a zero node. <br>
     *
     * @param i Variable fixed to 0. <br>
     * @param relevant List of relevant columns to keep. <br>
     * @return The "zero" node.
     */
    public Node createZeroNode(int i, List<Integer> relevant) {
        Node node = new Node(this);
        DecimalFormat format = new DecimalFormat("0.00000");
        node.key = "[";
        node.zvalues[i] = 0;
        node.countC = 0;        //After using a 0 arc the count resets to 0!

        node.Sigma = new OpenMapRealMatrix(n, n);
        for (Integer var : relevant) {
            node.Sigma.setColumnVector(var, Sigma.getColumnVector(var));
            for (int j = 0; j < node.Sigma.getColumnDimension(); j++) {
                if(Math.abs(node.Sigma.getEntry(var, j)) > 1e-5){
                    node.key+=" ("+var+","+j+","+format.format(node.Sigma.getEntry(var,j))+") ";
                }
            }
        }
        node.key += "]";
        node.key += " "+node.countC;
        node.aCost = aCost;
        node.cCost = cCost;

        return node;
    }

    /**
     * Creates a one node. <br>
     *
     * @param i Variable fixed to 0. <br>
     * @param relevant List of relevant variables. <br>
     * @param u Array to store vector U. <br>
     * @return The "one" node.
     */
    public Node createOneNode(int i, List<Integer> relevant, double[] u) {
        Node node = new Node(this);
        DecimalFormat format = new DecimalFormat("0.00000");
        node.key = "[";
        node.zvalues[i] = 1;
        if(node.countC < numC){node.countC++;}  //Increase the count of consecutive 1s unless already equal to numC
        RealVector q = Q.getColumnVector(i);
        RealVector SigmaQ = Sigma.operate(q);
        double qSigmaQ = SigmaQ.dotProduct(q);
        double denominator = Math.sqrt(q.getEntry(i) - qSigmaQ);
        SigmaQ.setEntry(i, -1);
        SigmaQ.mapDivideToSelf(denominator);
        System.arraycopy(SigmaQ.toArray(), 0, u, 0, n);
        RealMatrix newSigma = Sigma.add(SigmaQ.outerProduct(SigmaQ));
        node.Sigma = new OpenMapRealMatrix(n, n);
        for (Integer var : relevant) {
            node.Sigma.setColumnVector(var, newSigma.getColumnVector(var));
            for (int j = 0; j < node.Sigma.getColumnDimension(); j++) {
                if(Math.abs(node.Sigma.getEntry(var, j)) > 1e-5){
                    node.key+=" ("+var+","+j+","+format.format(node.Sigma.getEntry(var,j))+") ";
                }
            }
        }
        node.key += "]";
        node.key += " "+node.countC;
        double gain = -0.25 * Math.pow(SigmaQ.dotProduct(AlgHandler.a), 2);
        node.aCost = aCost + gain;


        node.cCost = cCost + b[i];

        return node;
    }

    

    public void print() {
        System.out.println(key);
        System.out.println();
    }


   

    /**
     * Returns the cost of the feasible solution obtained by setting everything
     * to 0. <br>
     *
     * @return cost.
     */
    public double getCost() {
        return aCost + cCost + AlgHandler.constant;
    }
   
    /**
     * Updates the cost of this node. <br>
     * @param node The comparison node.
     */
    public void updateCost(Node node)
    {
        if(aCost+cCost>node.aCost+node.cCost)
        {
            aCost=node.aCost;
            cCost=node.cCost;
        }
    }


  
    /**
     * Check whether the consectuve ones constraint is satisfied. <br>
     * @param countC The cuurent number of consecutive ones. <br>
     * @return  True if the constraint is satisfied. 
     */
    boolean checkConsecutiveOnes(int countC) {
        if(countC == 0){
            return true;
        }
        return countC >= numC;
    }

}
