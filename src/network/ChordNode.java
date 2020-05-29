package src.network;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.nio.file.Path;

import src.helper.FixFingersThread;
import src.helper.PredecessorThread;
import src.helper.StabilizeThread;
import src.service.Backup;
import src.service.Delete;
import src.service.Reclaim;
import src.service.Restore;
import src.service.State;

import static src.utils.Utils.*;

import src.utils.Disk;
import src.utils.FileInfo;
import src.utils.MessageType;

public class ChordNode implements RMI {

    private final Server server;
    private final Key local_key;
    private final InetSocketAddress local_address;
    private final ExecutorService executor;
    
    private final StabilizeThread stabilize_thread;
    private final FixFingersThread fixFingers_thread;
    private final PredecessorThread predecessor_thread;
    
    private InetSocketAddress predecessor;
    private HashMap<Integer, InetSocketAddress> finger_table;
    private String files_path;

    private Disk disk = new Disk();
    private ConcurrentHashMap<Long, FileInfo> files_list = new ConcurrentHashMap<Long, FileInfo>();
    private ConcurrentHashMap<Long, String> files_backed_up = new ConcurrentHashMap<Long, String>();
    private ConcurrentHashMap<Long, Path> files_restored = new ConcurrentHashMap<Long, Path>();
    private HashSet<Long> cancelled_backups = new HashSet<Long>();

    public ChordNode(final InetSocketAddress local_address) {
        // Initialize local address
        this.local_address = local_address;

        // Initialize local key
        local_key = Key.create_key_from_address(local_address);
        System.out.println("Key: " + local_key);

        // Initialize finger table
        finger_table = new HashMap<Integer, InetSocketAddress>();
        for (int i = 1; i <= KEY_SIZE; i++) {
            update_ith_finger(i, null);
        }

        // Initialize predecessor
        predecessor = null;

        // Initialize helper threads
        executor = Executors.newFixedThreadPool(250);
        stabilize_thread = new StabilizeThread(this, 3000);
        fixFingers_thread = new FixFingersThread(this, 3000);
        predecessor_thread = new PredecessorThread(this, 3000);

        // Start server
        server = new Server(this, local_address);
        server.start();

        // Initialize file system
        files_path = "peers/" + local_key.key + "/files";
        create_directory(files_path);
        create_directory("peers/" + local_key.key + "/restore");
    }

    /* Setters and getters */

    public String get_address() {
        return address_to_string(local_address);
    }

    public InetSocketAddress get_local_address() {
        return local_address;
    }

    public InetSocketAddress get_predecessor() {
        return predecessor;
    }

    public void set_predecessor(final InetSocketAddress predecessor) {
        this.predecessor = predecessor;
    }

    public InetSocketAddress get_successor() {
        return finger_table.get(1);
    }

    public InetSocketAddress get_ith_finger(final int i) {
        return finger_table.get(i);
    }

    public ExecutorService get_executor() {
        return this.executor;
    }

    public Key get_local_key() {
        return local_key;
    }

    public String get_files_path() {
        return files_path;
    }

    public Disk get_disk() {
        return disk;
    }

    public ConcurrentHashMap<Long, String> get_files_backed_up() {
        return files_backed_up;
    }

    public ConcurrentHashMap<Long, FileInfo> get_files_list() {
        return files_list;
    }

    /* Service Interface */

    public void backup(final String file_name, final int replication_degree) {
        System.out.println("Backup is being initiated");
        executor.submit(new Backup(this, file_name, replication_degree));
    }

    public void restore(final String filepath) {
        System.out.println("Restore is being initiated");
        executor.submit(new Restore(this, filepath));
    }

    public void delete(final String filepath) {
        System.out.println("Delete is being initiated");
        executor.submit(new Delete(this, filepath));
    }

    public void reclaim(final int disk_space_to_reclaim) {
        System.out.println("Reclaim is being initiated");
        executor.submit(new Reclaim(this, disk_space_to_reclaim));
    }

    public void state() {
        System.out.println(" *** State request ***");
        executor.submit(new State(this));
    }

    /* Chord related methods */

    /**
     * Updates the ith finger tuple, assigning the specified value to it.
     */
    public void update_ith_finger(final int ith, final InetSocketAddress value) {
        // Join circle recently
        if(get_successor() == null && ith == -1)
            finger_table.put(1, value);
        else
            finger_table.put(ith, value);
    }

    /**
     * Updates the sucessor node.
     */
    public void update_successor(final InetSocketAddress value) {
        update_ith_finger(1, value);
        if (value != null && !value.equals(local_address)) {
            notify_successor();
        }
    }

    /**
     * Handles the join process of a node in the circle.
     */
    public boolean join(final InetSocketAddress contact) {

        if (contact == null) {
            System.err.println("Contact cannot be null.\nJoin failed");
            return false;
        }
        if (contact.equals(local_address)) {
            // Circle is being created
            update_successor(local_address);
            start_helper_threads();

        } else {
            // Node is joining an existing circle   <TYPE> <SENDER_ID> <PEER_REQUESTING> <KEY>
            final Message msg = new Message(MessageType.FIND_SUCCESSOR_KEY, get_address(), get_address(), local_key);
            send_message(this, contact, msg);
        }
        return true;
    }

    /**
     * Puts the auxiliary Chord threads to run.
     */
    public void start_helper_threads() {
        stabilize_thread.start();
        fixFingers_thread.start();
        predecessor_thread.start();
    }

    /**
     * Sends a simple notification to the sucessor node.
     */
    public void notify_successor() {
        Message msg = new Message(MessageType.NOTIFY, get_address());
        send_message(this, get_successor(), msg);
    }
    
    /**
     * Updates predecessor after being notified.
     */ 
    public void notified(final InetSocketAddress possible_predecessor) {
        if(get_predecessor() == null){
            predecessor = possible_predecessor;
            return;
        }
        final Key predecessor_key = Key.create_key_from_address(get_predecessor());
        final Key possible_predecessor_key = Key.create_key_from_address(possible_predecessor);
        if(Key.betweenKeys(predecessor_key.key, possible_predecessor_key.key, local_key.key)){
            predecessor = possible_predecessor;
            return;
        }
    }

    /**
     * Finds the successor of key.
     */
	public InetSocketAddress find_successor_addr(final long key, final Message message) {

        final Key successor_key = Key.create_key_from_address(get_successor());

        // If key ∈ ]this_node_key, successor_key] then
        if(Key.betweenKeys(this.local_key.key, key, successor_key.key )) {
            return get_successor();
        } else { 
            // Forward the query around the circle
            final InetSocketAddress n0_addr = closest_preceding_node_addr(key);
            if(n0_addr.equals(local_address))
                return local_address;

            send_message(this, n0_addr, message);
        }

        return null;
	}

    /**
     * Searches the local table for the highest predecessor of key.
     */
    private InetSocketAddress closest_preceding_node_addr(final long key) {

        for (int i = KEY_SIZE; i >= 1; i--) {
            if(finger_table.get(i) != null){
                final Key finger_i_key = Key.create_key_from_address(finger_table.get(i));
            
                // If finger[i] ∈ ]this_node_key, key] then
                if(Key.betweenKeys(this.local_key.key, finger_i_key.key, key))
                    return finger_table.get(i);
            }
        }
        return local_address;
    }

    /* Information retrieval & update */

    public void add_cancelled_backup(long key) {
        cancelled_backups.add(key);
    }

    public boolean contains_cancelled_backup(long key) {
        return cancelled_backups.contains(key);
    }

    public void remove_cancelled_backup(long key) {
        cancelled_backups.remove(key);
    }

    public boolean is_responsible_for_key(long key) {
        Key predecessor_key = Key.create_key_from_address(get_predecessor());
        return Key.betweenKeys(predecessor_key.key, key, local_key.key);
    }
    
    public void store_file_key(Long file_key, Path file_path, int replication_degree) {
        FileInfo file_info = new FileInfo(file_path, replication_degree);
        files_list.put(file_key, file_info);
    }

    public void store_restore_file(Long file_key, Path file_path) {
        files_restored.put(file_key, file_path);
    }
    
    public void deleteFile_files_list(long key) {
        files_list.remove(key);
    }

    public void delete_restore_file(long key) {
        files_restored.remove(key);
    }

    public Path get_file_path(long key) {
        FileInfo fi;
        if ((fi = files_list.get(key)) == null) {
            return null;
        }
        return fi.path;
    }

    public int get_file_rep_degree(long key) {
        return files_list.get(key).replication_degree;
    }

    public Path get_restore_file_path(long key) {
        return files_restored.get(key);
    }

    public void store_files_backed_up_key(Long file_key, String file_path) {
        files_backed_up.put(file_key, file_path);
    }

	public boolean has_file(long key) {
        return files_backed_up.containsKey(key);
    }

    public String get_backed_up_file_name(long key) {
        return files_backed_up.get(key);
    }
    
    public void deleteFile_files_backed_up(long key) {
        files_backed_up.remove(key);
    }
}