package src.service;

import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import src.network.ChordNode;
import src.network.RMI;

public class Peer {
    public static void main(final String[] args) {
        if (args.length != 2 && args.length != 4) {
            System.out.println("Usage: <port> <access_point> [<contact_address> <contact_port>]");
            return;
        }

        final String host_address = "localhost";
        final int port = Integer.parseInt(args[0]);
        final String access_point = args[1];
        final InetSocketAddress local_address = new InetSocketAddress(host_address, port);
        InetSocketAddress contact = local_address;
        if (args.length == 4) {
            final String contact_address = args[2];
            final int contact_port = Integer.parseInt(args[3]);
            contact = new InetSocketAddress(contact_address, contact_port);
        }

        System.setProperty("javax.net.ssl.keyStore", "../keys/server.keys");
        System.setProperty("javax.net.ssl.keyStorePassword", "123456");
        System.setProperty("javax.net.ssl.trustStore", "../keys/truststore");
        System.setProperty("javax.net.ssl.trustStorePassword", "123456");

        ChordNode node = new ChordNode(local_address);

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

        node.join(contact);
    }

}