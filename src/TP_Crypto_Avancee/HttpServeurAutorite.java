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
    // Paramètres système IBE (P, P_pub) et Maître Clé Secrète (s)
    private static SettingParameters pp;
    private static Element msk;
    private static Pairing pairing;
    
    // Couple de clés RSA pour sécuriser la transmission de la clé IBE au client
    private static RSATunnelKey authRSA; 
    private static String data;
    private static String codeClient;
    private static String codeClient2FA;
    private static OffsetTime expirationCode2FA;
    
    // Configuration du compte mail de l'autorité pour l'envoi des codes OTP
    private static String emailAutorite="autorite.autoreply@gmail.com";
    private static String passwordAutorite="ajbv mfmd pgyq lgzd";
    
    /** * Registre temporaire des sessions de demande de clés.
     * Format : [email, codeClient, commande, codeverif, expiration, CleIBE]
     */
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
            // Initialisation du couplage et génération des paramètres maîtres (Setup de Boneh-Franklin)
            pairing = it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory.getPairing("/home/shila/Documents/CryptoAvancée/lib/jpbc-2.0.0/params/curves/a.properties");
            pp = FluxMessagerieIBE.etape1_Autorite_Initialisation(pairing);
            msk = pp.getMsk();
            
            // Génération de la paire RSA pour le handshake avec les clients
            authRSA = FluxMessagerieIBE.generateRSAKeyPair();
            
            print("==================================================");
            print("[INITIALISATION SERVEUR]");
            print("Ma Clé Publique RSA (Base64) : " + encodeKey(authRSA.getPublicKey()));
            print("Serveur prêt sur le port 8080...");
            print("==================================================");

            // Lancement du serveur HTTP sur l'IP locale (Port 8080)
            HttpServer server = HttpServer.create(new InetSocketAddress("10.29.124.129",8080), 0);
            server.createContext("/service", he -> {
                
                try {
                    String responseStr = "ERROR";
                    String requestRaw = new String(he.getRequestBody().readAllBytes());

                    print("\n[TRAFFIC ENTRANT] Nouveau paquet reçu :");
                    print("------------------------------------------");
                    
                    // Cas 0 : Envoi de la pp (Public Parameters)
                    if (requestRaw.equals("SEND_PP")){
                        print("Type : Envoi des paramètres publiques");
                        // On transmet P et P_pub séparés par un délimiteur pour le client
                        responseStr = "P:" + pp.getP() + "::SPLIT::PP:"+pp.getP_pub();
                    }
                    // CAS 1 : Demande de clé publique serveur (RSA Handshake)
                    if (requestRaw.equals("REQ_AUTH_RSA")) {
                        print("Type : Handshake RSA");
                        responseStr = encodeKey(authRSA.getPublicKey());
                        print("Action : Envoi de ma clé publique au client.");
                    } 
                    // CAS 3 : Requête composite (Clé Client + Donnée chiffrée)
                    else if (requestRaw.contains("::ENCRYPTED::")) {

                        String[] parts = requestRaw.split("::ENCRYPTED::"); 
                        String encryptedPayload = parts[1];
                        
                        // DECHIFFREMENT du payload envoyé par le client via la clé privée RSA du serveur
                        String decryptedReq = FluxMessagerieIBE.decryptRSA(encryptedPayload, authRSA.getPrivateKey());
                        print("ici décrypté : " + decryptedReq);
                        
                        String[] cmdParts = decryptedReq.split("\\|");
                        String commande = cmdParts[0];
                        
                        // Extraction du code client utilisé pour le futur chiffrement AES de la réponse
                        if (cmdParts[1].split("::SPLIT::").length==2){
                            codeClient=cmdParts[1].split("::SPLIT::")[1];
                        }
                        
                        String code = null;
                        OffsetTime expirationDate = null;
                        boolean userExists = false;
                        int index=-1;

                        // Gestion de la session utilisateur dans le tableau de bord
                        Code2FA code2FA = new Code2FA("0", null);
                        for (List<String> user : tableau_users) {
                            if (user.get(0).equals(cmdParts[1].split("::SPLIT::")[0])) {
                                userExists = true;
                                index = tableau_users.indexOf(user);
                                user.set(2, commande);
                            }
                        }
                        if (index==-1){
                            index=tableau_users.size();
                        }
                        if (!userExists) {
                            // Initialisation d'une nouvelle entrée utilisateur si première demande
                            tableau_users.add(new ArrayList<>(List.of(cmdParts[1].split("::SPLIT::")[0], codeClient, commande, "", "", ""))); /*email, codeClient, commande, codeverif, experitationCode, CleIBE*/
                        }
                        
                        /**
                         * Traitement de la commande DEMANDE_CLE :
                         * Génère un code 2FA et l'envoie par mail à l'utilisateur.
                         */
                        if (commande.equals("DEMANDE_CLE")) {
                            String destinataire=cmdParts[1].split("::SPLIT::")[0];
                            print(destinataire);
                            codeClient=cmdParts[1].split("::SPLIT::")[1];
                            
                            // Génération du challenge OTP
                            code2FA.setCode();
                            code2FA.setExpirationTime();
                            tableau_users.get(index).set(3, code2FA.getCode());
                            tableau_users.get(index).set(4, code2FA.getExpirationTime().toString());
                            
                            // Envoi du mail via le service SMTP de l'autorité
                            String message= "Votre code de validation pour l'authentification à deux facteurs est : \n\n" + tableau_users.get(index).get(3) + "\n\n Ce code est valide pour une seule utilisation et expire dans 10 minutes.";
                            Mail mailCode=new Mail(destinataire,emailAutorite,emailAutorite,passwordAutorite,"Code de Validation pour 2FA",message,null);
                            SendMail.sendMail(mailCode);
                            responseStr = "CHALLENGE_ENVOYE";
                        } 
                        /**
                         * Traitement de la commande VERIF_CODE :
                         * Vérifie l'OTP et génère la clé privée IBE (Extract) si valide.
                         */
                        else if (commande.equals("VERIF_CODE")) {
                            String email = cmdParts[1];
                            String codeRecu = cmdParts[2];
                            print("Vérification du code 2FA pour " + email);
                            print("ici : "+ tableau_users.get(index).get(3));
                            print("la : "+codeRecu);
                            
                            // Vérification de la validité du code et de l'expiration temporelle
                            if (tableau_users.get(index).get(3).equals(codeRecu) && java.time.OffsetTime.now().toString().compareTo(tableau_users.get(index).get(4)) < 0) {
                                print("Code 2FA valide pour " + email);

                                print("Action : Génération de la clé IBE pour " + email);
                                // Calcul de d_ID = s * Q_ID (H_1(ID))
                                tableau_users.get(index).set(5, Base64.getEncoder().encodeToString(FluxMessagerieIBE.etape6_Autorite_GenererClePriveeClient(pairing, msk, email).getSk().toBytes()));
                                
                                print("[CRYPTO] Clé IBE brute (Base64) : " + tableau_users.get(index).get(5).substring(0, 30) + "...");
                                
                                // CHIFFREMENT DE LA RÉPONSE (AES hybride utilisant le code client et le code 2FA)
                                responseStr=FluxMessagerieIBE.encryptMessageWithCodes(codeClient, tableau_users.get(index).get(3), tableau_users.get(index).get(5)+"::SPLIT::"+tableau_users.get(index).get(1));
                                System.out.println("Chiffré avec AES : "+ responseStr);
                                
                                // Nettoyage de la session après succès
                                tableau_users.remove(index);

                            } else {
                                // Gestion des erreurs d'authentification 2FA
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

                    // Envoi de la réponse HTTP au client
                    he.sendResponseHeaders(200, responseStr.length());
                    he.getResponseBody().write(responseStr.getBytes());
                    he.getResponseBody().close();
                    print("------------------------------------------");
                    print("[TRAFFIC SORTANT] Réponse envoyée.");

                } catch (Exception e) { 
                    System.err.println("[ERREUR] " + e.getMessage());
                    System.err.println(Arrays.toString(e.getStackTrace()));
                }
            });
            server.start();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // Utilitaires de conversion pour le transport des clés
    private static String encodeKey(Key key) { return Base64.getEncoder().encodeToString(key.getEncoded()); }
    private static PublicKey decodeKey(String base64) throws Exception {
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(base64)));
    }
}