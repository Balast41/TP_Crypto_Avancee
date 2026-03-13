package TP_Crypto_Avancee.InterfaceGraphique;
import javax.swing.*;
import java.awt.event.*;

import java.awt.*;

public class ToolPanel extends JPanel
{
    private int panelWidth;

    public ToolPanel(int panelWidth, int panelHeight)
    {
        // Variables
        this.panelWidth = panelWidth;

        // Organisation Générale
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setPreferredSize(new Dimension(panelWidth, panelHeight));
        setBackground(Color.GRAY);

        // Titre
        JLabel titleLabel = new JLabel("Fonctionnalités", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(titleLabel);
        add(Box.createRigidArea(new Dimension(0, 15))); // espace après titre

        // Création des boutons
        addButton1();
        addButton("Supprimer Mail", 0.8);
        addButton("Autre ?", 0.8);
    }


    
    // Bouttons ----------------------------------------------------------------------------------------------------
    private void addButton(String text, double widthPercent) {
        JButton button = new JButton(text);

        // Largeur en pourcentage du panel
        int buttonWidth = (int)(panelWidth * widthPercent);
        button.setMaximumSize(new Dimension(buttonWidth, 40)); // 40px de hauteur fixe
        button.setAlignmentX(Component.CENTER_ALIGNMENT);

        add(button);
        add(Box.createRigidArea(new Dimension(0, 10))); // espace vertical
    }

    private void addButton1() {
        JButton button = new JButton("Envoyer Mail");

        // Largeur en pourcentage du panel
        int buttonWidth = (int)(panelWidth * 0.8);
        button.setMaximumSize(new Dimension(buttonWidth, 40));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Ouvrir ecritureMail
        button.addActionListener((ActionEvent e) -> {
            JFrame nouvelleFrame = new ecritureMail();
            nouvelleFrame.setSize(400, 400);
            nouvelleFrame.setLocationRelativeTo(null);
            nouvelleFrame.setVisible(true);
        });

        add(button);
        add(Box.createRigidArea(new Dimension(0, 10))); // espace vertical
    }
}