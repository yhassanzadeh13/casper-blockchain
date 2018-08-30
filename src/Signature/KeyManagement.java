package Signature;

import java.io.*;
import java.security.*;
import java.security.cert.Certificate;

public class KeyManagement {
    // Reference doc on how to use keytool from the command line:
    // https://www.sslshopper.com/article-most-common-java-keytool-keystore-commands.html
    static private KeyStore keyStore;
    static private String directory;
    static String keyStoreName;


    static boolean createKeyStore(String password, String keyStorePath){
        try {
            keyStore = KeyStore.getInstance("JKS");
            char[] keyStorePassword = password.toCharArray();
            keyStore.load(null, keyStorePassword);
            FileOutputStream keyStoreOutputStream = new FileOutputStream(keyStorePath);
            keyStore.store(keyStoreOutputStream, keyStorePassword);
            InputStream keyStoreData = new FileInputStream(keyStorePath);
            keyStore.load(keyStoreData, keyStorePassword);
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }

    }

    static boolean loadKeyStore(String password, String keyStorePath) {
        try {
            String[] splitPath = keyStorePath.split("/");
            keyStoreName = splitPath[splitPath.length-1];
            directory = "";
            for(int i = 0; i < splitPath.length-1; i++){
                directory += splitPath[i]+"/";
            }
            System.out.println("directory is "+directory);
            System.out.println("keyStoreName is "+keyStoreName);

            keyStore = KeyStore.getInstance("JKS");
            InputStream keyStoreData = new FileInputStream(keyStorePath);
            char[] keyStorePassword = password.toCharArray();
            keyStore.load(keyStoreData, keyStorePassword);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    static PrivateKey getPrivateKey(String keyName, String password){
        // TODO this only works if keystore and key passwords are the same
        // https://stackoverflow.com/questions/22230815/java-server-ssl-with-different-storepass-and-keypass

        char[] keyPassword = password.toCharArray();

        try {
            PrivateKey key = (PrivateKey) keyStore.getKey(keyName, keyPassword);
            if(key == null){
                System.out.println("Key is null.");
            }
            return key;
        } catch (Exception e) {
            System.out.println("Caught exception in getPrivateKey");
            e.printStackTrace();
            return null;
        }


    }

    static Key getPublicKey(String keyName){

        try {
            Certificate cert = keyStore.getCertificate(keyName);
            if(cert == null){
                System.out.println("Certificate is null.");
            }else {
                Key key = cert.getPublicKey();
                if (key == null) {
                    System.out.println("Key is null.");
                }
                return key;
            }
            return null;
        } catch (Exception e) {
            System.out.println("Caught exception in getPublicKey");
            e.printStackTrace();
            return null;
        }

    }



    static boolean createKey(String keyName, String keyGenAlg, String keyLenght, String keyPassword, String keyStorePassword){

        Runtime rt = Runtime.getRuntime();
        try {
            File dir = new File(directory);

            //Constructing command array
            String[] cmdArray = new String[16];
            cmdArray[0] = "keytool";
            cmdArray[1] = "-genkey";
            cmdArray[2] = "-alias";
            cmdArray[3] = keyName;
            cmdArray[4] = "-keyalg";
            cmdArray[5] = keyGenAlg;
            cmdArray[6] = "-keysize";
            cmdArray[7] = keyLenght;
            cmdArray[8] = "-keystore";
            cmdArray[9] = keyStoreName;
            cmdArray[10] = "-dname";
            cmdArray[11] = "CN=EceTanova, OU=UGrad, O=Koc, L=Ist, S=Sariyer, C=TR";
            cmdArray[12] = "-storepass";
            cmdArray[13] = keyStorePassword;
            cmdArray[14] = "-keypass";
            cmdArray[15] = keyPassword;

            //Executing command
            Process pr = rt.exec(cmdArray, null, dir);

            //Checking for output at the command line
            InputStream is = pr.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line;

            //Printing output
            boolean first = true;
            while ((line = br.readLine()) != null) {
                if(first) { System.out.println("Output is:\n"); first = false; }
                System.out.println(line);
            }

            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }


    public static KeyPair generateKeyPair(String keyGenAlg, int keyLenght){
        //Generate public and private keys
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(keyGenAlg);
            SecureRandom random = SecureRandom.getInstanceStrong();
            keyGen.initialize(keyLenght, random);

            KeyPair pair = keyGen.generateKeyPair();

            return pair;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }

    }



    static boolean deleteKey(String keyName, String keyStorePassword) {

        Runtime rt = Runtime.getRuntime();
        try {
            File dir = new File(directory);

            //Constructing command array
            String[] cmdArray = new String[8];
            cmdArray[0] = "keytool";
            cmdArray[1] = "-delete";
            cmdArray[2] = "-alias";
            cmdArray[3] = keyName;
            cmdArray[4] = "-keystore";
            cmdArray[5] = keyStoreName;
            cmdArray[6] = "-storepass";
            cmdArray[7] = keyStorePassword;

            //Executing command
            Process pr = rt.exec(cmdArray, null, dir);

            //Checking for output at the command line
            InputStream is = pr.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line;

            //Printing output
            boolean first = true;
            while ((line = br.readLine()) != null) {
                if(first) { System.out.println("Output is:\n"); first = false; }
                System.out.println(line);
            }

            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }


}
