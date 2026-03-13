package TP_Crypto_Avancee;


import java.awt.*;
import java.awt.event.*;
import java.io.File;
import javax.print.Doc;
import javax.swing.*;

/**
 * Simple AWT-based GUI that simulates a mail composition page.
 * Contains fields for recipient, subject and message body, plus
 * a button that prints the entered values to the console.
 */
public class ecritureMail extends JFrame {
    private JTextField destinataire;
    private JTextField sujet;
    private JTextPane texte_mail;
    private File fichier_attache;
    private HttpClient client;

    public ecritureMail(HttpClient client) {
        super("Rédaction de mail");
        this.client=client;
        setLayout(new BorderLayout());

        // panneau des en-têtes (destinataire + sujet)
        Panel en_tete = new Panel(new GridLayout(2, 2));// permet d'associer les labels et les champs de texte
        // destinataire
        en_tete.add(new Label("À :"));
		destinataire = new JTextField(30);
        en_tete.add(destinataire);
		// sujet
        en_tete.add(new Label("Sujet :"));
        sujet = new JTextField(30);
        en_tete.add(sujet);
        add(en_tete, BorderLayout.NORTH);
		//fin panneau des en-têtes

        // panneau pour le corps avec entête
        Panel corps = new Panel(new BorderLayout());
        
        // entête avec boutons d'ajout de fichier et d'affichage du fichier attaché
        Panel barre_outils = new Panel(new FlowLayout());
        Button AjoutFichierButton = new Button("Document");

        Button DocButton = new Button("Afficher le document attaché");
        DocButton.setEnabled(false); // Désactiver le bouton d'affichage du document tant qu'aucun fichier n'est sélectionné

        AjoutFichierButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                 JFileChooser fileChooser = new JFileChooser();
                int retour = fileChooser.showOpenDialog(null);
                if (retour == JFileChooser.APPROVE_OPTION) {
                    fichier_attache = fileChooser.getSelectedFile();
                    DocButton.setEnabled(true); // Activer le bouton d'affichage du document
                    DocButton.setLabel( fichier_attache.getName()); // Mettre à jour le label du bouton avec le nom du fichier
                    DocButton.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            Desktop desktop = Desktop.getDesktop();
                            try {
                                desktop.open(fichier_attache);
                            } catch (Exception ex) {
                                System.out.println("Erreur lors de l'ouverture du fichier : " + ex.getMessage());
                            }}
                    });
                    barre_outils.revalidate(); // Met à jour l'affichage de la barre d'
                } else {
                    System.out.println("Aucun fichier sélectionné.");
                }
            }
        });

        barre_outils.add(AjoutFichierButton);
        barre_outils.add(DocButton);
        corps.add(barre_outils, BorderLayout.NORTH);
        
        // zone de texte du message
        texte_mail = new JTextPane();
        texte_mail.setPreferredSize(new Dimension(10, 50));
        corps.add(texte_mail, BorderLayout.CENTER);
        
        add(corps, BorderLayout.CENTER);

        // bouton d'envoi
        JButton b_envoyer = new JButton("Envoyer");
        b_envoyer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Regarder pour les fichiers + Rajouter une case "A chiffrer ?"
                Mail mail= new Mail(destinataire.getText(),client.getEmail(),client.getEmail(),client.getPassword(),sujet.getText(),texte_mail.getText(), new String[0]);
                client.sendingAMailCrypted(mail);
            }
        });
        add(b_envoyer, BorderLayout.SOUTH);
    }
}