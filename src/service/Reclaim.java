package src.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import src.network.ChordNode;
import src.network.Key;
import src.network.Message;
import src.utils.Disk;
import src.utils.MessageType;
import static src.utils.Utils.*;

public class Reclaim implements Runnable {
    private final ChordNode node;
    private final int disk_space_to_reclaim;

    public Reclaim(final ChordNode node, final int disk_space_to_reclaim) {
        this.disk_space_to_reclaim = disk_space_to_reclaim;
        this.node = node;
    }

    @Override
    public void run() {
        final Disk disk = node.get_disk();
        
        if (disk.get_used_space() <= disk_space_to_reclaim) {
            System.out.println("Reclaim completed, no need to transfer any files!");
        } else {
            List<Long> keys_to_delete = new ArrayList<Long>(); 
            for (final Map.Entry<Long, String> entry : node.get_files_backed_up().entrySet()) {
                final long key = entry.getKey();
                final String file_name = entry.getValue();
                final String file_path = node.get_files_path() + '/' + file_name;
                final Path path = Paths.get(file_path);

                /* Request a backup of the file in the successor peer */
                final Message msg = new Message(MessageType.BACKUP_FILE, node.get_address(), node.get_address(),
                    new Key(key), path.getFileName().toString(), 1, true);

                byte[] file_content = null;
                try {
                    file_content = Files.readAllBytes(path);
                    msg.set_body(file_content);
                } catch (final IOException e) {
                    e.printStackTrace();
                }

                send_message(node, node.get_successor() , msg);

                /* Remove the file from this peer */
                disk.decrease_used_space(file_content.length);
                keys_to_delete.add(key);
                node.add_cancelled_backup(key);
                try {
                    Files.delete(path);
                } catch (final IOException e) {
                    e.printStackTrace();
                }

                /* If the used space is small enough, there is no need to transfer more files */
                if(disk.get_used_space() <= disk_space_to_reclaim) {
                    break;
                }
            }
            for (Long key: keys_to_delete) {
                node.deleteFile_files_backed_up(key);
            }
        }
        disk.set_max_capacity(disk_space_to_reclaim);
        disk.print_state();
    }
}