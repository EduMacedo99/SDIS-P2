package src.network;

import java.nio.ByteBuffer;

import static src.utils.Utils.*;
import src.utils.MessageType;

public class MessageReceiver implements Runnable {

    private String header;
    private byte[] body;
    private String type;

    public MessageReceiver(ByteBuffer msg) {
        byte[] message = trim_message(msg.array());
        String string_msg = new String(message);
        String[] pieces = string_msg.split(CRLF);
        header = pieces[0];
        type = header.split(" ")[0];
        int separation_index = string_msg.indexOf(CRLF);
        body = new byte[message.length - header.length() - 4];
        System.arraycopy(message, separation_index + 4, body, 0, body.length);
        System.out.println("Message received: " + string_msg);
    }
    
    @Override
    public void run() {
        switch(type) {
            case MessageType.JOIN:
                System.out.println("Message received successfully!");
                break;
            case MessageType.PREDECESSOR:
                System.out.println("Predecessor message received successfully!");
                break;
            case MessageType.REQUEST_KEY:
                System.out.println("Message requesting key received");
                
                break;
        }
    }
    
}