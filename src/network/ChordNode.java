package src.network;

import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.net.UnknownHostException;

import static src.utils.Utils.*;

import src.helper.UpdateFingersThread;
import src.helper.HelperThread;
import src.helper.PredecessorThread;
import  src.utils.MessageType;

public class ChordNode implements RMI {

    private ServerRunnable serverRun;
    private Key local_key;
    private InetSocketAddress local_address;
    private InetSocketAddress predecessor;
    private HashMap<Integer, InetSocketAddress> finger_table;
    private ExecutorService executor;
    private String last_response;

    private HelperThread checkFingers;
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
            update_ith_finger(i, local_address);
        }

        // initialize predecessor
        predecessor = null;

        //TODO initialize helper threads
        executor = Executors.newFixedThreadPool(250);
        checkFingers = new UpdateFingersThread(this, 1000);
        predecessor_thread = new PredecessorThread(this);
        //predecessor_thread.start();


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

    public InetSocketAddress get_predecessor() {
        return predecessor;
    }

    public ExecutorService get_executor() {
        return this.executor;
    }

    public String get_last_responde(){
        return last_response;
    }

    public void set_last_responde(String response) {
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
        finger_table.put(key, value);

        if (key == 1 && value != null && !value.equals(local_address)) {
            Message msg = new Message(MessageType.PREDECESSOR, get_address());
            MessageSender msg_sender = new MessageSender(this, value, msg);
            executor.execute(msg_sender);
        }
    }

    public void update_successor(InetSocketAddress value) {
        update_ith_finger(1, value);
    }

    public boolean join(InetSocketAddress contact) {
        if (contact == null) {
            System.err.println("Contact cannot be null.\nJoin failed");
            return false;
        }
        InetSocketAddress successor;
        if (contact.equals(local_address)) {
            // circle is being created
            successor = local_address;
        } else {
            // node is joining an existing circle
            Message msg = new Message(MessageType.JOIN, get_address());
            MessageSender msg_sender = new MessageSender(this, contact, msg);
            executor.execute(msg_sender); 
            successor = null;
        }
        update_successor(successor);

        // TODO start helper threads
        this.checkFingers.start();

        System.out.println("Joined circle successfully!");
        return true;
    }

    public boolean betweenKeys(long key0, long key, long key1) {
        // if key is between the two keys in the ring: ... key0 -> key -> key1 ...
        //System.out.println("*---------------------- " + key0 + "---" + key + "---" + key1);
        if ((key0 < key && key <= key1) || ((key0 > key1) && (key0 < key || key <= key1))){
            return true;
        }
        return false;
    } 

    public InetSocketAddress getSuccessor(long key) {

        InetSocketAddress successor_addr = null;

        // If node joined recently in the circle and has no info, ask predecessor
        if(finger_table.get(1) == null){
            System.out.println("Sending message to " + predecessor + " to search key " + key );
            Message msg = new Message(MessageType.SEARCH_SUCCESSOR_KEY, get_address());
            msg.set_body(("" + key).getBytes());
            MessageSender msg_sender = new MessageSender(this, predecessor, msg);
            executor.execute(msg_sender); 

            //TODO: get response
            return null;
        }
   
        // Search in Finger Table who has the key 
        for (Entry<Integer, InetSocketAddress> i : finger_table.entrySet()) {
            if (i.getValue() != null){
                successor_addr = i.getValue();
                // If key is between the two nodes
                //long successor_i_key = (long) ((local_key.key + Math.pow(2, i.getKey() - 1)) % Math.pow(2, KEY_SIZE));
                Key successor_i = Key.create_key_from_address(successor_addr);
                if(betweenKeys(this.local_key.key, key, successor_i.key )){
                    return successor_addr;
                }
            }
        }

        // Only one node in the ring
        if(successor_addr == local_address){
            return local_address;
        }
        // Send search for key to the largest successor/finger entry
        else if(successor_addr != null){
            System.out.println("Sending message to " + successor_addr + " to search key " + key );
            Message msg = new Message(MessageType.SEARCH_SUCCESSOR_KEY, get_address());
            msg.set_body(("" + key).getBytes());
            MessageSender msg_sender = new MessageSender(this, successor_addr, msg);
            executor.execute(msg_sender); 

            //TODO: get response

        }else
            System.err.println("No nodes to send shearch successor key message!!");

        return null;
    }

	public void updateJoinFingers(InetSocketAddress new_addr) {
        Key new_key = Key.create_key_from_address(new_addr);

        for (int i = 1; i <= KEY_SIZE; i++) {
            long i_key = (long) ((local_key.key + Math.pow(2, i - 1)) % Math.pow(2, KEY_SIZE));
            if(betweenKeys(this.local_key.key, new_key.key, i_key )){
                update_ith_finger(i, new_addr);
            }
        }
    }

	public void startJoinFingers(InetSocketAddress new_addr) {
        predecessor = new_addr;
        //for (int i = 1; i <= KEY_SIZE; i++) 
        //   update_ith_finger(i, new_addr);
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