package td2cryptoavancee;

import com.sun.net.httpserver.*;
import it.unisa.dia.gas.jpbc.*;
import td2cryptoavancee.RSATunnelKey;
import td2cryptoavancee.SettingParameters;

import java.io.*;
import java.net.*;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

//Compilation : javac -cp ".:../../lib/jpbc-2.0.0/jars/*" td2cryptoavancee/*.java (in CorrectionBasicIdentIBE !)
//Execution : java -cp ".:../../lib/jpbc-2.0.0/jars/*" td2cryptoavancee.HttpServeurAutorite

public class HttpServeurAutorite {
    private static SettingParameters pp;
    private static Element msk;
    private static Pairing pairing;
    private static RSATunnelKey authRSA; 
    private static String data;
    private static String codeClient;

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
            server.createContext("/data", he -> {
                try {
                    String responseStr = "ERROR";
                    String requestRaw = new String(he.getRequestBody().readAllBytes());

                    System.out.println("\n[TRAFFIC ENTRANT] Nouveau paquet reçu :");
                    System.out.println("------------------------------------------");
                    
                    // CAS 1 : Demande de clé publique serveur
                    if (requestRaw.contains("DATA_SENT")) {
                        System.out.println("Stocking Data :");
                        data=requestRaw.split("DATA_SENT::SPLIT::")[1];
                        System.out.println("Data stocked : "+ data);
                        he.sendResponseHeaders(200, "Received the data".length());
                        he.getResponseBody().write("Received the data".getBytes());
                        he.getResponseBody().close();
                        System.out.println("------------------------------------------");
                        System.out.println("[TRAFFIC SORTANT] Data envoyée.");
                    } 
                    else if (requestRaw.contains("REQUEST_DATA")){
                        System.out.println("Sending Data : ");
                        //Message de la forme REQUEST_DATA::SPLIT::Public_key
                        String public_key_str=requestRaw.split("::SPLIT::")[1];
                        PublicKey public_key = decodeKey(public_key_str);
                        //responseStr=FluxMessagerieIBE.encryptRSA(data.toString(),public_key);
                        he.sendResponseHeaders(200, data.length());
                        he.getResponseBody().write(data.getBytes());
                        he.getResponseBody().close();
                        System.out.println("------------------------------------------");
                        System.out.println("[TRAFFIC SORTANT] Data envoyée.");
                    }

                }
                catch (Exception e) { 
                    System.err.println("[ERREUR] " + e.getMessage());
                }
            });
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
                    // CAS 2 : Demande de paramètres PP
                    else if (requestRaw.equals("REQ_PP")) {
                        System.out.println("Type : Handshake IBE");
                        responseStr = pp.getP().toString();
                        
                        System.out.println("Action : Envoi des paramètres publics (P).");
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

                        if (commande.equals("DEMANDE_CLE")) {
                            responseStr = "CHALLENGE_ENVOYE";
                            System.out.println("Action : Validation mail simulée.");
                        } 
                        else if (commande.equals("VERIF_CODE")) {
                            String email = cmdParts[1];
                            System.out.println("Action : Génération de la clé IBE pour " + email);
                            
                            KeyPair ibeKey = FluxMessagerieIBE.etape6_Autorite_GenererClePriveeClient(pairing, msk, email);
                            String skStr = Base64.getEncoder().encodeToString(ibeKey.getSk().toBytes());
                            
                            System.out.println("[CRYPTO] Clé IBE brute (Base64) : " + skStr.substring(0, 30) + "...");
                            
                            // CHIFFREMENT DE LA RÉPONSE
                            PublicKey clientPub = decodeKey(clientPubKeyStr);
                            responseStr = FluxMessagerieIBE.encryptRSA(skStr+"::SPLIT::"+codeClient, clientPub);
                            System.out.println("[CRYPTO] Clé IBE chiffrée avec la clé client : " + responseStr.substring(0, 50) + "...");
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