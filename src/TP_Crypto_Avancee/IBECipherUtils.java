package TP_Crypto_Avancee;

import java.util.Base64;
import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;

public class IBECipherUtils {

    public static String serializeIBECipher(IBEcipher cipher){

        String U = Base64.getEncoder().encodeToString(cipher.getU().toBytes());
        String V = Base64.getEncoder().encodeToString(cipher.getV());
        String C = Base64.getEncoder().encodeToString(cipher.getAescipher());

        return "U:" + U +
               "::SPLIT::V:" + V +
               "::SPLIT::CIPHER:" + C;
    }

    public static IBEcipher deserializeIBECipher(String data, Pairing pairing){

        String[] parts = data.split("::SPLIT::");

        String U_str = parts[0].split("U:")[1];
        String V_str = parts[1].split("V:")[1];
        String C_str = parts[2].split("CIPHER:")[1];

        byte[] U_bytes = Base64.getDecoder().decode(U_str);
        byte[] V_bytes = Base64.getDecoder().decode(V_str);
        byte[] C_bytes = Base64.getDecoder().decode(C_str);

        Element U = pairing.getG1().newElementFromBytes(U_bytes);

        return new IBEcipher(U, V_bytes, C_bytes);
    }
}