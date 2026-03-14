package TP_Crypto_Avancee;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

public class ecritureMail extends JFrame {
    private JTextField destinataire;
    private JTextField sujet;
    private JTextPane texte_mail;
    private List<String> fichiersAttaches; 
    private JLabel nomsFichiersLabel; 
    private JCheckBox chiffrerMail;
    private Button DocButton;

    public ecritureMail(HttpClient client) {
        super("Rédaction de mail");

        fichiersAttaches = new ArrayList<>();
        setLayout(new BorderLayout());

        // --- SECTION NORD : DESTINATAIRE ET SUJET ---
        Panel en_tete = new Panel(new GridLayout(2, 2));
        en_tete.add(new Label("À :"));
        destinataire = new JTextField(30);
        en_tete.add(destinataire);

        en_tete.add(new Label("Sujet :"));
        sujet = new JTextField(30);
        en_tete.add(sujet);
        add(en_tete, BorderLayout.NORTH);

        // --- SECTION CENTRALE : CORPS ET PIÈCES JOINTES ---
        Panel corps = new Panel(new BorderLayout());
        Panel barre_outils = new Panel(new FlowLayout(FlowLayout.LEFT));

        Button AjoutFichierButton = new Button("Ajouter Document");
        DocButton = new Button("Supprimer une PJ");
        DocButton.setEnabled(false);
        
        // Rétablissement de la Checkbox de chiffrement
        chiffrerMail = new JCheckBox("Chiffrer le mail");

        // Label pour afficher les noms des fichiers
        nomsFichiersLabel = new JLabel("Aucun fichier sélectionné");
        nomsFichiersLabel.setFont(new Font("SansSerif", Font.ITALIC, 11));
        nomsFichiersLabel.setForeground(Color.GRAY);

        // ACTION : AJOUTER DES FICHIERS
        AjoutFichierButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setMultiSelectionEnabled(true);
                int retour = fileChooser.showOpenDialog(null);

                if (retour == JFileChooser.APPROVE_OPTION) {
                    File[] fichiers = fileChooser.getSelectedFiles();
                    for (File f : fichiers) {
                        fichiersAttaches.add(f.getAbsolutePath());
                    }
                    updateFilesUI();
                }
            }
        });

        // ACTION : SUPPRIMER UNE PJ VIA LE BOUTON DÉDIÉ
        DocButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (fichiersAttaches.isEmpty()) return;

                String[] fileNames = new String[fichiersAttaches.size()];
                for (int i = 0; i < fichiersAttaches.size(); i++) {
                    fileNames[i] = new File(fichiersAttaches.get(i)).getName();
                }

                String selectedFileName = (String) JOptionPane.showInputDialog(
                        null,
                        "Choisissez la pièce jointe à retirer :",
                        "Supprimer une PJ",
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        fileNames,
                        fileNames[0]
                );

                if (selectedFileName != null) {
                    // On retire le path correspondant au nom
                    fichiersAttaches.removeIf(path -> new File(path).getName().equals(selectedFileName));
                    updateFilesUI();
                }
            }
        });

        barre_outils.add(AjoutFichierButton);
        barre_outils.add(DocButton);
        barre_outils.add(chiffrerMail); // Elle est bien là !

        // Assemblage du panneau de contrôle
        Panel controleAttachements = new Panel(new GridLayout(2, 1));
        controleAttachements.add(barre_outils);
        controleAttachements.add(nomsFichiersLabel);
        corps.add(controleAttachements, BorderLayout.NORTH);

        // Zone de texte
        texte_mail = new JTextPane();
        JScrollPane scrollPane = new JScrollPane(texte_mail);
        scrollPane.setPreferredSize(new Dimension(500, 300));
        corps.add(scrollPane, BorderLayout.CENTER);

        add(corps, BorderLayout.CENTER);

        // --- SECTION SUD : BOUTON ENVOYER ---
        JButton b_envoyer = new JButton("Envoyer le Message");
        b_envoyer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String[] fichiers = fichiersAttaches.toArray(new String[0]);

                Mail mail = new Mail(
                        destinataire.getText(),
                        client.getEmail(),
                        client.getEmail(),
                        client.getPassword(),
                        sujet.getText(),
                        texte_mail.getText(),
                        fichiers
                );

                try {
                    boolean isSent;
                    if (chiffrerMail.isSelected()) {
                        isSent = client.sendingAMailCrypted(mail);
                    } else {
                        isSent = client.sendingAMail(mail);
                    }

                    if (isSent) {
                        JOptionPane.showMessageDialog(null, "Mail envoyé avec succès !", "Succès", JOptionPane.INFORMATION_MESSAGE);
                        dispose(); 
                    } else {
                        JOptionPane.showMessageDialog(null, "L'envoi a échoué.", "Erreur", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "Erreur : " + ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        add(b_envoyer, BorderLayout.SOUTH);
        
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void updateFilesUI() {
        if (fichiersAttaches.isEmpty()) {
            nomsFichiersLabel.setText("Aucun fichier sélectionné");
            nomsFichiersLabel.setForeground(Color.GRAY);
            DocButton.setEnabled(false);
            return;
        }

        StringBuilder sb = new StringBuilder("Fichiers : ");
        for (String path : fichiersAttaches) {
            sb.append(new File(path).getName()).append(", ");
        }

        String display = sb.toString();
        if (display.endsWith(", ")) {
            display = display.substring(0, display.length() - 2);
        }

        nomsFichiersLabel.setText(display);
        nomsFichiersLabel.setForeground(new Color(0, 100, 0)); // Un vert propre
        DocButton.setEnabled(true);
        revalidate();
    }
}