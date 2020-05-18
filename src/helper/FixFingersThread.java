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
            long key = (node.get_local_key().key + (int) Math.pow(2, i - 1)) % (int) Math.pow(2, KEY_SIZE);
            Message msg = new Message(MessageType.FIND_SUCCESSOR_FINGER, node.get_address(), node.get_address(), new Key(key), i);
            InetSocketAddress finger = node.find_successor_addr(key, msg);
            if (finger != null) {
                node.update_ith_finger(i, finger);
            }
        }
    }
}