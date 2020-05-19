package src.network;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static src.utils.Utils.*;

import src.helper.FixFingersThread;
import src.helper.PredecessorThread;
import src.helper.PrintThread;
import src.helper.StabilizeThread;
import src.utils.MessageType;

public class ChordNode implements RMI {

    private final Server server;
    private final Key local_key;
    private final InetSocketAddress local_address;
    private InetSocketAddress predecessor;
    public HashMap<Integer, InetSocketAddress> finger_table;
    private final ExecutorService executor;

    private final StabilizeThread stabilize_thread;
    private final FixFingersThread fixFingers_thread;
    private final PredecessorThread predecessor_thread;
    private final PrintThread print_thread;

    public ChordNode(final InetSocketAddress local_address) {
        // initialize local address
        this.local_address = local_address;

        // initialize local key
        local_key = Key.create_key_from_address(local_address);
        System.out.println("Key: " + local_key);

        // initialize finger table
        finger_table = new HashMap<Integer, InetSocketAddress>();
        for (int i = 1; i <= KEY_SIZE; i++) {
            update_ith_finger(i, null);
        }

        // initialize predecessor
        predecessor = null;

        // initialize helper threads
        executor = Executors.newFixedThreadPool(250);
        stabilize_thread = new StabilizeThread(this, 3000);
        fixFingers_thread = new FixFingersThread(this, 3000);
        predecessor_thread = new PredecessorThread(this, 3000);
        // provisory
        print_thread = new PrintThread(this, 3000);

        // start server
        server = new Server(this, local_address.getPort());
        server.start();


    }

    private void createDirectory(String path) {
        File file = new File(path);
        if (file.mkdirs()) System.out.println("New directory created: " + path);
        else System.out.println("Directory " + path + "already eists");
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

    /* Service Interface */

    public void backup(final String filepath, final int replication_degree) {
        System.out.println("Backup is being initiated");
    }

    public void restore(final String filepath) {
        System.out.println("Restore is being initiated");
    }

    public void delete(final String filepath) {
        System.out.println("Delete is being initiated");
    }

    /* Chord related methods */

    public void update_ith_finger(final int key, final InetSocketAddress value) {
        //join circle recently
        if(get_successor() == null && key == -1)
            finger_table.put(1, value);
        else
            finger_table.put(key, value);
    }

    public void update_successor(final InetSocketAddress value) {
        update_ith_finger(1, value);
        if (value != null && !value.equals(local_address)) {
            notify_successor();
        }
    }

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

    public void start_helper_threads() {
        stabilize_thread.start();
        fixFingers_thread.start();
        predecessor_thread.start();
        print_thread.start();
    }
    
    /****************************** STABILIZE THREAD *******************************/

    public void notify_successor() {
        Message msg = new Message(MessageType.NOTIFY, get_address());
        send_message(this, get_successor(), msg);
    }
    
    /**
     * possible_predecessor thinks it might be your predecessor.
     */ 
    public void notified(final InetSocketAddress possible_predecessor) {

        if(get_predecessor() == null){
            predecessor = possible_predecessor;
            return;
        }
        final Key predecessor_key = Key.create_key_from_address(get_predecessor());
        final Key possible_predecessor_key = Key.create_key_from_address(possible_predecessor);
        if(betweenKeys(predecessor_key.key, possible_predecessor_key.key, local_key.key)){
            predecessor = possible_predecessor;
            return;
        }
    }

    /****************************** FIX FINGERS THREAD ******************************/

    public boolean betweenKeys(final long key0, final long key, final long key1) {
        // if key is between the two keys in the ring: ... key0 -> key -> key1 ...
        if ((key0 < key && key <= key1) || ((key0 >= key1) && (key0 < key || key <= key1))){
            return true;
        }
        return false;
    } 

    /**
     * Finds the successor of key
     */
	public InetSocketAddress find_successor_addr(final long key, final Message message) {

        final Key successor_key = Key.create_key_from_address(get_successor());

        //if key ∈ ]this_node_key, successor_key] then
        if(betweenKeys(this.local_key.key, key, successor_key.key )) {
            return get_successor();
        }

        else { // forward the query around the circle
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
            
                //if finger[i] ∈ ]this_node_key, key] then
                if(betweenKeys(this.local_key.key, finger_i_key.key, key))
                    return finger_table.get(i);
            }
        }
        return local_address;
    }


    /**
     * BACKUP subprotocol
     * 
     * - Sends the file to the circle through a new thread
     * 
     * - After sending the message, the peer collects the confirmation messages during a 
     * time interval of one second to see if that chunk is backed up with the desired 
     * replication degree.
     * 
     * - If the number of confirmation messages it received up to the end of that interval 
     * is lower than the desired replication degree ... nao sei
     * 
     * @param fileBytes
     * @param key
     * @param replication_degree
     */
	public void requestFileBackup(byte[] fileBytes, long key, int replication_degree) {

      /*  Runnable runnable = () -> {

            // Create message
            
            InetSocketAddress successor = get_successor();
            Message msg = new Message(MessageType.REQUEST_BACKUP, get_address());
            Message response = request_message(this, successor, msg);
            

            // Waits the specific time interval for the confirmation messages
            try {
                Thread.sleep(time_interval);
            } catch (InterruptedException e) {
                System.err.println("Peer - Thread was interrupted while waiting for the confirmation messages");
                continue;
            }


            int achievedRepDeg = chunksController.getChunkReplicationDegree(fileID, Integer.toString(chunkNo));
            if (achievedRepDeg < repDegree) {
                System.out.println("Fail to achieved replication degree, only achieved:" + achievedRepDeg);
            } else System.out.println("Successfully backed up chunk "+ chunkNo + " of file " + fileID + ", achieved: " + achievedRepDeg);

        };

        Thread thread = new Thread(runnable);
        thread.start();*/

	} 
}