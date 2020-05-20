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

public class Backup implements Runnable {

    private static final int GET_FILE_KEY = 0;
    private static final int EXECUTE_BACKUP = 1;

    private final ChordNode node;
    private final String file_name;
    private final int replication_degree;
    private final int task;

    public Backup(ChordNode node, String file_name, int replication_degree) {
        this.task = GET_FILE_KEY;
        this.node = node;
        this.file_name = file_name;
        this.replication_degree = replication_degree;
    }

    @Override
    public void run() {

        switch(task) {
            case GET_FILE_KEY:
                get_file_key();
                break;
            case EXECUTE_BACKUP:
                //backup_file(key, file_name, replication_degree, bFile);
                break;
        }
        
    }

    /**
     * 
     */
    public void get_file_key() {
        System.out.println("Backup is being initiated");

        String file_path = node.get_files_path() + '/' + file_name;
        Path path = Paths.get(file_path);

        // Get file key
        Key key_file = null;
        try {
            key_file = Key.create_key_file(file_path);
            node.store_file_key(key_file.key, path);
            System.err.println("Key File: " + key_file.key);
        } catch (NoSuchAlgorithmException | IOException e) {
            System.err.println("Something went wrong while hashing the file key!\n");
            return;
        }

        // Send to successor of file key
        Message find_succ_msg = new Message(MessageType.FIND_BACKUP_NODE, node.get_address(), node.get_address(), key_file);
        InetSocketAddress successor = node.find_successor_addr(key_file.key, find_succ_msg);
        if (successor != null) {
            send_file(node, key_file, path, successor);
        }
    }

    /**
     * Sends the file to the node that needs to store it.
     */
    public static void send_file(ChordNode sender_node, Key key_file, Path path, InetSocketAddress destination) {
        System.out.println("key: " + key_file);
        System.out.println("successor: " + destination);

        if(path != null) {
            Message msg = new Message(MessageType.BACKUP_FILE, sender_node.get_address(), sender_node.get_address(), key_file);

            byte[] bFile = null;

            // Get file bytes via Java.nio
            try {
                bFile = Files.readAllBytes(path);
            } catch (IOException ex) {
                System.err.println("The file you want to backup was not found\n");
                return;
            }

            msg.set_body(bFile);
            send_message(sender_node, destination , msg);
        }
    }

    /**
     * Store file in the node via Java.nio.
     */
	public void backup_file(long key, String file_name, long replication_degree, byte[] bFile) {

        //create file

        //write to file
        Path path = Paths.get(node.get_files_path() + '/' + file_name);
        try {
            Files.write(path, bFile);
        } catch(IOException ex){
            ex.printStackTrace();
        }

    }
}