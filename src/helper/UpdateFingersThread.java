package src.helper;
import src.network.ChordNode;

import java.net.InetSocketAddress;
import static src.utils.Utils.*;

public class UpdateFingersThread extends HelperThread{

    private ChordNode node;

    public UpdateFingersThread(ChordNode node, int time_interval) {
        super(time_interval);
        this.node = node;
    }

    @Override
    public void run() {

        for (int i = 1; i <= KEY_SIZE; i++) {
            int key = (int) ((int) (node.get_local_key().key + Math.pow(2, i - 1)) % Math.pow(2, KEY_SIZE));
            InetSocketAddress sucessor = node.getSuccessor(key);
            
            node.update_ith_finger(i, sucessor);
            System.out.println("FT[" + i + "] key " + key + " -> " + sucessor);
        }
        System.out.println();
    }


    
}