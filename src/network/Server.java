package src.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

public class Server extends Thread {

    private ChordNode node;
    private SSLServerSocket server_socket;

    public Server(ChordNode node, InetSocketAddress node_address) { 
        this.node = node;
        initialize_server_socket(node_address);
        System.out.println("Server Ready!");
    }

    private void initialize_server_socket(InetSocketAddress node_address) {
        SSLServerSocketFactory ssl_server_socket_factory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        int port = node_address.getPort();
        InetAddress address = node_address.getAddress();

        try {
            server_socket = (SSLServerSocket) ssl_server_socket_factory.createServerSocket(port, 0, address);
            server_socket.setEnabledCipherSuites(server_socket.getSupportedCipherSuites());

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error opening port " + port);
        }

        server_socket.setNeedClientAuth(true);
    }

    @Override
    public void run() {
        while(true) {
            try { 
                final SSLSocket socket = (SSLSocket) server_socket.accept();
                node.get_executor().submit(() -> handle_connection(socket));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handle_connection(SSLSocket socket) {
        ObjectInputStream input = null;
        try {
            input = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        Message message = null;
        try {
            message = (Message) input.readObject();
        } catch (Exception e) {
            System.err.println("Error while reading message!");
        }

        MessageReceiver.handle_message(message, node, socket);
    }
}