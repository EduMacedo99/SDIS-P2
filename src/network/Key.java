package src.network;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static src.utils.Utils.*;

public class Key implements Serializable{
    
    private static final long serialVersionUID = 1L;

    public static final int MAXIMUM_KEY_SIZE = (int) Math.pow(2, KEY_SIZE);

    public long key;

    public Key(long key) {
        this.key = key;
    }

    public Key(int key) {
        this.key = key & 0x000000000000001fL;
        this.key = this.key % MAXIMUM_KEY_SIZE;
    }

    public static Key create_key_from_address(InetSocketAddress address) {
        String ip = address.getAddress().getHostAddress();
        int port = address.getPort();
        String hashed = hash((ip + port).getBytes()); 
        return new Key(hashed.hashCode());
    }

    @Override
    public String toString() {
        return key + "";
    }
    
    /**
     *
     * @param string to hash
     * @return
     * @throws IOException
     * @throws NoSuchAlgorithmException
     *
     *
     * This code of this function was was made with the help of this website as a resource
     * https://www.baeldung.com/sha-256-hashing-java
     */
    private static String getHash(String string) throws IOException, NoSuchAlgorithmException {


        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(string.getBytes());
        StringBuffer hexString = new StringBuffer();

        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if (hex.length() == 1)
                hexString.append('0');
            hexString.append(hex);
            }
            return hexString.toString();

    }

    public static Key create_key_file(String path) throws IOException, NoSuchAlgorithmException{
        String hashed = getHash(path); 
        return new Key(hashed.hashCode());
       
    }

}