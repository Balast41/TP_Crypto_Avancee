package TP_Crypto_Avancee;

import it.unisa.dia.gas.jpbc.*;
import java.io.*;
import java.net.*;
import java.security.*;
import java.util.Base64;
import java.util.Scanner;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;


//Compilation : javac -cp ".:../../lib/jpbc-2.0.0/jars/*" TP_Crypto_Avancee/*.java (in CorrectionBasicIdentIBE !)
//Execution : java -cp ".:../../lib/jpbc-2.0.0/jars/*" TP_Crypto_Avancee.HttpClientTest
public class HttpClientTest {
    //Parameter to define by the client when creating the class
    private static String email;
    // Curves used
    private static Pairing pairing;
    // URL of the HTTP Server (Autority)
    private static URL url_service;
    private static URL url_data;
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


    public static void Setup(){
        try{
            System.out.println("[INITIALISATION CLIENT]");
            pairing = it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory.getPairing("C:\\Users\\Quentin\\Documents\\jpbc-2.0.0\\params\\curves\\a.properties");

            myRSA = FluxMessagerieIBE.generateRSAKeyPair();
            myPubStr = Base64.getEncoder().encodeToString(myRSA.getPublicKey().getEncoded());
            url_service = new URL("http://127.0.0.1:8080/service");
            url_data=new URL("http://127.0.0.1:8080/data");

            email="qbalazot@gmail.com";
        } catch (Exception e) { 
            System.err.println("\n[ERREUR] " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void PP(){
        try{
            System.out.println("\n [0] Procedure to get the PP");
            String PPText= new String(sendPost(url_service,"SEND_PP"));
            AutorityP = pairing.getG1().newElementFromBytes(PPText.split("::SPLIT::")[0].split("P:")[1].strip().split(",")[0].getBytes());
            AutorityPP = pairing.getG1().newElementFromBytes(PPText.split("::SPLIT::")[1].split("PP:")[1].strip().split(",")[0].getBytes());
            System.out.println("\n P : " + AutorityP.toString().substring(0,50)  + "... | PP : " + AutorityPP.toString().substring(0,50) + " ...");
        } catch (Exception e) { 
            System.err.println("\n[ERREUR] " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static PublicKey decodeKey(String base64) throws Exception {
        return KeyFactory.getInstance("RSA").generatePublic(new java.security.spec.X509EncodedKeySpec(Base64.getDecoder().decode(base64)));
    }

    private static byte[] sendPost(URL url, String data) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.getOutputStream().write(data.getBytes());
        return conn.getInputStream().readAllBytes();
    }
    
    public static void RSA(){
        try{

        System.out.println("\n [1] Procedure to get the RSA Public Key of the autority");
        String RSAKeyStr=new String(sendPost(url_service, "REQ_AUTH_RSA"));
        authPub = decodeKey(RSAKeyStr);
        System.out.println("RSAKey of the Autority : "+RSAKeyStr);
        
        } catch (Exception e) { 
            System.err.println("\n[ERREUR] " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void IBE_Key_Sending(){
        try{
        System.out.println("Asking the private Key");
        String encPayload1 = FluxMessagerieIBE.encryptRSA("DEMANDE_CLE|" + email, authPub);
        String resp1 = new String(sendPost(url_service, myPubStr + "::SPLIT::" + encPayload1+"::SPLIT::"+internCodeIBE));
                } catch (Exception e) { 
            System.err.println("\n[ERREUR] " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static String code_verification(String code){
        try{        
        System.out.println("\n[3] VÉRIFICATION DU CODE");
        String encPayload2 = FluxMessagerieIBE.encryptRSA("VERIF_CODE|" + email + "|" + code, authPub);
        byte[] responseRaw = sendPost(url_service, myPubStr + "::SPLIT::" + encPayload2);
        return new String(responseRaw);
        } catch (Exception e) { 
            System.err.println("\n[ERREUR] " + e.getMessage());
            e.printStackTrace();
            return "";
        }
    }

    public static void decryption_IBE_key(String response){
        try{
        System.out.println("\n[4] DÉCHIFFREMENT DE LA CLE IBE");
        System.out.println("Avant déchiffrement du message par RSA :" + response);
        String[] responsesBlock = FluxMessagerieIBE.decryptRSA(response, myRSA.getPrivateKey()).split("::SPLIT::");
        System.out.println("Après déchiffrement RSA :"+responsesBlock[0]);
        if (responsesBlock[1].strip().equals(internCodeIBE)){
            IBEKey= pairing.getG1().newElementFromBytes(Base64.getDecoder().decode(responsesBlock[0].strip()));
            System.out.println("Clé IBE :" + IBEKey);
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
                    String[] parts = pathArray[i].split("\\\\");
                    String filen = parts[parts.length - 1];
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

    public static Mail DecryptAMail(Mail mail){
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

                    // On recrée l'objet IBEcipher avec U et V connus
                    IBEcipher cipherFichier = new IBEcipher(U, V, encryptedBytes);
                    byte[] FichierClair = IBEBasicIdent.IBEdecryption(pairing, AutorityP, AutorityPP, IBEKey, cipherFichier);

                    String[] partsFiles = path[i].split("\\\\");
                    String filen = partsFiles[partsFiles.length - 1].replace(".enc","");
                    Files.write(java.nio.file.Paths.get(filen), FichierClair);

                    path[i] = filen;
                }
                mail.setPath(path);
                return mail;
            }
            catch (Exception e) { 
            System.err.println("\n[ERREUR] " + e.getMessage());
            e.printStackTrace();
            return new Mail();
        }      

    }
    public static void main(String[] args) {
            Setup();
            PP();
            RSA();
            IBE_Key_Sending();
            String response=code_verification("123456");
            decryption_IBE_key(response);
            Scanner scanner = new Scanner(System.in);
            System.out.println("1 POUR ENVOYER, 2 POUR RECEVOIR");
            int i = scanner.nextInt();
            if (i==1){
                    String to = "qbalazot@gmail.com";
                    String from = "qbalazot@gmail.com";
                    String username = "qbalazot@gmail.com";
                    String password = "yqvi txzx srtu csye";
                    String subject = "Lettre pour t'avouer mes crimes";
                    String content = "Carotte";
                    String[] attachmentPaths = {"C:\\Users\\Quentin\\Pictures\\05-03-08_1832E001hh.jpg","C:\\Users\\Quentin\\Pictures\\PlayboiCartiStandingGoat.webp"};
                    Mail mailAChiffrer= new Mail(to,from,username,password,subject,content,attachmentPaths);
                    sendingAMail(mailAChiffrer);
            }
            else if (i==2){
                //Récupérer la liste de mail
                String host = "imap.gmail.com";// change 
                String mailStoreType = "imaps";
                String username = 
                    "qbalazot@gmail.com";// change accordingly
                String password = "yqvi txzx srtu csye";// change accordingly
                String senderFilter = "qbalazot@gmail.com";// change accordingly

                // Call method fetch and keep attachment paths in an array.
                Mail[] mails = FetchingEmail.fetch(host, mailStoreType, username, password, senderFilter, 1);
                for (Mail mail:mails){
                    Mail mailDechiffre=DecryptAMail(mail);
                    System.out.println("Mail déchiffré :" + mailDechiffre.toString());
                }

                }

            }
    }
