package src.helper;

import src.network.ChordNode;
import src.network.Message;
import src.network.MessageSender;
import src.network.SSLClient;
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
        
        byte[] a = requestMessage(node,  successor, 100, msg);




       /* while(node.get_last_response() == null || !node.get_last_response().contains("NOTIFY_IM_PREDECESSOR")){}
        System.out.println("SAIU NOTIFY_IM_PREDECESSOR RECEBIDO ");

        String response = node.get_last_response();

        String[] pieces = response.split(CRLF);
        String header = pieces[0];
        //String type = header.split(" ")[0];

        String ip_sender_str = header.split(" ")[1];
        String ip = ip_sender_str.split(":")[0];
        int port = Integer.parseInt(ip_sender_str.split(":")[1]);
        InetSocketAddress ip_sender = new InetSocketAddress(ip, port);

        node.notify(ip_sender);*/

        
       
    }


    
}