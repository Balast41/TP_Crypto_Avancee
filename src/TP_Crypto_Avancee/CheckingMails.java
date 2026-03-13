package TP_Crypto_Avancee;

import java.util.Properties;

import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.NoSuchProviderException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.search.FromStringTerm;



public class CheckingMails {

   private static final int MAX_RECENT_TO_DISPLAY = 10;

   public static void check(String host, String storeType, String user,
      String password, String senderFilter) 
   {
      try {

      //create properties field
      Properties properties = new Properties();

      properties.put("mail.store.protocol", "imaps");
      properties.put("mail.imap.host", host);
      properties.put("mail.imap.port", "993");
      properties.put("mail.imap.ssl.enable", "true");
      Session emailSession = Session.getDefaultInstance(properties);
  
      //create the IMAP store object and connect with the mail server
      Store store = emailSession.getStore("imaps");

      store.connect(host, user, password);

      //create the folder object and open it
      Folder emailFolder = store.getFolder("INBOX");
      emailFolder.open(Folder.READ_ONLY);

      // Count all messages in inbox
      int totalMessages = emailFolder.getMessageCount();

      // Filter only messages from a specific sender (or part of sender address)
      FromStringTerm fromTerm = new FromStringTerm(senderFilter);
      Message[] filteredMessages = emailFolder.search(fromTerm);

      int toDisplay = Math.min(MAX_RECENT_TO_DISPLAY, filteredMessages.length);
      for (int i = filteredMessages.length - 1, shown = 0; i >= 0 && shown < toDisplay; i--, shown++) {
         Message message = filteredMessages[i];
      }

      //close the store and folder objects
      emailFolder.close(false);
      store.close();

      } catch (NoSuchProviderException e) {
         e.printStackTrace();
      } catch (MessagingException e) {
         e.printStackTrace();
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
}