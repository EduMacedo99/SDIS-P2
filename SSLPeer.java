import javax.net.ssl.*;
import javax.net.ssl.SSLEngineResult.*;
import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.nio.*;
import java.nio.channels.SocketChannel;
import java.security.KeyStore;

public abstract class SSLPeer {

    protected ByteBuffer my_app_data;
    protected ByteBuffer my_net_data;
    protected ByteBuffer peer_app_data;
    protected ByteBuffer peer_net_data;

    protected ExecutorService executor = Executors.newSingleThreadExecutor();
    protected abstract void read(SocketChannel socketChannel, SSLEngine engine) throws Exception;
    protected abstract void write(SocketChannel socketChannel, SSLEngine engine, String message) throws Exception;

    protected boolean has_finished(HandshakeStatus handshake_status) {
        return handshake_status == SSLEngineResult.HandshakeStatus.FINISHED || handshake_status == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING;
    }

    protected boolean do_handshake(SocketChannel socketChannel, SSLEngine engine) throws IOException {

        System.out.println("Beggining handshake...");

        SSLEngineResult result;
        HandshakeStatus handshake_status;

        int app_buffer_size = engine.getSession().getApplicationBufferSize();
        my_app_data = ByteBuffer.allocate(app_buffer_size);
        peer_app_data = ByteBuffer.allocate(app_buffer_size);
        my_net_data.clear();
        peer_net_data.clear();

        handshake_status = engine.getHandshakeStatus();
        while (!has_finished(handshake_status)) {
            switch (handshake_status) {
            case NEED_UNWRAP:
                if (socketChannel.read(peer_net_data) < 0) {
                    if (engine.isInboundDone() && engine.isOutboundDone()) {
                        return false;
                    }
                    engine.closeInbound();
                    engine.closeOutbound();
                    handshake_status = engine.getHandshakeStatus();
                    break;
                }
                peer_net_data.flip();
                result = engine.unwrap(peer_net_data, peer_app_data);
                peer_net_data.compact();
                handshake_status = result.getHandshakeStatus();

                switch (result.getStatus()) {
                case OK:
                    break;
                case BUFFER_OVERFLOW:
                    peer_app_data = handle_overflow_application(engine, peer_app_data);
                    break;
                case BUFFER_UNDERFLOW:
                    peer_net_data = handle_buffer_underflow(engine, peer_net_data);
                    break;
                case CLOSED:
                    if (engine.isOutboundDone()) {
                        return false;
                    } else {
                        engine.closeOutbound();
                        handshake_status = engine.getHandshakeStatus();
                        break;
                    }
                default:
                    throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
                }
                break;
            case NEED_WRAP:
                my_net_data.clear();
                try {
                    result = engine.wrap(my_app_data, my_net_data);
                    handshake_status = result.getHandshakeStatus();
                } catch (SSLException sslException) {
                    System.err.println("A problem was encountered while processing the data that caused the SSLEngine to abort. Will try to properly close connection...");
                    engine.closeOutbound();
                    handshake_status = engine.getHandshakeStatus();
                    break;
                }
                switch (result.getStatus()) {
                case OK :
                    my_net_data.flip();
                    while (my_net_data.hasRemaining()) {
                        socketChannel.write(my_net_data);
                    }
                    break;
                case BUFFER_OVERFLOW:
                    my_net_data = handle_overflow_packet(engine, my_net_data);
                    break;
                case CLOSED:
                    my_net_data.flip();
                    while (my_net_data.hasRemaining()) {
                        socketChannel.write(my_net_data);
                    }
                    peer_net_data.clear();
                    break;
                default:
                    throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
                }
                break;
            case NEED_TASK:
                Runnable task;
                while ((task = engine.getDelegatedTask()) != null) {
                    executor.execute(task);
                }
                handshake_status = engine.getHandshakeStatus();
                break;
            case FINISHED:
                break;
            case NOT_HANDSHAKING:
                break;
            default:
                throw new IllegalStateException("Invalid SSL status: " + handshake_status);
            }
        }

        return true;

    }

    protected ByteBuffer handle_overflow_packet(SSLEngine engine, ByteBuffer buffer) {
        return handle_overflow(buffer, engine.getSession().getPacketBufferSize());
    }

    protected ByteBuffer handle_overflow_application(SSLEngine engine, ByteBuffer buffer) {
        return handle_overflow(buffer, engine.getSession().getApplicationBufferSize());
    }

    protected ByteBuffer handle_overflow(ByteBuffer buffer, int capacity_proposed) {
        if (capacity_proposed > buffer.capacity()) {
            buffer = ByteBuffer.allocate(capacity_proposed);
        } else {
            buffer = ByteBuffer.allocate(buffer.capacity() * 2);
        }
        return buffer;
    }

    protected ByteBuffer handle_buffer_underflow(SSLEngine engine, ByteBuffer buffer) {
        if (engine.getSession().getPacketBufferSize() < buffer.limit()) {
            return buffer;
        } else {
            ByteBuffer replaceBuffer = handle_overflow_packet(engine, buffer);
            buffer.flip();
            replaceBuffer.put(buffer);
            return replaceBuffer;
        }
    }

    protected void close_connection(SocketChannel socket_channel, SSLEngine engine) throws IOException  {
        engine.closeOutbound();
        do_handshake(socket_channel, engine);
        socket_channel.close();
        System.out.println("Connection closed!");
    }

    protected void handle_end_of_stream(SocketChannel socketChannel, SSLEngine engine) throws IOException  {
        engine.closeInbound();
        close_connection(socketChannel, engine);
    }

    protected KeyManager[] create_key_managers(String filepath, String keystorePassword, String keyPassword) throws Exception {
        KeyStore key_store = KeyStore.getInstance("JKS");
        InputStream key_store_IS = new FileInputStream(filepath);
        try {
            key_store.load(key_store_IS, keystorePassword.toCharArray());
        } finally {
            if (key_store_IS != null) {
                key_store_IS.close();
            }
        }
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(key_store, keyPassword.toCharArray());
        return kmf.getKeyManagers();
    }

    protected TrustManager[] create_trust_managers(String filepath, String keystorePassword) throws Exception {
        KeyStore trust_store = KeyStore.getInstance("JKS");
        InputStream trust_store_IS = new FileInputStream(filepath);
        try {
            trust_store.load(trust_store_IS, keystorePassword.toCharArray());
        } finally {
            if (trust_store_IS != null) {
                trust_store_IS.close();
            }
        }
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trust_store);
        return tmf.getTrustManagers();
    }

}