package src.helper;

import src.network.ChordNode;
import static src.utils.Utils.*;

public class PrintThread extends HelperThread {

    private ChordNode peer;

    public PrintThread(ChordNode peer, int time_interval) {
        super(time_interval);
        this.peer = peer;
    }

    @Override
    public void run() {
        System.out.println("Predecessor: " + peer.get_predecessor());
        for (int i = 1; i <= KEY_SIZE; i++) {
            System.out.println("Finger " + i + ": " + peer.get_ith_finger(i));
        }
        System.out.println();
    }
    
}