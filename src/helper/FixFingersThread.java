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

        InetSocketAddress previous_finger = null;

        for (int i = 1; i <= KEY_SIZE; i++) {

            InetSocketAddress current_finger = node.get_ith_finger(i);
            if(current_finger != null && (previous_finger == null || !current_finger.equals(previous_finger))) {
                Message alive_msg = new Message(MessageType.ARE_YOU_ALIVE);
                Message response = request_message(node, current_finger, alive_msg);
                previous_finger = current_finger;
                if(response == null) {
                    node.update_ith_finger(i, node.get_local_address());
                }
            }

            long key = (node.get_local_key().key + (int) Math.pow(2, i - 1)) % (int) Math.pow(2, KEY_SIZE);
            Message fs_msg = new Message(MessageType.FIND_SUCCESSOR_FINGER, node.get_address(), node.get_address(), new Key(key), i);
            InetSocketAddress finger = node.find_successor_addr(key, fs_msg);
            if (finger != null) {
                node.update_ith_finger(i, finger);
            }
        }
    }
}