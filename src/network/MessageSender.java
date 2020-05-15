package src.network;

import java.nio.ByteBuffer;


public class MessageSender implements Runnable {

    private Message msg;
    public ByteBuffer response;
    private SSLClient client;

    public MessageSender(Message msg, SSLClient client) {
        this.msg = msg;
        this.client = client;
    }

    @Override
    public void run() {
        try {
            client.connect();
            client.write(msg.get_bytes());
        } catch (Exception e) {
            System.err.println("One or more problems occured while sending the message!");
            e.printStackTrace();
        }
	}
}