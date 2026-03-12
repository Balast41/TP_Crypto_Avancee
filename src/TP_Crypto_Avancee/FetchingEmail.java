package TP_Crypto_Avancee;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

import jakarta.mail.Address;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.NoSuchProviderException;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.search.FromStringTerm;

public class FetchingEmail {

   private static final int MAX_RECENT_TO_DISPLAY = 10;
   private static final String ATTACHMENTS_DIR = "attachments";

   public static void fetch(String imapHost, String storeType, String user,
      String password, String senderFilter) {
      try {
         // create properties field
         Properties properties = new Properties();
         properties.put("mail.store.protocol", "imaps");
         properties.put("mail.imap.host", imapHost);
         properties.put("mail.imap.port", "993");
         properties.put("mail.imap.ssl.enable", "true");
         Session emailSession = Session.getDefaultInstance(properties);
         // emailSession.setDebug(true);

         // create the IMAP store object and connect with the mail server
         Store store = emailSession.getStore("imaps");

         store.connect(imapHost, user, password);

         // create the folder object and open it
         Folder emailFolder = store.getFolder("INBOX");
         emailFolder.open(Folder.READ_ONLY);

         BufferedReader reader = new BufferedReader(new InputStreamReader(
	      System.in));

         // Count all messages in inbox
         int totalMessages = emailFolder.getMessageCount();
         System.out.println("messages.length---" + totalMessages);

         // Server-side filter: only messages from a specific sender
         FromStringTerm fromTerm = new FromStringTerm(senderFilter);
         Message[] filteredMessages = emailFolder.search(fromTerm);
         System.out.println("Mails matching sender filter '" + senderFilter + "': " + filteredMessages.length);

         // Display only a limited number of recent messages
         int toDisplay = Math.min(MAX_RECENT_TO_DISPLAY, filteredMessages.length);
         int displayedCount = 0;
         for (int i = filteredMessages.length - 1, shown = 0; i >= 0 && shown < toDisplay; i--, shown++) {
            Message message = filteredMessages[i];
            System.out.println("---------------------------------");
            writePart(message);
            displayedCount++;

            String line = reader.readLine();
            if ("YES".equals(line)) {
               message.writeTo(System.out);
            } else if ("QUIT".equals(line)) {
               break;
            }
         }
         System.out.println("\nDisplayed mails: " + displayedCount + " / " + filteredMessages.length);

         // close the store and folder objects
         emailFolder.close(false);
         store.close();

      } catch (NoSuchProviderException e) {
         e.printStackTrace();
      } catch (MessagingException e) {
         e.printStackTrace();
      } catch (IOException e) {
         e.printStackTrace();
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
   public static void main(String[] args) {

      String host = "imap.gmail.com";// change accordingly
      String mailStoreType = "imaps";
      String username = 
         "qbalazot@gmail.com";// change accordingly
      String password = "yqvi txzx srtu csye";// change accordingly
      String senderFilter = "qbalazot@gmail.com";// change accordingly

      //Call method fetch
      fetch(host, mailStoreType, username, password, senderFilter);

   }

   /*
   * This method checks for content-type 
   * based on which, it processes and
   * fetches the content of the message
   */
   public static void writePart(Part p) throws Exception {
      if (p instanceof Message)
         //Call methos writeEnvelope
         writeEnvelope((Message) p);

      System.out.println("----------------------------");
      System.out.println("CONTENT-TYPE: " + p.getContentType());

      // Check if the content has attachment or multiple body parts.
      if (p.isMimeType("multipart/*")) {
         System.out.println("This is a Multipart");
         System.out.println("---------------------------");
         Multipart mp = (Multipart) p.getContent();
         int count = mp.getCount();
         for (int i = 0; i < count; i++)
            writePart(mp.getBodyPart(i));
      }
      // Check if the content is a nested message.
      else if (p.isMimeType("message/rfc822")) {
         System.out.println("This is a Nested Message");
         System.out.println("---------------------------");
         writePart((Part) p.getContent());
      }
      // Save explicit attachments to disk instead of printing binary data.
      else if (Part.ATTACHMENT.equalsIgnoreCase(p.getDisposition()) || p.getFileName() != null) {
         String fileName = p.getFileName() != null ? p.getFileName() : "attachment.bin";
         File dir = new File(ATTACHMENTS_DIR);
         if (!dir.exists()) {
            dir.mkdirs();
         }
         File out = new File(dir, fileName.replaceAll("[\\\\/:*?\"<>|]", "_"));
         try (InputStream in = p.getInputStream(); FileOutputStream fos = new FileOutputStream(out)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
               fos.write(buffer, 0, bytesRead);
            }
         }
         System.out.println("Attachment saved: " + out.getAbsolutePath());
      }
      // Check if the content is plain text.
      else if (p.isMimeType("text/plain") || p.isMimeType("text/html")) {
         System.out.println("This is text content");
         System.out.println("---------------------------");
         System.out.println((String) p.getContent());
      }
      else {
         Object o = p.getContent();
         if (o instanceof String) {
            System.out.println("This is a string");
            System.out.println("---------------------------");
            System.out.println((String) o);
         }
         else if (o instanceof InputStream) {
            System.out.println("Binary stream detected (not printed to console).");
            System.out.println("---------------------------");
         }
         else {
            System.out.println("This is an unknown type");
            System.out.println("---------------------------");
            System.out.println(o.toString());
         }
      }

   }
   /*
   * This method would print FROM,TO and SUBJECT of the message
   */
   public static void writeEnvelope(Message m) throws Exception {
      System.out.println("This is the message envelope");
      System.out.println("---------------------------");
      Address[] a;

      // FROM
      if ((a = m.getFrom()) != null) {
         for (int j = 0; j < a.length; j++)
         System.out.println("FROM: " + a[j].toString());
      }

      // TO
      if ((a = m.getRecipients(Message.RecipientType.TO)) != null) {
         for (int j = 0; j < a.length; j++)
         System.out.println("TO: " + a[j].toString());
      }

      // SUBJECT
      if (m.getSubject() != null)
         System.out.println("SUBJECT: " + m.getSubject());

   }

}
