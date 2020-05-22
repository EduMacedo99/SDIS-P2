package src.service;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;

import src.network.ChordNode;
import src.network.Key;
import src.network.Message;
import src.utils.MessageType;

import static src.utils.Utils.*;


public class Delete implements Runnable {

    private static final int SEND_DELETE_MSG = 0;
    private static final int EXECUTE_DELETE = 1;

    private final ChordNode node;
    private String file_name;
    private final int task;
    private Long key;

    public Delete(ChordNode node, String file_name) {
        this.task = SEND_DELETE_MSG;
        this.node = node;
        this.file_name = file_name;
    }

    public Delete(ChordNode node, long key) {
        task = EXECUTE_DELETE;
        this.key = key;
        this.node = node;
    }

    @Override
    public void run() {
        switch(task) {
            case SEND_DELETE_MSG:
                get_file_key();
                break;
            case EXECUTE_DELETE:
                delete_file();
                break;
        } 
    }

    /**
     * Handles the process of finding the key for the file.
     */
    public void get_file_key() {

        String file_path = FILES_TO_BACKUP_DIR + '/' + file_name;

        // Get file key
        Key key_file = null;
        try {
            key_file = Key.create_key_file(file_path);
            System.out.println("Key File: " + key_file.key);
        } catch (NoSuchAlgorithmException | IOException e) {
            System.err.println("Something went wrong while hashing the file key!\n");
            return;
        }

        if (node.is_responsible_for_key(key_file.key)) {
            this.key = key_file.key;
            delete_file();
            node.deleteFile_files_list(key);
            return;
        }

        // Send to successor of file key
        Message find_succ_msg = new Message(MessageType.FIND_DELETE_FILE_NODE, node.get_address(), node.get_address(), key_file);
        InetSocketAddress successor = node.find_successor_addr(key_file.key, find_succ_msg);
        if (successor != null) {
            System.out.println("Sending to successor");
            send_message(node, successor, find_succ_msg);
        }

        node.deleteFile_files_list(key);
    }

    /**
    * Deletes the file. 
    */
	public void delete_file() {
        String file_name = node.get_backed_up_file_name(key);
        String file_path = node.get_files_path() + '/' + file_name;
        Path path = Paths.get(file_path);

        if(node.get_file_path(key) != null || file_name != null) {
            Message msg = new Message(MessageType.DELETE_FILE, node.get_address(), node.get_address(), new Key(key));
            send_message(node, node.get_successor(), msg);
        }

        if(file_name != null) {
            // Delete the file via Java.nio
            try {
                long file_size = Files.size(path); 
                Files.delete(path);
                node.get_disk().decrease_used_space((int) file_size);
                node.get_disk().print_state();
            } catch (IOException ex) {
                System.err.println("The file you want to delete was not found!\n");
                return;
            }
        }
    
        node.deleteFile_files_backed_up(key);
     
    }

}