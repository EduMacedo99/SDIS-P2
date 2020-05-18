package src.helper;

import src.network.ChordNode;
import src.network.Key;
import src.network.Message;
import src.utils.MessageType;

import java.net.InetSocketAddress;

import static src.utils.Utils.*;

public class StabilizeThread extends HelperThread{

    private ChordNode node;

    public StabilizeThread(ChordNode node, int time_interval) {
        super(time_interval);
        this.node = node;
    }

    @Override
    public void run() {

        if(node.get_successor() == null )
           return;

        InetSocketAddress successor = node.get_successor();

        Message msg = new Message(MessageType.GET_PREDECESSOR, node.get_address());
        
        Message response = requestMessage(node, successor, 100, msg);

        String candidate_str = new String(response.get_body());

        if (candidate_str.equals("null")) return;

        String candidate_address = candidate_str.split(":")[0];
        int candidate_port = Integer.parseInt(candidate_str.split(":")[1]);

        InetSocketAddress candidate = new InetSocketAddress(candidate_address, candidate_port);

        Key successor_key = Key.create_key_from_address(successor);
        Key candidate_key = Key.create_key_from_address(candidate);

        if (node.betweenKeys(node.get_local_key().key, candidate_key.key, successor_key.key) || node.get_local_address().equals(successor)) {
            node.update_successor(candidate);
        }
       
    }


    
}