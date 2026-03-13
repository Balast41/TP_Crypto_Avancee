package TP_Crypto_Avancee;
import javax.swing.*;

import TP_Crypto_Avancee.Mail;

import java.awt.*;
import java.util.List;

public class ListeMailsPanel extends JPanel {

    public ListeMailsPanel(List<Mail> mails)
    {
        setLayout(new BorderLayout());

        // JList avec les objets Mail
        JList<Mail> jlist = new JList<>(mails.toArray(new Mail[0]));
        jlist.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);


        // Affichage des mails en bloc ---------------------------------------------------------------------------
        jlist.setCellRenderer(new ListCellRenderer<>()
        {
            @Override
            public Component getListCellRendererComponent(
            JList<? extends Mail> list, Mail mail, int index, boolean isSelected, boolean cellHasFocus)
            {
                // Panel pour le bloc
                JPanel panel = new JPanel(new BorderLayout());
                panel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
                panel.setBackground(isSelected ? Color.LIGHT_GRAY : Color.WHITE);

                // Textes
                JLabel expLabel = new JLabel(mail.getFrom());
                expLabel.setFont(expLabel.getFont().deriveFont(Font.BOLD));
                JLabel libLabel = new JLabel(mail.getObjet());

                // Ajout au panel, return
                panel.add(expLabel, BorderLayout.NORTH);
                panel.add(libLabel, BorderLayout.CENTER);
                return panel;
            }
        });

        // Scroll si la liste est grande
        JScrollPane scrollPane = new JScrollPane(jlist);
        add(scrollPane, BorderLayout.CENTER);


        // Gestion du clic pour afficher le contenu ---------------------------------------------------------------------------
        jlist.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Mail selected = jlist.getSelectedValue();
                if (selected != null) {
                    JOptionPane.showMessageDialog(
                        this,
                        "Objet : "+selected.getObjet()+"\n\n"+selected.getMessage(),
                        selected.getFrom(),
                        JOptionPane.PLAIN_MESSAGE);
                }
            }
        });
    }
}
