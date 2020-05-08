package src.network;

import java.net.InetSocketAddress;

import src.utils.Utils;

public class Key {
    public static final int KEY_SIZE = 32;

    public static final int MAXIMUM_KEY_SIZE = (int) Math.pow(2, KEY_SIZE);

    public final int key;

    public Key(int key) {
        key = key & 0x00000000ffffffff;
        this.key = key % MAXIMUM_KEY_SIZE;
    }

    public static Key createKeyFromAddress(InetSocketAddress address) {
        String ip = address.getAddress().getHostAddress();
        int port = address.getPort();
        String hashed = Utils.hash((ip + port).getBytes()); 
        return new Key(hashed.hashCode());
    } 
}