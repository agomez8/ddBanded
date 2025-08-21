package minlpdd_ConsecutiveOnes;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import org.apache.commons.math3.linear.OpenMapRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

/**
 *
 * @author lozanolo
 */
public class AlgHandler {

    static int n;          // Number of variables
    static int k;          // Number of row in F
// Number of row in F
    static int numC;

    static double[] atilde; //Transformend coefficients for the x-variables
    static double[] c;  // Coefficients for the z-variables
    static double[][] Q;   // Q matrix
    static RealMatrix QMatrix;
    static double constant;
    static RealVector a;

    public static final int TIME_LIMIT = 1800;
    boolean reachLimit = false;

    DecisionDiagram DD;

    List<Integer> varOrder;
    List<Integer>[] relevantVars; // List of relevant variables by depth

    int maxQueue = 0;

    public void regressionData(int n, int k, double[] atilde, double[] beta, double[][] Q, double constant, int numConsecutive) {
        AlgHandler.n = n;
        AlgHandler.k = k;
        AlgHandler.numC = numConsecutive;

        AlgHandler.atilde = atilde;
        AlgHandler.c = beta;
        AlgHandler.Q = Q;

        AlgHandler.QMatrix = new OpenMapRealMatrix(n, n);
        AlgHandler.QMatrix.setSubMatrix(Q, 0, 0);
        AlgHandler.constant = constant;

        varOrder = new ArrayList<>();

    }

    /**
     * Builds the decision diagram. <br>
     *
     * @param zVal Values from continuous relaxation (to order the variables).
     */
    void buildDD(double[] zVal) {
        // Create DD object
        DD = new DecisionDiagram();

        //Sorts variable in the natural order
        orderingHeuristic(zVal);

        computeRelevantByDepth();

        System.out.println("Relevant by depth");
        for (int i = 0; i < relevantVars.length; i++) {
            System.out.print(i + "\t");
            for (int j = 0; j < relevantVars[i].size(); j++) {
                System.out.print(relevantVars[i].get(j) + " ");
            }
            System.out.println("");
        }

        buildDD(varOrder, DD);
    }

    private void orderingHeuristic(double[] zVal) {
        List<Item> order = new ArrayList<>();
        for (int i = 0; i < zVal.length; i++) {
            order.add(new Item(i, zVal[i]));
        }
        Collections.sort(order);
        for (int i = 0; i < order.size(); i++) {
            varOrder.add(order.get(i).pos);
        }
    }

    /**
     * Identifies the relevant values.
     */
    private void computeRelevantByDepth() {
        relevantVars = new List[n + 1];
        for (int i = 0; i < relevantVars.length; i++) {
            relevantVars[i] = new ArrayList<>();
        }

        int var;
        boolean isZero;
        for (int i = 0; i < n; i++) {
            var = varOrder.get(i);
            isZero = true;
            for (int j = n - 1; j > i; j--) {

                if (Q[var][varOrder.get(j)] != 0) {
                    isZero = false;
                }
                if (!isZero) {
                    relevantVars[j].add(var);
                }
            }

        }
    }

    /**
     * Construct the decision diagram. <br>
     *
     * @param varOrder Order in which to process the variables. <br>
     * @param dd Pointer to the decision diagram to be constructed. <br>
     * @return The decision diagram.
     */
    private DecisionDiagram buildDD(List<Integer> varOrder, DecisionDiagram dd) {
        int nodeCount = 0;
        Queue<Node> currentLayerQueue = new ArrayDeque<>(); //Nodes being processed at some layer
        Queue<Node> nextLayerQueue = new ArrayDeque<>();    //Nodes to be processed at layer+1
        Hashtable<String, Node> nextLayerMap = new Hashtable<>(); //States already created
//        double UB=constant;
        // Create initial node
        Node root = new Node();
        root.initRoot();
        root.ID = nodeCount;
        dd.sourceId = root.ID;
        NodeLight rootL = new NodeLight(root);
        dd.nodes.add(rootL);
        currentLayerQueue.add(root); //Add it to the queue
        nodeCount++;

        // Create final node
        Node sink = new Node();
        sink.key = "END";
        sink.ID = nodeCount;
        dd.sinkId = sink.ID;
        NodeLight sinkL = new NodeLight(sink);
        dd.nodes.add(sinkL);
        dd.lastNode = nodeCount;
        nodeCount++;

        System.out.println("MAXIMUM CONSECUTIVE ONES " + numC);
        long initTime = System.currentTimeMillis();

        //Process nodes in the queue for every position according to the order(layer by layer!)
        for (int layer = 0; layer < n; layer++) {
            int varIndex = varOrder.get(layer);
            System.out.println("LAYER " + layer + " VAR " + varIndex + " QUEUE " + currentLayerQueue.size());
            maxQueue = Math.max(currentLayerQueue.size(), maxQueue);
            if (System.currentTimeMillis() - initTime > TIME_LIMIT * 1000) // Time limit reached
            {
                reachLimit = true;
                System.out.println("Time limit reached");
                return null;
            }
            while (!currentLayerQueue.isEmpty()) {
                Node node = currentLayerQueue.poll();

                ////////////////////////////////////////// Add zero-arc (always feasible)
                Node noHead = node.createZeroNode(varIndex, relevantVars[layer + 1]);  // Create node as copy of parent            
                if (noHead.checkConsecutiveOnes(node.countC)) {
                    if (nextLayerMap.containsKey(noHead.key) == false) { // New state, no merging
                        //Add node to the DD and to the queue for next layer
                        noHead.ID = nodeCount;
                        dd.nodes.add(new NodeLight(noHead));
                        nextLayerQueue.add(noHead);
                        nextLayerMap.put(noHead.key, noHead);
                        nodeCount++;
                        // Create arc
                        Arc noArc = new Arc(varIndex, 0, node.ID, noHead.ID);
                        dd.arcs.add(noArc);

                    } else {// Create arc and point to the existent node
                        Arc noArc = new Arc(varIndex, 0, node.ID, nextLayerMap.get(noHead.key).ID);
                        dd.arcs.add(noArc);
                        nextLayerMap.get(noHead.key).updateCost(noHead);

                        //System.out.println("WEPAAAAA EXISTENT NODE: "+noHead.key);
                    }
                }

                // One arcs don't care about consecutive 1s!!!! 
                
                // Create head node
                double[] u = new double[k];
                Node yesHead = node.createOneNode(varIndex, relevantVars[layer + 1], u);
                //UB = Math.min(UB, yesHead.getCost());
                //System.out.println(UB);

                if (nextLayerMap.containsKey(yesHead.key) == false) {
                    //Add node to the DD and to the queue for next layer
                    yesHead.ID = nodeCount;
                    dd.nodes.add(new NodeLight(yesHead));
                    nextLayerQueue.add(yesHead);
                    nextLayerMap.put(yesHead.key, yesHead);
                    nodeCount++;
                    // Create arc                 
                    Arc arc = new Arc(varIndex, 1, node.ID, yesHead.ID, u);
                    dd.arcs.add(arc);

                } else {
                    Arc arc = new Arc(varIndex, 1, node.ID, nextLayerMap.get(yesHead.key).ID, u);
                    dd.arcs.add(arc);
                    nextLayerMap.get(yesHead.key).updateCost(yesHead);
                    //System.out.println("WEPAAAAA EXISTENT NODE: "+yesHead.key);
                }

                
            }

            currentLayerQueue = nextLayerQueue;
            nextLayerQueue = new ArrayDeque<>();
            nextLayerMap.clear();
            System.gc();
        }
//Add dummy arcs to end node
        while (!currentLayerQueue.isEmpty()) {
            Node node = currentLayerQueue.poll();
            if (node.countC == 0 || node.countC >= numC) { //Check consecutive 1s at the end!
                Arc dummyArc = new Arc(-1, 0, node.ID, dd.sinkId);
                dd.arcs.add(dummyArc);
            }
        }

        for (int i = 0; i < dd.arcs.size(); i++) {
            Arc arc = dd.arcs.get(i);
            dd.nodes.get(arc.tail).out.add(arc);
            dd.nodes.get(arc.head).in.add(arc);
        }

        dd.print2();
        return dd;

    }

   /**
    * Solves the shortest path in the DD. <br>
    * @param ans Array to store the solution <br>
    * @return The optimal objective value.
    */
    double solveSP(int[] ans) {
        // Shortest path algorithm for acyclic networks
        double[] d = new double[DD.nodes.size()];        //Distance label
        Arc[] p = new Arc[DD.nodes.size()];              //Predecessor arc to recover the solution

        //Initialize
        for (int i = 0; i < DD.nodes.size(); i++) {
            d[i] = Double.POSITIVE_INFINITY;
        }

        // Start at the first node with 0 distance
        d[0] = 0;
        p[0] = null;

        // add it to the queue to start the algorithm
        Queue<NodeLight> queue = new ArrayDeque<>();
        queue.add(DD.nodes.get(0));

        // Update the labels for each arc in a topological order
        int[] nodeInQueue = new int[DD.nodes.size()];  // To make sure we don't put a node in queue twice
        while (!queue.isEmpty()) {
            NodeLight node = queue.poll();
            for (int i = 0; i < node.out.size(); i++) {
                Arc arc = node.out.get(i);
                double cost = 0;
                if (arc.varIndex >= 0) {
                    cost = c[arc.varIndex] * arc.arcVal - 0.25 * arc.costCoeff;     //@GASMOR: DOUBLE CHECK THIS COST!!!!
                }
                //Update the label
                if (d[arc.head] > d[node.ID] + cost) {
                    //System.out.println("NEW LABEL FOR "+arc.head+" = " +(d[node.ID]+cost));

                    d[arc.head] = d[node.ID] + cost;
                    p[arc.head] = arc;
                    if (nodeInQueue[arc.head] == 0) {
                        queue.add(DD.nodes.get(arc.head));	//Add head node to the queue if not added before
                        nodeInQueue[arc.head]++;
                    }
                }
            }
        }

        System.out.println("DD Shortest Path: " + (d[DD.lastNode] + constant));
        //Recover the solution
        Arc arc = p[DD.lastNode];
        int[] z = new int[n];
        while (arc != null) {
            //System.out.println( arc.varIndex + " , " + arc.arcVal);
            if (arc.varIndex >= 0) {
                z[arc.varIndex] = arc.arcVal;
            }
            arc = p[arc.tail];
        }
        // Print z variables
        System.out.print("z = [");
        for (int i = 0; i < n; i++) {
            System.out.print(" " + z[i]);
        }
        System.out.println(" ]");
        System.arraycopy(z, 0, ans, 0, n);

        return d[DD.lastNode] + constant;
    }

    /**
     * Solves the shortest path in the decision diagram online. <br>
     * @param ans Array to store the solution. <br>
     * @param a Costs associated with the continuous variables. <br>
     * @param c Costs associated with the discrete variables. <br>
     * @return The optimal objective value.
     */
    double solveSP(int[] ans, double[] a, double[] c) {
        // Shortest path algorithm for acyclic networks
        double[] d = new double[DD.nodes.size()];        //Distance label
        Arc[] p = new Arc[DD.nodes.size()];              //Predecessor arc to recover the solution

        //Initialize
        for (int i = 0; i < DD.nodes.size(); i++) {
            d[i] = Double.POSITIVE_INFINITY;
        }

        // Start at the first node with 0 distance
        d[0] = 0;
        p[0] = null;

        // add it to the queue to start the algorithm
        Queue<NodeLight> queue = new ArrayDeque<>();
        queue.add(DD.nodes.get(0));

        // Update the labels for each arc in a topological order
        int[] nodeInQueue = new int[DD.nodes.size()];  // To make sure we don't put a node in queue twice
        while (!queue.isEmpty()) {
            NodeLight node = queue.poll();
            for (int i = 0; i < node.out.size(); i++) {
                Arc arc = node.out.get(i);
                double cost = 0, temp;
                if (arc.varIndex >= 0) {
                    temp = 0;
                    if (arc.uVector != null) {
                        for (int j = 0; j < a.length; j++) {
                            temp += a[j] * arc.uVector[0][j];
                        }
                    }

                    cost = c[arc.varIndex] * arc.arcVal - 0.25 * temp * temp;
//                    cost = beta[arc.varIndex] * arc.arcVal - 0.25 * arc.costCoeff;     //@GASMOR: DOUBLE CHECK THIS COST!!!!
                }
                //Update the label
                if (d[arc.head] > d[node.ID] + cost) {
                    //System.out.println("NEW LABEL FOR "+arc.head+" = " +(d[node.ID]+cost));

                    d[arc.head] = d[node.ID] + cost;
                    p[arc.head] = arc;
                    if (nodeInQueue[arc.head] == 0) {
                        queue.add(DD.nodes.get(arc.head));	//Add head node to the queue if not added before
                        nodeInQueue[arc.head]++;
                    }
                }
            }
        }


        //Recover the solution
        Arc arc = p[DD.lastNode];
        int[] z = new int[n];
        while (arc
                != null) {
            //System.out.println( arc.varIndex + " , " + arc.arcVal);
            if (arc.varIndex >= 0) {
                z[arc.varIndex] = arc.arcVal;
            }
            arc = p[arc.tail];
        }
        
        System.arraycopy(z,
                0, ans, 0, n);

        return d[DD.lastNode] + constant;
    }

// Auxiliary class for sorting
    class Item implements Comparable<Item> {

        int pos;
        double val;

        public Item(int pos, double val) {
            this.pos = pos;
            this.val = val;
        }

        @Override
        public int compareTo(Item o) {
            return -Double.compare(val, o.val);
        }

    }

}
