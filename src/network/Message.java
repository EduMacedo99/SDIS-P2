package src.network;

import static src.utils.Utils.*;

import java.nio.ByteBuffer;

public class Message {
    String header;
    byte[] body;

    public Message(String header, byte[] body) {
        this.header = header;
        this.body = body;
    }
    
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

    public static Message build_msg_from_byte_buffer(ByteBuffer msg) {
        try {
            byte[] message = trim_message(msg.array());
            String string_msg = new String(message);
            String[] pieces = string_msg.split(CRLF);
            String header = pieces[0];
    
            int separation_index = string_msg.indexOf(CRLF);
            byte[] body = new byte[message.length - header.length() - 4];
            System.arraycopy(message, separation_index + 4, body, 0, body.length);
            Message ret = new Message(header, body);
            return ret;
        } catch (Exception e) {
            System.err.println("Message cannot be parsed! NULL message returned!");
            return null;
        }
    }

    public String get_header() {
        return header;
    }

    public byte[] get_body() {
        return body;
    }
}