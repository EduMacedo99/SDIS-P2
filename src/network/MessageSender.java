package src.network;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class MessageSender {

    public static void send_message(Message msg, SSLSocket socket) {
        try {
            ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
            output.writeObject(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static SSLSocket send_message(Message msg, InetSocketAddress server) {
        SSLSocket socket = connect(server);
        try {
            ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
            output.writeObject(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return socket;
    }

    private static SSLSocket connect(InetSocketAddress server) {
        SSLSocketFactory ssl_socket_factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket socket = null;

        try {
            socket = (SSLSocket) ssl_socket_factory.createSocket(server.getAddress(), server.getPort());
        } catch (IOException e) {
            e.printStackTrace();
        }

        socket.setEnabledCipherSuites(socket.getSupportedCipherSuites());

        return socket;
    }
}