package src.network;

import java.net.InetSocketAddress;

public class MessageSender implements Runnable {

    private ChordNode peer;
    private InetSocketAddress destination;
    private Message msg;

    public MessageSender(ChordNode peer, InetSocketAddress destination, Message msg) {
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
            client.read();
            client.shutdown();
        } catch (Exception e) {
            System.err.println("One or more problems occured while sending the message!");
        }
	}
}