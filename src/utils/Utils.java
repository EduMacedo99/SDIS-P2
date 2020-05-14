package src.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
        
        while((int) string_msg.charAt(final_index) == 0) {
            final_index--;
        }

        byte[] trimmed = new byte[final_index + 1];
        System.arraycopy(message, 0, trimmed, 0, final_index + 1);
        return trimmed;
    }
}