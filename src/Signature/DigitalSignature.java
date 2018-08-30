package Signature;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;

public class DigitalSignature {

     static boolean GenSig(String filePath, PrivateKey privateKey, String signAlg) {

        try {
            //Sign the data
            Signature signature = Signature.getInstance(signAlg);
            signature.initSign(privateKey);

            Path p = FileSystems.getDefault().getPath(filePath);
            byte [] fileData = Files.readAllBytes(p);
            signature.update(fileData);

            byte[] realSig = signature.sign();

            //Save the signature in a file
            FileOutputStream sigfos = new FileOutputStream(filePath+"_Signature");
            sigfos.write(realSig);
            sigfos.close();

            return true;


        } catch (Exception e) {
            System.err.println("Caught exception " + e.toString());

            return false;
        }
    }

     static void VerSig(String filePath, String signaturePath, PublicKey publicKey, String signAlg) {

        try{
            /* input the signature bytes */
            FileInputStream sigfis = new FileInputStream(signaturePath);
            byte[] signatureToVerify = new byte[sigfis.available()];
            sigfis.read(signatureToVerify);

            sigfis.close();

            /* create a Signature object and initialize it with the public key */
            Signature signature = Signature.getInstance(signAlg);
            signature.initVerify(publicKey);

            /* Update and verify the data */

            FileInputStream datafis = new FileInputStream(filePath);
            BufferedInputStream bufin = new BufferedInputStream(datafis);

            byte[] buffer = new byte[1024];
            int len;
            while (bufin.available() != 0) {
                len = bufin.read(buffer);
                signature.update(buffer, 0, len);
            }

            bufin.close();


            boolean verifies = signature.verify(signatureToVerify);

            System.out.println("SIGNATURE VERIFIES: " + verifies);


        } catch (Exception e) {
            System.err.println("Caught exception " + e.toString());
        }

    }

    public static byte[] GenSig(byte[] data, PrivateKey privateKey, String signAlg) {

        try {
            //Sign the data
            Signature signature = Signature.getInstance(signAlg);
            signature.initSign(privateKey);

            signature.update(data);
            byte[] realSig = signature.sign();
            return realSig;
        } catch (Exception e) {
            System.err.println("Caught exception " + e.toString());

            return null;
        }
    }

    public static boolean VerSig(byte[] data, byte[] signatureBytes, PublicKey publicKey, String signAlg) {

        try{
            /* create a Signature object and initialize it with the public key */
            Signature signature = Signature.getInstance(signAlg);
            signature.initVerify(publicKey);


            signature.update(data);

            boolean verifies = signature.verify(signatureBytes);
            return  verifies;

        } catch (Exception e) {
            System.err.println("Caught exception " + e.toString());

            return false;
        }

    }

}