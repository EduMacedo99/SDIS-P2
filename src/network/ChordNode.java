package src.network;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static src.utils.Utils.*;

import src.helper.FixFingersThread;
import src.helper.PredecessorThread;
import src.helper.PrintThread;
import src.helper.StabilizeThread;
import  src.utils.MessageType;

public class ChordNode implements RMI {

    private final ServerRunnable serverRun;
    private final Key local_key;
    private final InetSocketAddress local_address;
    private InetSocketAddress predecessor;
    public HashMap<Integer, InetSocketAddress> finger_table;
    private final ExecutorService executor;
    private String last_response;

    private final StabilizeThread stabilize_thread;
    private final FixFingersThread fixFingers_thread;
    private final PredecessorThread predecessor_thread;
    private final PrintThread print_thread;

    public ChordNode(final InetSocketAddress local_address){
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
        predecessor_thread = new PredecessorThread(this);
        // provisory
        print_thread = new PrintThread(this, 3000);

        
        // start server
        final String host_address = local_address.getAddress().getHostAddress();
        final int port = local_address.getPort();
        serverRun = new ServerRunnable(this, host_address, port);
        executor.execute(serverRun);
    }

    public static void main(final String[] args) {
        if (args.length != 3 && args.length != 5) {
            System.out.println("Usage: <host_address> <port> <access_point> [<contact_address> <contact_port>]");
            return;
        }

        final String host_address = args[0];
        final int port = Integer.parseInt(args[1]);
        final String access_point = args[2];
        final InetSocketAddress local_address = new InetSocketAddress(host_address, port);
        InetSocketAddress contact = local_address;
        if (args.length == 5) {
            final String contact_address = args[3];
            final int contact_port = Integer.parseInt(args[4]);
            contact = new InetSocketAddress(contact_address, contact_port);
        }

        final ChordNode node = new ChordNode(local_address);

        try { /* RMI */
            final RMI stub = (RMI) UnicastRemoteObject.exportObject(node, 0);
            Registry registry;
            try {
                registry = LocateRegistry.createRegistry(1099);
                registry.bind(access_point, stub);
            } catch (final RemoteException e) {
                registry = LocateRegistry.getRegistry();
                registry.rebind(access_point, stub);
            }
        } catch (final Exception e) {
            System.err.println("RMI Exception:");
            e.printStackTrace();
        }

        if (!node.join(contact)) {
            return;
        }

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

    public String get_last_response(){
        return last_response;
    }

    public void set_last_response(final String response) {
        this.last_response = response;
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
            // circle is being created
            update_successor(local_address);
            start_helper_threads();

        } else {
            // node is joining an existing circle   <TYPE> <SENDER_ID> <PEER_REQUESTING> <KEY>

            final Message msg = new Message(MessageType.FIND_SUCCESSOR_KEY, get_address(), get_address(), local_key);

            send_message(this, contact, msg);

        }

        return true;
    }

    public void start_helper_threads() {
        stabilize_thread.start();
        //fixFingers_thread.start();
        //predecessor_thread.start();
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
        if(betweenKeys(this.local_key.key, key, successor_key.key ))
            return get_successor();

        // forward the query around the circle
        else{
            final InetSocketAddress n0_addr = closest_preceding_node_addr(key);
            if(n0_addr == local_address)
                return local_address;

            //return n0_addr.find_successor(key) -> send message 
            System.out.println("Message sent: find successor of key  " + key + " in " + n0_addr);
            final Message msg = new Message(MessageType.FIND_SUCCESSOR_KEY, get_address());
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = null;
            byte[] keyBytes = null;
            try {
                out = new ObjectOutputStream(bos);   
                out.writeObject(key);
                out.flush();
                keyBytes = bos.toByteArray();
            } catch (final IOException e) {
                // ignore exception
            } finally {
                try {
                    bos.close();
                } catch (final IOException ex) {
                    // ignore close exception
                }
            }
			msg.set_body(keyBytes);
            requestMessage(this,  n0_addr, 100, msg);

        }

        return null;
	}

    /**
     * search the local table for the highest predecessor of key
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
}

class ServerRunnable implements Runnable {

    SSLServer server;

    public ServerRunnable(final ChordNode peer, final String ip, final int port) {
        try {
            server = new SSLServer(peer, ip, port);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            server.start();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

}