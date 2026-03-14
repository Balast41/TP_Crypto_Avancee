package TP_Crypto_Avancee;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import jakarta.mail.*;
import com.sun.mail.imap.IMAPFolder;
import jakarta.mail.search.FromStringTerm;

public class FetchingEmail {

    // On récupère les propriétés IMAP standard
    private static Properties getImapProperties(String host) {
        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imap.host", host);
        props.put("mail.imap.port", "993");
        props.put("mail.imap.ssl.enable", "true");
        return props;
    }

    /**
     * RÉCUPÉRATION LÉGÈRE : Objet, Message, Expéditeur, Destinataire + ID
     */
public static List<Mail> fetchLight(String host, String user, String password, String senderFilter, int numberOfMails) {
    List<Mail> mailList = new ArrayList<>();
    try {
        Session session = Session.getInstance(getImapProperties(host));
        Store store = session.getStore("imaps");
        store.connect(host, user, password);

        IMAPFolder inbox = (IMAPFolder) store.getFolder("INBOX");
        inbox.open(Folder.READ_ONLY);

        Message[] messages = (senderFilter != null && !senderFilter.isEmpty()) 
                             ? inbox.search(new FromStringTerm(senderFilter)) 
                             : inbox.getMessages();

        int count = 0;
        for (int i = messages.length - 1; i >= 0; i--) {
            if (count >= numberOfMails) break;

            Message msg = messages[i];
            
            // Extraction des noms de fichiers (sans télécharger le contenu)
            List<String> attachments = new ArrayList<>();
            if (msg.getContent() instanceof Multipart) {
                Multipart mp = (Multipart) msg.getContent();
                for (int j = 0; j < mp.getCount(); j++) {
                    BodyPart bp = mp.getBodyPart(j);
                    if (bp.getFileName() != null) {
                        attachments.add(bp.getFileName());
                    }
                }
            }

            String from = (msg.getFrom() != null) ? msg.getFrom()[0].toString() : "Inconnu";
            String to = (msg.getRecipients(Message.RecipientType.TO) != null) 
                        ? msg.getRecipients(Message.RecipientType.TO)[0].toString() : "Inconnu";
            
            Mail mail = new Mail(to, from, msg.getSubject(), getTextFromPart(msg));
            mail.setId(String.valueOf(inbox.getUID(msg)));
            
            // On injecte les noms chiffrés ici !
            mail.setPath(attachments.toArray(new String[0]));
            
            mailList.add(mail);
            count++;
        }
        inbox.close(false);
        store.close();
    } catch (Exception e) { e.printStackTrace(); }
    return mailList;
}

    /**
     * Récupère un objet Mail complet (Métadonnées + Liste des noms de PJ) sans télécharger le contenu des PJ.
     */
    public static Mail getMail(String host, String user, String password, String mailId) {
        try {
            Session session = Session.getInstance(getImapProperties(host));
            Store store = session.getStore("imaps");
            store.connect(host, user, password);
            IMAPFolder inbox = (IMAPFolder) store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            Message msg = inbox.getMessageByUID(Long.parseLong(mailId));
            String from = (msg.getFrom() != null) ? msg.getFrom()[0].toString() : "Inconnu";
            String to = (msg.getRecipients(Message.RecipientType.TO) != null) ? msg.getRecipients(Message.RecipientType.TO)[0].toString() : "Inconnu";
            
            // On récupère le corps du texte (méthode utilitaire getTextFromPart déjà définie)
            Mail mail = new Mail(to, from, msg.getSubject(), getTextFromPart(msg));
            mail.setId(mailId);

            // Récupération des noms de fichiers (chiffrés ou non)
            List<String> fileNames = new ArrayList<>();
            if (msg.getContent() instanceof Multipart) {
                Multipart mp = (Multipart) msg.getContent();
                for (int i = 0; i < mp.getCount(); i++) {
                    BodyPart bp = mp.getBodyPart(i);
                    if (bp.getFileName() != null) fileNames.add(bp.getFileName());
                }
            }
            mail.setPath(fileNames.toArray(new String[0]));

            inbox.close(false);
            store.close();
            return mail;
        } catch (Exception e) { e.printStackTrace(); return null; }
    }

    /**
     * Récupère les octets bruts d'une pièce jointe en mémoire.
     */
    public static byte[] downloadSpecificFile(String host, String user, String password, String mailId, String fileName) {
        try {
            Session session = Session.getInstance(getImapProperties(host));
            Store store = session.getStore("imaps");
            store.connect(host, user, password);
            IMAPFolder inbox = (IMAPFolder) store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            Message msg = inbox.getMessageByUID(Long.parseLong(mailId));
            if (msg.getContent() instanceof Multipart) {
                Multipart mp = (Multipart) msg.getContent();
                for (int i = 0; i < mp.getCount(); i++) {
                    BodyPart bp = mp.getBodyPart(i);
                    if (fileName.equals(bp.getFileName())) {
                        try (InputStream is = bp.getInputStream();
                            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream()) {
                            byte[] buf = new byte[8192];
                            int len;
                            while ((len = is.read(buf)) != -1) bos.write(buf, 0, len);
                            return bos.toByteArray(); 
                        }
                    }
                }
            }
            inbox.close(false);
            store.close();
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }


    private static String getTextFromPart(Part p) throws MessagingException, IOException {
        if (p.isMimeType("text/plain")) return (String) p.getContent();
        if (p.isMimeType("multipart/*")) {
            Multipart mp = (Multipart) p.getContent();
            for (int i = 0; i < mp.getCount(); i++) {
                String s = getTextFromPart(mp.getBodyPart(i));
                if (s != null && !s.isEmpty()) return s;
            }
        }
        return "";
    }
}