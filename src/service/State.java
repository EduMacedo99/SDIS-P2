package src.service;

import java.util.HashMap;
import java.util.Map;

import src.network.ChordNode;
import src.utils.FileInfo;
import static src.utils.Utils.*;

public class State implements Runnable {

    private ChordNode node;

    public State(ChordNode node) {
        this.node = node;
    }

    @Override
    public void run() {
        
        /* Looking into the data stored in the files_list HashMap */
        System.out.println("----> LIST OF INITIATED BACKUPS <----");
        HashMap<Long, FileInfo> files_list = node.get_files_list();
        for (Map.Entry<Long, FileInfo> entry : files_list.entrySet()) {
            long key = entry.getKey();
            FileInfo file_details = entry.getValue();
            System.out.println("------ FILE ------");
            System.out.println("File name: " + get_file_name(file_details.path.toString()));
            System.out.println("Desired rep degree: " + file_details.replication_degree);
        }
        System.out.println("----> END OF LIST <----");

        /* Disk state */
        System.out.println("\n----> DISK STATUS <----");
        node.get_disk().print_state();
    }

    
}