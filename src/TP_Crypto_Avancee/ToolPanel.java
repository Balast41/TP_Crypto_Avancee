package TP_Crypto_Avancee;

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;

public class ToolPanel extends JPanel
{
    private int panelWidth;
    private HttpClient client;

    public ToolPanel(int panelWidth, int panelHeight , HttpClient client)
    {
        // Variables
        this.client=client;
        this.panelWidth = panelWidth;

        // Organisation Générale
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setPreferredSize(new Dimension(panelWidth, panelHeight));
        setBackground(new Color(70, 130, 180));

        // Création des boutons
        add(Box.createRigidArea(new Dimension(0, 10)));
        addButton("Envoyer Mail", this::ouvrirEcritureMail);
        addButton("Supprimer Mail", () -> {});
        addButton("Autre ?", () -> {});
    }


    
    // Bouttons ----------------------------------------------------------------------------------------------------
    private void addButton(String text, Runnable action)
    {
        JButton button = new JButton(text);

        // Taille
        int buttonWidth = (int)(panelWidth * 0.8);
        button.setMaximumSize(new Dimension(buttonWidth, 45));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Apparence
        Color normal = new Color(90, 155, 213);
        Color hover = new Color(110, 175, 233);
        button.setBackground(normal);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        // Hover
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) { button.setBackground(hover); }
            public void mouseExited(java.awt.event.MouseEvent evt) { button.setBackground(normal); }
        });

        // Fonction exécutée
        button.addActionListener(e -> action.run());

        // Ajout au panel
        add(Box.createRigidArea(new Dimension(0, 13)));
        add(button);
    }

    // Fonctions ----------------------------------------------------------------------------------------------------
    private void ouvrirEcritureMail() {
        JFrame nouvelleFrame = new ecritureMail(client);
        nouvelleFrame.setSize(400, 400);
        nouvelleFrame.setLocationRelativeTo(null);
        nouvelleFrame.setVisible(true);
    };
}