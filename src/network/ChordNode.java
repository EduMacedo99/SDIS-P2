package src.network;

import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;

import static src.utils.Utils.*;

public class ChordNode implements RMI {

    ServerRunnable serverRun;
    Key local_key;
    InetSocketAddress local_address;
    InetSocketAddress predecessor;
    private HashMap<Integer, InetSocketAddress> finger_table;

    public ChordNode(InetSocketAddress local_address) {
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

        // start server
        String host_address = local_address.getAddress().getHostAddress();
        int port = local_address.getPort();
        serverRun = new ServerRunnable(host_address, port);
        new Thread(serverRun).start();
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
            //TODO notify successor
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
            // TODO search for successor through contact
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

    public ServerRunnable(String ip, int port) {
        try {
            server = new SSLServer(ip, port);
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