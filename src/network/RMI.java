package src.network;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMI extends Remote {
    public void backup(String filepath, int replication_degree) throws RemoteException;
    public void restore(String filepath) throws RemoteException;
    public void delete(String filepath) throws RemoteException;
    public void reclaim(int disk_space_to_reclaim) throws RemoteException;
    public void state() throws RemoteException;
}