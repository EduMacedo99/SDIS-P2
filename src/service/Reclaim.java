package src.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import src.network.ChordNode;
import src.network.Key;
import src.network.Message;
import src.utils.Disk;
import src.utils.MessageType;
import static src.utils.Utils.*;

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
        if (disk.get_used_space() <= disk_space_to_reclaim) {
            System.out.println("Reclaim completed, no need to transfer any files!");
        } else {
            for (Map.Entry<Long, String> entry : node.get_files_backed_up().entrySet()) {
                long key = entry.getKey();
                String file_name = entry.getValue();
                String file_path = node.get_files_path() + '/' + file_name;
                Path path = Paths.get(file_path);

                /* Request a backup of the file in the successor peer */
                Message msg = new Message(MessageType.BACKUP_FILE, node.get_address(), node.get_address(),
                    new Key(key), path.getFileName().toString(), 1, true);

                byte[] file_content = null;
                try {
                    file_content = Files.readAllBytes(path);
                    msg.set_body(file_content);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                send_message(node, node.get_successor() , msg);

                /* Remove the file from this peer */
                disk.decrease_used_space(file_content.length);
                node.deleteFile_files_backed_up(key);
                node.add_cancelled_backup(key);
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                /* If the used space is small enough, there is no need to transfer more files */
                if(disk.get_used_space() <= disk_space_to_reclaim) {
                    break;
                }
            }
        }
        disk.set_max_capacity(disk_space_to_reclaim);
        disk.print_state();
    }
    
}