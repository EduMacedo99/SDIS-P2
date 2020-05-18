package src.network;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import javax.net.ssl.SSLEngine;

import static src.utils.Utils.*;
import src.utils.MessageType;

public class MessageReceiver implements Runnable {

    private String header;
    private String[] header_pieces;
    private byte[] body;
    private String type;
    private ChordNode peer;
    private InetSocketAddress sender_address;
    private String string_msg;
    private SocketChannel socket_channel;
    private SSLEngine engine;

    private Message msg = null;

    public MessageReceiver(ByteBuffer msg, ChordNode peer, SocketChannel socket_channel, SSLEngine engine) {

        this.socket_channel = socket_channel;
        this.engine = engine;
        this.peer = peer;

        byte[] message = trim_message(msg.array());
        string_msg = new String(message);
        String[] pieces = string_msg.split(CRLF);
        header = pieces[0];
        type = header.split(" ")[0];
        header_pieces = header.split(" ");

        String sender_address_str = header.split(" ")[1];
        String ip = sender_address_str.split(":")[0];
        int port = Integer.parseInt(sender_address_str.split(":")[1]);
        sender_address = new InetSocketAddress(ip, port);

        int separation_index = string_msg.indexOf(CRLF);
        body = new byte[message.length - header.length() - 4];
        System.arraycopy(message, separation_index + 4, body, 0, body.length);
    }

    @Override
    public void run() {
        switch (type) {
            case MessageType.OK:
                System.out.println("Response message received successfully!");
                break;

            case MessageType.GET_PREDECESSOR:
                handle_get_predecessor();
                break;

            case MessageType.NOTIFY:
                handle_notify();
                break;

            case MessageType.REQUEST_KEY:
                System.out.println("Message received: asking if i am alive");
                msg = new Message(MessageType.SENDING_KEY, peer.get_address());
                requestMessage(peer,  sender_address, 100, msg);
                break;
            
            case MessageType.SENDING_KEY:
                System.out.println("Message received: your predecessor is still alive");
                break;

            case MessageType.FIND_SUCCESSOR_KEY:
                handle_find_successor_key();
                break;

            case MessageType.FIND_SUCCESSOR_FINGER:
                handle_find_successor_finger();
                break;

            case MessageType.FOUND_SUCCESSOR_KEY:
                handle_found_successor_key();
                break;

            case MessageType.FOUND_SUCCESSOR_FINGER:
                handle_found_successor_finger();
                break;
        }
    }

    private void handle_notify() {
        peer.notified(sender_address);
    }

    private void handle_get_predecessor() {
        InetSocketAddress predecessor = peer.get_predecessor();
        msg = new Message(MessageType.PREDECESSOR);
        msg.set_body(address_to_string(predecessor).getBytes());
        send_response(peer, msg, socket_channel, engine);
    }

    private void handle_found_successor_key() {
        String[] pieces = new String(body).split(":");
        String address = pieces[0];
        int port = Integer.parseInt(pieces[1]);
        peer.update_successor(new InetSocketAddress(address, port));
        peer.start_helper_threads();
    }

    private void handle_find_successor_key() {
        long key = Long.parseLong(header_pieces[3]);
        String peer_requesting = header_pieces[2];
        msg = new Message(MessageType.FIND_SUCCESSOR_KEY, peer.get_address(), peer_requesting, new Key(key));
        InetSocketAddress successor = peer.find_successor_addr(key, msg);
        if (successor != null) {
            msg = new Message(MessageType.FOUND_SUCCESSOR_KEY, peer.get_address());
            msg.set_body(address_to_string(successor).getBytes());
            String peer_requesting_address = peer_requesting.split(":")[0];
            int peer_requesting_port = Integer.parseInt(peer_requesting.split(":")[1]);
            send_message(peer, new InetSocketAddress(peer_requesting_address, peer_requesting_port), msg);
        }
    }

    private void handle_find_successor_finger() {
        long key = Long.parseLong(header_pieces[3]);
        String peer_requesting = header_pieces[2];
        int ith_finger = Integer.parseInt(header_pieces[4]);
        msg = new Message(MessageType.FIND_SUCCESSOR_FINGER, peer.get_address(), peer_requesting, new Key(key), ith_finger);
        InetSocketAddress successor = peer.find_successor_addr(key, msg);
        if (successor != null) {
            msg = new Message(MessageType.FOUND_SUCCESSOR_FINGER, peer.get_address(), ith_finger);
            msg.set_body(address_to_string(successor).getBytes());
            String peer_requesting_address = peer_requesting.split(":")[0];
            int peer_requesting_port = Integer.parseInt(peer_requesting.split(":")[1]);
            send_message(peer, new InetSocketAddress(peer_requesting_address, peer_requesting_port), msg);
        }
    }

    private void handle_found_successor_finger() {
        String[] pieces = new String(body).split(":");
        String address = pieces[0];
        int port = Integer.parseInt(pieces[1]);
        int ith_finger = Integer.parseInt(header.split(" ")[2]);
        peer.update_ith_finger(ith_finger, new InetSocketAddress(address, port));
    }
    
}