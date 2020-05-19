package src.helper;

import src.network.*;
import src.utils.MessageType;
import java.net.InetSocketAddress;
import static src.utils.Utils.*;

/**
 * Called periodically. 
 * Checks whether predecessor has failed.
 */
public class PredecessorThread extends HelperThread {
    
    private ChordNode node;

    public PredecessorThread(ChordNode node, int wait_time) {
        super(wait_time);
        this.node = node;
    }

    @Override
    public void run() {

        InetSocketAddress predecessor = node.get_predecessor();

        if (predecessor == null) {
            return;
        }
        
        Message msg = new Message(MessageType.ARE_YOU_ALIVE);
        Message response = request_message(node, predecessor, msg);

        if (response == null || !response.get_type().equals(MessageType.I_AM_ALIVE)) {
            node.set_predecessor(null);
        }
    }


}