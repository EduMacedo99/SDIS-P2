package src.helper;

import src.network.*;
import src.utils.MessageType;

import java.util.concurrent.ExecutorService;
import java.net.InetSocketAddress;

/**
 * called periodically. 
 * checks whether predecessor has failed.
 */
public class PredecessorThread extends HelperThread{
    

    private static final int wait_time = 3000;
    private ExecutorService executor;
    private ChordNode chordNode;

    public PredecessorThread(ChordNode node) {
        super(wait_time);
        this.chordNode = node;
        executor = chordNode.get_executor();
    }

    @Override
    public void run() {

        if(chordNode.get_successor() == null )
        return;

        InetSocketAddress predecessor = chordNode.get_predecessor();
        if (predecessor == null) {
            System.out.println("No predecessor on node with local key: " + chordNode.get_local_key().toString());
            return;
        }
        
        // checks if predecessor still lives
        // TODO: apanhar o erro de envio de mensagem caso não esteja vivo
        Message msg = new Message(MessageType.REQUEST_KEY, chordNode.get_address());
        MessageSender msg_sender = new MessageSender(chordNode, predecessor, msg);
        executor.execute(msg_sender);

    }


}