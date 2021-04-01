package de.energiequant.vatsim.compatibility.legacyproxy.gui;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ChangeEvent;

import de.energiequant.vatsim.compatibility.legacyproxy.Main;

public class DisclaimerPanel extends JPanel {
    private final JCheckBox checkBox;

    public DisclaimerPanel() {
        super();

        JEditorPane editorPane = new JEditorPane();
        editorPane.setContentType("text/plain");
        editorPane.setText(Main.getDisclaimer());
        editorPane.setEditable(false);
        editorPane.setFont(Font.decode(Font.MONOSPACED));
        JScrollPane scrollPane = new JScrollPane(editorPane);

        checkBox = new JCheckBox("I understand and accept the disclaimer (required to start the server)");
        Main.getConfiguration().addDisclaimerListener(this::updateCheckBoxState);
        updateCheckBoxState();
        checkBox.addChangeListener(this::onCheckBoxStateChanged);

        JButton saveButton = new JButton("Save configuration");
        saveButton.addActionListener(this::onSaveConfigurationClicked);

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        add(scrollPane, gbc);

        gbc.gridy++;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        add(checkBox, gbc);

        gbc.gridx++;
        gbc.anchor = GridBagConstraints.EAST;
        add(saveButton, gbc);
    }

    private void updateCheckBoxState() {
        checkBox.setSelected(Main.getConfiguration().isDisclaimerAccepted());
    }

    private void onCheckBoxStateChanged(ChangeEvent event) {
        Main.getConfiguration().setDisclaimerAccepted(checkBox.isSelected());
    }

    private void onSaveConfigurationClicked(ActionEvent event) {
        Main.getConfiguration().save();
    }
}
