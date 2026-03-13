package TP_Crypto_Avancee;

import java.util.Date;
import java.util.Arrays;

public class Mail {

    private String destinataire;
    private String from;
    private String username;
    private String password;
    private String objet;
    private String message;
    private Date date;
    private String[] path;

    public Mail() {}

    public Mail(String destinataire, String from, String username, String password,
                String objet, String message, String[] path) {
        this.destinataire = destinataire;
        this.from = from;
        this.username = username;
        this.password = password;
        this.objet = objet;
        this.message = message;
        this.date = new Date();
        this.path = path;
    }

        public Mail(String destinataire, String from,
                String objet, String message, String[] path) {
        this.destinataire = destinataire;
        this.from = from;
        this.objet = objet;
        this.message = message;
        this.path = path;
        this.username="";
        this.password="";
    }

    public String getDestinataire() {
        return destinataire;
    }

    public void setDestinataire(String destinataire) {
        this.destinataire = destinataire;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getObjet() {
        return objet;
    }

    public void setObjet(String objet) {
        this.objet = objet;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String[] getPath() {
        return path;
    }

    public void setPath(String[] path) {
        this.path = path;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Date getDate() {
        return date;
    }

    @Override
    public String toString() {
        return "Mail {\n" +
                "  destinataire='" + destinataire + "',\n" +
                "  from='" + from + "',\n" +
                "  username='" + username + "',\n" +
                "  password='" + password + "',\n" +
                "  objet='" + objet + "',\n" +
                "  message='" + message + "',\n" +
                "  date=" + date + ",\n" +
                "  path=" + Arrays.toString(path) + "\n" +
                "}";
    }
}