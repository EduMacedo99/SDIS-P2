package src.helper;

import src.network.ChordNode;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map.Entry;

public class UpdateFingersThread extends HelperThread{

    private ChordNode node;
    private HashMap<Integer, InetSocketAddress> finger_table;

    public UpdateFingersThread(ChordNode node, HashMap<Integer, InetSocketAddress> finger_table, int time_interval) {
        super(time_interval);
        this.node = node;
        this.finger_table = finger_table;
    }

    @Override
    public void run() {

        for (Entry<Integer, InetSocketAddress> finger : finger_table.entrySet()) {
            //System.out.println("Key: "+ finger.getKey() + " & Value: " + finger.getValue());

            int key = finger.getKey();
            InetSocketAddress sucessor = node.getSuccessor(key);

            node.update_ith_finger(key, sucessor);

        }

   
    }


    
}