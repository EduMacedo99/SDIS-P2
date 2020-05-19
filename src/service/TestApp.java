package src.service;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import src.network.RMI;

public class TestApp {

    private TestApp() {}

    public static void main(String[] args) {
        try {
            if (args.length < 2 || args.length > 4) {
                System.out.println("USAGE: java TestApp <> <hostname>/<peer_access_point> <sub_protocol> <opnd_1> <opnd_2>");
                return;
            }

            String access_point = args[0];

            Registry registry = LocateRegistry.getRegistry(0);
            RMI stub = (RMI) registry.lookup(access_point);

            String filePath;
            int diskSpaceToReclaim;
            int replicationDegree;

            /* Performing the selected operation */
            switch (args[1]) {
                case "BACKUP":
                    if (args.length != 4) {
                        System.out.println("Usage: java TestApp <hostname>/<peer_access_point> BACKUP <file_path> <replication_degree>");
                        return;
                    }
                    System.out.println("Backup is being initiated");
                    filePath = args[2];
                    replicationDegree = Integer.parseInt(args[3]);

                    stub.backup(filePath, replicationDegree);
                    break;
                case "RESTORE":
                    if (args.length != 3) {
                        System.out.println("Usage: java TestApp <hostname>/<peer_access_point> RESTORE <file_path>");
                        return;
                    }
                    filePath = args[2];
                    stub.restore(filePath);
                    break;
                case "DELETE":
                    if (args.length != 3) {
                        System.out.println("Usage: java TestApp <hostname>/<peer_access_point> DELETE <file_path>");
                        return;
                    }
                    filePath = args[2];
                    stub.delete(filePath);
                    break;
                /*case "RECLAIM":
                    if (args.length != 3) {
                        System.out.println("Usage: java TestApp <hostname>/<peer_access_point> RECLAIM <disk_space_to_reclaim>");
                        return;
                    }
                    diskSpaceToReclaim = Integer.parseInt(args[2]);
                    stub.reclaim(diskSpaceToReclaim);
                    break;
                case "STATE":
                    if (args.length != 2) {
                        System.out.println("Usage: java TestApp <hostname>/<peer_access_point> STATE");
                        return;
                    }
                    stub.state();
                    break;*/
            }

        } catch (Exception e) {
            System.out.println("Could not connect to RMI service!");
        }
    }
}
