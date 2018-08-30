import Signature.DigitalSignature;
import Util.BytesUtil;
import Util.SHA3util;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.io.Serializable;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.LinkedList;

public class Block implements Comparable<Block>, Serializable {

    LinkedList<Transaction> transactions;
    String hash;
    String previousBlocksHash;
    int dynasty;
    PublicKey minter;
    String signature;
    private boolean rewarding; //is true if the block is used for rewarding a validator
                    // in this case:
                    // 1. the block must have only one transaction
                    // 2. the signature on the transaction is not by the sender, but the block's minter
                    // 3. justification of reward will be searched for in each node's local vote history
                        // evidence should be added
    private boolean slashing; //is true if the block is used for slashing
                    // in this case:
                    // 1. the block must have only one transaction
                    // 2. the signature on the transaction is not by the sender, but the block's minter
                    // 3. evidence of commandment violation must be provided
    private Vote[] evidence; //evidence of slashing condition

    public LinkedList<Transaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(LinkedList<Transaction> transactions) {
        this.transactions = transactions;
    }

    public String getPreviousBlocksHash() {
        return previousBlocksHash;
    }

    public void setPreviousBlocksHash(String previousBlocksHash) {
        this.previousBlocksHash = previousBlocksHash;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public int getDynasty() {
        return dynasty;
    }

    public void setDynasty(int dynasty) {
        this.dynasty = dynasty;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public Vote[] getEvidence(){ return evidence; }


    public Block(){
        System.out.println("no arg constructor used for block");
    }

    public Block(PublicKey minter, String previousBlocksHash, LinkedList<Transaction> transactions, int dynasty){
        this.minter = minter;
        this.previousBlocksHash = previousBlocksHash;
        this.transactions = transactions;
        this.dynasty = dynasty;
        slashing = false;
        rewarding = false;

    }

    public void setSlashing(Vote vote1, Vote vote2){
        slashing = true;
        evidence = new Vote[]{vote1, vote2};
    }

    public boolean isSlashing(){
        return slashing;
    }

    public boolean isRewarding() {
        return rewarding;
    }

    public void setRewarding(Vote vote) {
        this.rewarding = true;
        evidence = new Vote[]{vote, null};
    }

    public boolean isRoot(){
        if(previousBlocksHash.compareToIgnoreCase("NONE") == 0){
            return true;
        }
        return false;
    }

    public String updateHash(){
        String data = previousBlocksHash.concat(String.valueOf(dynasty));
        for(Transaction trans : transactions) {
            data = data.concat(trans.toString());
        }
        String hash = SHA3util.digest(data);
        this.hash = hash;
        return hash;
    }

    public void sign(PrivateKey privKey){
        this.signature = bytesToHex(this.generateSignature(privKey));
        System.out.println("Block signed:");
        System.out.println(this.toString());

    }

    public byte[] generateSignature(PrivateKey privKey){

        if(previousBlocksHash == null || transactions == null || hash == null || dynasty < 0 || minter == null){
            System.out.println("Block does not have enough information to sign.");
            if(previousBlocksHash == null ){
                System.out.println("previousBlocksHash == null ");
            }else if(transactions == null){
                System.out.println("transactions == null");
            }else if(dynasty < 0){
                System.out.println("dynasty < 0");
            }else if(minter == null){
                System.out.println("minter == null");
            }
            return null;
        }
        byte[] data = this.toByteArray();
        return DigitalSignature.GenSig(data, privKey, "SHA256withRSA");
    }

    public boolean signed(){
        if(this.signature == null){
            return false;
        }
        return true;
    }

    public byte[] toByteArray(){
        //System.out.println("Making byte array for block.");

        try {
            byte[] first = BytesUtil.toByteArray(this.previousBlocksHash);
            byte[] second = BytesUtil.toByteArray(this.transactions);
            byte[] third = BytesUtil.toByteArray(this.hash);
            byte[] fourth = BytesUtil.toByteArray(this.dynasty);
            byte[] fifth = BytesUtil.toByteArray(this.minter);
            byte[] result = new byte[first.length + second.length + third.length + fourth.length + fifth.length];
            System.arraycopy(first, 0, result, 0, first.length);
            System.arraycopy(second, 0, result, first.length, second.length);
            System.arraycopy(third, 0, result, first.length + second.length, third.length);
            System.arraycopy(fourth, 0, result, first.length + second.length + third.length, fourth.length);
            System.arraycopy(fifth, 0, result, first.length + second.length + third.length + fourth.length, fifth.length);
            return result;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }

    @Override
    public String toString() {
        String st = "";
        if(this.slashing){
            st += "Slashing block by:\n"+this.minter+"\n";
            st += "Evidence: \n";
            for(Vote vote : this.evidence){
                if(vote != null) {
                    st += vote.toString();
                }
            }
        }
        if(this.isRewarding()){
            st += "Rewarding block for validator:\n"+this.minter+"\n";
            st += "Evidence: \n";
            for(Vote vote : this.evidence){
                if(vote != null) {
                    st += vote.toString();
                }
            }
        }
        st += "Dynasty: "+this.dynasty+"\n";
        st += "Previous hash: "+this.previousBlocksHash+"\n";
        st += "Hash: "+this.hash+"\n";
        for(Transaction trans : transactions){
            st+= trans.toString();
        }
        st += "Minter: \n"+this.minter+"\n";
        st += "Signature: \n"+this.signature+"\n";
        st += "Has a signature that ";
        boolean temp = DigitalSignature.VerSig(this.toByteArray(), hexToBytes(this.signature), this.minter, "SHA256withRSA");
        if(temp){
            st += "is valid.";
        }else{
            st += "is NOT valid.";
        }

        return st;
    }

    @Override
    public int compareTo(Block b) {
        if(this.hash.compareTo(b.hash) == 0){
            return 0;
        }else{
            return -1;
        }
    }

    public static String bytesToHex(byte[] bytes){
        String hex = DatatypeConverter.printHexBinary(bytes);
        return hex;
    }

    public static byte[] hexToBytes(String hex){
        byte[] decodedHex = DatatypeConverter.parseHexBinary(hex);
        return decodedHex;
    }

}
