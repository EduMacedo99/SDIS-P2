package src.helper;

import src.network.ChordNode;
import src.network.Key;
import src.network.Message;
import src.utils.MessageType;

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
            long key = node.get_local_key().key + (int) Math.pow(2, i - 1) % (int) Math.pow(2, KEY_SIZE);
            Message msg = new Message(MessageType.FIND_SUCCESSOR_FINGER, node.get_address(), node.get_address(), new Key(key));
            node.find_successor_addr(key, msg);
            //final InetSocketAddress n0_addr = node.closest_preceding_node_addr(key);
            //requestMessage(this,  n0_addr, 100, msg);

            //InetSocketAddress successor = node.find_successor_addr(key, msg);
            //node.update_ith_finger(i, successor);
            //System.out.println("FT[" + i + "] key " + key + " -> " + node.finger_table.get(i));
        }
        //System.out.println("FT predecessor -> " + node.get_predecessor());
        //System.out.println("FT successor -> " + node.get_successor() + "\n");


    }


    
}