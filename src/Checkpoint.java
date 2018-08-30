import Signature.DigitalSignature;
import Util.BytesUtil;
import Util.SHA3util;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashSet;
import java.util.LinkedList;

public class Checkpoint extends Block {

    HashSet<PublicKey> validatorSet1; // at time t-1
    HashSet<PublicKey> validatorSet2; // at time t

    public Checkpoint(PublicKey minter, String previousBlocksHash, LinkedList<Transaction> transactions, int dynasty,
                      HashSet<PublicKey> validatorSet1, HashSet<PublicKey> validatorSet2) {
        super(minter, previousBlocksHash, transactions, dynasty);
        this.validatorSet1 = validatorSet1;
        this.validatorSet2 = validatorSet2;
    }


    public String updateHash(){

        String data = previousBlocksHash.concat(String.valueOf(dynasty));
        for(Transaction trans : transactions) {
            data = data.concat(trans.toString());
        }
        data = data.concat(validatorSet1.toString());
        data = data.concat(validatorSet2.toString());
        String hash = SHA3util.digest(data);
        this.hash = hash;
        return hash;
    }

    public void sign(PrivateKey privKey){
        this.signature = bytesToHex(this.generateSignature(privKey));
        System.out.println("Block signed.");
        //System.out.println(this.toString());

    }

    public byte[] generateSignature(PrivateKey privKey){

        if(previousBlocksHash == null || transactions == null || hash == null || dynasty < 0 || minter == null){
            System.out.println("Checkpoint does not have enough information to sign.");
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
        boolean temp = DigitalSignature.VerSig(data, DigitalSignature.GenSig(data, privKey, "SHA256withRSA"), minter, "SHA256withRSA");
        System.out.println(temp);
        return DigitalSignature.GenSig(data, privKey, "SHA256withRSA");
    }

    public byte[] toByteArray(){
        //System.out.println("Making byte array for checkpoint.");
        try {
            byte[] first = super.toByteArray();
            byte[] second = BytesUtil.toByteArray("");
            byte[] third = BytesUtil.toByteArray("");
            byte[] result = new byte[first.length + second.length + third.length];
            System.arraycopy(first, 0, result, 0, first.length);
            System.arraycopy(second, 0, result, first.length, second.length);
            System.arraycopy(third, 0, result, first.length + second.length, third.length);
            return result;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }

    @Override
    public String toString() {
        String st = "Is a checkpoint.\n";
        if(validatorSet1.isEmpty()){
            st += "Validator set 1 is empty.\n";
        }else{
            st += "Validator set 1:\n";
            for(PublicKey val : validatorSet1){
                st += val;
            }
            st += "\n";
        }
        if(validatorSet2.isEmpty()){
            st += "Validator set 2 is empty.\n";
        }else{
            st += "Validator set 2:\n";
            for(PublicKey val : validatorSet2){
                st += val;
            }
            st += "\n";
        }
        if(this.isSlashing()){
            st += "Slashing block by:\n"+this.minter+"\n";
            st += "Evidence: \n";
            for(Vote vote : this.getEvidence()){
                if(vote != null) {
                    st += vote.toString();
                }
            }
        }
        if(this.isRewarding()){
            st += "Rewarding block for validator:\n"+this.minter+"\n";
            st += "Evidence: \n";
            for(Vote vote : this.getEvidence()){
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

}
