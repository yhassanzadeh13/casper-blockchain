import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.PublicKey;
import java.util.LinkedList;

public class RMIFunctionsImpl extends UnicastRemoteObject implements RMIFunctions {

    private Node servernode;

    protected RMIFunctionsImpl(Node node) throws RemoteException {
        super();
        this.servernode = node;
    }

    @Override
    public LinkedList<Node> tellNeighbors() throws RemoteException {
        //System.out.println("is in tellNeighbors RMI function with server node with port "+servernode.getPort());
        return servernode.tellNeighbors();

    }

    @Override
    public void onReceive(Message message) throws RemoteException {
        //System.out.println("is in onReceive(message) RMI function with server node with port "+servernode.getPort());
        servernode.onReceive(message);
    }

    @Override
    public void onReceive(Block block) throws RemoteException {
        //System.out.println("is in onReceive(block) RMI function with server node with port "+servernode.getPort());
        servernode.onReceive(block);
    }

    public Blockchain getMainChain(){
        //System.out.println("is in getMainChain() RMI function with server node with port "+servernode.getPort());
        return servernode.getMainChain();
    }

    @Override
    public PublicKey getPublicKey() throws RemoteException {
        //System.out.println("is in getPublicKey() RMI function with server node with port "+servernode.getPort());
        return servernode.getPublicKey();
    }

    @Override
    public double getPublicAccountDeposit() throws RemoteException {
        //System.out.println("is in getPublicAccountDeposit() RMI function with server node with port "+servernode.getPort());
        return servernode.getLocalRep().getOrDefault(servernode.publicAccount, 0.0);
    }

    @Override
    public PublicKey getPublicAccountKey() throws RemoteException {
        //System.out.println("is in getPublicAccountKey() RMI function with server node with port "+servernode.getPort());
        return servernode.publicAccount;
    }

    @Override
    public boolean iAmUp() throws RemoteException {
        //System.out.println("is in iAmUp() RMI function with server node with port "+servernode.getPort());
        return true;
    }

    @Override
    public int getJustifiedHeight() throws RemoteException {
        return servernode.justifiedHeight();
    }


    /*
    @Override
    public void onReceive(Message m) throws RemoteException {

    }

    @Override
    public String searchByNameID(String targetNameID, int level) throws RemoteException {
        String result = sgNode.searchByNameID(targetNameID, level);
        return result;
    }

    @Override
    public String searchByNumID(int numID, int level) throws RemoteException {
        String result = sgNode.searchByNumID(numID, level);
        return result;
    }

    @Override
    public String[] searchByNumIDlevel(int numID, int level) throws RemoteException {
        String[] result = sgNode.searchByNumIDlevel(numID, level);
        return result;
    }

    @Override
    public String tellMeYourNameId() throws RemoteException {

        return sgNode.getNameID();
    }

    @Override
    public int tellMeYourNumId() throws RemoteException {

        return sgNode.getNumericalID();
    }

    public String[][] tellMeYourLookupTable() throws RemoteException {

        return sgNode.getLookupTable();
    }

    @Override
    public void updateYourTable(String leftRight, int level, String address) throws RemoteException {
        if (leftRight.equalsIgnoreCase("left"))
            sgNode.getLookupTable()[level][0] = address;
        else
            sgNode.getLookupTable()[level][1] = address;
    }

    @Override
    public void insert(String newNodesAddress, int numID, String nameID, int level, boolean ifRight) throws RemoteException {
        sgNode.insert(newNodesAddress, numID, nameID, level, ifRight);
    }

    @Override
    public void findInsert(String newNodesAddress, int numID, String nameID, int level) throws RemoteException {
        sgNode.findInsert(newNodesAddress, numID, nameID, level);
    }
    */

}
