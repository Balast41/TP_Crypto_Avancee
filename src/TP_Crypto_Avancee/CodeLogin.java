package TP_Crypto_Avancee;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


/**
 * Fenêtre de connexion demandant le code de l'utilisateur.
 */
public class CodeLogin extends JFrame
{
    private JTextField codeField;
    private HttpClient client;
    private boolean isConnected;

    public boolean isConnected() {
        return isConnected;
    }

    public HttpClient getClient() {
        return client;
    }

    public CodeLogin(HttpClient client) {

        super("Connexion");
        this.client = client;
        setLayout(new BorderLayout());

        // Panneau pour les champs de connexion
        Panel loginPanel = new Panel(new GridLayout(2, 2, 5, 5));

        // Champ pour l'email
        loginPanel.add(new Label("Veuillez entrer le code de vérification :"));
        codeField = new JTextField(20);
        loginPanel.add(codeField);


           add(loginPanel, BorderLayout.CENTER);

        // Bouton "Envoyer"
        JButton envoyerButton = new JButton("Envoyer");
        envoyerButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String code = codeField.getText();

                // Ici, tu peux ajouter la logique de vérification des identifiants
                // Par exemple, ouvrir la fenêtre de rédaction de mail si la connexion réussit
                String resultVerifyingTheCode = client.VerifyingTheCode(code);
                int i=0;
                switch (resultVerifyingTheCode){
                    case "VRAI":
                        isConnected = true;
                        // fermer la fenêtre login
                        dispose();
                        break;

                    case "FAUX":
                        i+=1;
                        JOptionPane.showMessageDialog(null, "Code faux !", "Erreur", JOptionPane.ERROR_MESSAGE);
                        if (i==3){
                            isConnected=false;
                            JOptionPane.showMessageDialog(null, "Nombre de tentatives dépassé !", "Erreur", JOptionPane.ERROR_MESSAGE);
                            dispose();
                        }
                        break;
                    case "EXPIRE":
                        isConnected=false;
                        JOptionPane.showMessageDialog(null, "Délais expiré !", "Erreur", JOptionPane.ERROR_MESSAGE);
                        dispose();
                        break;
                }
            }
        });

        add(envoyerButton, BorderLayout.SOUTH);
        setSize(400, 150); // width x height in pixels
        setLocationRelativeTo(null); // center it
    }

}
