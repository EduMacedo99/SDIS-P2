package src.network;

import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.net.UnknownHostException;

import static src.utils.Utils.*;
import  src.utils.MessageType;

public class ChordNode implements RMI {

    private ServerRunnable serverRun;
    private Key local_key;
    private InetSocketAddress local_address;
    private InetSocketAddress predecessor;
    private HashMap<Integer, InetSocketAddress> finger_table;
    private ExecutorService executor;

    public ChordNode(InetSocketAddress local_address){
        // initialize local address
        this.local_address = local_address;

        // initialize local key
        local_key = Key.create_key_from_address(local_address);
        System.out.println("Key: " + local_key);

        // initialize finger table
        finger_table = new HashMap<Integer, InetSocketAddress>();
        for (int i = 1; i < KEY_SIZE; i++) {
            update_ith_finger(i, null);
        }

        // initialize predecessor
        predecessor = null;

        //TODO initialize helper threads
        executor = Executors.newFixedThreadPool(250);

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

    private void update_ith_finger(int key, InetSocketAddress value) {
        finger_table.put(key, value);

        if (key == 1 && value != null && !value.equals(local_address)) {
            //not sure if okay but think so
            Message msg = new Message(MessageType.PREDECESSOR, get_address());
            MessageSender msg_sender = new MessageSender(this, value, msg);
            executor.execute(msg_sender);
        }
    }

    private void update_successor(InetSocketAddress value) {
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

        System.out.println("Joined circle successfully!");
        return true;
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