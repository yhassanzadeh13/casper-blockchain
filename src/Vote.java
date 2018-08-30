import Signature.DigitalSignature;
import Util.BytesUtil;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;

public class Vote extends Message {

    String sourceCheckpoint;
    String targetCheckpoint;
    int sourceHeight;
    int targetHeight;
    PublicKey validator;
    String signature;

    public Vote(PublicKey validator, String checkpoint1, String checkpoint2, int height1, int height2) {
        this.validator = validator;
        this.sourceCheckpoint = checkpoint1;
        this.targetCheckpoint = checkpoint2;
        this.sourceHeight = height1;
        this.targetHeight = height2;
    }

    public boolean isValid(){
        byte[] data = this.toByteArray();
        boolean verified = DigitalSignature.VerSig(data, hexToBytes(this.signature), validator,
                "SHA256withRSA");
        if(!verified){
            System.out.println("Signature invalid.");
            return false;
        }
        return true;
    }

    public void sign(PrivateKey privKey){
        if(sourceCheckpoint == null || targetCheckpoint == null || validator == null || sourceHeight < 0 || targetHeight <0){
            System.out.println("vote does not have enough information to sign.");
            return;
        }


        byte[] data = this.toByteArray();
        byte[] sgn = DigitalSignature.GenSig(data, privKey, "SHA256withRSA");
        this.signature = bytesToHex(sgn);
        System.out.println("Vote signed.");
    }

    public boolean signed(){
        if(this.signature == null){
            return false;
        }
        return true;
    }

    public byte[] toByteArray(){
        try {
            byte[] first = BytesUtil.toByteArray(sourceCheckpoint);
            byte[] second = BytesUtil.toByteArray(targetCheckpoint);
            byte[] third = BytesUtil.toByteArray(sourceHeight);
            byte[] fourth = BytesUtil.toByteArray(targetHeight);
            byte[] fifth = BytesUtil.toByteArray(validator);
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

    public String toString(){
        String st = "";
        st += "Source: "+sourceCheckpoint+"\n";
        st += "Source height: "+sourceHeight+"\n";
        st += "Target: "+targetCheckpoint+"\n";
        st += "Target height: "+targetHeight+"\n";
        st += "Validator: \n"+validator+"\n";
        st += "Signature: "+signature+"\n";
        return st;
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
