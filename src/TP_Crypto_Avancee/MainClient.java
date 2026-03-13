package TP_Crypto_Avancee;
import TP_Crypto_Avancee.InterfaceGraphique.AppliMail;

public class MainClient {
    public static void main(String[] args) {
            HttpClient client= new HttpClient("qbalazot@gmail.com");
            client.VerifyingTheCode("internCodeIBE");
        // Création de l'application et affichage immédiat
        new AppliMail(listeMails);
    
    }
}
