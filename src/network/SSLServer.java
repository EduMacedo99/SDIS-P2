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
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLSession;

public class SSLServer extends SSLPeer {

    private boolean active;
    private SSLContext context;
    private Selector selector;

    public SSLServer(ChordNode peer, String host_address, int port) throws Exception {
        this.peer = peer;
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
                    read((SocketChannel) key.channel(), (SSLEngine) key.attachment() );
                }
            }
        }

        System.out.println("Server closed!");
    }

    protected void read(SocketChannel socket_channel, SSLEngine engine) throws Exception {
        
        peer_net_data.clear();
        int bytesRead = socket_channel.read(peer_net_data);
        if (bytesRead > 0) {
            peer_net_data.flip();
            while (peer_net_data.hasRemaining()) {
                peer_app_data.clear();
                SSLEngineResult result = engine.unwrap(peer_net_data, peer_app_data);
                switch (result.getStatus()) {
                    case OK:
                        peer_app_data.flip();
                        peer.get_executor().execute(new MessageReceiver(peer_app_data, peer, socket_channel, engine ));
                        break;
                    case BUFFER_OVERFLOW:
                        peer_app_data = handle_overflow_application(engine, peer_app_data);
                        break;
                    case BUFFER_UNDERFLOW:
                        peer_net_data = handle_buffer_underflow(engine, peer_net_data);
                        break;
                    case CLOSED:
                        //System.out.println("Client requested to close the connection...");
                        close_connection(socket_channel, engine);
                        return;
                    default:
                        throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
                }
            }

        } else if (bytesRead < 0) {
            System.err.println("Received end of stream. Will try to close connection with client...");
            handle_end_of_stream(socket_channel, engine);
        }
    }

    public void stop() {
        System.out.println("Will now close server...");
        active = false;
        executor.shutdown();
        selector.wakeup();
    }

    private void accept(SelectionKey key) throws Exception {

        //System.out.println("New connection request!");

        SocketChannel socketChannel = ((ServerSocketChannel) key.channel()).accept();
        socketChannel.configureBlocking(false);

        SSLEngine engine = context.createSSLEngine();
        engine.setUseClientMode(false);
        engine.beginHandshake();

        if (do_handshake(socketChannel, engine)) {
            //System.out.println("Connection established!");
            socketChannel.register(selector, SelectionKey.OP_READ, engine);
        } else {
            socketChannel.close();
            System.out.println("Connection closed due to handshake failure.");
        }
    }
}