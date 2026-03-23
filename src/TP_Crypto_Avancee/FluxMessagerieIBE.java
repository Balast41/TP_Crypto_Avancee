package TP_Crypto_Avancee;

import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;

/**
 * FluxMessagerieIBE : Point central de l'implémentation du protocole IBE.
 * Cette classe orchestre les primitives de chiffrement basées sur l'identité
 * et sécurise les échanges via RSA et AES.
 */
public class FluxMessagerieIBE {

    /**
     * Initialisation du PKG (Private Key Generator).
     * On génère les paramètres publics (P, P_pub) à diffuser et la clé maître (msk) 
     * que l'autorité doit impérativement garder secrète.
     */
    public static SettingParameters etape1_Autorite_Initialisation(Pairing pairing) {
        return IBEBasicIdent.setup(pairing);
    }

    /**
     * Côté expéditeur : On chiffre le message directement avec l'identifiant (email) du destinataire.
     * Le couplage permet de dériver une clé sans avoir besoin du certificat du correspondant.
     */
    public static IBEcipher etape4_Client_Chiffrer(Pairing pairing, SettingParameters pp, String message, String idDestinataire) throws Exception {
        byte[] messageBytes = message.getBytes("UTF-8");
        
        // IBEencryption encapsule la génération de la clé de session et le chiffrement symétrique.
        return IBEBasicIdent.IBEencryption(
                pairing, 
                pp.getP(), 
                pp.getP_pub(), 
                messageBytes, 
                idDestinataire
        );
    }

    /**
     * Côté Autorité : Génère la clé privée d'un utilisateur (d_ID) à partir de son email.
     * C'est l'étape d'extraction (Extract) du schéma de Boneh-Franklin.
     */
    public static KeyPair etape6_Autorite_GenererClePriveeClient(Pairing pairing, Element msk, String idClient) throws Exception {
        return IBEBasicIdent.keygen(pairing, msk, idClient);
    }

    /**
     * Côté destinataire : Utilise la clé secrète dID reçue de l'autorité pour
     * retrouver le message clair à partir du cipher IBE (U, V, C).
     */
    public static String etape9_Client_DechiffrerMail(Pairing pairing, SettingParameters pp, KeyPair maClePrivee, IBEcipher mailRecu) throws Exception {
        byte[] resultatClair = IBEBasicIdent.IBEdecryption(
                pairing, 
                pp.getP(), 
                pp.getP_pub(), 
                maClePrivee.getSk(), 
                mailRecu
        );
        
        return new String(resultatClair, "UTF-8");
    }

    /**
     * Prépare un tunnel RSA pour sécuriser la transmission initiale entre le client et l'autorité.
     * Indispensable avant d'avoir établi les clés IBE.
     */
    public static RSATunnelKey generateRSAKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        java.security.KeyPair jkp = generator.generateKeyPair();
        return new RSATunnelKey(jkp.getPrivate(), jkp.getPublic());
    }

    /**
     * Chiffre une chaîne de caractères en RSA (PKCS1) pour le transport de secrets vers l'autorité.
     */
    public static String encryptRSA(String data, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    /**
     * Déchiffre un message RSA reçu du client.
     */
    public static String decryptRSA(String encryptedData, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decodedBytes = Base64.getDecoder().decode(encryptedData);
        byte[] decryptedBytes = cipher.doFinal(decodedBytes);
        return new String(decryptedBytes);
    }

    /**
     * Mécanisme de sécurisation de la clé IBE :
     * Combine le code client et le code 2FA via un XOR, puis dérive une clé AES-128 via SHA-256.
     * Cela garantit que seule la personne ayant accès aux deux codes peut lire la réponse.
     */
    public static String encryptMessageWithCodes(String code1, String code2, String message) throws Exception {
        byte[] c1 = code1.getBytes(StandardCharsets.UTF_8);
        byte[] c2 = code2.getBytes(StandardCharsets.UTF_8);

        // Combinaison des secrets par XOR
        int max = Math.max(c1.length, c2.length);
        byte[] combined = new byte[max];
        for (int i = 0; i < max; i++) {
            byte b1 = c1[i % c1.length];
            byte b2 = c2[i % c2.length];
            combined[i] = (byte) (b1 ^ b2);
        }

        // Dérivation de la clé AES
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] hash = sha.digest(combined);
        byte[] keyBytes = Arrays.copyOf(hash, 16);
        SecretKey aesKey = new SecretKeySpec(keyBytes, "AES");

        // Chiffrement symétrique
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, aesKey);
        byte[] encrypted = cipher.doFinal(message.getBytes(StandardCharsets.UTF_8));

        return Base64.getEncoder().encodeToString(encrypted);
    }

    /**
     * Déchiffre la réponse de l'autorité en reconstituant la clé AES à partir des deux codes.
     */
    public static String decryptMessageWithCodes(String code1, String code2, String encryptedMessage) throws Exception {
        byte[] c1 = code1.getBytes(StandardCharsets.UTF_8);
        byte[] c2 = code2.getBytes(StandardCharsets.UTF_8);

        int max = Math.max(c1.length, c2.length);
        byte[] combined = new byte[max];
        for (int i = 0; i < max; i++) {
            byte b1 = c1[i % c1.length];
            byte b2 = c2[i % c2.length];
            combined[i] = (byte) (b1 ^ b2);
        }

        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] hash = sha.digest(combined);
        byte[] keyBytes = Arrays.copyOf(hash, 16);
        SecretKey aesKey = new SecretKeySpec(keyBytes, "AES");

        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, aesKey);

        byte[] decoded = Base64.getDecoder().decode(encryptedMessage);
        byte[] decrypted = cipher.doFinal(decoded);

        return new String(decrypted, StandardCharsets.UTF_8);
    }
}