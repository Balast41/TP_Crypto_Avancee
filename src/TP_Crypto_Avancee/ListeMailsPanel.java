package TP_Crypto_Avancee;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ListeMailsPanel extends JPanel {

    private HttpClient client;
    private JList<Mail> jlist;
    private DefaultListModel<Mail> listModel;
    private Map<String, Mail> mailCache = new HashMap<>();

    // Composants de la zone de détail
    private JPanel detailPanel;
    private JLabel fromLabel, toLabel, subjectLabel;
    private JTextArea messageArea;
    private JPanel attachmentsPanel;

    public ListeMailsPanel(HttpClient client) {
        this.client = client;
        this.listModel = new DefaultListModel<>();
        this.jlist = new JList<>(listModel);
        
        setLayout(new BorderLayout());

        // 1. Barre d'outils (Bouton Actualiser)
        JToolBar toolBar = new JToolBar();
        JButton refreshBtn = new JButton("Actualiser");
        refreshBtn.addActionListener(e -> refreshMails(false,50));
        toolBar.add(refreshBtn);
        add(toolBar, BorderLayout.NORTH);

        // 2. Configuration de la liste (Gauche)
        jlist.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jlist.setCellRenderer(new MailListRenderer());
        jlist.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadMailDetail(jlist.getSelectedValue());
            }
        });

        // 3. Configuration du détail (Droite)
        setupDetailPanel();

        // 4. SplitPane pour séparer liste et détail
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(jlist), new JScrollPane(detailPanel));
        splitPane.setDividerLocation(300);
        add(splitPane, BorderLayout.CENTER);

        // Chargement initial
        refreshMails(true,10);
        new Thread(() -> {
            refreshMails(true, 50);
        }).start();
    }

    private void setupDetailPanel() {
        detailPanel = new JPanel();
        detailPanel.setLayout(new BoxLayout(detailPanel, BoxLayout.Y_AXIS));
        detailPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        fromLabel = new JLabel("De : ");
        toLabel = new JLabel("À : ");
        subjectLabel = new JLabel("Objet : ");
        subjectLabel.setFont(new Font("Arial", Font.BOLD, 14));

        messageArea = new JTextArea(15, 40);
        messageArea.setEditable(false);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);

        attachmentsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        attachmentsPanel.setBorder(BorderFactory.createTitledBorder("Pièces jointes"));

        detailPanel.add(fromLabel);
        detailPanel.add(toLabel);
        detailPanel.add(Box.createVerticalStrut(10));
        detailPanel.add(subjectLabel);
        detailPanel.add(Box.createVerticalStrut(10));
        detailPanel.add(new JScrollPane(messageArea));
        detailPanel.add(Box.createVerticalStrut(10));
        detailPanel.add(attachmentsPanel);
    }

private void refreshMails(boolean forceDecrypt, int number) {
        // 1. Récupération des mails (on demande n=50)
        // Note : On passe false à getAllMails pour récupérer les données brutes/légères
        Mail[] fetchedMails = client.getAllMails(client.getHost(), HttpClient.getEmail(), HttpClient.getPassword(), client.getFilter(), number, forceDecrypt);
        
        listModel.clear();

        if (fetchedMails != null) {
            for (Mail m : fetchedMails) {
                String uid = m.getId();
                Mail mailToDisplay;

                // 2. Logique du cache
                if (!forceDecrypt && mailCache.containsKey(uid)) {
                    // On utilise la version déjà déchiffrée en mémoire
                    mailToDisplay = mailCache.get(uid);
                } else {
                    // Nouveau mail ou forçage : on déchiffre via le client
                    mailToDisplay = client.decryptMailMetadata(m);
                    mailCache.put(uid, mailToDisplay);
                }
                
                listModel.addElement(mailToDisplay);
            }
        }
    }

private void loadMailDetail(Mail selectedHeader) {
    if (selectedHeader == null) return;
    
    // On regarde dans le cache rempli par refreshMails
    Mail fullMail = mailCache.get(selectedHeader.getId());
    
    if (fullMail != null) {
        // C'est instantané ! Plus besoin de SwingWorker pour le réseau ici.
        displayMail(fullMail);
    }
}

private boolean isMailFullyLoaded(Mail m) {
    // Si le corps contient encore le marqueur ::KEY::, c'est qu'il n'est pas déchiffré
    // Ou si tu as une logique spécifique pour savoir si les PJ ont été listées
    return m.getMessage() != null && !m.getMessage().contains("::KEY::");
}

    private void displayMail(Mail mail) {
        fromLabel.setText("De : " + mail.getFrom());
        toLabel.setText("À : " + mail.getDestinataire());
        subjectLabel.setText("Objet : " + mail.getObjet());
        messageArea.setText(mail.getMessage());

attachmentsPanel.removeAll();
    String[] files = mail.getPath();
    if (files != null) {
        for (String name : files) {
            JButton fileBtn = new JButton(name);
            // On passe l'objet 'mail' complet à l'action
            fileBtn.addActionListener(e -> downloadAction(mail, name));
            attachmentsPanel.add(fileBtn);
        }
    }
    attachmentsPanel.revalidate();
    attachmentsPanel.repaint();
    }

private void downloadAction(Mail mail, String suggestedName) {
    JFileChooser chooser = new JFileChooser();
    chooser.setSelectedFile(new File(suggestedName));
    int ret = chooser.showSaveDialog(this);
    
    if (ret == JFileChooser.APPROVE_OPTION) {
        String destPath = chooser.getSelectedFile().getAbsolutePath();
        // On passe 'mail' au lieu de mailId
        client.downloadFile(client.getHost(), client.getUser(), client.getPass(), mail, suggestedName, destPath);
        JOptionPane.showMessageDialog(this, "Téléchargement terminé !");
    }
}

    // Rendu visuel de la liste à gauche
    private class MailListRenderer implements ListCellRenderer<Mail> {
        @Override
        public Component getListCellRendererComponent(JList<? extends Mail> list, Mail mail, int index, boolean isSelected, boolean cellHasFocus) {
            JPanel panel = new JPanel(new BorderLayout());
            panel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
            panel.setBackground(isSelected ? new Color(230, 240, 255) : Color.WHITE);

            JLabel exp = new JLabel(mail.getFrom());
            exp.setFont(new Font("Arial", Font.BOLD, 12));
            JLabel obj = new JLabel(mail.getObjet());
            obj.setForeground(Color.DARK_GRAY);

            panel.add(exp, BorderLayout.NORTH);
            panel.add(obj, BorderLayout.CENTER);
            panel.setPreferredSize(new Dimension(0, 50));
            panel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

            return panel;
        }
    }
}