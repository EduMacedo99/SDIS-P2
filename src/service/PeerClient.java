package src.service;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.stream.Stream;

import src.network.ChordNode;
import src.network.Key;
import src.network.RMI;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;


public class PeerClient implements RMI {

    private final ChordNode peer;

    public PeerClient(ChordNode peer) {
        this.peer = peer;

    }

    @Override
    public void backup(String fileName, int replication_degree)throws RemoteException {
        System.out.println("sssssssssssssssss");
        //Check arguments
        if(fileName == null) throw new IllegalArgumentException("Invalid arguments");

        // .... using thread-pools and asynchronous I/O, i.e. Java NIO
        //Reading file with the NIO API 
        Path path = Paths.get("files/" + fileName);
        System.out.println("path " + path);
        try(BufferedReader reader = Files.newBufferedReader(path, Charset.forName("UTF-8"))){
 
            String currentLine = null;
            while((currentLine = reader.readLine()) != null){//while there is content on the current line
              System.out.println(currentLine); // print the current line
            }
        }catch(IOException ex){
            ex.printStackTrace(); 
        }
        


       /* FileInputStream file;
        byte[] fileBytes;
        int nBytesToRead = -1;
        try{
            file = new FileInputStream(filePath);
            nBytesToRead = file.available();
            fileBytes = new byte[nBytesToRead];
            file.read(fileBytes);
            file.close();

        }catch(IOException e) {
            System.err.print("The file you want to backup was not found\n");
            return;
        }*/

        //get key file
        Key fileKey = Key.create_key_file(fileName);

        //peer.requestFileBackup(fileBytes, fileKey.key, replication_degree);

        // peer.registerFile(ID, repDegree, chunkCounter, fpath);

    }

    @Override
    public void restore(String filepath) throws RemoteException {
        // TODO Auto-generated method stub

    }

    @Override
    public void delete(String filepath) throws RemoteException {
        // TODO Auto-generated method stub

    }
    
}