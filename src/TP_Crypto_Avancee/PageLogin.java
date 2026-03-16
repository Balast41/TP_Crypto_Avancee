
package TP_Crypto_Avancee;

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
import java.util.Properties;
import jakarta.mail.AuthenticationFailedException;
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
    
    public PageLogin() {
        super("Connexion");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(500,250));

        // Charger l'icône de la fenêtre
        ImageIcon logoIcon = new ImageIcon("/home/shila/Documents/CryptoAvancée/TPJavaMail/SMails_logo.png");
        setIconImage(logoIcon.getImage());

        // Créer un panneau principal avec deux colonnes
        JPanel mainPanel = new JPanel(new GridLayout(1, 2));
        
        
        // Colonne de gauche : image centrée et redimensionnée
        JPanel leftPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // Redessiner l'image pour qu'elle s'adapte à la taille du panneau
                Image image =  new ImageIcon("/home/shila/Documents/CryptoAvancée/TPJavaMail/SMails_logo.png").getImage();
                int width = getWidth();
                int height = getHeight();
                g.drawImage(image, 0, 0, width, height, this);
            }
        };
        leftPanel.setBackground(new Color(250, 250, 250)); // Couleur gris clair (RGB)

        // Colonne de droite : formulaire de connexion
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(new Color(200, 250, 250)); // Couleur gris clair (RGB)

        Panel loginPanel = new Panel(new GridLayout(5, 5, 5, 5));

        // Champ pour l'email
        loginPanel.add(new Label("Email :"));
        emailField = new JTextField(30);
        emailField.setPreferredSize(new Dimension(5, 5));
        loginPanel.add(emailField);

        // Champ pour le mot de passe
        loginPanel.add(new Label("Mot de passe :"));
        mdpField = new JPasswordField(30);
        loginPanel.add(mdpField);

        rightPanel.add(loginPanel, BorderLayout.CENTER);

        // Bouton "Envoyer"
        JButton envoyerButton = new JButton("Envoyer");
        envoyerButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String email = emailField.getText();
                String mdp = new String(mdpField.getPassword());

                if (checkConnection(email, mdp)) {
                    user = new User(email, mdp);
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(null, "Email ou mot de passe incorrect !", "Erreur", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        rightPanel.add(envoyerButton, BorderLayout.SOUTH);

        // Ajouter les deux colonnes au panneau principal
        mainPanel.add(leftPanel);
        mainPanel.add(rightPanel);

        // Ajouter le panneau principal à la fenêtre
        add(mainPanel, BorderLayout.CENTER);

        setSize(600, 200); // Augmenter la largeur pour les deux colonnes
        setLocationRelativeTo(null); // Centrer la fenêtre
    }


    }



