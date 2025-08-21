package minlpdd_ConsecutiveOnes;

import java.util.ArrayList;

/**
 *
 * @author lozanolo
 */
public class DecisionDiagram {
    
    int sourceId;
    int sinkId;

    ArrayList<NodeLight> nodes;  //Nodes in the DD
    ArrayList<Arc> arcs;    //Arcs in the DD
    int lastNode; //Points to the index of the last node in the nodes array, source node is always 0
    

    public DecisionDiagram() {
        nodes = new ArrayList<>();
        arcs = new ArrayList<>();
    }

    public void print() {

        System.out.println("**********************************************DD*****************************************************************");
        for (int i = 0; i < nodes.size(); i++) {
                System.out.println("Node" + nodes.get(i).ID );
                for (int j = 0; j < nodes.get(i).out.size(); j++) {
                    System.out.println("   "+nodes.get(i).out.get(j).key+", "+nodes.get(i).out.get(j).tail+" --> "+nodes.get(i).out.get(j).head);
                }
        }
    }

    public void clearInOut() {
        for (int i = 0; i < nodes.size(); i++) {
            nodes.get(i).in.clear();
            nodes.get(i).out.clear();
        }

    }

    void print2() {
       System.out.println("Number of Nodes: "+nodes.size());
       System.out.println("Number of Arcs: "+arcs.size());
    }

}
