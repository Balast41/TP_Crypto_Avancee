package td2cryptoavancee;

import it.unisa.dia.gas.jpbc.*;
import java.io.*;
import java.net.*;
import java.security.*;
import java.util.Base64;
import java.util.Scanner;


//Compilation : javac -cp ".:../../lib/jpbc-2.0.0/jars/*" td2cryptoavancee/*.java (in CorrectionBasicIdentIBE !)
//Execution : java -cp ".:../../lib/jpbc-2.0.0/jars/*" td2cryptoavancee.HttpClientTest
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
                        pairing = it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory.getPairing("/home/shila/Documents/CryptoAvancée/lib/jpbc-2.0.0/params/curves/a.properties");

            myRSA = FluxMessagerieIBE.generateRSAKeyPair();
            myPubStr = Base64.getEncoder().encodeToString(myRSA.getPublicKey().getEncoded());
            url_service = new URL("http://127.0.0.1:8080/service");
            url_data=new URL("http://127.0.0.1:8080/data");

            email="Test@mail.com";
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

    public static void sendingAMail(String destinataire, String objet, String message, String path){
        try{
                System.out.println(" On envoie des données");
                IBEcipher AESKey = IBEBasicIdent.IBEGlobalKey(AutorityPP,AutorityP, destinataire);
                System.out.println("AESKey:"+AESKey.getAescipher().toString());
                IBEcipher encryptedMessage =
                    IBEBasicIdent.IBEencryption(AESKey,message.getBytes());
                System.out.println("Message avant : "+ message + " \n Message après : " + encryptedMessage.getAescipher().toString());
                String serializedCipher = IBECipherUtils.serializeIBECipher(encryptedMessage);
                System.out.println("SerializedCipher:"+serializedCipher);
                String message_envoye="DATA_SENT::SPLIT::"+serializedCipher;

                String respData = new String(sendPost(url_data,message_envoye));
                System.out.println("Data envoyé !");
                          } catch (Exception e) { 
            System.err.println("\n[ERREUR] " + e.getMessage());
            e.printStackTrace();
        }      
    }

    public static void receivingAMail(){
        try{
                System.out.println("Récupération de data :");

                String message_envoye="REQUEST_DATA::SPLIT::"+myPubStr;
                System.out.println("MEssage envoyée : "+message_envoye);
                String encryptedResponse= new String(sendPost(url_data,message_envoye));

                System.out.println("EncryptedResponse : " + encryptedResponse.substring(0,50)+"...");

                //String cipherSerialized = FluxMessagerieIBE.decryptRSA(encryptedResponse, myRSA.getPrivateKey());

                //System.out.println("Cipher reçu : "+ cipherSerialized);

                IBEcipher cipher = IBECipherUtils.deserializeIBECipher(encryptedResponse, pairing);

                byte[] resultatClair = IBEBasicIdent.IBEdecryption(
                        pairing,
                        AutorityP,
                        AutorityPP,
                        IBEKey,
                        cipher
                );

                System.out.println("Message décrypté : "+ new String(resultatClair));
            }
                                      catch (Exception e) { 
            System.err.println("\n[ERREUR] " + e.getMessage());
            e.printStackTrace();
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
                sendingAMail("Test@mail.com","Mail","Test du serveur, ca marche totalement ;)","");
            }
            else if (i==2){
                receivingAMail();
            }
    }

}