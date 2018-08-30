import java.security.PublicKey;

public class Validator extends Node {

    int startDynasty;
    int endDynasty;

    public Validator(String address, int port, boolean real) {
        super(address, port, real);
    }

    void onReceive(Block block) {
        System.out.println("validator at port '"+this.getPort()+"' has recieved a block");
        if(!check(block)){
            System.out.println("Block not accepted.");
            return;
        }
        System.out.println("Block accepted.");
        this.getMainChain().add(block);
        broadcast(block);
        if(block instanceof Checkpoint){
            checkpointTree.add((Checkpoint) block);

            if( ((Checkpoint) block).validatorSet1.contains(this.getPublicKey())
                    || ((Checkpoint) block).validatorSet2.contains(this.getPublicKey()) ){
                if(block.isRewarding()){
                    System.out.println("Checkpoint is rewarding, will not vote.");
                    //otherwise there is an infinite loop as the validator votes for the rewarding block and mints another rewarding block and votes for it and...
                    return;

                }
                System.out.println("validator at port '"+this.getPort()+"' will vote for this checkpoint");
                Blockchain<Block> chain = this.getMainChain();
                Checkpoint lastCheckpoint = chain.getLastCheckpoint();
                Vote vote;
                if(lastCheckpoint.getHash().compareTo(block.getHash()) == 0) {
                    vote = new Vote(this.getPublicKey(), "", block.getHash(),
                            0, block.getDynasty());
                }else{
                    vote = new Vote(this.getPublicKey(), lastCheckpoint.getHash(), block.getHash(),
                            lastCheckpoint.getDynasty(), block.getDynasty());

                }
                vote.sign(this.getPrivateKey());
                broadcast(vote);

                this.mintRewardingBlock(vote);
            }

        }

    }

    public Node getNode() {

        Node node = new Validator(this.getAddress(), this.getPort(), true);
        node.setMainChain(this.getMainChain());
        node.setCheckpointTree(this.getCheckpointTree());
        node.setNeighbors(this.getNeighbors());
        node.setPublicKey(this.getPublicKey());
        node.setPrivateKey(this.getPrivateKey());
        node.setLocalRep(this.getLocalRep());
        node.setRewarded(this.getRewarded());
        node.setValidatorFees(this.getValidatorFees());
        node.setVoteHistory(this.getVoteHistory());
        node.setPublicAccount(this.getPublicAccount());
        for(PublicKey val : this.getValidatorSet()){
            node.addValidator(val);
        }

        return node;
    }
}
