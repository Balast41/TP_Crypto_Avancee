package TP_Crypto_Avancee;
import TP_Crypto_Avancee.InterfaceGraphique.AppliMail;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    private static boolean waitForCode(PageCode code) {
        while (code.isDisplayable()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return code.isValidated();
    }
    public static void main(String[] args) {
            PageLogin loginPage=new PageLogin();
            loginPage.setVisible(true);
            User user=waitForLogin(loginPage);
            if (user==null){
                System.out.println("Login failed");
                return;
            }
            HttpClient client= new HttpClient("qbalazot@gmail.com");
            CodeLogin codePage=new CodeLogin(client);
            codePage.setVisible(true);
            boolean isCodeValid=waitForCode(codePage);
            if (!isCodeValid){
                System.out.println("Code validation failed");
                return;
            }

                String host = "imap.gmail.com";// change 
                String mailStoreType = "imaps";
                String username = 
                "qbalazot@gmail.com";// change accordingly
                String password = "yqvi txzx srtu csye";// change accordingly
                String senderFilter = "qbalazot@gmail.com";
                Mail[] mails = client.getAllMails(host,mailStoreType,username,password,senderFilter,10);
                for (Mail mail : mails) {
                    System.out.println(mail.toString());
                }
                // Convertir le tableau en List<Mail>
                List<Mail> listeMails = new ArrayList<>(Arrays.asList(mails));
                // Création de l'application et affichage immédiat
                new AppliMail(listeMails);
    
    }
}
