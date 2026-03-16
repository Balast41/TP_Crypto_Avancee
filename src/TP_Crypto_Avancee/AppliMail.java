package TP_Crypto_Avancee;
import java.awt.*;
import java.util.List;
import javax.swing.*;

public class AppliMail {

    public AppliMail(HttpClient client) {
        // Création de la fenêtre principale
        JFrame frame = new JFrame("Application Mail");
        ImageIcon logoIcon = new ImageIcon("/home/shila/Documents/CryptoAvancée/TPJavaMail/SMails_logo.png");
        frame.setIconImage(logoIcon.getImage());
        
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 800);

        // Création du panel qui affichera la liste
        ListeMailsPanel panelListe = new ListeMailsPanel(client);
        frame.add(panelListe, BorderLayout.CENTER);

        // Créer et ajouter ToolPanel
        ToolPanel toolPanel = new ToolPanel(200, 800, client);
        frame.add(toolPanel, BorderLayout.WEST);

        // Affichage immédiat
        frame.setVisible(true);
    }
}
