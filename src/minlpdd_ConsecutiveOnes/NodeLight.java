package minlpdd_ConsecutiveOnes;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author lozanolo
 */
public class NodeLight {
    int ID;                     //Position in the DD nodes array
    List<Arc> out;     //Arcs going out
    List<Arc> in;      //Arcs coming in
    double cost;

    //Constructor for new node
    public NodeLight() {
        ID = 0;
        out = new ArrayList<>();
        in = new ArrayList<>();
    }

    NodeLight(Node n) {
        ID = n.ID;
        out = new ArrayList<>();
        in = new ArrayList<>();
        this.cost=n.getCost();
    }

    public void print() {
        System.out.println(ID);
        System.out.println();
    }
    
    public void updateCost(double cost)
    {
        this.cost=Math.min(cost,this.cost);
    }

}
