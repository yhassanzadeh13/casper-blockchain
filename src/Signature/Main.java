package Signature;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        System.out.println("Usage:\n(Please don't use whitespace in names, passwords or file paths.)\n" +
                "(Type 'help' to see which key generation and signature algorithms are available.)\n" +
                "load keystore 'keystorepath'\n" +
                "create keystore 'keystorepath'\n" +
                "generate key 'keyname' 'keygenerationalgorithm' 'keylenght'\n" +
                "delete key 'keyname'\n" +
                "sign 'filepath' 'keyname' 'signalgorithm'\n" +
                "verify 'filepath' 'signaturepath' 'keyname' 'signalgorithm'\n");
        String line = scanner.nextLine();
        while(line.compareToIgnoreCase("quit")!=0){
            String[] command = line.split(" ");

            switch (command[0].toLowerCase()){
                case "load" :
                    if(command.length != 3) {
                        System.out.println("Invalid number of arguments, please try again.");
                        break;
                    }
                    String[] tempArray = command[2].split("/");
                    System.out.println("Please enter password for '" + tempArray[tempArray.length-1] + "' :");
                    String keyStorePassword = scanner.nextLine();

                    boolean success = KeyManagement.loadKeyStore(keyStorePassword ,command[2]);
                    if(success) System.out.println("Keystore successfully loaded.");
                    else System.out.println("Unable to load keystore.");

                    break;
                case "create" :
                    if (command.length != 3) {
                        System.out.println("Invalid number of arguments, please try again.");
                        break;
                    }
                    String[] tempArray2 = command[2].split("/");
                    System.out.println("Please enter password for '" + tempArray2[tempArray2.length-1] + "' :");
                    String keyStorePassword2 = scanner.nextLine();
                    while(keyStorePassword2.length() < 6){
                        System.out.println("Keystore password is too short - must be at least 6 characters.\n" +
                                "Please enter password for '" + tempArray2[tempArray2.length-1] + "' :");
                        keyStorePassword2 = scanner.nextLine();
                    }
                    success = KeyManagement.createKeyStore(keyStorePassword2 ,command[2]);
                    if(success){
                        System.out.println("Keystore successfully created.");
                        success = KeyManagement.loadKeyStore(keyStorePassword2 ,command[2]);
                        if(success) System.out.println("Keystore successfully loaded.");
                        else System.out.println("Unable to load keystore.");
                    }
                    else System.out.println("Unable to create keystore.");

                    break;
                case "generate" :
                    if (command.length != 5) {
                        System.out.println("Invalid number of arguments, please try again.");
                        break;
                    }
                    System.out.println("Please enter password for '" + command[2] + "' :");
                    String keyPassword = scanner.nextLine();
                    while(keyPassword.length() < 6){
                        System.out.println("Key password is too short - must be at least 6 characters.\n" +
                                "Please enter password for '" + command[2] + "' :");
                        keyPassword = scanner.nextLine();
                    }

                    System.out.println("Please enter password for '" + KeyManagement.keyStoreName + "' :");
                    String keyStorePassword3 = scanner.nextLine();
                    while(keyStorePassword3.length() < 6){
                        System.out.println("Keystore password is too short - must be at least 6 characters.\n" +
                                "Please enter password for '" + KeyManagement.keyStoreName + "' :");
                        keyStorePassword3 = scanner.nextLine();
                    }
                    success = KeyManagement.createKey(command[2], command[3],
                            command[4], keyPassword, keyStorePassword3);
                    if(success) System.out.println("Key successfully created.");
                    else System.out.println("Unable to create key.");
                    break;
                case "delete" :
                    if (command.length != 3) {
                        System.out.println("Invalid number of arguments, please try again.");
                        break;
                    }
                    System.out.println("Please enter password for '" + KeyManagement.keyStoreName + "' :");
                    String keyStorePassword4 = scanner.nextLine();
                    while(keyStorePassword4.length() < 6){
                        System.out.println("Keystore password is too short - must be at least 6 characters.\n" +
                                "Please enter password for '" + KeyManagement.keyStoreName + "' :");
                        keyStorePassword4 = scanner.nextLine();
                    }
                    success = KeyManagement.deleteKey(command[2], keyStorePassword4);
                    if(success) System.out.println("Key successfully deleted.");
                    else System.out.println("Unable to delete key.");
                    break;
                case "sign" :
                    if(command.length != 4) System.out.println("Invalid number of arguments, please try again.");
                    System.out.println("Please enter password for '" + command[2] + "' :");
                    keyPassword = scanner.nextLine();

                    PrivateKey key = KeyManagement.getPrivateKey(command[2], keyPassword);
                    if(key == null){
                        System.out.println("Could not get public key.");
                    }else{
                        System.out.println(key.toString());
                        success = DigitalSignature.GenSig(command[1], key, command[3]);
                        if(success){
                            System.out.println("Generated signature.");
                        }else{
                            System.out.println("Could not generate signature.");
                        }
                    }

                    break;
                case "verify" :
                    if(command.length != 5) System.out.println("Invalid number of arguments, please try again.");
                    PublicKey pubKey = (PublicKey) KeyManagement.getPublicKey(command[3]);
                    if(pubKey == null){
                        System.out.println("Could not get public key.");
                    }else{
                        System.out.println(pubKey.toString());
                        DigitalSignature.VerSig(command[1], command[2], pubKey, command[4]);
                    }

                    break;
                case "help" :
                    String help = "(For more info see: https://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html)\n";
                    help += "Key Generation Algorithms:\n";
                    help += "'DiffieHellman'\n'DSA'\n'RSA'\n'EC'\n";
                    help += "Signature Algorithms:\n";
                    help += "'NONEwithRSA'\n" +
                            "'MD2withRSA', 'MD5withRSA'\n" +
                            "'SHA1withRSA', 'SHA256withRSA', 'SHA384withRSA', 'SHA512withRSA'\n" +
                            "'NONEwithDSA'\n" +
                            "'SHA1withDSA'\n" +
                            "'NONEwithECDSA', 'SHA1withECDSA', 'SHA256withECDSA', 'SHA384withECDSA', 'SHA512withECDSA'\n";
                    System.out.println(help);
                    break;
                default :
                    System.out.println("Invalid command, please try again.");

            }
            line = scanner.nextLine();
        }

        /*
        String keyStorePath = null;
        System.out.println("Do you want to use an existing KeyStore? (Y/N)");
        String line = scanner.nextLine();
        while(line.compareToIgnoreCase("Y") != 0 && line.compareToIgnoreCase("N") != 0){
            System.out.println("Invalid choice.\nDo you want to use an existing KeyStore? (Y/N)");
            line = scanner.nextLine();
        }
        if(line.compareToIgnoreCase("Y") == 0){
            System.out.println("Enter the path of the KeyStore:");
            keyStorePath = scanner.nextLine();
        }
        KeyManagement.initializeKeyStore("", keyStorePath);

        System.out.println("Do you want to use an existing key? (Y/N)");
        line = scanner.nextLine();
        while(line.compareToIgnoreCase("Y") != 0 && line.compareToIgnoreCase("N") != 0){
            System.out.println("Invalid choice.\nDo you want to use an existing key? (Y/N)");
            line = scanner.nextLine();
        }

        String keyGenAlg=null;
        int keyLenght = 0;

        if(line.compareToIgnoreCase("Y") == 0){
            System.out.println("Enter key name:");
            String keyName = scanner.nextLine();
            System.out.println("Enter key password:");
            String password = scanner.nextLine();
            KeyManagement.getKey(keyName,password);


        }else if(line.compareToIgnoreCase("N") == 0) {

            System.out.println("Which key generation algorithm do you want to use?\n" +
                    "(ex: \"DH\", \"DSA\", \"RSA\", \"EC\")");
            keyGenAlg = scanner.nextLine();

            System.out.println("What should the key lenght be?");
            line = scanner.nextLine();
            keyLenght = Integer.parseInt(line);
        }

        System.out.println("Which signature algorithm do you want to use?\n" +
                "(ex: \"MD5withRSA\", \"SHA256withRSA\", \"SHA1withDSA\", \"SHA256withECDSA\" ...)");
        String signAlg = scanner.nextLine();

        System.out.println("Enter path of text file to be signed\n" +
                "(ex: \"/Users/ecetanova/Desktop/shorts.txt\")");
        String path = scanner.nextLine();

        boolean success = DigitalSignature.GenSig(path, keyGenAlg, keyLenght, signAlg);
        if(success){
            System.out.println("Signature successfully generated.");
        }else{
            System.out.println("Signature generation unsuccessful.");
            return;
        }

*/

    }
}
