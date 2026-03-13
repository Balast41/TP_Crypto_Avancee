package TP_Crypto_Avancee;

import java.lang.*;
import java.security.SecureRandom;
import java.time.*;

public class Code2FA {
    private String code;
    private OffsetTime expirationTime;

    public Code2FA(String code, OffsetTime expirationTime) {
        this.code = code;
        this.expirationTime = expirationTime;
    }

    public String getCode() {
        return code;
    }

    public void setCode() {
        SecureRandom random = new SecureRandom();
        this.code = Integer.toString(random.nextInt(100000, 1000000)); // Génère un code à 6 chiffres
    }

    public OffsetTime getExpirationTime() {
        return expirationTime;
    }

    public void setExpirationTime() {
        this.expirationTime = java.time.OffsetTime.now().plusMinutes(10);
    }
    
}
