package src.network;

import static src.utils.Utils.*;

public class Message {
    String header;
    byte[] body;
    
    public Message(String type, String sender_address) {
        this.header = type + " " + sender_address + CRLF;
        this.body = "".getBytes();
    }

    public Message(String type, String sender_address, Key key) {
        this.header = type + " " + sender_address + " " + key + CRLF;
        this.body = "".getBytes();
    }

    public void set_body(byte[] body) {
        this.body = body;
    }

    public byte[] get_bytes() {
        byte[] result = new byte[header.length() + body.length];
        byte[] headerB = header.getBytes();
        System.arraycopy(headerB, 0, result, 0, headerB.length);
        System.arraycopy(body, 0, result, headerB.length, body.length);
        return result;
    }
}