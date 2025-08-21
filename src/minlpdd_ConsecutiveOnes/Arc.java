package minlpdd_ConsecutiveOnes;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.linear.ArrayRealVector;

/**
 *
 * @author lozanolo
 */
public class Arc {

    int tail;
    int head;
    int arcVal;
    int varIndex;
    double costCoeff;
    double[][] uVector;
    String key;
    List<Integer> vars;

    /**
     * Constructor by parameters <br>
     *
     * @param _index Variable associated with arc. <br>
     * @param value Value of the arc. <br>
     * @param _tail Tail of the arc. <br>
     * @param _head Head of the arc. <br>
     */
    public Arc(int _index, int value, int _tail, int _head) {
        tail = _tail;
        head = _head;
        arcVal = value;
        varIndex = _index;
        costCoeff = 0;
        //uVector = new double[k]; Probably we don't even have to store this... 
        if (_index >= 0) {
            key = "NO arc for variable z_" + varIndex;
        } else {
            key = "Artificial arc";
        }
    }

    Arc(int _index, int value, int _tail, int _head, double[] u) {
        key = "";
        tail = _tail;
        head = _head;
        arcVal = value;
        varIndex = _index;
        uVector = new double[1][u.length];
        System.arraycopy(u, 0, uVector[0], 0, u.length);
        costCoeff = Math.pow(AlgHandler.a.dotProduct(new ArrayRealVector(u)), 2);
        key = "YES arc for variable z_" + varIndex + ", uVector: [ ";
        for (int i = 0; i < uVector.length; i++) {
            key = key + uVector[0][i] + " ";
        }
        key += "], Cost: " + costCoeff;
    }



    public void print() {

        System.out.println(key);

    }

}
