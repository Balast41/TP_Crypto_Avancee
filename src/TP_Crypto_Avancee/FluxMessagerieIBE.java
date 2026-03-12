package TP_Crypto_Avancee;

import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

import javax.crypto.Cipher;

import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;
import TP_Crypto_Avancee.IBEBasicIdent;
import TP_Crypto_Avancee.IBEcipher;
import TP_Crypto_Avancee.RSATunnelKey;
import TP_Crypto_Avancee.SettingParameters;

public class FluxMessagerieIBE {

    // ==========================================
    // 1. ÉTAPE SETUP (AUTORITÉ UNIQUEMENT)
    // ==========================================
    public static SettingParameters etape1_Autorite_Initialisation(Pairing pairing) {
        // Génère le triplet (P, P_pub, msk)
        SettingParameters ttp_params = IBEBasicIdent.setup(pairing);
        
        // RÈGLE : L'autorité garde ttp_params.getMsk() SECRET.
        // Elle diffuse P et P_pub aux clients (PP).
        return ttp_params;
    }

    // ==========================================
    // 2. ÉTAPE CHIFFREMENT (CLIENT EXPÉDITEUR)
    // ==========================================
    public static IBEcipher etape4_Client_Chiffrer(Pairing pairing, SettingParameters pp, String message, String idDestinataire) throws Exception {
        // Variables :
        // - pp : Paramètres Publics reçus de l'autorité (P et P_pub)
        // - message : Le texte du mail à envoyer
        // - idDestinataire : L'adresse email de la personne à qui on écrit
        
        byte[] messageBytes = message.getBytes("UTF-8");
        
        // Cette fonction génère en interne la clé AES, la chiffre avec l'ID, 
        // et produit l'objet IBEcipher (U, V, Aescipher).
        IBEcipher mailChiffre = IBEBasicIdent.IBEencryption(
                pairing, 
                pp.getP(), 
                pp.getP_pub(), 
                messageBytes, 
                idDestinataire
        );
        
        // DESTINATION : Cet objet 'mailChiffre' est envoyé au serveur de mail ou au destinataire.
        return mailChiffre;
    }

    // ==========================================
    // 3. ÉTAPE GÉNÉRATION CLÉ (AUTORITÉ VERS DESTINATAIRE)
    // ==========================================
    public static KeyPair etape6_Autorite_GenererClePriveeClient(Pairing pairing, Element msk, String idClient) throws Exception {
        // Variables :
        // - msk : La Master Secret Key que l'autorité a générée à l'étape 1.
        // - idClient : L'identifiant du client qui veut lire ses mails.
        
        // Calcule la clé de déchiffrement sk = msk * H1(idClient)
        KeyPair clePriveeUtilisateur = IBEBasicIdent.keygen(pairing, msk, idClient);
        
        // DESTINATION : Cet objet doit être envoyé au Client via un canal sécurisé (HTTPS).
        return clePriveeUtilisateur;
    }

    // ==========================================
    // 4. ÉTAPE DÉCHIFFREMENT (CLIENT DESTINATAIRE)
    // ==========================================
    public static String etape9_Client_DechiffrerMail(Pairing pairing, SettingParameters pp, KeyPair maClePrivee, IBEcipher mailRecu) throws Exception {
        // Variables :
        // - pp : Paramètres Publics (P, P_pub)
        // - maClePrivee : L'objet KeyPair reçu de l'autorité à l'étape 8 (contient sk)
        // - mailRecu : L'objet IBEcipher (U, V, Aescipher) trouvé sur le serveur
        
        // On utilise votre fonction d'origine pour retrouver le message clair.
        byte[] resultatClair = IBEBasicIdent.IBEdecryption(
                pairing, 
                pp.getP(), 
                pp.getP_pub(), 
                maClePrivee.getSk(), // On extrait l'élément sk de la clé
                mailRecu
        );
        
        return new String(resultatClair, "UTF-8");
    }

// Dans FluxMessagerieIBE.java
public static RSATunnelKey generateRSAKeyPair() throws Exception {
    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    java.security.KeyPair jkp = generator.generateKeyPair();
    
    // On retourne ton nouvel objet personnalisé
    return new RSATunnelKey(jkp.getPrivate(), jkp.getPublic());
}

    // Chiffrement avec une clé publique RSA
    public static String encryptRSA(String data, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedBytes = cipher.doFinal(data.getBytes());
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    // Déchiffrement avec une clé privée RSA
    public static String decryptRSA(String encryptedData, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decodedBytes = Base64.getDecoder().decode(encryptedData);
        byte[] decryptedBytes = cipher.doFinal(decodedBytes);
        return new String(decryptedBytes);
    }
}