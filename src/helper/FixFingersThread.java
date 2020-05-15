package src.helper;
import src.network.ChordNode;

import java.net.InetSocketAddress;
import static src.utils.Utils.*;

/**
 * called periodically. 
 * updates/refresh finger table entries.
 */
public class FixFingersThread extends HelperThread{

    private ChordNode node;

    public FixFingersThread(ChordNode node, int time_interval) {
        super(time_interval);
        this.node = node;
    }

    @Override
    public void run() {

        if(node.get_successor() == null )
            return;

        for (int i = 1; i <= KEY_SIZE; i++) {
            //finger[next] := find_successor(n+2^(..));
            int key = (int) ((int) (node.get_local_key().key + Math.pow(2, i - 1)) % Math.pow(2, KEY_SIZE));
            InetSocketAddress sucessor = node.find_successor_addr(key);
            node.update_ith_finger(i, sucessor);
            System.out.println("FT[" + i + "] key " + key + " -> " + node.finger_table.get(i));
        }
        System.out.println("FT predecessor -> " + node.get_predecessor());
        System.out.println("FT successor -> " + node.get_successor() + "\n");


    }


    
}