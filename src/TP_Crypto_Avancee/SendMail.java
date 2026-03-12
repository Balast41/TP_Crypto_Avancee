package TP_Crypto_Avancee;
import java.util.Properties;

import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.activation.FileDataSource;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;


public class SendMail {

   public static String sendMail(Mail mail) {
   // Assuming you are sending email through Gmail SMTP.
      String host = "smtp.gmail.com";
      //Old parameters with new mail
      //String to, String from, String username, String password, String subject, String content, String[] attachmentPaths,String U, String V
      String to= mail.getDestinataire();
      String from= mail.getFrom();
      String username=mail.getUsername();
      String password=mail.getPassword();
      String subject=mail.getObjet();
      String content=mail.getMessage();
      String[] attachmentPaths=mail.getPath();

      Properties props = new Properties();
      props.put("mail.smtp.auth", "true");
      props.put("mail.smtp.host", host);
      props.put("mail.smtp.port", "465");
      props.put("mail.smtp.ssl.enable", "true");
      props.put("mail.smtp.connectiontimeout", "15000");
      props.put("mail.smtp.timeout", "15000");
      props.put("mail.smtp.writetimeout", "15000");

      // Get the Session object.
      Session session = Session.getInstance(props,
      new jakarta.mail.Authenticator() {
         protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(username, password);
         }
      });

      try {
         // Create a default MimeMessage object.
         Message message = new MimeMessage(session);

         // Set From: header field of the header.
         message.setFrom(new InternetAddress(from));

         // Set To: header field of the header.
         message.setRecipients(Message.RecipientType.TO,
         InternetAddress.parse(to));

         // Set Subject: header field
         message.setSubject(subject);

      // Create the message part
         BodyPart messageBodyPart = new MimeBodyPart();

         // Now set the actual message
         messageBodyPart.setText(content);
         // Create a multipar message
         Multipart multipart = new MimeMultipart();

         // Set text message part
         multipart.addBodyPart(messageBodyPart);

         // Add each attachment as its own body part.
         if (attachmentPaths != null){
            for (int i = 0; i < attachmentPaths.length; i++) {
               String[] parts = attachmentPaths[i].split("\\\\");
               String filen = parts[parts.length - 1];
               String filename = attachmentPaths[i];
               BodyPart attachmentPart = new MimeBodyPart();
               DataSource source = new FileDataSource(filename);
               attachmentPart.setDataHandler(new DataHandler(source));
               attachmentPart.setFileName(filen);
               multipart.addBodyPart(attachmentPart);
            }
         }

         // Send the complete message parts
         message.setContent(multipart);

         
         // Send message
         Transport.send(message);

         System.out.println("Sent message successfully....");
         return "Mail sent successfully";

      } catch (MessagingException e) {
            throw new RuntimeException(e);
      }
      
   }
public static void main(String[] args) {


   }
    
}
