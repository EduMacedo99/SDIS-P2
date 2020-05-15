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
import src.helper.StabilizeThread;
import  src.utils.MessageType;

public class ChordNode implements RMI {

    private ServerRunnable serverRun;
    private Key local_key;
    private InetSocketAddress local_address;
    private InetSocketAddress predecessor;
    public HashMap<Integer, InetSocketAddress> finger_table;
    private ExecutorService executor;
    private String last_response;

    private StabilizeThread stabilize_thread;
    private FixFingersThread fixFingers_thread;
    private PredecessorThread predecessor_thread;

    public ChordNode(InetSocketAddress local_address){
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
        

        // start server
        String host_address = local_address.getAddress().getHostAddress();
        int port = local_address.getPort();
        serverRun = new ServerRunnable(this, host_address, port);
        executor.execute(serverRun);
    }

    public static void main(String[] args) {
        if (args.length != 3 && args.length != 5) {
            System.out.println("Usage: <host_address> <port> <access_point> [<contact_address> <contact_port>]");
            return;
        }

        String host_address = args[0];
        int port = Integer.parseInt(args[1]);
        String access_point = args[2];
        InetSocketAddress local_address = new InetSocketAddress(host_address, port);
        InetSocketAddress contact = local_address;
        if (args.length == 5) {
            String contact_address = args[3];
            int contact_port = Integer.parseInt(args[4]);
            contact = new InetSocketAddress(contact_address, contact_port);
        }

        ChordNode node = new ChordNode(local_address);

        try { /* RMI */
            RMI stub = (RMI) UnicastRemoteObject.exportObject(node, 0);
            Registry registry;
            try {
                registry = LocateRegistry.createRegistry(1099);
                registry.bind(access_point, stub);
            } catch (RemoteException e) {
                registry = LocateRegistry.getRegistry();
                registry.rebind(access_point, stub);
            }
        } catch (Exception e) {
            System.err.println("RMI Exception:");
            e.printStackTrace();
        }

        if (!node.join(contact)) {
            return;
        }

    }

    /* Setters and getters */
    public String get_address() {
        return local_address.getAddress().getHostAddress() + ":" + local_address.getPort();
    }

    public InetSocketAddress get_local_address() {
        return local_address;
    }

    public InetSocketAddress get_predecessor() {
        return predecessor;
    }

    public void set_predecessor(InetSocketAddress predecessor) {
        this.predecessor = predecessor;
    }

    public InetSocketAddress get_successor() {
        return finger_table.get(1);
    }

    public ExecutorService get_executor() {
        return this.executor;
    }

    public String get_last_response(){
        return last_response;
    }

    public void set_last_response(String response) {
        this.last_response = response;
    }

    public Key get_local_key() {
        return local_key;
    }

    /* Service Interface */

    public void backup(String filepath, int replication_degree) {
        System.out.println("Backup is being initiated");
    }

    public void restore(String filepath) {
        System.out.println("Restore is being initiated");
    }

    public void delete(String filepath) {
        System.out.println("Delete is being initiated");
    }

    /* Chord related methods */

    public void update_ith_finger(int key, InetSocketAddress value) {
        //join circle recently
        if(get_successor() == null && key == -1)
            finger_table.put(1, value);
        else
            finger_table.put(key, value);

        // EDU
        /*if (key == 1 && value != null && !value.equals(local_address)) {
            
            //this is probably wrong, dont really know what I should be doing here >:(
            Message msg = new Message(MessageType.PREDECESSOR, get_address());
            MessageSender msg_sender = new MessageSender(this, value, msg);
            executor.execute(msg_sender);
        }*/
    }

    public void update_successor(InetSocketAddress value) {
        update_ith_finger(1, value);
    }

    public boolean join(InetSocketAddress contact) {

        if (contact == null) {
            System.err.println("Contact cannot be null.\nJoin failed");
            return false;
        }
        if (contact.equals(local_address)) {
            // circle is being created
            update_successor(local_address);

        } else {
            // node is joining an existing circle
            Message msg = new Message(MessageType.JOIN, get_address());
            byte[] a = requestMessage(this,  contact, 100, msg);
    

            // this_successor := contact_node.find_successor(this) -> send message 
            Key key = Key.create_key_from_address(local_address);
            System.out.println("Message sent: find successor of key  " + key + " in " + contact);
            Message msg2 = new Message(MessageType.FIND_SUCCESSOR_KEY, get_address());
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = null;
            byte[] keyBytes = null;
            try {
                out = new ObjectOutputStream(bos);   
                out.writeObject((int) key.key);
                out.flush();
                keyBytes = bos.toByteArray();
            } catch (IOException e) {
                e.printStackTrace();
                // ignore exception
            } finally {
                try {
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    // ignore close exception
                }
            }
			msg2.set_body(keyBytes);
            byte[] a5 = requestMessage(this,  contact, 100, msg2);
            

        }

        // start helper threads
        stabilize_thread.start();
        fixFingers_thread.start();
        predecessor_thread.start();

        System.out.println("Joined circle successfully!");
        return true;
    }
    
    /****************************** STABILIZE THREAD *******************************/
    
    /**
     * possible_predecessor thinks it might be your predecessor.
     */ 
    public void notify(InetSocketAddress possible_predecessor) {

        //if predecessor is null or ...
        if(get_predecessor() == null){
            predecessor = possible_predecessor;
            return;
        }
        // ... possible_predecessor ∈ (predecessor, n) then
        Key predecessor_key = Key.create_key_from_address(get_predecessor());
        Key possible_predecessor_key = Key.create_key_from_address(possible_predecessor);
        if(betweenKeys(predecessor_key.key, possible_predecessor_key.key, local_key.key)){
            predecessor = possible_predecessor;
            return;
        }

        // EDU
        /*if (successor.equals(this.get_local_address())) {
            System.out.println("successor is self :/");
            return false;
        }

        Message msg = new Message(MessageType.PREDECESSOR, get_address());
        MessageSender msg_sender = new MessageSender(this, successor, msg);
        executor.execute(msg_sender);
       
        return true;
        */
    }

    /****************************** FIX FINGERS THREAD ******************************/

    public boolean betweenKeys(long key0, long key, long key1) {
        // if key is between the two keys in the ring: ... key0 -> key -> key1 ...
        if ((key0 < key && key <= key1) || ((key0 >= key1) && (key0 < key || key <= key1))){
            return true;
        }
        return false;
    } 

    /**
     * ask this node to find the successor of key
     */
	public InetSocketAddress find_successor_addr(int key) {

        Key successor_key = Key.create_key_from_address(get_successor());

        //if key ∈ ]this_node_key, successor_key] then
        if(betweenKeys(this.local_key.key, key, successor_key.key ))
            return get_successor();

        // forward the query around the circle
        else{
            InetSocketAddress n0_addr = closest_preceding_node_addr(key);
            if(n0_addr == local_address)
                return local_address;

            //return n0_addr.find_successor(key) -> send message 
            System.out.println("Message sent: find successor of key  " + key + " in " + n0_addr);
            Message msg = new Message(MessageType.FIND_SUCCESSOR_KEY, get_address());
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = null;
            byte[] keyBytes = null;
            try {
                out = new ObjectOutputStream(bos);   
                out.writeObject(key);
                out.flush();
                keyBytes = bos.toByteArray();
            } catch (IOException e) {
                // ignore exception
            } finally {
                try {
                    bos.close();
                } catch (IOException ex) {
                    // ignore close exception
                }
            }
			msg.set_body(keyBytes);
            byte[] a = requestMessage(this,  n0_addr, 100, msg);

        }

        return null;
	}

    /**
     * search the local table for the highest predecessor of key
     */
    private InetSocketAddress closest_preceding_node_addr(int key) {

        for (int i = 1; i <= KEY_SIZE; i++) {
            if(finger_table.get(i) != null){
                Key finger_i_key = Key.create_key_from_address(finger_table.get(i));
            
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

    public ServerRunnable(ChordNode peer, String ip, int port) {
        try {
            server = new SSLServer(peer, ip, port);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}