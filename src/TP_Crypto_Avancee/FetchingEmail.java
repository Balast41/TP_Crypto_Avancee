package TP_Crypto_Avancee;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.NoSuchProviderException;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.search.FromStringTerm;
import java.util.ArrayList;

public class FetchingEmail {

   private static final int MAX_RECENT_TO_DISPLAY = 10;
   private static final String ATTACHMENTS_DIR = "attachments";

   public static Mail[] fetch(String imapHost, String storeType, String user,
      String password, String senderFilter, int nb) {
      try {
         List<String> attachmentList = new ArrayList<>();
         Mail[] mails = new Mail[0];
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

         // Count all messages in inbox
         int totalMessages = emailFolder.getMessageCount();
         System.out.println("messages.length---" + totalMessages);

         // Server-side filter: only messages from a specific sender
         FromStringTerm fromTerm = new FromStringTerm(senderFilter);
         Message[] filteredMessages = emailFolder.search(fromTerm);
         System.out.println("Mails matching sender filter '" + senderFilter + "': " + filteredMessages.length);

         // Display only a limited number of recent messages.
         int maxByNb = nb > 0 ? Math.min(nb, filteredMessages.length) : filteredMessages.length;
         int toDisplay = Math.min(MAX_RECENT_TO_DISPLAY, maxByNb);
         int displayedCount = 0;
         for (int i = filteredMessages.length - 1, shown = 0; i >= 0 && shown < toDisplay; i--, shown++) {
            Message message = filteredMessages[i];
            System.out.println("---------------------------------");
            Mail mail = new Mail();
            mail.setDate(message.getSentDate());
            writePart(message, attachmentList, mail);
            mails = Arrays.copyOf(mails, mails.length + 1);
            mails[mails.length - 1] = mail;
            displayedCount++;
         }
         System.out.println("\nDisplayed mails: " + displayedCount + " / " + filteredMessages.length);

         // close the store and folder objects
         emailFolder.close(false);
         store.close();

         return mails;

      } catch (NoSuchProviderException e) {
         e.printStackTrace();
      } catch (MessagingException e) {
         e.printStackTrace();
      } catch (IOException e) {
         e.printStackTrace();
      } catch (Exception e) {
         e.printStackTrace();
      }

      return null;
   }
   public static void main(String[] args) {
      Mail mail= new Mail();
      String host = "imap.gmail.com";// change accordingly
      String mailStoreType = "imaps";
      String username = 
         "qbalazot@gmail.com";// change accordingly
      String password = "yqvi txzx srtu csye";// change accordingly
      String senderFilter = "qbalazot@gmail.com";// change accordingly

      // Call method fetch and keep attachment paths in an array.
      Mail[] mails = fetch(host, mailStoreType, username, password, senderFilter, 3);
      for (int i=0; mails.length > 0 && i < mails.length; i++){
         System.out.println(mails[i].toString());
      }
   }

   /*
   * This method checks for content-type 
   * based on which, it processes and
   * fetches the content of the message
   */

public static Mail writePart(Part p, List<String> attachmentList, Mail mail) throws Exception {
    if (p instanceof Message)
        writeEnvelope((Message) p, mail);

    System.out.println("----------------------------");
    System.out.println("CONTENT-TYPE: " + p.getContentType());

    // Multipart
    if (p.isMimeType("multipart/*")) {
        System.out.println("This is a Multipart");
        System.out.println("---------------------------");

        Multipart mp = (Multipart) p.getContent();
        int count = mp.getCount();

        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = mp.getBodyPart(i);
            writePart(bodyPart, attachmentList, mail);
        }
    }
    // Nested message
    else if (p.isMimeType("message/rfc822")) {
        System.out.println("This is a Nested Message");
        System.out.println("---------------------------");
        writePart((Part) p.getContent(), attachmentList, mail);
    }
    // Attachments
    else if (Part.ATTACHMENT.equalsIgnoreCase(p.getDisposition()) || p.getFileName() != null) {

        String fileName = p.getFileName() != null ? p.getFileName() : "attachment.bin";

        File dir = new File(ATTACHMENTS_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File out = new File(dir, fileName.replaceAll("[\\\\/:*?\"<>|]", "_"));

        try (InputStream in = p.getInputStream();
             FileOutputStream fos = new FileOutputStream(out)) {

            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = in.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }

        System.out.println("On ajoute un fichier : " + out.getAbsolutePath());
        attachmentList.add(out.getAbsolutePath());

        // Conversion en tableau pour le Mail
        mail.setPath(attachmentList.toArray(new String[0]));

        System.out.println("Attachment saved: " + out.getAbsolutePath());
    }
    // Text content
    else if (p.isMimeType("text/plain") || p.isMimeType("text/html")) {
        System.out.println("This is text content");
        System.out.println("---------------------------");

        String content = (String) p.getContent();
        System.out.println(content);

        mail.setMessage(content);
    }
    // Other types
    else {
        Object o = p.getContent();

        if (o instanceof String) {
            System.out.println("This is a string");
            System.out.println("---------------------------");
            System.out.println((String) o);
        } else if (o instanceof InputStream) {
            System.out.println("Binary stream detected (not printed to console).");
            System.out.println("---------------------------");
        } else {
            System.out.println("This is an unknown type");
            System.out.println("---------------------------");
            System.out.println(o.toString());
        }
    }

    System.out.println(mail.toString());
    return mail;
}
   /*
   * This method would print FROM,TO and SUBJECT of the message
   */
   public static void writeEnvelope(Message m, Mail mail) throws Exception {
      System.out.println("This is the message envelope");
      System.out.println("---------------------------");
      Address[] a;

      // FROM
      if ((a = m.getFrom()) != null) {
         for (int j = 0; j < a.length; j++)
            System.out.println("FROM: " + a[j].toString());
         mail.setFrom(a[0].toString());
      }

      // TO
      if ((a = m.getRecipients(Message.RecipientType.TO)) != null) {
         for (int j = 0; j < a.length; j++)
            System.out.println("TO: " + a[j].toString());
         mail.setDestinataire(a[0].toString());
      }

      // SUBJECT
      if (m.getSubject() != null)
            System.out.println("SUBJECT: " + m.getSubject());
         mail.setObjet(m.getSubject());

   }

   /*
public static void writePart3(Part p) throws Exception {
      if (p instanceof Message)
         //Call methos writeEnvelope
         writeEnvelope((Message) p);

      System.out.println("----------------------------");
      System.out.println("CONTENT-TYPE: " + p.getContentType());

      //check if the content is plain text
      if (p.isMimeType("text/plain")) {
         System.out.println("This is plain text");
         System.out.println("---------------------------");
         System.out.println((String) p.getContent());
      } 
      //check if the content has attachment
      else if (p.isMimeType("multipart/*")) {
         System.out.println("This is a Multipart");
         System.out.println("---------------------------");
         Multipart mp = (Multipart) p.getContent();
         int count = mp.getCount();
         for (int i = 0; i < count; i++)
            writePart3(mp.getBodyPart(i));
      } 
      //check if the content is a nested message
      else if (p.isMimeType("message/rfc822")) {
         System.out.println("This is a Nested Message");
         System.out.println("---------------------------");
         writePart3((Part) p.getContent());
      } 
      //check if the content is an inline image
      else {
         Object o = p.getContent();
         if (o instanceof String) {
            System.out.println("This is a string");
            System.out.println("---------------------------");
            System.out.println((String) o);
         } 
         else if (o instanceof InputStream) {
            System.out.println("This is just an input stream");
            System.out.println("---------------------------");
            InputStream is = (InputStream) o;
            is = (InputStream) o;
            int c;
            while ((c = is.read()) != -1)
               System.out.write(c);
         } 
         else {
            System.out.println("This is an unknown type");
            System.out.println("---------------------------");
            System.out.println(o.toString());
         }
      }

   }
   */


   
}
