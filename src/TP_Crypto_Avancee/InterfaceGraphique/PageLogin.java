package TP_Crypto_Avancee.InterfaceGraphique;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import TP_Crypto_Avancee.User;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.NoSuchProviderException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.search.FromStringTerm;
/**
 * Fenêtre de connexion demandant l'email et le mot de passe de l'utilisateur.
 */
public class PageLogin extends JFrame
{
    private JTextField emailField;
    private JPasswordField mdpField;
    private User user;

    public User getUser() {
        return user;
    }

    private boolean checkConnection(String username, String password){
        try {     
            String host = "imap.gmail.com";
            Properties properties = new Properties();
            properties.put("mail.store.protocol", "imaps");
            properties.put("mail.imap.host", host);
            properties.put("mail.imap.port", "993");
            properties.put("mail.imap.ssl.enable", "true");
            Session emailSession = Session.getDefaultInstance(properties);

            //create the IMAP store object and connect with the mail server
            Store store = emailSession.getStore("imaps");

            store.connect(host, username, password);
            boolean connected = store.isConnected();
            store.close();

            return connected;
            } catch (AuthenticationFailedException e) {
                System.out.println("Identifiants invalides");
            } catch (MessagingException e) {
                System.out.println("Erreur de connexion : " + e.getMessage());
            }

            return false;
        }
    
    public PageLogin(){
        super("Connexion");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        // Panneau pour les champs de connexion
        Panel loginPanel = new Panel(new GridLayout(2, 2, 5, 5));

        // Champ pour l'email
        loginPanel.add(new Label("Email :"));
        emailField = new JTextField(20);
        loginPanel.add(emailField);

        // Champ pour le mot de passe
        loginPanel.add(new Label("Mot de passe :"));
        mdpField = new JPasswordField(20);
        loginPanel.add(mdpField);

           add(loginPanel, BorderLayout.CENTER);

        // Bouton "Envoyer"
        JButton envoyerButton = new JButton("Envoyer");
        envoyerButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String email = emailField.getText();
                String mdp = new String(mdpField.getPassword()); // Récupère le mot de passe
                System.out.println("=== Informations de connexion ===");
                System.out.println("Email : " + email);
                System.out.println("Mot de passe : " + mdp);

                // Ici, tu peux ajouter la logique de vérification des identifiants
                // Par exemple, ouvrir la fenêtre de rédaction de mail si la connexion réussit
                if (checkConnection(email, mdp)) {
                    System.out.println("Connexion réussie !");
                    user= new User(email,mdp);
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(null, "Email ou mot de passe incorrect !", "Erreur", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        add(envoyerButton, BorderLayout.SOUTH);
    }
    };

