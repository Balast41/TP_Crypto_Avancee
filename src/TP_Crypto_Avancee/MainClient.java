package TP_Crypto_Avancee;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//javac -cp "lib/jpbc-2.0.0/jars/*:lib/JakartaMail/*:." -d . TPJavaMail/TP_Crypto_Avancee/src/TP_Crypto_Avancee/*.java

// java -cp ".:lib/jpbc-2.0.0/jars/*:lib/JakartaMail/*" TP_Crypto_Avancee.MainClient

    public class MainClient {
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
            PageLogin loginPage=new PageLogin();
            loginPage.setVisible(true);
            User user=waitForLogin(loginPage);
            if (user==null){
                System.out.println("Login failed");
                return;
            }
            HttpClient client= new HttpClient(user.getUsername(), user.getPassword());
            CodeLogin codePage=new CodeLogin(client);
            codePage.setVisible(true);
            boolean isCodeValid=waitForCode(codePage);
            if (!isCodeValid){
                System.out.println("Code validation failed");
                return;
            }

                String host = "imap.gmail.com";// change 
                String mailStoreType = "imaps";
                String senderFilter = "qbalazot@gmail.com";
                // yqvi txzx srtu csye
                Mail[] mails = client.getAllMails(host,mailStoreType,client.getEmail(),client.getPassword(),senderFilter,10);
                // Convertir le tableau en List<Mail>
                List<Mail> listeMails = new ArrayList<>(Arrays.asList(mails));
                // Création de l'application et affichage immédiat
                new AppliMail(listeMails, client);
    
    }
}
