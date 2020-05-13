package src.helper;

import src.network.*;
import src.utils.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.net.InetSocketAddress;

/**
 * TO DO helpers:
 *  -ver se o predecessor checks out
 *  -arranjar a finger table
 *  -confirmar o sucessor
 *  há mais???
*/



public class PredecessorThread extends HelperThread{
    

    private static final int wait_time = 2500;
    private ExecutorService executor;
    private ChordNode chordNode;

    public PredecessorThread(ChordNode node) {
        super(wait_time);
        this.chordNode = node;
        
        executor = chordNode.get_executor();
    }

    @Override
    public void run() {

        InetSocketAddress predecessor = chordNode.get_predecessor();
        if (predecessor == null) {
            System.out.println("No predecessor on node with local key: " + chordNode.get_local_key().toString());
            return;
        }

        Message msg = new Message(MessageType.REQUEST_KEY, chordNode.get_address());
        MessageSender msg_sender = new MessageSender(chordNode, predecessor, msg);
        executor.execute(msg_sender);




    }


}