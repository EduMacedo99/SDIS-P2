package src.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.net.ssl.SSLSocket;

import static src.utils.Utils.*;

import src.service.Backup;
import src.service.Restore;
import src.utils.MessageType;

public class MessageReceiver {

    public static void handle_message(Message msg, ChordNode node, SSLSocket socket) {
        String type = msg.get_type();
        switch (type) {
            case MessageType.GET_PREDECESSOR:
                handle_get_predecessor(msg, node, socket);
                break;

            case MessageType.NOTIFY:
                handle_notify(msg, node);
                break;

            case MessageType.FIND_SUCCESSOR_KEY:
                handle_find_successor_key(msg, node);
                break;

            case MessageType.FIND_SUCCESSOR_FINGER:
                handle_find_successor_finger(msg, node);
                break;

            case MessageType.FOUND_SUCCESSOR_KEY:
                handle_found_successor_key(msg, node);
                break;

            case MessageType.FOUND_SUCCESSOR_FINGER:
                handle_found_successor_finger(msg, node);
                break;

            case MessageType.ARE_YOU_ALIVE:
                handle_are_you_alive(node, socket);
                break;

            case MessageType.FIND_BACKUP_NODE:
                handle_find_backup_node(msg, node);
                break;

            case MessageType.FOUND_BACKUP_NODE:
                handle_found_backup_node(msg, node);
                break;

            case MessageType.BACKUP_FILE:
                node.get_executor().submit(new Backup(msg, node));
                break;
                
            case MessageType.RESTORE_FILE:
                handle_restore_file(msg, node);
                break;

            case MessageType.RETRIEVE_FILE:
                node.get_executor().submit(new Restore(msg, node));
                break;

            case MessageType.DELETE_FILE:
                handle_delete_file(msg, node);
                break;
        }
    }

    private static void handle_are_you_alive(ChordNode node, SSLSocket socket) {
        Message response = new Message(MessageType.I_AM_ALIVE);
        send_response(node, response, socket);
    }

    private static void handle_notify(Message msg, ChordNode node) {
        node.notified(msg.get_sender_address());
    }

    private static void handle_get_predecessor(Message msg, ChordNode node, SSLSocket socket) {
        InetSocketAddress predecessor = node.get_predecessor();
        msg = new Message(MessageType.PREDECESSOR);
        msg.set_body(address_to_string(predecessor).getBytes());
        send_response(node, msg, socket);
    }

    private static void handle_found_successor_key(Message msg, ChordNode node) {
        String[] pieces = new String(msg.get_body()).split(":");
        String address = pieces[0];
        int port = Integer.parseInt(pieces[1]);
        node.update_successor(new InetSocketAddress(address, port));
        node.start_helper_threads();
    }

    private static void handle_find_successor_key(Message msg, ChordNode node) {
        long key = msg.get_key();
        InetSocketAddress peer_requesting = msg.get_peer_requesting();
        Message message = new Message(MessageType.FIND_SUCCESSOR_KEY, node.get_address(), address_to_string(peer_requesting), new Key(key));
        InetSocketAddress successor = node.find_successor_addr(key, message);
        if (successor != null) {
            message = new Message(MessageType.FOUND_SUCCESSOR_KEY, node.get_address());
            message.set_body(address_to_string(successor).getBytes());
            send_message(node, peer_requesting, message);
        }
    }

    private static void handle_find_successor_finger(Message msg, ChordNode node) {
        long key = msg.get_key();
        InetSocketAddress peer_requesting = msg.get_peer_requesting();
        int ith_finger = Integer.parseInt(msg.get_header().split(" ")[4]);
        msg = new Message(MessageType.FIND_SUCCESSOR_FINGER, node.get_address(), address_to_string(peer_requesting), new Key(key), ith_finger);
        InetSocketAddress successor = node.find_successor_addr(key, msg);
        if (successor != null) {
            msg = new Message(MessageType.FOUND_SUCCESSOR_FINGER, node.get_address(), ith_finger);
            msg.set_body(address_to_string(successor).getBytes());
            send_message(node, peer_requesting, msg);
        }
    }

    private static void handle_found_successor_finger(Message msg, ChordNode node) {
        int ith_finger = Integer.parseInt(msg.get_header().split(" ")[2]);
        node.update_ith_finger(ith_finger, string_to_address(new String(msg.get_body())));
    }

    private static void handle_find_backup_node(Message msg, ChordNode node) {
        long key = msg.get_key();
        InetSocketAddress peer_requesting = msg.get_peer_requesting();
        msg = new Message(MessageType.FIND_BACKUP_NODE, node.get_address(), address_to_string(peer_requesting), new Key(key));
        InetSocketAddress successor = node.find_successor_addr(key, msg);
        if (successor != null) {
            msg = new Message(MessageType.FOUND_BACKUP_NODE, node.get_address(), address_to_string(peer_requesting), new Key(key));
            msg.set_body(address_to_string(successor).getBytes());
            send_message(node, peer_requesting, msg);
        }
    }
    
    private static void handle_found_backup_node(Message msg, ChordNode node) {
        String[] pieces = new String(msg.get_body()).split(":");
        String address = pieces[0];
        int port = Integer.parseInt(pieces[1]);
        System.out.println("----------");
        System.out.println("Key: " + msg.get_key() + "  /  Successor: " + address + " " + port);
        
        // Send file
        Path path = node.files_list.get(msg.get_key());
        if(path != null){
            Backup.send_file(node, new Key(msg.get_key()), path, new InetSocketAddress(address, port));
        }
    }

    private static void handle_restore_file(Message msg, ChordNode node) {
        long key = msg.get_key();
        InetSocketAddress peer_requesting = msg.get_peer_requesting();
        msg = new Message(MessageType.RESTORE_FILE, node.get_address(), address_to_string(peer_requesting), new Key(key));
        node.send_restore_msg(key, msg);
    }

    private static void handle_delete_file(Message msg, ChordNode node) {
        long key = msg.get_key();
        InetSocketAddress peer_requesting = msg.get_peer_requesting();
        msg = new Message(MessageType.DELETE_FILE, node.get_address(), address_to_string(peer_requesting), new Key(key));
        node.send_delete_msg(key, msg);
    }

}