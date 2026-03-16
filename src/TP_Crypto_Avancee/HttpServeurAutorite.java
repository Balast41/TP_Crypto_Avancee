package TP_Crypto_Avancee;

import com.sun.net.httpserver.*;
import it.unisa.dia.gas.jpbc.*;
import TP_Crypto_Avancee.RSATunnelKey;
import TP_Crypto_Avancee.SettingParameters;

import java.io.*;
import java.lang.reflect.Array;
import java.net.*;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.time.*;
import java.util.*;
import java.lang.*;

//Compilation : javac -cp "lib/jpbc-2.0.0/jars/*:lib/JakartaMail/*:." -d . TPJavaMail/TP_Crypto_Avancee/src/TP_Crypto_Avancee/*.java
//Execution : java -cp "lib/jpbc-2.0.0/jars/*:lib/JakartaMail/*:." TP_Crypto_Avancee.HttpServeurAutorite
public class HttpServeurAutorite {
    private static SettingParameters pp;
    private static Element msk;
    private static Pairing pairing;
    private static RSATunnelKey authRSA; 
    private static String data;
    private static String codeClient;
    private static String codeClient2FA;
    private static OffsetTime expirationCode2FA;
    private static String emailAutorite="autorite.autoreply@gmail.com";
    private static String passwordAutorite="ajbv mfmd pgyq lgzd";
    private static List<List<String>> tableau_users= new ArrayList<>();
    // Verbose
    private static boolean verbose=true;

    private static void print(String message) {
        if (verbose) {
            System.out.println(message);
        }
    }
    public static void main(String[] args) {
        print("Compilé");
        try {
            pairing = it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory.getPairing("/home/shila/Documents/CryptoAvancée/lib/jpbc-2.0.0/params/curves/a.properties");
            pp = FluxMessagerieIBE.etape1_Autorite_Initialisation(pairing);
            msk = pp.getMsk();
            authRSA = FluxMessagerieIBE.generateRSAKeyPair();
            
            print("==================================================");
            print("[INITIALISATION SERVEUR]");
            print("Ma Clé Publique RSA (Base64) : " + encodeKey(authRSA.getPublicKey()));
            print("Serveur prêt sur le port 8080...");
            print("==================================================");

            HttpServer server = HttpServer.create(new InetSocketAddress("10.29.124.129",8080), 0);
            server.createContext("/service", he -> {
                
                try {
                    String responseStr = "ERROR";
                    String requestRaw = new String(he.getRequestBody().readAllBytes());

                    print("\n[TRAFFIC ENTRANT] Nouveau paquet reçu :");
                    print("------------------------------------------");
                    // Cas 0 : Envoi de la pp
                    if (requestRaw.equals("SEND_PP")){
                        print("Type : Envoi des paramètres publiques");
                        responseStr = "P:" + pp.getP() + "::SPLIT::PP:"+pp.getP_pub();
                    }
                    // CAS 1 : Demande de clé publique serveur
                    if (requestRaw.equals("REQ_AUTH_RSA")) {
                        print("Type : Handshake RSA");
                        responseStr = encodeKey(authRSA.getPublicKey());
                        print("Action : Envoi de ma clé publique au client.");
                    } 
                    // CAS 3 : Requête composite (Clé Client + Donnée chiffrée)
                    else if (requestRaw.contains("::SPLIT::")) {
                        String[] parts = requestRaw.split("::SPLIT::"); 
                        String clientPubKeyStr = parts[0];
                        String encryptedPayload = parts[1];
                        if (parts.length==3){
                            codeClient= parts[2];
                        }
                        print("Clé Publique Client extraite (claire) : " + clientPubKeyStr + "...");

                        // DECHIFFREMENT
                        String decryptedReq = FluxMessagerieIBE.decryptRSA(encryptedPayload, authRSA.getPrivateKey());

                        String[] cmdParts = decryptedReq.split("\\|");
                        String commande = cmdParts[0];
                        String code = null;
                        OffsetTime expirationDate = null;
                        boolean userExists = false;
                        int index=-1;

                        Code2FA code2FA = new Code2FA("0", null);
                        for (List<String> user : tableau_users) {
                            if (user.get(0).equals(cmdParts[1])) {
                                userExists = true;
                                index = tableau_users.indexOf(user);
                                user.set(2, commande);
                            }
                        }
                        if (index==-1){
                            index=tableau_users.size();
                        }
                        if (!userExists) {
                            tableau_users.add(new ArrayList<>(List.of(cmdParts[1], codeClient, commande, "", "", "", parts[0]))); /*email, codeClient, commande, codeverif, experitationCode, CleIBE, clientPubKeyStr*/
                        }
                        

                        if (commande.equals("DEMANDE_CLE")) {
                            String destinataire=cmdParts[1];
                            code2FA.setCode();
                            code2FA.setExpirationTime();
                            tableau_users.get(index).set(3, code2FA.getCode());
                            tableau_users.get(index).set(4, code2FA.getExpirationTime().toString());
                            String message= "Votre code de validation pour l'authentification à deux facteurs est : \n\n" + tableau_users.get(index).get(3) + "\n\n Ce code est valide pour une seule utilisation et expire dans 10 minutes.";
                            Mail mailCode=new Mail(destinataire,emailAutorite,emailAutorite,passwordAutorite,"Code de Validation pour 2FA",message,null);
                            SendMail.sendMail(mailCode);
                            responseStr = "CHALLENGE_ENVOYE";
                        } 
                        else if (commande.equals("VERIF_CODE")) {
                            String email = cmdParts[1];
                            String codeRecu = cmdParts[2];
                            print("Vérification du code 2FA pour " + email);
                            if (tableau_users.get(index).get(3).equals(codeRecu) && java.time.OffsetTime.now().toString().compareTo(tableau_users.get(index).get(4)) < 0) {
                                print("Code 2FA valide pour " + email);

                                print("Action : Génération de la clé IBE pour " + email);
                                print("RSA Public Key du client utilisée pour le chiffrement (Base64) : " + tableau_users.get(index).get(6).substring(0, 50) + "...");
                                
                                tableau_users.get(index).set(5, Base64.getEncoder().encodeToString(FluxMessagerieIBE.etape6_Autorite_GenererClePriveeClient(pairing, msk, email).getSk().toBytes()));
                                
                                print("[CRYPTO] Clé IBE brute (Base64) : " + tableau_users.get(index).get(5).substring(0, 30) + "...");
                                
                                // CHIFFREMENT DE LA RÉPONSE
                                responseStr=FluxMessagerieIBE.encryptMessageWithCodes(codeClient, tableau_users.get(index).get(3), tableau_users.get(index).get(5)+"::SPLIT::"+tableau_users.get(index).get(1));
                                System.out.println("Chiffré avec AES : "+ responseStr);
                                tableau_users.remove(index);

                            } else {
                                if (!tableau_users.get(index).get(3).equals(codeRecu)) {
                                    print("Code 2FA invalide pour " + email);
                                    responseStr = "CODE_2FA_INVALIDE";
                                } else if (java.time.OffsetTime.now().toString().compareTo(tableau_users.get(index).get(4)) > 0) {
                                    print("Code 2FA expiré pour " + email);
                                    responseStr = "CODE_2FA_EXPIRE";
                                    tableau_users.remove(index);
                                }
                            }
                            
                        }
                    }

                    he.sendResponseHeaders(200, responseStr.length());
                    he.getResponseBody().write(responseStr.getBytes());
                    he.getResponseBody().close();
                    print("------------------------------------------");
                    print("[TRAFFIC SORTANT] Réponse envoyée.");

                } catch (Exception e) { 
                    System.err.println("[ERREUR] " + e.getMessage());
                }
            });
            server.start();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private static String encodeKey(Key key) { return Base64.getEncoder().encodeToString(key.getEncoded()); }
    private static PublicKey decodeKey(String base64) throws Exception {
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(base64)));
    }
}