package src.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.SecureRandom;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLSession;

public class SSLClient extends SSLPeer {

    private String remote_address;
	private int port;
    private SSLEngine engine;
    private SocketChannel socket_channel;

    public SSLClient(String remote_address, int port) throws Exception {
        this.remote_address = remote_address;
    	this.port = port;

        SSLContext context = SSLContext.getInstance("TLS");
        context.init(create_key_managers("../keys/client.jks", "storepass", "keypass"), create_trust_managers("../keys/trustedCerts.jks", "storepass"), new SecureRandom());
        engine = context.createSSLEngine(remote_address, port);
        engine.setUseClientMode(true);

        SSLSession session = engine.getSession();
        my_app_data = ByteBuffer.allocate(1024);
        my_net_data = ByteBuffer.allocate(session.getPacketBufferSize());
        peer_app_data = ByteBuffer.allocate(1024);
        peer_net_data = ByteBuffer.allocate(session.getPacketBufferSize());
    }

    public static void main(String[] args) throws Exception {
        SSLClient client = new SSLClient("localhost", 9222);
        client.connect();
        client.write("Hello! I am a client!");
		client.read();
		client.shutdown();
    }

    public void connect() throws Exception {
        System.out.println("Requesting connection...");
    	socket_channel = SocketChannel.open();
    	socket_channel.configureBlocking(false);
    	socket_channel.connect(new InetSocketAddress(remote_address, port));
    	while (!socket_channel.finishConnect()) {
    	}

        engine.beginHandshake();
        if (do_handshake(socket_channel, engine)) {
            System.out.println("Connection established!");
        } else {
            socket_channel.close();
            System.err.println("Connection closed due to handshake failure.");
        }
    }

    protected void read() throws Exception {
        read(socket_channel, engine);
    }

    @Override
    protected void read(SocketChannel socket_channel, SSLEngine engine) throws Exception {
        System.out.println("Reading from the server...");

        peer_net_data.clear();
        int waitToReadMillis = 50;
        boolean exitReadLoop = false;
        while (!exitReadLoop) {
            int bytesRead = socket_channel.read(peer_net_data);
            if (bytesRead > 0) {
                peer_net_data.flip();
                while (peer_net_data.hasRemaining()) {
                    peer_app_data.clear();
                    SSLEngineResult result = engine.unwrap(peer_net_data, peer_app_data);
                    switch (result.getStatus()) {
                    case OK:
                        peer_app_data.flip();
                        handle_server_response(peer_app_data);
                        exitReadLoop = true;
                        break;
                    case BUFFER_OVERFLOW:
                        peer_app_data = handle_overflow_application(engine, peer_app_data);
                        break;
                    case BUFFER_UNDERFLOW:
                        peer_net_data = handle_buffer_underflow(engine, peer_net_data);
                        break;
                    case CLOSED:
                        close_connection(socket_channel, engine);
                        return;
                    default:
                        throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
                    }
                }
            } else if (bytesRead < 0) {
                handle_end_of_stream(socket_channel, engine);
                return;
            }
            Thread.sleep(waitToReadMillis);
        }
    }

    private void handle_server_response(ByteBuffer peer_app_data) {
        System.out.println("Server response: " + new String(peer_app_data.array(), 0, 30));
    }

    protected void write(String message) throws Exception {
        write(socket_channel, engine, message);
    }

    @Override
    protected void write(SocketChannel socket_channel, SSLEngine engine, String message) throws Exception {
        System.out.println("Writing to the server...");

        my_app_data.clear();
        my_app_data.put(message.getBytes());
        my_app_data.flip();
        while (my_app_data.hasRemaining()) {
            // The loop has a meaning for (outgoing) messages larger than 16KB.
            // Every wrap call will remove 16KB from the original message and send it to the remote peer.
            my_net_data.clear();
            SSLEngineResult result = engine.wrap(my_app_data, my_net_data);
            switch (result.getStatus()) {
            case OK:
                my_net_data.flip();
                while (my_net_data.hasRemaining()) {
                    socket_channel.write(my_net_data);
                }
                System.out.println("Message sent to the server: " + message);
                break;
            case BUFFER_OVERFLOW:
                my_net_data = handle_overflow_packet(engine, my_net_data);
                break;
            case CLOSED:
                close_connection(socket_channel, engine);
                return;
            default:
                throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
            }
        }
    }

    public void shutdown() throws IOException {
        System.out.println("Requesting to close connection with the server...");
        close_connection(socket_channel, engine);
        executor.shutdown();
    }

}