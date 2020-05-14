package src.network;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;


public class MessageSender implements Runnable {

    private ChordNode peer;
    private InetSocketAddress destination;
    private Message msg;
    public ByteBuffer response;

    public MessageSender(ChordNode peer, InetSocketAddress destination, Message msg/*, SSLClient client*/) {
        this.peer = peer;
        this.destination = destination;
        this.msg = msg;
    }

    @Override
    public void run() {
        try {
            SSLClient client = new SSLClient(peer, destination.getAddress().getHostAddress(), destination.getPort());
            client.connect();
            client.write(msg.get_bytes());
            while(true){
                response = client.read();
                //System.out.println("message sender:  " + response);
                if(response == null)//Client requested to close the connection
                    break;
            }
            client.shutdown();
        } catch (Exception e) {
            System.err.println("One or more problems occured while sending the message!");
            e.printStackTrace();
        }
	}
}