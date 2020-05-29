# sdis1920-t7g23

Compile instructions:

In order to compile all the files, it is only needed to execute the compile.bat file from the project's root.

Run instructions:

    To run a peer:
        java src.service.Peer <host_address> <port> <access_point> [<contact_address> <contact_port>]
        
    To execute protocols via TestApp:
        java src.service.TestApp <peer access point> BACKUP <filename> <replication degree>
        java src.service.TestApp <peer access point> RESTORE <filename>
        java src.service.TestApp <peer access point> DELETE <filename>
        java src.service.TestApp <peer access point> RECLAIM <disk_space>
        java src.service.TestApp <peer access point> STATE
        

