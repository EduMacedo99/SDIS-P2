package src.helper;

import src.network.*;
import src.utils.MessageType;

import java.util.concurrent.ExecutorService;
import java.net.InetSocketAddress;

import static src.utils.Utils.requestMessage;

/**
 * called periodically. 
 * checks whether predecessor has failed.
 */
public class PredecessorThread extends HelperThread{
    

    private static final int wait_time = 3000;
    private ChordNode chordNode;

    public PredecessorThread(ChordNode node) {
        super(wait_time);
        this.chordNode = node;
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
        byte[] a = requestMessage(chordNode,  predecessor, 100, msg);
        System.out.println("PREDECESSOR ALIVE: " + new String(a) );


        //while(chordNode.get_last_response() == null || !chordNode.get_last_response().contains("SENDING_KEY")){}
        //System.out.println("Message received: your predecessor is still alive");

    }


}