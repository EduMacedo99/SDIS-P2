package src.service;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

import src.network.ChordNode;
import src.network.Key;
import src.network.Message;
import src.utils.MessageType;

import static src.utils.Utils.*;

public class Restore implements Runnable {

    private static final int SEND_RESTORE_MSG = 0;
    private static final int EXECUTE_RESTORE = 1;
    private static final int SAVE_FILE = 2;

    private final ChordNode node;
    private String file_name;
    private final int task;
    private long key;
    private Message msg;

    public Restore(ChordNode node, String file_name) {
        this.task = SEND_RESTORE_MSG;
        this.node = node;
        this.file_name = file_name;
        msg = null;
    }

    public Restore(ChordNode node, long key, Message msg) {
        task = EXECUTE_RESTORE;
        this.key = key;
        this.node = node;
        this.msg = msg;
        file_name = null;
    }

    public Restore(Message msg, ChordNode node) {
        task = SAVE_FILE;
        this.node = node;
        this.msg =  msg;
        file_name = null;
    }
    
    @Override
    public void run() {
        switch(task) {
            case SEND_RESTORE_MSG:
                get_file_key();
                break;
            case EXECUTE_RESTORE:
                get_restore_file();
                break;
            case SAVE_FILE:
                save_file();
                break;
        }
    }

    /**
     * Handles the process of finding the key for the file.
     */
    public void get_file_key() {

        String file_path = FILES_TO_BACKUP_DIR + '/' + file_name;
        Path path = Paths.get(file_path);

        // Get file key
        Key key_file = null;
        try {
            key_file = Key.create_key_file(file_path);
            node.store_restore_file(key_file.key, path);
            System.out.println("Key File: " + key_file.key);
        } catch (NoSuchAlgorithmException | IOException e) {
            System.err.println("Something went wrong while hashing the file key!\n");
            return;
        }

        Message find_succ_msg = new Message(MessageType.FIND_RESTORE_FILE, node.get_address(), node.get_address(), key_file);
        InetSocketAddress successor = node.find_successor_addr(key_file.key, find_succ_msg);
        if (successor != null) {
            Message found_succ_msg = new Message(MessageType.FOUND_RESTORE_FILE, node.get_address(), node.get_address(), key_file);
            send_message(node, successor, found_succ_msg);
        }
    }

    /**
     * Finds the file to restore and sends it to the requesting peer.
     */
	public void get_restore_file() {

        String file_name = node.get_backed_up_file_name(key);

        if (file_name != null) {

            String file_path = node.get_files_path() + '/' + file_name;
            Path path = Paths.get(file_path);

            Message msg2 = new Message(MessageType.RETRIEVE_FILE, node.get_address(), address_to_string(msg.get_peer_requesting()), new Key(msg.get_key()));

            byte[] bFile = null;

            // Get file bytes via Java.nio
            try {
                bFile = Files.readAllBytes(path);
            } catch (IOException ex) {
                System.err.println("The file you want to restore was not found!\n");
                return;
            }

            msg2.set_body(bFile);
            send_message(node, msg.get_peer_requesting() , msg2);

        } else {
            InetSocketAddress initiator;
            boolean first_time = false;
            if (msg.get_header().split(" ").length == 4) {
                initiator = node.get_local_address();
                first_time = true;
            } else {
                initiator = string_to_address(msg.get_header().split(" ")[4]);
            }

            if (!first_time && node.get_local_address().equals(initiator)) {
                System.out.println("Restore failed! File was not found!");
                return;
            }

            Message found_succ_msg = new Message(MessageType.FOUND_RESTORE_FILE, node.get_address(), address_to_string(msg.get_peer_requesting()), new Key(key), address_to_string(initiator));
            send_message(node, node.get_successor(), found_succ_msg);
        }

    }

    /**
     * Stores file in the restore directory via Java.nio.
     */
	public void save_file() {
        key = msg.get_key();
        String path_ini = node.get_restore_file_path(key).toString();
        file_name = path_ini.split(Pattern.quote("\\"))[1];
        
        // Create file
        File file = new File("peers/" + node.get_local_key().key + "/restore/" + file_name);
        file.getParentFile().mkdirs();
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Write to file
        Path path = Paths.get("peers/" + node.get_local_key().key + "/restore/" + file_name);
        try {
            Files.write(path, msg.get_body());
        } catch(IOException ex){
            ex.printStackTrace();
        }

        node.delete_restore_file(key);
    }
}