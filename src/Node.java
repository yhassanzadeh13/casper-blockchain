import Signature.DigitalSignature;

import java.io.Serializable;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.*;

public class Node implements Serializable {

    final static int vicinity = 2; // max count of neighbors

    private String address;
    private int port;
    private LinkedList<Node> neighbors;
    private HashSet<PublicKey> lastValidatorSet = new HashSet<>();
    private HashSet<PublicKey> validatorSet = new HashSet<>();
    Blockchain<Block> mainChain;
    Blockchain<Checkpoint> checkpointTree;
    private HashMap<PublicKey, Double> localRep; // the local representation of the blockchain
    private HashMap<PublicKey, LinkedList<Vote>> voteHistory;
    private HashMap<Vote, HashSet<PublicKey>> rewarded; // keeps record of already rewarded validators to prevent double rewarding
    PublicKey publicAccount;
    private HashMap<PublicKey, Boolean> validatorFees;
    private PublicKey publicKey;
    private PrivateKey privateKey;
    private ServerThread serverThread;

    Node(String address, int port, boolean real) {
        this.address = address;
        this.port = port;
        localRep = new HashMap<>();
        voteHistory = new HashMap<>();
        rewarded = new HashMap<>();
        mainChain = new Blockchain<>();
        checkpointTree = new Blockchain<>();
        validatorFees = new HashMap<>();
        if(real){
            this.neighbors = new LinkedList<>();
            this.validatorSet = new HashSet<PublicKey>();
            serverThread = new ServerThread(port, this);
            serverThread.start();
        }
    }

    public ServerThread getServerThread() {
        return serverThread;
    }

    public void setServerThread(ServerThread serverThread) {
        this.serverThread = serverThread;
    }

    public HashMap<PublicKey, LinkedList<Vote>> getVoteHistory() {
        return voteHistory;
    }

    public void setVoteHistory(HashMap<PublicKey, LinkedList<Vote>> voteHistory) {
        this.voteHistory = voteHistory;
    }

    public HashMap<Vote, HashSet<PublicKey>> getRewarded() {
        return rewarded;
    }

    public void setRewarded(HashMap<Vote, HashSet<PublicKey>> rewarded) {
        this.rewarded = rewarded;
    }

    public PublicKey getPublicAccount() {
        return publicAccount;
    }

    public void setPublicAccount(PublicKey publicAccount) {
        this.publicAccount = publicAccount;
    }

    public HashMap<PublicKey, Boolean> getValidatorFees() {
        return validatorFees;
    }

    public void setValidatorFees(HashMap<PublicKey, Boolean> validatorFees) {
        this.validatorFees = validatorFees;
    }

    public HashMap<PublicKey, Double> getLocalRep() {
        return localRep;
    }

    public void setLocalRep(HashMap<PublicKey, Double> localRep) {
        this.localRep = localRep;
    }

    PublicKey getPublicKey() {
        return publicKey;
    }

    void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    PrivateKey getPrivateKey() {
        return privateKey;
    }

    void setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    String getAddress() {
        return address;
    }

    void setAddress(String address) {
        this.address = address;
    }

    int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    LinkedList<Node>  getNeighbors() {
        return neighbors;
    }

    void setNeighbors(LinkedList<Node>  neighbors) {

        for(Node neighbor : neighbors){
            if(neighbor.getAddress().compareTo(this.getAddress()) == 0
                    && neighbor.getPort() == this.getPort()){
                neighbors.remove(neighbor);
            }
        }

        if(neighbors.size() > vicinity){
            int size = neighbors.size();
            System.out.println("Neighbor list has size "+size+", getting the last "+vicinity);
            for(int i = size-vicinity; i < size; i++){
                this.neighbors.add(neighbors.get(i));
            }
        }else{
            this.neighbors = neighbors;
        }
    }

    Blockchain<Block> getMainChain() {
        return mainChain;
    }

    void setMainChain(Blockchain<Block> mainChain) {
        this.mainChain = mainChain;
    }

    Blockchain<Checkpoint> getCheckpointTree() {
        return checkpointTree;
    }

    void setCheckpointTree(Blockchain<Checkpoint> checkpointTree) {
        this.checkpointTree = checkpointTree;
    }

    public HashSet<PublicKey> getValidatorSet() {
        return validatorSet;
    }

    public HashSet<PublicKey> getLastValidatorSet() {
        return lastValidatorSet;
    }

    public void addValidator(PublicKey newValidator) {
        this.lastValidatorSet = validatorSet;
        this.validatorSet.add(newValidator);
    }

    public void removeValidator(PublicKey validator){
        this.lastValidatorSet = validatorSet;
        this.validatorSet.remove(validator);
    }




    boolean areYouUp(Node neighbor){
        System.out.println("is in areYouUp at node with port "+this.port);
        Registry r = null;
        boolean result = false;
        try {
            r = LocateRegistry.getRegistry(neighbor.getAddress(), neighbor.getPort());
            RMIFunctions rmiFunctions = (RMIFunctions) r.lookup("serverFunctions");
            result = rmiFunctions.iAmUp();

        } catch (RemoteException e) {
            System.out.println("Server at " + neighbor.getPort() + " is not available right now.");
            e.printStackTrace();
        } catch (NotBoundException e) {
            System.out.println("Could not find the specified RMI function.");
            e.printStackTrace();
        }
        return result;
    }

    LinkedList<Node> askNeighbors(Node neighbor){
        System.out.println("is in askNeighbors at node with port "+this.port);
        Registry r = null;
        LinkedList<Node> result = null;
        try {
            r = LocateRegistry.getRegistry(neighbor.getAddress(), neighbor.getPort());
            RMIFunctions rmiFunctions = (RMIFunctions) r.lookup("serverFunctions");
            result = rmiFunctions.tellNeighbors();

        } catch (RemoteException e) {
            System.out.println("Server at " + neighbor.getPort() + " is not available right now.");
            e.printStackTrace();
        } catch (NotBoundException e) {
            System.out.println("Could not find the specified RMI function.");
            e.printStackTrace();
        }
        return result;

    }

    LinkedList<Node> tellNeighbors(){
        System.out.println("is in tellNeighbors at node with port "+this.port);
        return neighbors;

    }

    public void broadcast(Message message){
        System.out.println("is in broadcast(message) at node with port "+this.port);
        for(Node neighbor : neighbors){
            Registry r = null;
            try {
                System.out.println("trying to broadcast to neighbor with port "+neighbor.getPort());
                r = LocateRegistry.getRegistry(neighbor.getAddress(), neighbor.getPort());
                RMIFunctions rmiFunctions = (RMIFunctions) r.lookup("serverFunctions");
                rmiFunctions.onReceive(message);

            } catch (RemoteException e) {
                System.out.println("Server at " + neighbor.getPort() + " is not available right now.");
                e.printStackTrace();
            } catch (NotBoundException e) {
                System.out.println("Could not find the specified RMI function.");
                e.printStackTrace();
            }
        }

    }

    void onReceive(Message m){
        if(m instanceof Vote){

            Vote vote = (Vote) m;
            System.out.println("node at port '"+port+"' has received a vote");
//            if(!validatorSet.contains(vote.validator) && !lastValidatorSet.contains(vote.validator)){
//                System.out.println("Validator not recognized, vote discarded.");
//                return;
//            }
            if(voteHistory.containsKey(vote.validator)) {
                String voteString = vote.toString();
                for(Vote dum : voteHistory.get(vote.validator)){
                    if(voteString.compareTo(dum.toString()) == 0){
                        System.out.println("Vote already received by this node.");
                        return;

                    }
                }
            }
            if(!check(vote)){
                System.out.println("Vote not consistent, is discarded.");
                return;
            }
            if(voteHistory.containsKey(vote.validator)){
                LinkedList<Vote> temp = voteHistory.get(vote.validator);
                temp.add(vote);
                voteHistory.replace(vote.validator, temp);
            }else{
                LinkedList<Vote> temp = new LinkedList<>();
                temp.add(vote);
                voteHistory.put(vote.validator, temp);
            }
            System.out.println("Vote accepted, passing on to neighbors...");
            broadcast(vote);
            return;

        }else if(m instanceof Request){
            System.out.println("node at port '"+port+"' has received a request");

            Request r = (Request) m;
            if(r.text.compareToIgnoreCase("join validators") == 0){
                if(!r.isValid()){
                    System.out.println("Signature on the request is invalid.");
                    return;
                }
                if(!validatorFees.getOrDefault(r.writer, false)){
                    System.out.println("The node has not paid the fee.");
                    return;
                }
                this.addValidator(r.writer);
                validatorFees.replace(r.writer, false); //the fee has been used

            }else if(r.text.compareToIgnoreCase("quit validators") == 0){
                if(!r.isValid()){
                    System.out.println("Signature on the request is invalid.");
                    return;
                }
                this.removeValidator(r.writer);

            }


        }
    }

    boolean check(Vote vote){
        if(voteHistory.size() == 0){
            System.out.println("Caution: This is the first vote received.");
            return true;
        }
        if(!vote.isValid()){
            System.out.println("Signature invalid, vote discarded.");
            return false;
        }
        if(vote.sourceHeight > vote.targetHeight){
            System.out.println("Source height is greater than target height.");
            return false;
        }
        PublicKey validator = vote.validator;
        Checkpoint cp = checkpointTree.get(vote.sourceHeight);
        if(!cp.validatorSet1.contains(validator) && !cp.validatorSet2.contains(validator)){
            System.out.println("Validator not in the validator sets of the source checkpoint.");
            return false;
        }

        LinkedList<Vote> pastVotes = voteHistory.get(vote.validator);

        for(Vote pastVote : pastVotes) {
            if (pastVote.targetHeight == vote.targetHeight) {
                System.out.println("Commandment 1 violation!");
                if(localRep.get(vote.validator) != 0){
                    mintSlashingBlock(pastVote, vote);
                }
                return false;
            }
            if (pastVote.sourceHeight < vote.sourceHeight && vote.targetHeight < pastVote.targetHeight
                    || vote.sourceHeight < pastVote.sourceHeight && pastVote.targetHeight < vote.targetHeight) {
                System.out.println("Commandment 2 violation!");
                if(localRep.get(vote.validator) != 0){
                    mintSlashingBlock(pastVote, vote);
                }
                return false;
            }
        }

        return true;
    }

    public void mintRewardingBlock(Vote vote){
        LinkedList<Transaction> list = new LinkedList<>();
        //each validator gets 10 units of money for each vote
        Transaction transaction = new Transaction(this.getPublicKey(), publicAccount, 10.0);
        transaction.sign(this.getPrivateKey());
        list.add(transaction);

        Block lastBlock = this.getMainChain().getLast();
        Block block;
        // is this the root?
        if(mainChain == null || mainChain.size() == 0 || mainChain.getLast() == null){
            block = new Checkpoint(getPublicKey(), "NONE", list, 0,
                    this.getLastValidatorSet(), this.getValidatorSet());
        }else if(this.getMainChain().size() % Main.epoch == 0){
            // the block should be a checkpoint
            block = new Checkpoint(getPublicKey(), lastBlock.getHash(), list, lastBlock.getDynasty() + 1,
                    this.getLastValidatorSet(), this.getValidatorSet());
        } else {
            block = new Block(this.getPublicKey(), lastBlock.getHash(), list, lastBlock.getDynasty());
        }
        block.updateHash();
        block.setRewarding(vote);
        block.sign(this.getPrivateKey());
        this.broadcast(block);


    }

    public void mintSlashingBlock(Vote vote1, Vote vote2){
        LinkedList<Transaction> list = new LinkedList<>();
        PublicKey violator = vote1.validator;
        double amount = localRep.get(violator);
        Transaction transaction = new Transaction(publicAccount, violator, amount);
        transaction.sign(this.getPrivateKey());
        list.add(transaction);

        Block lastBlock = this.getMainChain().getLast();
        Block block;
        // is this the root?
        if(mainChain == null || mainChain.size() == 0 || mainChain.getLast() == null){
            block = new Checkpoint(getPublicKey(), "NONE", list, 0,
                    this.getLastValidatorSet(), this.getValidatorSet());
        }else if(this.getMainChain().size() % Main.epoch == 0){
            // the block should be a checkpoint
            block = new Checkpoint(getPublicKey(), lastBlock.getHash(), list, lastBlock.getDynasty() + 1,
                    this.getLastValidatorSet(), this.getValidatorSet());
        } else {
            block = new Block(this.getPublicKey(), lastBlock.getHash(), list, lastBlock.getDynasty());
        }
        block.updateHash();
        block.setSlashing(vote1, vote2);
        block.sign(this.getPrivateKey());
        this.broadcast(block);
    }

    void onReceive(Block block) {
        System.out.println("node at port '"+port+"' has recieved a block");
        if(!check(block)){
            System.out.println("Block not accepted.");
            return;
        }else{
            System.out.println("Block accepted.");
            mainChain.add(block);
            if (block instanceof Checkpoint) checkpointTree.add((Checkpoint) block);
            broadcast(block);

        }
    }

    void broadcast(Block block){
        System.out.println("is in broadcast(block) at node with port "+this.port);
        for(Node neighbor : neighbors){
            Registry r = null;
            try {
                System.out.println("trying to broadcast to neighbor with port "+neighbor.getPort());
                r = LocateRegistry.getRegistry(neighbor.getAddress(), neighbor.getPort());
                RMIFunctions rmiFunctions = (RMIFunctions) r.lookup("serverFunctions");
                rmiFunctions.onReceive(block);

            } catch (RemoteException e) {
                System.out.println("Server at " + neighbor.getPort() + " is not available right now.");
                e.printStackTrace();
            } catch (NotBoundException e) {
                System.out.println("Could not find the specified RMI function.");
                e.printStackTrace();
            }
        }
    }

    boolean check(Block block){
        if(block instanceof Checkpoint){
            Checkpoint checkpoint = (Checkpoint) block;

            // check signature, prev hash and hash
            if (!checkpoint.signed()) {
                System.out.println("Checkpoint unsigned.");
                return false;
            }
            byte[] data = checkpoint.toByteArray();
            boolean verified = DigitalSignature.VerSig(data, Block.hexToBytes(checkpoint.getSignature()), checkpoint.minter, "SHA256withRSA");
            if (!verified) {
                System.out.println("Signature invalid (on checkpoint).");
                return false;
            }

            if (mainChain == null || mainChain.size() == 0 || mainChain.getLast() == null){
                System.out.println("Caution: This is the root.");

            }else {
                if (checkpoint.previousBlocksHash.compareTo(mainChain.getLast().hash) != 0) {
                    System.out.println("Checkpoint hash inconsistent with mainchain.");
                    return false;
                }
            }

            String pastHash = checkpoint.getHash();
            if (checkpoint.updateHash().compareTo(pastHash) != 0) {
                System.out.println("Checkpoint's hash incorrect:\n"+pastHash+"\n"+checkpoint.getHash());
                return false;
            }

        }else {
            // check signature, prev hash and hash
            if (!block.signed()) {
                System.out.println("Block unsigned.");
                return false;
            }
            byte[] data = block.toByteArray();
            boolean verified = DigitalSignature.VerSig(data, Block.hexToBytes(block.getSignature()), block.minter, "SHA256withRSA");
            if (!verified) {
                System.out.println("Signature invalid (on block).");
                return false;
            }

            if(mainChain == null || mainChain.size() == 0 || mainChain.getLast() == null){
                System.out.println("Caution: This is the root block, BUT NOT A CHECKPOINT.");
            }else{
                if (block.previousBlocksHash.compareTo(mainChain.getLast().hash) != 0) {
                    System.out.println("Block hash inconsistent with mainchain.");
                    return false;
                }
            }

            String pastHash = block.getHash();
            if (block.updateHash().compareTo(pastHash) != 0) {
                System.out.println("Block's hash incorrect.");
                return false;
            }
        }
        if(block.isSlashing()){
            if(block.getTransactions().size() > 1){
                System.out.println("Slashing blocks can only have one transaction.");
                return false;
            }
            Transaction transaction = block.getTransactions().getFirst();
            if (!transaction.signed()) {
                System.out.println("Unsigned transaction.");
                return false;
            }
            byte[] transactiondata = transaction.toByteArray();
            boolean transactionverified = DigitalSignature.VerSig(transactiondata, transaction.signature,
                    block.minter, "SHA256withRSA");
            if (!transactionverified) {
                System.out.println("Invalid signature on transaction.");
                return false;
            }
            if(!transaction.receiver.equals(publicAccount)){
                System.out.println("Slashing blocks can only transfer money to the public account.");
                return false;
            }
            // check the violation evidence
            Vote vote1 = block.getEvidence()[0];
            Vote vote2 = block.getEvidence()[1];
            if(!(vote1.isValid() && vote2.isValid())){
                System.out.println("Signature on vote is invalid.");
                return false;
            }
            if(vote1.sourceHeight > vote1.targetHeight){
                System.out.println("Vote1 is invalid.");
                return false;
            }else if(vote2.sourceHeight > vote2.targetHeight){
                System.out.println("Vote2 is invalid.");
                return false;
            }
            if (vote1.targetHeight == vote2.targetHeight) {
                System.out.println("Commandment 1 violation confirmed.");
            }
            if (vote1.sourceHeight < vote2.sourceHeight && vote2.targetHeight < vote1.targetHeight
                    || vote2.sourceHeight < vote1.sourceHeight && vote1.targetHeight < vote2.targetHeight) {
                System.out.println("Commandment 2 violation confirmed.");
            }

            PublicKey sender_pubKey = transaction.getSender();
            localRep.replace(sender_pubKey, 0.0);
            if (localRep.containsKey(publicAccount)) {
                double receiver_deposit = localRep.get(publicAccount);
                localRep.replace(publicAccount, receiver_deposit + transaction.getAmount());
                return true;
            } else {
                localRep.put(publicAccount, transaction.getAmount());
                return true;
            }


        }else if(block.isRewarding()){

            if(block.getTransactions().size() > 1){
                System.out.println("Rewarding blocks can only have one transaction.");
                return false;
            }
            Transaction transaction = block.getTransactions().getFirst();
            if (!transaction.signed()) {
                System.out.println("Unsigned transaction.");
                return false;
            }
            byte[] transactiondata = transaction.toByteArray();
            boolean transactionverified = DigitalSignature.VerSig(transactiondata, transaction.signature,
                    block.minter, "SHA256withRSA");
            if (!transactionverified) {
                System.out.println("Invalid signature on transaction.");
                return false;
            }
            if(!transaction.sender.equals(publicAccount)){
                System.out.println("Rewarding blocks can only transfer money from the public account.");
                return false;
            }
            //check if the vote history confirms rewarding
            Vote theVote = block.getEvidence()[0];
            LinkedList<Vote> pastVotesOfValidator = voteHistory.get(block.minter);
            Vote localVote = null;
            if(pastVotesOfValidator == null){
                System.out.println("Vote not found in local history.");
                return false;
            }
            for(Vote vote : pastVotesOfValidator){
                if(vote.toString().compareTo(theVote.toString()) == 0){
                    localVote = vote;
                    break;
                }
            }
            if(localVote == null) {
                System.out.println("Vote not found in local history.");
                return false;
            }
            if(rewarded.containsKey(theVote)) {
                HashSet<PublicKey> rewardedValidators = rewarded.get(theVote);
                if (rewardedValidators.contains(block.minter)) {
                    System.out.println("This validator has already been rewarded for this block.");
                    return false;
                }
                rewardedValidators.add(block.minter);
                rewarded.replace(theVote, rewardedValidators);

            }else{
                HashSet<PublicKey> set = new HashSet<PublicKey>();
                set.add(block.minter);
                rewarded.put(theVote, set);
            }

            //public account has infinite deposit, so no need to check for sender's deposit or to update it
            PublicKey receiver_pubKey = transaction.getReceiver();
            if (localRep.containsKey(receiver_pubKey)) {
                double receiver_deposit = localRep.get(receiver_pubKey);
                localRep.replace(receiver_pubKey, receiver_deposit + transaction.getAmount());
                return true;
            } else {
                localRep.put(receiver_pubKey, transaction.getAmount()+100);
                System.out.println("First transaction of the receiver, assigned 100 deposit.");
                return true;
            }

        }else{
            for(int i = 0; i< block.getTransactions().size(); i ++) {
                Transaction transaction = block.getTransactions().get(i);

                PublicKey sender_pubKey = transaction.getSender();
                // verify signature on the transaction
                if (!transaction.signed()) {
                    System.out.println("Unsigned transaction.");
                    continue;
                }
                byte[] transactiondata = transaction.toByteArray();
                boolean transactionverified = DigitalSignature.VerSig(transactiondata, transaction.signature, sender_pubKey, "SHA256withRSA");
                if (!transactionverified) {
                    System.out.println("Invalid signature on transaction.");
                    continue;
                }

                // each node starts with 100 deposit
                double sender_deposit = localRep.getOrDefault(sender_pubKey, 100.0);
                if(!localRep.containsKey(sender_pubKey)){
                    System.out.println("First transaction of the sender, assigned 100 deposit.");
                    localRep.put(sender_pubKey, 100.0);
                }

                if(transaction.getAmount() > sender_deposit) {
                    System.out.println("Sender does not have enough deposit.");
                    continue;
                }
                localRep.replace(sender_pubKey, sender_deposit - transaction.getAmount());
                PublicKey receiver_pubKey = transaction.getReceiver();
                // becoming a validator requires paying a fee of 100
                if (receiver_pubKey.equals(publicAccount) && transaction.getAmount() >= 100.0) {
                    this.validatorFees.put(sender_pubKey, true);
                    System.out.println("Validator fee has been paid for:\n"+sender_pubKey);
                }
                if (localRep.containsKey(receiver_pubKey)) {
                    double receiver_deposit = localRep.get(receiver_pubKey);
                    localRep.replace(receiver_pubKey, receiver_deposit + transaction.getAmount());
                } else {
                    localRep.put(receiver_pubKey, transaction.getAmount()+100);
                    System.out.println("First transaction of the receiver, assigned 100 deposit.");
                }
            }
            return true;
        }
    }

    Blockchain askMainChain(String address, int port) {
//        System.out.println("is in askMainChain at node with port "+this.port);
        Registry r = null;
        Blockchain result = null;
        try {
            r = LocateRegistry.getRegistry(address, port);
            RMIFunctions rmiFunctions = (RMIFunctions) r.lookup("serverFunctions");
            result = rmiFunctions.getMainChain();

        } catch (RemoteException e) {
            System.out.println("Server at " + port + " is not available right now.");
            e.printStackTrace();
        } catch (NotBoundException e) {
            System.out.println("Could not find the specified RMI function.");
            e.printStackTrace();
        }
        return result;

    }

    PublicKey askPublicKey(Node receiver) {
//        System.out.println("is in askPublicKey at node with port "+this.port);
        Registry r = null;
        PublicKey result = null;
        try {
            r = LocateRegistry.getRegistry(receiver.getAddress(), receiver.getPort());
            RMIFunctions rmiFunctions = (RMIFunctions) r.lookup("serverFunctions");
            result = rmiFunctions.getPublicKey();

        } catch (RemoteException e) {
            System.out.println("Server at " + port + " is not available right now.");
            e.printStackTrace();
        } catch (NotBoundException e) {
            System.out.println("Could not find the specified RMI function.");
            e.printStackTrace();
        }
        return result;

    }

    PublicKey askPublicAccountKey(Node trustedNode) {
//        System.out.println("is in askPublicAccountKey at node with port "+this.port);
        Registry r = null;
        PublicKey result = null;
        try {
            r = LocateRegistry.getRegistry(trustedNode.getAddress(), trustedNode.getPort());
            RMIFunctions rmiFunctions = (RMIFunctions) r.lookup("serverFunctions");
            result = rmiFunctions.getPublicAccountKey();

        } catch (RemoteException e) {
            System.out.println("Server at " + port + " is not available right now.");
            e.printStackTrace();
        } catch (NotBoundException e) {
            System.out.println("Could not find the specified RMI function.");
            e.printStackTrace();
        }
        return result;
    }

    double askPublicAccountDeposit(Node trustedNode) {
//        System.out.println("is in askPublicAccountKey at node with port "+this.port);
        Registry r = null;
        double result = 0;
        try {
            r = LocateRegistry.getRegistry(trustedNode.getAddress(), trustedNode.getPort());
            RMIFunctions rmiFunctions = (RMIFunctions) r.lookup("serverFunctions");
            result = rmiFunctions.getPublicAccountDeposit();

        } catch (RemoteException e) {
            System.out.println("Server at " + port + " is not available right now.");
            e.printStackTrace();
        } catch (NotBoundException e) {
            System.out.println("Could not find the specified RMI function.");
            e.printStackTrace();
        }
        return result;
    }

    int askJustifiedHeight(Node trustedNode) {
//        System.out.println("is in askJustifiedHeight at node with port "+this.port);
        Registry r = null;
        int result = 0;
        try {
            r = LocateRegistry.getRegistry(trustedNode.getAddress(), trustedNode.getPort());
            RMIFunctions rmiFunctions = (RMIFunctions) r.lookup("serverFunctions");
            result = rmiFunctions.getJustifiedHeight();

        } catch (RemoteException e) {
            System.out.println("Server at " + port + " is not available right now.");
            e.printStackTrace();
        } catch (NotBoundException e) {
            System.out.println("Could not find the specified RMI function.");
            e.printStackTrace();
        }
        return result;
    }


    boolean load(Blockchain<Block> mainChain) {
        for(Block block : mainChain){
            if(!check(block)){
                System.out.println("Mainchain rejected.");
                return false;
            }
        }
        this.mainChain = mainChain;
        this.checkpointTree = new Blockchain();
        for(Block block : mainChain){
            if(block instanceof Checkpoint){
                checkpointTree.add((Checkpoint)block);
            }
        }
        System.out.println("All blocks controlled, mainchain accepted.");
        return true;
    }

    public int justifiedHeight(){
        Blockchain<Checkpoint> tree = this.getCheckpointTree();
        if(tree.size() == 0) return 0;
        Checkpoint chp = tree.getLast();
        if(isJustified(chp)) return chp.getDynasty();
        while(tree.getLastCheckpoint(chp) != null){
            chp = tree.getLastCheckpoint(chp);
            if(isJustified(chp)) return chp.getDynasty();
        }
        return 0;
    }

    public boolean isJustified(Checkpoint checkpoint){
        LinkedList<Vote> incomingVotes = new LinkedList<>();
        for(LinkedList<Vote> list : voteHistory.values()) {
            //votes.addAll(list);
            for(Vote vote : list){
                if(vote.targetCheckpoint.compareTo(checkpoint.hash) == 0) {
                    incomingVotes.add(vote);
                }
                // no need to check the signatures on the votes
                // because they are checked before being added to 'voteHistory'
            }
        }
        int incomingCount1 = 0;
        int incomingCount2 = 0;
        for(Vote vote : incomingVotes){
            Checkpoint source = (Checkpoint) checkpointTree.find(vote.sourceCheckpoint);
            if(isFinalized(source)){
                if(checkpoint.validatorSet1.contains(vote.validator)){
                    incomingCount1++;
                }else if(checkpoint.validatorSet2.contains(vote.validator)){
                    incomingCount2++;
                }else{
                    // Validator not recognized for this checkpoint
                    return false;
                }
            }
        }


        int validatorCount1 = checkpoint.validatorSet1.size();
        int validatorCount2 = checkpoint.validatorSet2.size();
        if(incomingCount1 >= (validatorCount1*(0.66))
                && incomingCount2 >= (validatorCount2*(0.66))){
            return true;
        }
        return false;
    }

    public boolean isFinalized(Checkpoint checkpoint){
        if(!this.isJustified(checkpoint)){
            return false;
        }
        LinkedList<Vote> outgoingVotes = new LinkedList<>();
        for(LinkedList<Vote> list : voteHistory.values()) {
            //votes.addAll(list);
            for(Vote vote : list){
                if(vote.sourceCheckpoint.compareTo(checkpoint.hash) == 0){
                    outgoingVotes.add(vote);
                }
                // no need to check the signatures on the votes
                // because they are checked before being added to 'voteHistory'
            }
        }
        int outgoingCount1 = 0;
        int outgoingCount2 = 0;
        for(Vote vote : outgoingVotes){
            if(checkpoint.validatorSet1.contains(vote.validator)){
                outgoingCount1++;
            }else if(checkpoint.validatorSet2.contains(vote.validator)){
                outgoingCount2++;
            }else{
                // Validator not recognized for this checkpoint
            }
        }

        int validatorCount1 = checkpoint.validatorSet1.size();
        int validatorCount2 = checkpoint.validatorSet2.size();
        if(outgoingCount1 >= (validatorCount1*(0.66))
                && outgoingCount2 >= (validatorCount2*(0.66))){
            return true;
        }
        return false;
    }

    public void closeServer(){
        try {
            System.out.println("Closing server thread...");
            this.serverThread.exit();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public Validator getValidator() {

        Validator validator = new Validator(this.address, this.port, true);
        validator.setMainChain(this.getMainChain());
        validator.setCheckpointTree(this.checkpointTree);
        validator.setNeighbors(this.neighbors);
        validator.setPublicKey(this.getPublicKey());
        validator.setPrivateKey(this.privateKey);
        validator.setLocalRep(this.getLocalRep());
        validator.setRewarded(this.rewarded);
        validator.setValidatorFees(this.validatorFees);
        validator.setVoteHistory(this.voteHistory);
        validator.setPublicAccount(this.publicAccount);
        for(PublicKey val : this.getValidatorSet()){
            validator.addValidator(val);
        }

        return validator;
    }

    public boolean removeNeighbor(Node neighbor) {
        Node removeThis = null;
        for(Node node : this.neighbors){
            if(neighbor.getAddress().compareTo(node.getAddress()) == 0
                    && neighbor.getPort() == node.getPort()){
                removeThis = node;
                break;
            }
        }
        if(removeThis == null){
            System.out.println("Node to be removed was not found in the neighbors list.");
            return false;
        }else {
            this.neighbors.remove(removeThis);
            return true;
        }
    }
}
