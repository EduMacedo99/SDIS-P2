package src.utils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLSocket;

import src.network.ChordNode;
import src.network.Message;
import src.network.MessageSender;

public class Utils {
    public static final int KEY_SIZE = 5;
    public static final String CRLF = "\r\n\r\n";
    public static final String FILES_TO_BACKUP_DIR = "files_to_backup";
    private static ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);    

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

    public static Message request_message(ChordNode node, InetSocketAddress destination, Message msg) {

        Message message = null;

        try {
            SSLSocket socket = MessageSender.send_message(msg, destination);

            if (socket == null) {
                return null;
            }
            
            //Thread.sleep(250);
            
            ObjectInputStream input = null;
            
            try {
                input = new ObjectInputStream(socket.getInputStream());
            } catch (IOException e) {
                //e.printStackTrace();
            }

            try {
                message = (Message) input.readObject();
            } catch (Exception e) {
                return null;
            }

            socket.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return message;
    }

    public static void send_response(ChordNode peer, Message msg, SSLSocket socket) {
        MessageSender.send_message(msg, socket);
    }

    public static void send_message(ChordNode peer, InetSocketAddress destination, Message msg) {
        try {
            SSLSocket socket = MessageSender.send_message(msg, destination);
            if (socket == null) return;
            socket.close();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    public static String address_to_string(InetSocketAddress address) {
        if (address == null) return "null";
        return address.getAddress().getHostAddress() + ":" + address.getPort();
    }

    public static InetSocketAddress string_to_address(String address_string) {
        try {
            return new InetSocketAddress(address_string.split(":")[0], Integer.parseInt(address_string.split(":")[1]));
        } catch(Exception e) {
            e.toString();
        }
        return null;
    }


    public static byte[] longToBytes(long x) {
        buffer.putLong(0, x);
        return buffer.array();
    }

    public static long bytesToLong(byte[] bytes) {
        buffer.put(bytes, 0, bytes.length);
        buffer.flip();
        return buffer.getLong();
    }

}