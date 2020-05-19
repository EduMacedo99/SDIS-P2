package src.network;

import static src.utils.Utils.*;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class Message implements Serializable{
    private static final long serialVersionUID = 1L;

    String type;
    String sender_address;
    String header;
    byte[] body;

    public Message(String header, byte[] body) {
        this.header = header;
        this.body = body;
    }

    public Message(String type) {
        this.type = type;
        this.header = type;
        this.body = "".getBytes();
    }
    
    public Message(String type, String sender_address) {
        this.type = type;
        this.sender_address = sender_address;
        this.header = type + " " + sender_address;
        this.body = "".getBytes();
    }

    public Message(String type, String sender_address, int ith_finger) {
        this.type = type;
        this.sender_address = sender_address;
        this.header = type + " " + sender_address + " " + ith_finger;
        this.body = "".getBytes();
    }

    public Message(String type, String sender_address, String successor) {
        this.type = type;
        this.sender_address = sender_address;
        this.header = type + " " + sender_address + " " + successor;
        this.body = "".getBytes();
    }

    public Message(String type, String sender_address, String peer_requesting_address, Key key) {
        this.type = type;
        this.sender_address = sender_address;
        this.header = type + " " + sender_address + " " + peer_requesting_address + " " + key;
        this.body = "".getBytes();
    }

    public Message(String type, String sender_address, String peer_requesting_address, Key key, int ith_finger) {
        this.type = type;
        this.sender_address = sender_address;
        this.header = type + " " + sender_address + " " + peer_requesting_address + " " + key + " " + ith_finger;
        this.body = "".getBytes();
    }

    public Message(String type, String sender_address, String peer_requesting_address, byte[] bFile, Key key, String file_name, int replication_degree) {
        this.type = type;
        this.sender_address = sender_address;
        this.header = type + " " + sender_address + " " + peer_requesting_address + " " + key + " " + file_name + " " + replication_degree;
        this.body = bFile;
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

    public String get_type() {
        return type;
    }

    public InetSocketAddress get_sender_address() {
        return string_to_address(sender_address);
    }

    public InetSocketAddress get_peer_requesting() {
        String address_string = header.split(" ")[2];
        return string_to_address(address_string);
    }

    public long get_key() {
        return Long.parseLong(header.split(" ")[3]);
    }

    public String get_file_name() {
        return header.split(" ")[4];
    }

    public long get_replication_degree() {
        return Long.parseLong(header.split(" ")[5]);
    }
}