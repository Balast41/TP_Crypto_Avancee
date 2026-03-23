package TP_Crypto_Avancee;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//javac -cp "lib/jpbc-2.0.0/jars/*:lib/JakartaMail/*:." -d . TPJavaMail/TP_Crypto_Avancee/src/TP_Crypto_Avancee/*.java

// java -cp ".:lib/jpbc-2.0.0/jars/*:lib/JakartaMail/*" TP_Crypto_Avancee.MainClient

public class MainClient {

    /**
     * Méthode de synchronisation : bloque le thread principal tant que l'utilisateur 
     * n'a pas validé ou fermé la fenêtre de login.
     */
    private static User waitForLogin(PageLogin login) {
        while (login.isDisplayable()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return login.getUser();
    }

    /**
     * Attend la validation du code 2FA saisi par l'utilisateur dans l'interface.
     */
    private static boolean waitForCode(CodeLogin code) {
        while (code.isDisplayable()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return code.isConnected();
    }

    public static void main(String[] args) {
            // Étape 1 : Affichage de la mire de connexion (Email/Password)
            PageLogin loginPage=new PageLogin();
            loginPage.setVisible(true);
            
            // On attend que l'utilisateur saisisse ses identifiants
            User user=waitForLogin(loginPage);
            if (user==null){
                System.out.println("Login failed");
                return;
            }

            // Étape 2 : Initialisation du client HTTP et demande de clé IBE initiale
            HttpClient client= new HttpClient(user.getUsername(), user.getPassword());
            
            // Étape 3 : Affichage de la fenêtre de saisie du code 2FA (reçu par mail)
            CodeLogin codePage=new CodeLogin(client);
            codePage.setVisible(true);
            
            // On attend la vérification du code auprès de l'autorité (PKG)
            boolean isCodeValid=waitForCode(codePage);
            if (!isCodeValid){
                System.out.println("Code validation failed");
                return;
            }
            // Convertir le tableau en List<Mail>
            
            /**
             * Étape finale : Lancement de l'interface principale de messagerie.
             * Le client possède désormais sa clé privée dID déchiffrée et prête à l'emploi.
             */
            System.out.println("Lancement de l'application...");
            new AppliMail(client);
    
    }
}