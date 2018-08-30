import Signature.DigitalSignature;
import Util.BytesUtil;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;

public class Request extends Message {

    String text;
    PublicKey writer;
    byte[] signature;


    public Request(String text){
        if(text.compareToIgnoreCase("join validators") == 0
                || text.compareToIgnoreCase("quit validators") == 0) {
            this.text = text;
        }else{
            System.out.println("Invalid message text.");
            text = null;
        }
    }

    public boolean isValid(){
        byte[] data = this.toByteArray();
        boolean verified = DigitalSignature.VerSig(data, signature, writer,
                "SHA256withRSA");
        if(!verified){
            System.out.println("Signature invalid.");
            return false;
        }
        return true;
    }

    public void sign(PrivateKey privKey){
        this.signature = generateSignature(privKey);
        System.out.println("Block signed.");
    }

    public byte[] generateSignature(PrivateKey privKey){

        if(text == null || writer == null){
            System.out.println("Block does not have enough information to sign.");
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
        try {
            byte[] first = BytesUtil.toByteArray(text);
            byte[] second = BytesUtil.toByteArray(writer);
            byte[] result = new byte[first.length + second.length];
            System.arraycopy(first, 0, result, 0, first.length);
            System.arraycopy(second, 0, result, first.length, second.length);
            return result;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }

}
