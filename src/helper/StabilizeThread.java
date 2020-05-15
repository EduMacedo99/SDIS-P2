package src.helper;

import src.network.ChordNode;
import src.network.Message;
import src.network.MessageSender;
import src.utils.MessageType;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;

import static src.utils.Utils.*;

/**
 * called periodically. 
 * n asks its successor for its predecessor p and decides whether p should be n‘s successor instead (this is the case if p recently joined the system).
 * notifies n‘s successor of its existence, so it can change its predecessor to n
 */
public class StabilizeThread extends HelperThread{

    private ChordNode node;
    private ExecutorService executor;

    public StabilizeThread(ChordNode node, int time_interval) {
        super(time_interval);
        this.node = node;
        executor = node.get_executor();
    }

    @Override
    public void run() {

        if(node.get_successor() == null )
           return;

        // asks its successor for its predecessor p
        InetSocketAddress successor = node.get_successor();

        Message msg = new Message(MessageType.GET_PREDECESSOR, node.get_address());
        MessageSender msg_sender = new MessageSender(node, successor, msg);
        executor.execute(msg_sender);


       
    }


    
}