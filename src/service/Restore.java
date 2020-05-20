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

public class Restore implements Runnable {

    private static final int SEND_RESTORE_MSG = 0;
    private static final int EXECUTE_RESTORE = 1;
    private static final int SAVE_FILE = 2;
    private static final String FILES_TO_BACKUP_DIR = "files_to_backup";

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
                restore_file();
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
            node.store_file_key(key_file.key, path);
            System.out.println("Key File: " + key_file.key);
        } catch (NoSuchAlgorithmException | IOException e) {
            System.err.println("Something went wrong while hashing the file key!\n");
            return;
        }

        Message find_succ_msg = new Message(MessageType.RESTORE_FILE, node.get_address(), node.get_address(), key_file);
        node.send_restore_msg(key_file.key, find_succ_msg);
        
    }


    /**
     * envia o file
     */
	public void restore_file() {

        String file_path = node.get_files_path() + '/' + node.getFileName(key);
        Path path = Paths.get(file_path);

        if(path != null) {
            Message msg2 = new Message(MessageType.RETRIEVE_FILE, node.get_address(), msg.get_peer_requesting().getHostName() , new Key(msg.get_key()));

            byte[] bFile = null;

            // Get file bytes via Java.nio
            try {
                bFile = Files.readAllBytes(path);
            } catch (IOException ex) {
                System.err.println("The file you want to restore was not found!\n");
                //TODO caso este node fizer reclaim 0, já não vai conter o file e é necessário procurar nos nodes seguintes ?? (por causa do replication degree)
                return;
            }

            msg2.set_body(bFile);
            send_message(node, msg.get_peer_requesting() , msg2);
        }

    }

    /**
     * Store file in the restore directory via Java.nio.
     */
	public void save_file() {
        key = msg.get_key();
        String path_ini =  node.getFilePath(key).toString();
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

    }
}