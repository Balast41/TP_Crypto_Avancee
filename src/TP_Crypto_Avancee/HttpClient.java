package TP_Crypto_Avancee;

import it.unisa.dia.gas.jpbc.*;
import java.io.*;
import java.net.*;
import java.security.*;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

//Compilation : javac -cp "lib/jpbc-2.0.0/jars/*:lib/JakartaMail/*" TPJavaMail/TP_Crypto_Avancee/src/TP_Crypto_Avancee/*.java
//Execution : java -cp "lib/jpbc-2.0.0/jars/*:lib/JakartaMail/*:TPJavaMail/TP_Crypto_Avancee/src" TP_Crypto_Avancee.HttpClient

public class HttpClient {
    //Parameter to define by the client when creating the class
    private static String email;
    private static String password;
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
    // Verbose
    private static boolean verbose=true;
    // Association nom de fichier chiffré <-> nom de fichier clair pour les PJ
    //Code de l'autorité
    private static String codeAutorite;

// Dans HttpClient.java (au début de la classe ou en haut des méthodes)
private static class MailTechnicalData {
    public String[] encryptedNames; // Ajoute 'public' pour être sûr
    public String uB64;
    public String vB64;

    public MailTechnicalData(String[] names, String u, String v) {
        this.encryptedNames = names;
        this.uB64 = u;
        this.vB64 = v;
    }
}

    private static Map<String, MailTechnicalData> globalTranslationTable = new HashMap<>();




    public String getHost() { return "imap.gmail.com"; }
public String getUser() { return getEmail(); } // Utilise ton getEmail()
public String getPass() { return getPassword(); } // Utilise ton getPassword()
public String getFilter() { return ""; } // Laisse vide pour tout recevoir ou mets getEmail()


    public static String getEmail() {
        return email;
    }

    public static String getPassword() {
        return password;
    }

    private static String PrivateVerificationCode() {
        Random random = new Random();
        int chiffre = random.nextInt(1000000); // Génère un entier entre 0 et 999999
        return String.format("%06d", chiffre); // Formate pour toujours avoir 6 chiffres avec des zéros devant
    }

    public HttpClient(String email, String password) {
        print("Compilé 2");
        Setup(email, password);
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

    private static void print(String message) {
        if (verbose) {
            System.out.println(message);
        }
    }

    private void Setup(String email, String password){
        try{
            url_service = new URL("http://10.29.124.129:8080/service");
            this.email=email;
            this.password=password;
            pairing = it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory.getPairing("/home/shila/Documents/CryptoAvancée/lib/jpbc-2.0.0/params/curves/a.properties");
            myRSA = FluxMessagerieIBE.generateRSAKeyPair();
            myPubStr = Base64.getEncoder().encodeToString(myRSA.getPublicKey().getEncoded());
            print("myPubStr: " + myPubStr);
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
            print(PPText);
        } catch (Exception e) { 
            System.err.println("\n[ERREUR] " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void RetrieveRSA(){
        try{
        String RSAKeyStr=new String(sendPost(url_service, "REQ_AUTH_RSA"));
        authPub = decodeKey(RSAKeyStr);
        print("RSA Key of the Autority : " + RSAKeyStr);
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
        codeAutorite=code;
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
        print("response"+response);
        if (response.equals("CODE_2FA_INVALIDE") || response.equals("CODE_2FA_EXPIRE")) {
            System.out.println("Code de validation 2FA invalide ou expiré. Impossible d'obtenir la clé IBE.");
        }
        else{
            System.out.println("Avant déchiffrement AES : " + response);
            String responseAES=FluxMessagerieIBE.decryptMessageWithCodes(codeAutorite, internCodeIBE, response );
            System.out.println("Message décrypté : "+responseAES);
            String[] responsesBlock=responseAES.split("::SPLIT::");
            if (responsesBlock.length>1){
                if (responsesBlock[1].strip().equals(internCodeIBE)){
                    IBEKey= pairing.getG1().newElementFromBytes(Base64.getDecoder().decode(responsesBlock[0].strip()));
                    print("IBEKey: " + IBEKey.toString());
                }
            }
        }
                } catch (Exception e) { 
            System.err.println("\n[ERREUR] " + e.getMessage());
            e.printStackTrace();
        }
    }

        public static boolean sendingAMail(Mail mail){
        try{
                SendMail.sendMail(mail);
                return true;
                // A chaque fois, le "message chiffré" est serializedCipher.getAescipher
                // Important, il faut inclure à la fin du mail, ou via un auter moyen (donnée caché) le U et le V (serializedCipher.getU et serializedCipher.getV) pour que le destinataire puisse déchiffrer le mail
                          } catch (Exception e) { 
            System.err.println("\n[ERREUR] " + e.getMessage());
            e.printStackTrace();
        }    
        return false;  
    }

    public static boolean sendingAMailCrypted(Mail mail){
        try{
                print(" On envoie des données");
                IBEcipher AESKey = IBEBasicIdent.IBEGlobalKey(AutorityPP,AutorityP, mail.getDestinataire());
                print("AESKey:"+AESKey.getAescipher().toString());
                IBEcipher encryptedObject =
                    IBEBasicIdent.IBEencryption(AESKey,mail.getObjet().getBytes());
                IBEcipher encryptedMessage =
                    IBEBasicIdent.IBEencryption(AESKey,mail.getMessage().getBytes());
                print("Message avant : "+ mail.getMessage() + " \n Message après : " + encryptedMessage.getAescipher().toString());
                String serializedCipher = IBECipherUtils.serializeIBECipher(encryptedMessage);
                print("SerializedCipher:"+serializedCipher);
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
                return true;
                // A chaque fois, le "message chiffré" est serializedCipher.getAescipher
                // Important, il faut inclure à la fin du mail, ou via un auter moyen (donnée caché) le U et le V (serializedCipher.getU et serializedCipher.getV) pour que le destinataire puisse déchiffrer le mail
                          } catch (Exception e) { 
            System.err.println("\n[ERREUR] " + e.getMessage());
            e.printStackTrace();
        }      
        return false;
    }

public static Mail decryptMailMetadata(Mail mail) {
    try {
        String content = mail.getMessage();
        if (!content.contains("::KEY::")) return mail;

        String[] parts = content.split("::KEY::");
        String encryptedMessageB64 = parts[0].trim();
        String[] keyParts = parts[1].split("::SPLIT::");
        
        String uB64 = keyParts[0].replace("U:", "").trim();
        String vB64 = keyParts[1].replace("V:", "").trim();

        // --- AJOUT : On sauvegarde les noms bruts et les clés avant déchiffrement ---
        globalTranslationTable.put(mail.getId(), new MailTechnicalData(mail.getPath(), uB64, vB64));

        Element U = pairing.getG1().newElementFromBytes(Base64.getDecoder().decode(uB64));
        byte[] V = Base64.getDecoder().decode(vB64);

        // 1. Décrypter le Message
        byte[] msgClair = IBEBasicIdent.IBEdecryption(pairing, AutorityP, AutorityPP, IBEKey, 
                        new IBEcipher(U, V, Base64.getDecoder().decode(encryptedMessageB64)));
        mail.setMessage(new String(msgClair));

        // 2. Décrypter l'Objet
        byte[] objClair = IBEBasicIdent.IBEdecryption(pairing, AutorityP, AutorityPP, IBEKey, 
                        new IBEcipher(U, V, Base64.getDecoder().decode(mail.getObjet())));
        mail.setObjet(new String(objClair));

        // 3. Décrypter les NOMS des fichiers
        String[] encPaths = mail.getPath();
        String[] decNames = new String[encPaths.length];
        for (int i = 0; i < encPaths.length; i++) {
            String cleanName = encPaths[i].replace(".enc", "").trim();
            byte[] nameClair = IBEBasicIdent.IBEdecryption(pairing, AutorityP, AutorityPP, IBEKey, 
                            new IBEcipher(U, V, Base64.getDecoder().decode(cleanName)));
            decNames[i] = new String(nameClair);
        }
        mail.setPath(decNames);

        return mail;
    } catch (Exception e) {
        return mail;
    }
}

    public static Mail[] getAllMails(String host, String user, String pass, String filter, int n) {
            long start = System.nanoTime();
            List<Mail> mailsFetch = FetchingEmail.fetchLight(host, user, pass, filter, n);
            long stop = System.nanoTime();
        System.out.println("Temps d'exécution : " + ((stop - start) / 1_000_000.0) + " ms (fetchLight)");
            Mail[] result = new Mail[mailsFetch.size()];
        // Dans HttpClient.java -> getAllMails
        start = System.nanoTime();
        for (int i = 0; i < mailsFetch.size(); i++) {
            result[i] = mailsFetch.get(i);
            
            if (!globalTranslationTable.containsKey(result[i].getId())) {
                globalTranslationTable.put(result[i].getId(),
                    new MailTechnicalData(result[i].getPath(), null, null));
            }
        }
        stop = System.nanoTime();
        System.out.println("Temps d'exécution : " + ((stop - start) / 1_000_000.0) + " ms (stockage dans la table de traduction)");
            return result;
        }

public void downloadFile(String host, String user, String pass, Mail currentMail, String displayName, String destPath) {
    try {
        String mailId = currentMail.getId();
        MailTechnicalData techData = globalTranslationTable.get(mailId);
        String realNameOnServer = displayName;

        if (techData != null) {
            String[] displayList = currentMail.getPath();
            for (int i = 0; i < displayList.length; i++) {
                if (displayList[i].equals(displayName)) {
                    realNameOnServer = techData.encryptedNames[i];
                    break;
                }
            }
        }

        System.out.println("Translation : " + displayName + " -> " + realNameOnServer);
        byte[] data = FetchingEmail.downloadSpecificFile(host, user, pass, mailId, realNameOnServer);

        if (data != null) {
            // --- AJOUT : Déchiffrement du contenu du fichier ---
            if (techData != null && techData.uB64 != null) {
                Element U = pairing.getG1().newElementFromBytes(Base64.getDecoder().decode(techData.uB64));
                byte[] V = Base64.getDecoder().decode(techData.vB64);
                
                byte[] fichierClair = IBEBasicIdent.IBEdecryption(pairing, AutorityP, AutorityPP, IBEKey, 
                                    new IBEcipher(U, V, data));
                java.nio.file.Files.write(java.nio.file.Paths.get(destPath), fichierClair);
            } else {
                java.nio.file.Files.write(java.nio.file.Paths.get(destPath), data);
            }
            System.out.println("Fichier enregistré vers : " + destPath);
        }
    } catch (Exception e) { e.printStackTrace(); }
}
}
