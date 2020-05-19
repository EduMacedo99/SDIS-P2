package src.network;

import java.io.Serializable;
import java.net.InetSocketAddress;

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

    public static Key create_key_file(String path) {
        String hashed = hash(path.getBytes()); 
        return new Key(hashed.hashCode());
    }

}