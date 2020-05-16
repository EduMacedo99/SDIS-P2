package src.utils;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLEngine;

import src.network.ChordNode;
import src.network.Message;
import src.network.MessageSender;
import src.network.SSLClient;

public class Utils {
    public static final int KEY_SIZE = 5;
    public static final String CRLF = "\r\n\r\n";

    public static String hash(byte[] data) {
        MessageDigest message_digest = null;
        try {
            message_digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        message_digest.update(data);
        String encrypted_string = new String(message_digest.digest());

        return encrypted_string;
    }

    public static byte[] trim_message(byte[] message) {
        String string_msg = new String(message);
        int final_index = string_msg.length() - 1;

        while ((int) string_msg.charAt(final_index) == 0) {
            final_index--;
        }

        byte[] trimmed = new byte[final_index + 1];
        System.arraycopy(message, 0, trimmed, 0, final_index + 1);
        return trimmed;
    }

    public static Message requestMessage(ChordNode node, InetSocketAddress destination, int time, Message msg) {

        ByteBuffer response = null;

        try {
            SSLClient client = new SSLClient(node, destination.getAddress().getHostAddress(), destination.getPort());
            MessageSender msg_sender = new MessageSender(msg, client);
            node.get_executor().execute(msg_sender);
            
            Thread.sleep(1000);
    
            response = client.read();

            client.shutdown();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return Message.build_msg_from_byte_buffer(response);
    }

    public static void send_response(ChordNode peer, Message msg, SocketChannel socket_channel, SSLEngine engine) {
        try {
            SSLClient client = new SSLClient(peer, null, 0);
            client.write(socket_channel, engine, msg.get_bytes());
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

}