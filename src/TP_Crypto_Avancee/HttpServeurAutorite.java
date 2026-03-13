package TP_Crypto_Avancee;

import com.sun.net.httpserver.*;
import it.unisa.dia.gas.jpbc.*;
import TP_Crypto_Avancee.RSATunnelKey;
import TP_Crypto_Avancee.SettingParameters;

import java.io.*;
import java.net.*;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;

//Compilation : javac -cp "lib/jpbc-2.0.0/jars/*:lib/JakartaMail/*" -d . TPJavaMail/TP_Crypto_Avancee/**/*.java
//Execution : java -cp "lib/jpbc-2.0.0/jars/*:lib/JakartaMail/*:TPJavaMail/TP_Crypto_Avancee/src" TP_Crypto_Avancee.HttpServeurAutorite

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

    public static void main(String[] args) {
        try {
            pairing = it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory.getPairing("/home/shila/Documents/CryptoAvancée/lib/jpbc-2.0.0/params/curves/a.properties");
            pp = FluxMessagerieIBE.etape1_Autorite_Initialisation(pairing);
            msk = pp.getMsk();
            authRSA = FluxMessagerieIBE.generateRSAKeyPair();
            
            System.out.println("==================================================");
            System.out.println("[INITIALISATION SERVEUR]");
            System.out.println("Ma Clé Publique RSA (Base64) : " + encodeKey(authRSA.getPublicKey()));
            System.out.println("Serveur prêt sur le port 8080...");
            System.out.println("==================================================");

            HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
            server.createContext("/service", he -> {
                
                try {
                    String responseStr = "ERROR";
                    String requestRaw = new String(he.getRequestBody().readAllBytes());

                    System.out.println("\n[TRAFFIC ENTRANT] Nouveau paquet reçu :");
                    System.out.println("------------------------------------------");
                    // Cas 0 : Envoi de la pp
                    if (requestRaw.equals("SEND_PP")){
                        System.out.println("Type : Envoi des paramètres publiques");
                        responseStr = "P:" + pp.getP() + "::SPLIT::PP:"+pp.getP_pub();
                    }
                    // CAS 1 : Demande de clé publique serveur
                    if (requestRaw.equals("REQ_AUTH_RSA")) {
                        System.out.println("Type : Handshake RSA");
                        responseStr = encodeKey(authRSA.getPublicKey());
                        System.out.println("Action : Envoi de ma clé publique au client.");
                    } 
                    // CAS 3 : Requête composite (Clé Client + Donnée chiffrée)
                    else if (requestRaw.contains("::SPLIT::")) {
                        System.out.println("Type : Requête Composite (Stateless)");
                        System.out.println(requestRaw);
                        String[] parts = requestRaw.split("::SPLIT::");
                        System.out.println(parts);    
                        String clientPubKeyStr = parts[0];
                        String encryptedPayload = parts[1];
                        if (parts.length==3){
                            codeClient= parts[2];
                        }
                        System.out.println("Clé Publique Client extraite (claire) : " + clientPubKeyStr.substring(0, 50) + "...");
                        System.out.println("Payload chiffré reçu : " + encryptedPayload.substring(0, 50) + "...");

                        // DECHIFFREMENT
                        String decryptedReq = FluxMessagerieIBE.decryptRSA(encryptedPayload, authRSA.getPrivateKey());
                        System.out.println("[CRYPTO] Payload déchiffré avec succès : " + decryptedReq);

                        String[] cmdParts = decryptedReq.split("\\|");
                        String commande = cmdParts[0];
                        String code = null;
                        OffsetTime expirationDate = null;

                        tableau_users.add(new ArrayList<>(List.of(cmdParts[1], codeClient, commande)));

                        if (commande.equals("DEMANDE_CLE")) {
                            String destinataire=cmdParts[1];
                            Code2FA code2FA = new Code2FA("0", null);
                            code2FA.setCode();
                            code2FA.setExpirationTime();
                            codeClient2FA = code2FA.getCode();
                            expirationCode2FA = code2FA.getExpirationTime();
                            String message= "Votre code de validation pour l'authentification à deux facteurs est : \n\n" + code + "\n\n Ce code est valide pour une seule utilisation et expire dans 10 minutes.";
                            Mail mailCode=new Mail(destinataire,emailAutorite,emailAutorite,passwordAutorite,"Code de Validation pour 2FA",message,null);
                            SendMail.sendMail(mailCode);
                            responseStr = "CHALLENGE_ENVOYE";
                            System.out.println("Action : Validation mail simulée.");
                        } 
                        else if (commande.equals("VERIF_CODE")) {
                            String email = cmdParts[1];
                            String codeRecu = cmdParts[2];
                            System.out.println("Vérification du code 2FA pour " + email);
                            if (codeClient2FA.equals(codeRecu) && java.time.OffsetTime.now().isBefore(expirationCode2FA)) {
                                System.out.println("Code 2FA valide pour " + email);

                                System.out.println("Action : Génération de la clé IBE pour " + email);
                            
                                KeyPair ibeKey = FluxMessagerieIBE.etape6_Autorite_GenererClePriveeClient(pairing, msk, email);
                                String skStr = Base64.getEncoder().encodeToString(ibeKey.getSk().toBytes());
                                
                                System.out.println("[CRYPTO] Clé IBE brute (Base64) : " + skStr.substring(0, 30) + "...");
                                
                                // CHIFFREMENT DE LA RÉPONSE
                                PublicKey clientPub = decodeKey(clientPubKeyStr);
                                responseStr = FluxMessagerieIBE.encryptRSA(skStr+"::SPLIT::"+codeClient, clientPub);
                                System.out.println("[CRYPTO] Clé IBE chiffrée avec la clé client : " + responseStr.substring(0, 50) + "...");

                            } else {
                                if (!code.equals(codeRecu)) {
                                    System.out.println("Code 2FA invalide pour " + email);
                                    responseStr = "CODE_2FA_INVALIDE";
                                } else if (java.time.OffsetTime.now().isAfter(expirationDate)) {
                                    System.out.println("Code 2FA expiré pour " + email);
                                    responseStr = "CODE_2FA_EXPIRE";
                                }
                            }
                            
                        }
                    }

                    he.sendResponseHeaders(200, responseStr.length());
                    he.getResponseBody().write(responseStr.getBytes());
                    he.getResponseBody().close();
                    System.out.println("------------------------------------------");
                    System.out.println("[TRAFFIC SORTANT] Réponse envoyée.");

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