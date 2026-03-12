package td2cryptoavancee;

import java.security.PrivateKey;
import java.security.PublicKey;

public class RSATunnelKey {
    private PrivateKey privateKey;
    private PublicKey publicKey;

    public RSATunnelKey(PrivateKey privateKey, PublicKey publicKey) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }
}