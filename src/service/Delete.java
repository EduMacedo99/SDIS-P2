package src.service;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

import javax.swing.JToolBar.Separator;

import src.network.ChordNode;
import src.network.Key;
import src.network.Message;
import src.utils.MessageType;

import static src.utils.Utils.*;

import src.service.Backup;

public class Delete implements Runnable {

    private static final int SEND_DELETE_MSG = 0;
    private static final int EXECUTE_DELETE = 1;
    private static final String FILES_TO_BACKUP_DIR = "files_to_backup";

    private final ChordNode node;
    private String file_name;
    private final int task;
    private Message msg;
    private Long key;

    public Delete(ChordNode node, String file_name) {
        this.task = SEND_DELETE_MSG;
        this.node = node;
        this.file_name = file_name;
    }

    public Delete(ChordNode node, long key, Message msg) {
        task = EXECUTE_DELETE;
        this.key = key;
        this.node = node;
        this.msg = msg;
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
        Path path = Paths.get(file_path);

        // Get file key
        Key key_file = null;
        try {
            key_file = Key.create_key_file(file_path);
            node.store_file_key(key_file.key, path);
            System.out.println("Key File: " + key_file.key);
        } catch (NoSuchAlgorithmException | IOException e) {
            System.err.println("Something went wrong while hashing the file key!\n");
            return;
        }

        Message msg = new Message(MessageType.DELETE_FILE, node.get_address(), node.get_address(), key_file);
        send_message(node, node.get_successor(), msg);

        node.deleteFile_files_list(key);
        
    }

     /**
     * Delete file 
     */
	public void delete_file() {

        String file_path = node.get_files_path() + '/' + node.getFileName(key);
        Path path = Paths.get(file_path);

        if(path != null) {

            // Delete the file via Java.nio
            try {
                Files.delete(path);
            } catch (IOException ex) {
                System.err.println("The file you want to delete was not found!\n");
                return;
            }
        }
    
        node.deleteFile_files_backed_up(key);
     
    }

}