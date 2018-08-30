import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;


public class ServerThread extends Thread {
    private static int PORT;
    private Node node;
    private Registry reg;

    public ServerThread(int port, Node node){
        PORT = port;
        this.node = node;
    }

    public void run(){
        try {

            Registry registry = LocateRegistry.createRegistry(PORT);

            RMIFunctions rmiFunctions = new RMIFunctionsImpl(node);


            registry.bind("serverFunctions", rmiFunctions);

            reg = registry;


            System.out.println("Created a server on port " + PORT +" and registered RMI functions successfully.");

        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void exit() throws RemoteException
    {
        try{
//            // Unregister ourself
//            //System.out.println("Unbinding: "+InetAddress.getLocalHost().toString());
//            System.out.println("Unbinding: "+this.getName());
//            Naming.unbind();
//
//            // Unexport; this will also remove us from the RMI runtime
//            UnicastRemoteObject.unexportObject((java.rmi.Remote)this, true);
//

            reg.unbind("serverFunctions");
            UnicastRemoteObject.unexportObject(reg, true);

            System.out.println("Server on port "+PORT+" has exited.");
        }
        catch(Exception e){
            e.printStackTrace();


        }
    }



}
