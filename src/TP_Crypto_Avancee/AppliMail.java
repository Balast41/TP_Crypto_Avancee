package TP_Crypto_Avancee;
import java.awt.*;
import java.util.List;
import javax.swing.*;

public class AppliMail {

    public AppliMail(List<Mail> listeMails) {
        // Création de la fenêtre principale
        JFrame frame = new JFrame("Application Mail");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 800);

        // Création du panel qui affichera la liste
        ListeMailsPanel panelListe = new ListeMailsPanel(listeMails);
        frame.add(panelListe, BorderLayout.CENTER);

        // Créer et ajouter ToolPanel
        ToolPanel toolPanel = new ToolPanel(200, 800);
        frame.add(toolPanel, BorderLayout.WEST);

        // Affichage immédiat
        frame.setVisible(true);
    }
}
