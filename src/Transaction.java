import Signature.DigitalSignature;
import Util.BytesUtil;

import java.io.IOException;
import java.io.Serializable;
import java.security.PrivateKey;
import java.security.PublicKey;

public class Transaction implements Serializable{

    PublicKey sender;
    PublicKey receiver;
    double amount;
    byte[] signature;


    public Transaction(){
        System.out.println("no arg constructor used for transaction");
    }

    public Transaction(PublicKey receiver, PublicKey sender, double amount) {
        this.receiver = receiver;
        this.sender = sender;
        this.amount = amount;
    }

    public PublicKey getReceiver() {
        return receiver;
    }

    public void setReceiver(PublicKey receiver) {
        this.receiver = receiver;
    }

    public PublicKey getSender() {
        return sender;
    }

    public void setSender(PublicKey sender) {
        this.sender = sender;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public byte[] getSignature() {
        return signature;
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }

    public void sign(PrivateKey privKey){
        if(sender == null || receiver == null || amount <= 0){
            System.out.println("Transaction does not have enough information to sign.");
            if(sender == null){
                System.out.println("sender == null");
            }else if(receiver == null){
                System.out.println("receiver == null");
            }else if(amount <= 0){
                System.out.println("amount <= 0");
            }
            return;
        }
        byte[] data = this.toByteArray();
        this.signature = DigitalSignature.GenSig(data, privKey, "SHA256withRSA");
        System.out.println("Transaction signed.");
    }

    public boolean signed(){
        if(this.signature == null){
            return false;
        }
        return true;
    }

    public byte[] toByteArray(){
        try {
            byte[] first = BytesUtil.toByteArray(sender);
            byte[] second = BytesUtil.toByteArray(receiver);
            byte[] third = BytesUtil.toByteArray(amount);
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

    public String toString(){
        String st = "";
        st += "Sender: \n"+sender+"\n";
        st += "Receiver: \n"+receiver+"\n";
        st += "Amount: "+amount+"\n";
        st += "Signature: "+Block.bytesToHex(signature)+"\n";

        return st;
    }

}
