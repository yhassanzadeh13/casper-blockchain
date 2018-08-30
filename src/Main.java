import java.security.KeyPair;
import java.security.PublicKey;
import java.util.*;
import java.util.concurrent.*;

import Signature.*;

public class Main {

    final static int gossipFreq = 10; // minutes
    final static int epoch = 1;

    static Node me;
    static Scanner scanner = new Scanner(System.in);
    static ScheduledExecutorService scheduledExecutorService;

    public static void main(String[] args) {

        System.out.println("Enter address for the node: ");
        String address = scanner.nextLine();
        System.out.println("Enter port number for the node: ");
        String line = scanner.nextLine();
        me = new Node(address, Integer.parseInt(line), true);
        generateKey();
        System.out.println("Key pair generated.\n" +
                "You need public account info. Add neighbor or generate public account yourself?");
        while(true) {
            line = scanner.nextLine();
            if (line.toLowerCase().compareTo("add") == 0) {
                addNeighbor();
                System.out.println("Add another? (y/n)");
                line = scanner.nextLine();
                while(line.compareToIgnoreCase("y") == 0){
                    addNeighbor();
                    System.out.println("Add another? (y/n)");
                    line = scanner.nextLine();
                }
                System.out.println("Gossiping for public account info...");
                gossipPublicAccount();
                break;
            } else if (line.toLowerCase().compareTo("generate") == 0) {
                generatePublicAccount();
                System.out.println("Public account set to: " + me.publicAccount+"\n");
                break;
            } else {
                System.out.println("Invalid command, please try again. (type 'add' or 'generate')");
            }
        }
        System.out.println("Initializing mainchain...");
        initializeMainchain();

        // setup gossip to be executed at fixed intervals
        scheduledExecutorService =
                Executors.newScheduledThreadPool(1);

        ScheduledFuture scheduledFuture =
                scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
                                                      public void run(){
                                                          System.out.println("Gossip time!");
                                                          gossip();
                                                      }
                                                  },
                        0,
                        gossipFreq,
                        TimeUnit.MINUTES);


        System.out.println("Usage:\n" +
        //        "generate key\n" +
        //        "import key from keystore\n"+
        //        "generate public account\n" +
                "add neighbor\n" +
                "gossip\n" +
                "gossip neighbors/mainchain/public account\n" +
                "mint block\n" +
                "join validators\n" +
                "leave validators\n" +
                "print neighbors/mainchain/localrep/validators\n");

        line = scanner.nextLine();
        while(true){

            switch (line.toLowerCase()) {
                case "generate key":
                    generateKey();
                    break;
                case "import key from keystore":

                    break;
                case "add neighbor":
                    addNeighbor();
                    break;
                case "gossip":
                    gossip();
                    break;
                case "gossip mainchain":
                    gossipMainchain();
                    break;
                case "gossip public account":
                    gossipPublicAccount();
                    break;
                case "gossip neighbors":
                    gossipNeighbors();
                    break;
                case "mint block":
                    mintBlock();
                    break;
                case "join validators":
                    if (me.getLocalRep().getOrDefault(me.getPublicKey(), 100.0) < 100) {
                        System.out.println("You do not have enough deposit.\n(Becoming a validator requires paying a 100 fee.)");
                    }else{
                        joinValidators();
                    }
                    break;
                case "leave validators":
                    if(!(me instanceof Validator)){
                        System.out.println("You are not a validator.");
                    }else {
                        Validator valMe = (Validator) me;
                        me = valMe.getNode();

                        // broadcast quit request
                        Request quitReq = new Request("quit validators");
                        quitReq.writer = me.getPublicKey();
                        quitReq.sign(me.getPrivateKey());
                        me.broadcast(quitReq);
                    }
                    break;
                case "print neighbors":
                    printNeighbors();
                    break;
                case "print mainchain":
                    System.out.println(me.getMainChain().toString());
                    break;
                case "print localrep":
                    printLocalRep();
                    break;
                case "print validators":
                    printValidators();
                    break;
                case "print checkpoints":
                    System.out.println(me.getCheckpointTree().toString());
                    break;
                case "print justified":
                    printJustified();
                    break;
                case "generate public account":
                    generatePublicAccount();
                    break;
                case "q":
                    scheduledExecutorService.shutdown();
                    break;
                case "quit":
                    scheduledExecutorService.shutdown();
                    break;
                default :
                    System.out.println("Invalid command, please try again.");
            }
            line = scanner.nextLine();
        }
    }

    private static void printJustified() {

        for(Checkpoint chp : me.getCheckpointTree()){
            if(me.isJustified(chp)) System.out.println(chp.toString());
        }
    }


    private static void gossip() {

        gossipNeighbors();
        gossipMainchain();
        gossipPublicAccount();

    }

    // This method first checks current neighbors to see if they are all active, it removes the inactive ones from the list.
    // Then it starts asking its neighbors for their neighbors, checks if they are active, and adds them as a neighbor if they are.
    // Goes on until it reaches the max number of neighbors (ie vicinity).
    private static void gossipNeighbors(){

        for(Node neighbor : me.getNeighbors()){
            if(!me.areYouUp(neighbor)){
                me.removeNeighbor(neighbor);
            }
        }

        LinkedList<Node> newList = me.getNeighbors();
        for(Node neighbor : me.getNeighbors()) {
            LinkedList<Node> neighborsNeighbors = me.askNeighbors(neighbor);
            if(neighborsNeighbors != null && neighborsNeighbors.size() > 0){
                for(Node neighborsNeighbor : neighborsNeighbors){
                    if((neighbor.getAddress().compareTo(me.getAddress()) != 0 || neighbor.getPort() != me.getPort()) //is it me?
                                    && me.areYouUp(neighborsNeighbor) ){
                        newList.add(neighborsNeighbor);
                        System.out.println("Added neighbor with address: '"+neighborsNeighbor.getAddress()+"' and port: '"+neighborsNeighbor.getPort()+"'.");
                    }
                    if(newList.size() >= Node.vicinity){
                        break;
                    }
                }

            }
            if(newList.size() >= Node.vicinity){
                break;
            }
        }
        me.setNeighbors(newList);
        System.out.println("Updated neighbors via gossip.");
    }

    private static void gossipPublicAccount(){

        if(me.getNeighbors().size() == 0){
            System.out.println("No neighbors found, cannot get public account info.");
            return;
        }

        HashMap<PublicKey, Integer> set = new HashMap<>();
        for(Node neighbor : me.getNeighbors()){
            PublicKey key = me.askPublicAccountKey(neighbor);
            if(key == null){
                continue;
            }
            if(set.containsKey(key)){
                Integer old = set.get(key);
                set.replace(key, old+1);
            }else{
                set.put(key, 1);
            }
        }
        int max = 0;
        PublicKey maxKey = null;
        for(PublicKey key : set.keySet()){
            if(set.get(key) > max){
                max = set.get(key);
                maxKey = key;
            }
        }
        System.out.println("with a majority of "+max+" nodes, " +
                "the following key is chosen as valid: \n"+maxKey);
        Node trustedNode = null;
        for(Node neighbor : me.getNeighbors()) {
            PublicKey key = me.askPublicAccountKey(neighbor);
            if(key != null && maxKey.toString().compareTo(key.toString()) == 0){
                System.out.println("decided to trust node at port "+neighbor.getPort());
                trustedNode = neighbor;
                break;
            }
        }

        me.publicAccount = me.askPublicAccountKey(trustedNode);
        HashMap<PublicKey, Double> localRep = me.getLocalRep();
        double deposit = me.askPublicAccountDeposit(trustedNode);
        localRep.replace(me.publicAccount, deposit);
        System.out.println("Public account set to: \n"+me.publicAccount+"\nDeposit:\n"+deposit+"\n");
    }

    private static void gossipMainchain() {
        // "Follow the chain containing the justified checkpoint of the greatest height."

        if(me.getNeighbors().size() == 0){
            System.out.println("No neighbors found, cannot get mainchain.");
            return;
        }
        HashSet<Integer> set = new HashSet<>();
        for (Node neighbor : me.getNeighbors()) {
            int justifiedHeight = me.askJustifiedHeight(neighbor);
            set.add(justifiedHeight);
            System.out.println("neighbor at port "+neighbor.getPort()+" has greatest justified height "+justifiedHeight);

        }
        boolean result = false;
        while(!result) {
            if(set.size() == 0){
                System.out.println("None of the mainchains were valid.");
                return;
            }
            int maxH = 0;
            for (Integer h : set) {
                if (h > maxH) maxH = h;
            }
            Node trusted = null;
            for (Node neighbor : me.getNeighbors()) {
                int justifiedHeight = me.askJustifiedHeight(neighbor);
                if (justifiedHeight == maxH) {
                    trusted = neighbor;
                    break;
                }
            }

            Blockchain<Block> newChain = me.askMainChain(trusted.getAddress(), trusted.getPort());
            me.setMainChain(new Blockchain<>());
            me.setLocalRep(new HashMap<>());
            me.setCheckpointTree(new Blockchain<>());
            result = me.load(newChain);
            set.remove(maxH);
        }


    }

    private static void initializeMainchain() {
        if(me.getNeighbors().size() == 0){
            System.out.println("No neighbors found, cannot get mainchain.");
            return;
        }
        String lastHash;
        HashMap<String, Integer> map = new HashMap<>();
        for(Node neighbor : me.getNeighbors()){
            LinkedList<Block> chain = me.askMainChain(neighbor.getAddress(), neighbor.getPort());
            if(chain == null || chain.size() == 0){
                lastHash = "";
            }else {
                lastHash = chain.getLast().getHash();
            }
            System.out.println("neighbor at port "+neighbor.getPort()+" has last hash "+lastHash);
            if(map.containsKey(lastHash)){
                int lastcount = map.get(lastHash);
                map.replace(lastHash, lastcount + 1);
            }else{
                map.put(lastHash, 1);
            }
        }
        boolean result = false;
        while(!result) {
            if(map.size() == 0){
                System.out.println("None of the mainchains were valid.");
                return;
            }
            int max = 0;
            String maxHash = "";
            for (String entry : map.keySet()) {
                if (map.get(entry) > max) {
                    max = map.get(entry);
                    maxHash = entry;
                }
            }
            if(max == 0){
                System.out.println("None of the mainchains were valid.");
                return;
            }
            System.out.println("with a majority of " + max + " nodes, " +
                    "the following last hash is chosen as valid: " + maxHash);
            Node trustedNode = null;
            for (Node neighbor : me.getNeighbors()) {
                LinkedList<Block> chain = me.askMainChain(neighbor.getAddress(), neighbor.getPort());
                if (chain == null || chain.size() == 0) {
                    lastHash = "";
                } else {
                    lastHash = chain.getLast().getHash();
                }
                if (lastHash.compareTo(maxHash) == 0) {
                    System.out.println("decided to trust node at port " + neighbor.getPort());
                    trustedNode = neighbor;
                    break;
                }
            }
            Blockchain<Block> newChain = me.askMainChain(trustedNode.getAddress(), trustedNode.getPort());
            me.setMainChain(new Blockchain<>());
            me.setLocalRep(new HashMap<>());
            me.setCheckpointTree(new Blockchain<>());
            result = me.load(newChain);
            map.replace(maxHash, max - 1);
        }
    }

    private static void printValidators() {
        String st = "";
        for(PublicKey validator : me.getValidatorSet()){
            st += validator+"\n";
        }
        System.out.println(st);
    }

    private static void printLocalRep() {
        HashMap<PublicKey, Double> localRep = me.getLocalRep();
        String st = "Public Account:\n"+me.publicAccount+"\n"+
                "Deposit:\n"+localRep.getOrDefault(me.publicAccount, 0.0)+"\n";
        for(PublicKey node : localRep.keySet()){
            if(node.equals(me.publicAccount)){
                continue;
            }
            st += "Node:\n"+node+"\n";
            st += "Deposit: "+localRep.get(node)+"\n";
        }
        System.out.println(st);

    }

    private static void joinValidators() {

        // transfer fee to public account
        LinkedList<Transaction> list = new LinkedList<>();
        double amount = 100.0; // fee for becoming a validator
        PublicKey receiver_pubKey = me.publicAccount;
        Transaction transaction = new Transaction(receiver_pubKey, me.getPublicKey(), amount);
        transaction.sign(me.getPrivateKey());
        list.add(transaction);
        Blockchain<Block> mainChain = me.getMainChain();

        Block block;
        // is this the root?
        if(mainChain == null || mainChain.size() == 0 || mainChain.getLast() == null ){
            block = new Checkpoint(me.getPublicKey(), "NONE", list, 0,
                    me.getLastValidatorSet(), me.getValidatorSet());
        }else if(me.getMainChain().size() % epoch == 0){
            // the block should be a checkpoint
            Block lastBlock = me.getMainChain().getLast();
            block = new Checkpoint(me.getPublicKey(), lastBlock.getHash(), list, lastBlock.getDynasty() + 1,
                    me.getLastValidatorSet(), me.getValidatorSet());
        } else {
            Block lastBlock = me.getMainChain().getLast();
            block = new Block(me.getPublicKey(), lastBlock.getHash(), list, lastBlock.getDynasty());
        }
        block.updateHash();
        block.sign(me.getPrivateKey());
        me.broadcast(block);

        // broadcast join request
        Request joinReq = new Request("join validators");
        joinReq.writer = me.getPublicKey();
        joinReq.sign(me.getPrivateKey());
        me.broadcast(joinReq);

        me.addValidator(me.getPublicKey());

        me.closeServer();
        me = me.getValidator();

    }

    private static void generateKey(){
        KeyPair pair = KeyManagement.generateKeyPair("RSA",1024);
        me.setPrivateKey(pair.getPrivate());
        me.setPublicKey(pair.getPublic());
        //System.out.println("Set private key to:\n"+pair.getPrivate()+"\nand public key to:\n"+pair.getPublic()+"\n");

    }

    private static void generatePublicAccount(){
        KeyPair pair = KeyManagement.generateKeyPair("RSA",1024);
        me.publicAccount = pair.getPublic();
        System.out.println("");

    }

    private static void addNeighbor(){

        System.out.println("Enter address for neighbor: ");
        String address = scanner.nextLine();
        System.out.println("Enter port number for neighbor: ");
        String line = scanner.nextLine();
        Node tempNode = new Node(address, Integer.parseInt(line), false);
        LinkedList<Node>  tempList = me.getNeighbors();
        tempList.add(tempNode);
        me.setNeighbors(tempList);
        System.out.println("");

    }

    private static void mintBlock(){
        String line;
        String address;
        LinkedList<Transaction> list = new LinkedList<>();
        do {
            System.out.println("How much do you want to transfer?");
            line = scanner.nextLine();
            double amount = Integer.parseInt(line);
            System.out.println("Enter address for receiver: ");
            address = scanner.nextLine();
            System.out.println("Enter port number for receiver: ");
            line = scanner.nextLine();
            Node receiver = new Node(address, Integer.parseInt(line), false);
            PublicKey receiver_pubKey = me.askPublicKey(receiver);
            Transaction transaction = new Transaction(receiver_pubKey, me.getPublicKey(), amount);
            transaction.sign(me.getPrivateKey());
            list.add(transaction);
            System.out.println("Do you want to add another transaction? (y/n)");
            line = scanner.nextLine();
        }
        while(line.compareToIgnoreCase("y") == 0);
        System.out.println("You have entered "+list.size()+" transactions.");

        Block block;
        // is this the root?
        if(me.getMainChain().size() == 0){
            block = new Checkpoint(me.getPublicKey(),"NONE", list, 0,
                    me.getLastValidatorSet(), me.getValidatorSet());
        }else if(me.getMainChain().size() % epoch == 0){
            Block lastBlock = me.getMainChain().getLast();
            // the block should be a checkpoint
            block = new Checkpoint(me.getPublicKey(), lastBlock.getHash(), list, lastBlock.getDynasty() + 1,
                    me.getLastValidatorSet(), me.getValidatorSet());
        }else{
            Block lastBlock = me.getMainChain().getLast();
            block = new Block(me.getPublicKey(), lastBlock.getHash(), list, lastBlock.getDynasty());
        }
        if(block instanceof Checkpoint){
            Checkpoint ch = (Checkpoint) block;
            ch.updateHash();
            ch.sign(me.getPrivateKey());
            me.onReceive(ch);
        }else{
            block.updateHash();
            block.sign(me.getPrivateKey());
            me.onReceive(block);

        }
    }

    private static void printNeighbors(){
        try {

            for(Node peer : me.getNeighbors()) {
                System.out.println("Neighbor: \naddress: "+peer.getAddress()+"\nport: "+peer.getPort());
                LinkedList<Node> result = me.askNeighbors(peer);
                if(result == null){
                    System.out.println("Could not get neighbor list from this peer.");
                }else if(result.size() == 0){
                    System.out.println("This neighbor has no neighbors.");
                }else {
                    System.out.println("Neighbors of this neighbor:");
                    for(int i = 0; i < result.size(); i ++) {
                        Node node = result.get(i);
                        System.out.println("\taddress '" + node.getAddress() + "', port '" + node.getPort() + "'");
                    }
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }




}
