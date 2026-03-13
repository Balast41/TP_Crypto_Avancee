package TP_Crypto_Avancee;

import it.unisa.dia.gas.jpbc.*;
import java.io.*;
import java.net.*;
import java.security.*;
import java.util.Base64;
import java.util.Random;
import java.util.Scanner;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

//Compilation : javac -cp "lib/jpbc-2.0.0/jars/*:lib/JakartaMail/*" TPJavaMail/TP_Crypto_Avancee/src/TP_Crypto_Avancee/*.java
//Execution : java -cp "lib/jpbc-2.0.0/jars/*:lib/JakartaMail/*:TPJavaMail/TP_Crypto_Avancee/src" TP_Crypto_Avancee.HttpClientTest

public class HttpClient {
    //Parameter to define by the client when creating the class
    private static String email;
    // Curves used
    private static Pairing pairing;
    // URL of the HTTP Server (Autority)
    private static URL url_service;
    // PP of the autority, for IBE
    private static Element AutorityP;
    private static Element AutorityPP;
    //Used for the RSA encryption
    private static RSATunnelKey myRSA;
    private static String myPubStr;
    //RSA Public key of the server
    private static PublicKey authPub;
    //Used to get the IBE Key 
    private static String internCodeIBE="123456";
    private static Element IBEKey;

    private static String PrivateVerificationCode() {
        Random random = new Random();
        int chiffre = random.nextInt(1000000); // Génère un entier entre 0 et 999999
        return String.format("%06d", chiffre); // Formate pour toujours avoir 6 chiffres avec des zéros devant
    }

    public HttpClient(String email){
        Setup(email);
        System.out.println("Setup client... Done.");
        RetrievePP();
        System.out.println("Retrieving the PP... Done.");
        RetrieveRSA();
        System.out.println("Retrieving the RSA Key of the Autority... Done.");
        Asking_IBE_Key();
        System.out.println("Asking the IBE Key... Done.");
    }

    public String VerifyingTheCode(String code){
        String response= VerifyCodeAutority(code);
        if (response.equals("CODE_2FA_INVALIDE")){
            return "FAUX";
        }
        if (response.equals("CODE_2FA_EXPIRE")){
            return "EXPIRE";
        }
        System.out.println("Sending the code... Done and Accepted.");
        decryption_IBE_key(response);
        System.out.println("Got the IBE Key");
        return "VRAI";
    }


    private static byte[] sendPost(URL url, String data) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.getOutputStream().write(data.getBytes());
        return conn.getInputStream().readAllBytes();
    }

    private static PublicKey decodeKey(String base64) throws Exception {
        return KeyFactory.getInstance("RSA").generatePublic(new java.security.spec.X509EncodedKeySpec(Base64.getDecoder().decode(base64)));
    }

    private void Setup(String email){
        try{
            url_service = new URL("http://127.0.0.1:8080/service");
            this.email=email;
            pairing = it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory.getPairing("/home/shila/Documents/CryptoAvancée/lib/jpbc-2.0.0/params/curves/a.properties");
            myRSA = FluxMessagerieIBE.generateRSAKeyPair();
            myPubStr = Base64.getEncoder().encodeToString(myRSA.getPublicKey().getEncoded());
            internCodeIBE=PrivateVerificationCode();
            } 
            catch (Exception e) { 
            System.err.println("\n[ERREUR] " + e.getMessage());
            e.printStackTrace();
            }
    }


    private static void RetrievePP(){
        try{
            String PPText= new String(sendPost(url_service,"SEND_PP"));
            AutorityP = pairing.getG1().newElementFromBytes(PPText.split("::SPLIT::")[0].split("P:")[1].strip().split(",")[0].getBytes());
            AutorityPP = pairing.getG1().newElementFromBytes(PPText.split("::SPLIT::")[1].split("PP:")[1].strip().split(",")[0].getBytes());
        } catch (Exception e) { 
            System.err.println("\n[ERREUR] " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void RetrieveRSA(){
        try{
        String RSAKeyStr=new String(sendPost(url_service, "REQ_AUTH_RSA"));
        authPub = decodeKey(RSAKeyStr);
        
        } catch (Exception e) { 
            System.err.println("\n[ERREUR] " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void Asking_IBE_Key(){
        try{
        String encPayload1 = FluxMessagerieIBE.encryptRSA("DEMANDE_CLE|" + email, authPub);
        String resp1 = new String(sendPost(url_service, myPubStr + "::SPLIT::" + encPayload1+"::SPLIT::"+internCodeIBE));
                } catch (Exception e) { 
            System.err.println("\n[ERREUR] " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String VerifyCodeAutority(String code){
        try{        
        String encPayload2 = FluxMessagerieIBE.encryptRSA("VERIF_CODE|" + email + "|" + code, authPub);
        byte[] responseRaw = sendPost(url_service, myPubStr + "::SPLIT::" + encPayload2);
        return new String(responseRaw);
        } catch (Exception e) { 
            System.err.println("\n[ERREUR] " + e.getMessage());
            e.printStackTrace();
            return "";
        }
    }

    private static void decryption_IBE_key(String response){
        try{
        String[] responsesBlock = FluxMessagerieIBE.decryptRSA(response, myRSA.getPrivateKey()).split("::SPLIT::");
        if (responsesBlock[1].strip().equals(internCodeIBE)){
            IBEKey= pairing.getG1().newElementFromBytes(Base64.getDecoder().decode(responsesBlock[0].strip()));
        }
        else{
            System.out.println("Code secret non reconnu. Ca ne doit pas être l'autorité ! ");
            return;
        }
                } catch (Exception e) { 
            System.err.println("\n[ERREUR] " + e.getMessage());
            e.printStackTrace();
        }
    }

        public static void sendingAMail(Mail mail){
        try{
                System.out.println(" On envoie des données");
                SendMail.sendMail(mail);
                // A chaque fois, le "message chiffré" est serializedCipher.getAescipher
                // Important, il faut inclure à la fin du mail, ou via un auter moyen (donnée caché) le U et le V (serializedCipher.getU et serializedCipher.getV) pour que le destinataire puisse déchiffrer le mail
                System.out.println("Data envoyé !");
                          } catch (Exception e) { 
            System.err.println("\n[ERREUR] " + e.getMessage());
            e.printStackTrace();
        }      
    }

    public static void sendingAMailCrypted(Mail mail){
        try{
                System.out.println(" On envoie des données");
                IBEcipher AESKey = IBEBasicIdent.IBEGlobalKey(AutorityPP,AutorityP, mail.getDestinataire());
                System.out.println("AESKey:"+AESKey.getAescipher().toString());
                IBEcipher encryptedObject =
                    IBEBasicIdent.IBEencryption(AESKey,mail.getObjet().getBytes());
                IBEcipher encryptedMessage =
                    IBEBasicIdent.IBEencryption(AESKey,mail.getMessage().getBytes());
                System.out.println("Message avant : "+ mail.getMessage() + " \n Message après : " + encryptedMessage.getAescipher().toString());
                String serializedCipher = IBECipherUtils.serializeIBECipher(encryptedMessage);
                System.out.println("SerializedCipher:"+serializedCipher);
                String[] pathArray= mail.getPath();
                for(int i=0;i<mail.getPath().length;i++){
                    byte[] fichier = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(pathArray[i]));
                    IBEcipher encryptedFile = IBEBasicIdent.IBEencryption(AESKey, fichier);

                    // Récupère les bytes du cipher (U et V déjà connus)
                    byte[] encryptedBytes = encryptedFile.getAescipher();

                    // Écriture binaire directe, sans Base64
                    String[] parts = pathArray[i].split("[\\\\/]");
                    String filen = Base64.getEncoder().encodeToString(
                            IBEBasicIdent.IBEencryption(AESKey,parts[parts.length - 1].getBytes()).getAescipher()
                    );
                    Files.write(java.nio.file.Paths.get(filen + ".enc"), encryptedBytes);
                    pathArray[i] = filen + ".enc";
                }

                String ObjetEncrypte = Base64.getEncoder().encodeToString(encryptedObject.getAescipher());
                String MessageEncrypte = Base64.getEncoder().encodeToString(encryptedMessage.getAescipher());
                mail.setObjet(ObjetEncrypte);
                String encodedU = Base64.getEncoder().encodeToString(AESKey.getU().toBytes());
                String encodedV = Base64.getEncoder().encodeToString(AESKey.getV());
                mail.setMessage(MessageEncrypte + "\n\n ::KEY:: U:" + encodedU + " \n ::SPLIT:: V:" + encodedV);
                mail.setPath(pathArray);
                SendMail.sendMail(mail);
                // A chaque fois, le "message chiffré" est serializedCipher.getAescipher
                // Important, il faut inclure à la fin du mail, ou via un auter moyen (donnée caché) le U et le V (serializedCipher.getU et serializedCipher.getV) pour que le destinataire puisse déchiffrer le mail
                System.out.println("Data envoyé !");
                          } catch (Exception e) { 
            System.err.println("\n[ERREUR] " + e.getMessage());
            e.printStackTrace();
        }      
    }

    private static Mail DecryptAMail(Mail mail){
        try{
                System.out.println("Récupération de data :");
                String[] parts=mail.getMessage().split("::KEY::");
                if (parts.length>1){
                    System.out.println("Le mail est chiffré avec notre système, on le comprend ;)");
                }
                else{
                    System.out.println("Le mail n'est pas chiffré avec notre truc, on ne le comprend pas :(");
                    return mail;
                }
                String encryptedMessageB64 = parts[0].trim();
                if (encryptedMessageB64.startsWith("[")) {
                    System.out.println("Payload legacy detecte ([B@...). Ce mail a ete chiffre avec l'ancien format et ne peut pas etre decode en Base64.");
                    return mail;
                }
                String[] keyParts = parts[1].split("::SPLIT::");
                String uB64 = keyParts[0].replace("U:", "").trim();
                String vB64 = keyParts[1].replace("V:", "").trim();
                Element U = pairing.getG1().newElementFromBytes(Base64.getDecoder().decode(uB64));
                byte[] V = Base64.getDecoder().decode(vB64);
                //Message à déchiffrer
                IBEcipher cipherMessage= new IBEcipher(U,V,Base64.getDecoder().decode(encryptedMessageB64));
                byte[] MessageClair = IBEBasicIdent.IBEdecryption(
                        pairing,
                        AutorityP,
                        AutorityPP,
                        IBEKey,
                        cipherMessage
                );
                mail.setMessage(new String(MessageClair));
                System.out.println("Message décrypté : "+ new String(MessageClair));
                IBEcipher cipherObjet= new IBEcipher(U,V,Base64.getDecoder().decode(mail.getObjet()));
                byte[] ObjetClair = IBEBasicIdent.IBEdecryption(
                        pairing,
                        AutorityP,
                        AutorityPP,
                        IBEKey,
                        cipherObjet
                );
                System.out.println("Objet décrypté : "+ new String(ObjetClair));
                mail.setObjet(new String(ObjetClair));
                String[] path= mail.getPath();
                for (int i=0; i < path.length;i++){
                    byte[] encryptedBytes = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(path[i]));

                    // On recrée l'objet IBEcipher avec U et V connus-
                    IBEcipher cipherFichier = new IBEcipher(U, V, encryptedBytes);
                    byte[] FichierClair = IBEBasicIdent.IBEdecryption(pairing, AutorityP, AutorityPP, IBEKey, cipherFichier);
                    String[] partsFiles = path[i].split("[\\\\/]");
                    String filen = partsFiles[partsFiles.length - 1]
                            .replace(".enc","")
                            .replace("\uFEFF", "")
                            .trim();

                    System.out.println("Ici : " + filen);

                    IBEcipher cipherNameFichier = new IBEcipher(
                            U,
                            V,
                            Base64.getDecoder().decode(filen)
                    );
                    String Filename= new String(IBEBasicIdent.IBEdecryption(pairing, AutorityP, AutorityPP,IBEKey,cipherNameFichier));
                    Files.write(java.nio.file.Paths.get(Filename), FichierClair);

                    path[i] = Filename;
                }
                mail.setPath(path);
                return mail;
            }
            catch (Exception e) { 
            System.err.println("\n[ERREUR] " + e.getMessage());
            e.printStackTrace();
            return mail;
        }    
    }

    public static Mail[] getAllMails(String host, String mailStoreType, String username, String password, String senderFilter, int NumberOfMail){
                Mail[] mailsFetch = FetchingEmail.fetch(host, mailStoreType, username, password, senderFilter, 1);
                Mail[] mails= new Mail[mailsFetch.length];
                int i = 0;
                for (Mail mail:mailsFetch){
                    Mail mailDechiffre=DecryptAMail(mail);
                    mails[i]=mailDechiffre;
                    i+=1;
                }
                return mails;
        }
    }
