package src.network;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;

import static src.utils.Utils.*;
import src.utils.MessageType;

public class MessageReceiver implements Runnable {

    private String header;
    private byte[] body;
    private String type;
    private ChordNode peer;
    private InetSocketAddress ip_sender;
    private String string_msg;

    public MessageReceiver(ByteBuffer msg, ChordNode peer){
        this.peer = peer;
        byte[] message = trim_message(msg.array());
        string_msg = new String(message);
        String[] pieces = string_msg.split(CRLF);
        header = pieces[0];
        type = header.split(" ")[0];

        String ip_sender_str = header.split(" ")[1];
        String ip = ip_sender_str.split(":")[0];
        int port = Integer.parseInt(ip_sender_str.split(":")[1]);
        ip_sender = new InetSocketAddress(ip, port);

        int separation_index = string_msg.indexOf(CRLF);
        body = new byte[message.length - header.length() - 4];
        System.arraycopy(message, separation_index + 4, body, 0, body.length);
        //System.out.println("Message received: " + ip_sender);
        //System.out.println("Node peer: " + peer.get_address());
    }
    
    @Override
    public void run() {
        peer.set_last_responde(string_msg);
        switch(type) {
            case MessageType.JOIN:
                Message msg = new Message(MessageType.OK , peer.get_address());
                MessageSender msg_sender = new MessageSender(peer, ip_sender, msg);
                peer.get_executor().execute(msg_sender); 
                break;
            case MessageType.PREDECESSOR:
                System.out.println("Predecessor message received successfully!");
                break;
            case MessageType.REQUEST_KEY:
                System.out.println("Message requesting key received");
                Message msg_key = new Message(peer.get_local_key().toString(), peer.get_address());
                MessageSender msg_key_sender = new MessageSender(peer, ip_sender, msg_key);
                peer.get_executor().execute(msg_key_sender);
                break;
                
            case MessageType.OK:
                System.out.println("Response message received successfully!");
                break;
        }
    }
    
}