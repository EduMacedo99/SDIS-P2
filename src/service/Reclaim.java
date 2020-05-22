package src.service;

import src.network.ChordNode;
import src.utils.Disk;

public class Reclaim implements Runnable {
    private ChordNode node;
    private int disk_space_to_reclaim;

    public Reclaim(ChordNode node, int disk_space_to_reclaim) {
        this.disk_space_to_reclaim = disk_space_to_reclaim;
        this.node = node;
    }

    @Override
    public void run() {
        Disk disk = node.get_disk();
        if(disk.get_used_space() <= disk_space_to_reclaim) {
            disk.set_max_capacity(disk_space_to_reclaim);
            System.out.println("Reclaim completed, no need to transfer any files!");
        } else {
            // TODO
        }
    }
    
}