import javafx.util.Pair;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.security.PublicKey;
import java.util.LinkedList;

public interface RMIFunctions extends Remote {

    LinkedList<Node> tellNeighbors()  throws RemoteException;
    void onReceive(Message m) throws RemoteException;
    void onReceive(Block block) throws RemoteException;
    Blockchain getMainChain() throws RemoteException;
    PublicKey getPublicKey() throws RemoteException;
    double getPublicAccountDeposit() throws RemoteException;
    PublicKey getPublicAccountKey() throws RemoteException;
    boolean iAmUp() throws RemoteException;
    int getJustifiedHeight() throws RemoteException;


    /*
    String searchByNameID(String targetNameID, int level) throws RemoteException;
    String searchByNumID(int numID, int level) throws RemoteException;

    String[] searchByNumIDlevel(int numID, int level) throws RemoteException;
    String tellMeYourNameId() throws RemoteException;
    int tellMeYourNumId() throws RemoteException;
    String[][] tellMeYourLookupTable() throws RemoteException;
    void updateYourTable(String leftRight, int level, String address) throws RemoteException;

    void insert(String newNodesAddress, int numID, String nameID, int level, boolean ifRight) throws RemoteException;

    void findInsert(String newNodesAddress, int numID, String nameID, int level) throws RemoteException;
    */

}
