package src.network;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.security.SecureRandom;
import java.util.Iterator;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;

public class SSLServer extends SSLPeer {

    private boolean active;
    private SSLContext context;
    private Selector selector;

    public SSLServer(String host_address, int port) throws Exception {
        context = SSLContext.getInstance("TLS");
        context.init(create_key_managers("../keys/server.jks", "storepass", "keypass"),
                create_trust_managers("../keys/trustedCerts.jks", "storepass"), new SecureRandom());
        SSLSession ssl_session = context.createSSLEngine().getSession();
        my_app_data = ByteBuffer.allocate(ssl_session.getApplicationBufferSize());
        my_net_data = ByteBuffer.allocate(ssl_session.getPacketBufferSize());
        peer_app_data = ByteBuffer.allocate(ssl_session.getApplicationBufferSize());
        peer_net_data = ByteBuffer.allocate(ssl_session.getPacketBufferSize());
        ssl_session.invalidate();

        selector = SelectorProvider.provider().openSelector();
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.socket().bind(new InetSocketAddress(host_address, port));
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        active = true;
    }

    public static void main(String[] args) {
        String host = "localhost";
        int port = 9222;
        if (args.length == 2) {
            host = args[0];
            port = Integer.parseInt(args[1]);
        }
        try {
            SSLServer server = new SSLServer(host, port);
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isActive() {
        return active;
    }

    public void start() throws Exception {

        System.out.println("Server ready!\nWaiting for new connections...");

        while (isActive()) {
            selector.select();
            Iterator<SelectionKey> selected_keys = selector.selectedKeys().iterator();
            while (selected_keys.hasNext()) {
                SelectionKey key = selected_keys.next();
                selected_keys.remove();
                if (!key.isValid()) {
                    continue;
                }
                if (key.isAcceptable()) {
                    accept(key);
                } else if (key.isReadable()) {
                    read((SocketChannel) key.channel(), (SSLEngine) key.attachment());
                }
            }
        }

        System.out.println("Server closed!");
    }

    public void stop() {
        System.out.println("Will now close server...");
        active = false;
        executor.shutdown();
        selector.wakeup();
    }

    private void accept(SelectionKey key) throws Exception {

        System.out.println("New connection request!");

        SocketChannel socketChannel = ((ServerSocketChannel) key.channel()).accept();
        socketChannel.configureBlocking(false);

        SSLEngine engine = context.createSSLEngine();
        engine.setUseClientMode(false);
        engine.beginHandshake();

        if (do_handshake(socketChannel, engine)) {
            System.out.println("Connection established!");
            socketChannel.register(selector, SelectionKey.OP_READ, engine);
        } else {
            socketChannel.close();
            System.out.println("Connection closed due to handshake failure.");
        }
    }
}