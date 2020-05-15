package src.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.SecureRandom;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;

public class SSLClient extends SSLPeer {

    private String remote_address;
	private int port;
    private SSLEngine engine;
    private SocketChannel socket_channel;

    public SSLClient(ChordNode peer, String remote_address, int port) throws Exception {
        this.peer = peer;
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

    public void connect() throws Exception {
        //System.out.println("Requesting connection...");
    	socket_channel = SocketChannel.open();
    	socket_channel.configureBlocking(false);
    	socket_channel.connect(new InetSocketAddress(remote_address, port));
    	while (!socket_channel.finishConnect()) {
    	}

        engine.beginHandshake();
        if (do_handshake(socket_channel, engine)) {
            //System.out.println("Connection established!");
        } else {
            socket_channel.close();
            System.err.println("Connection closed due to handshake failure.");
        }
    }

    protected ByteBuffer read() throws Exception {
       return read(socket_channel, engine);
    }

    protected void write(byte[] message) throws Exception {
        write(socket_channel, engine, message);
    }

    public void shutdown() throws IOException {
        //System.out.println("Requesting to close connection with the server...");
        close_connection(socket_channel, engine);
        executor.shutdown();
    }

}