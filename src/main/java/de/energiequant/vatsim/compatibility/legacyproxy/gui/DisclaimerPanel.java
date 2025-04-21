package de.energiequant.vatsim.compatibility.legacyproxy.gui;

import static de.energiequant.apputils.misc.gui.SwingHelper.onChange;

import java.awt.*;
import java.awt.event.ActionEvent;

import javax.swing.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.energiequant.vatsim.compatibility.legacyproxy.Configuration;
import de.energiequant.vatsim.compatibility.legacyproxy.Main;

public class DisclaimerPanel extends JPanel {
    private static final Logger LOGGER = LoggerFactory.getLogger(DisclaimerPanel.class);

    private final JCheckBox checkBox;

    public DisclaimerPanel() {
        super();

        Configuration config = Main.getConfiguration();

        JEditorPane editorPane = new JEditorPane();
        editorPane.setContentType("text/plain");
        editorPane.setText(Main.getDisclaimer());
        editorPane.setEditable(false);
        editorPane.setFont(Font.decode(Font.MONOSPACED));
        JScrollPane scrollPane = new JScrollPane(editorPane);

        checkBox = new JCheckBox("I understand and accept the disclaimer and licenses (required to start the server)");
        config.addDisclaimerListener(this::updateCheckBoxState);
        updateCheckBoxState();
        onChange(checkBox, this::onCheckBoxStateChanged);

        JButton saveButton = new JButton("Save configuration");
        if (!config.isSaneLocation()) {
            saveButton.setEnabled(false);

            // if the button is visible but disabled it's misleading as it looks like the
            // user would need to perform some other action to accept the disclaimer
            saveButton.setVisible(false);
        }
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

    private void onCheckBoxStateChanged() {
        boolean newValue = checkBox.isSelected();

        Configuration config = Main.getConfiguration();
        boolean oldValue = config.isDisclaimerAccepted();
        if (oldValue == newValue) {
            return;
        }

        LOGGER.debug("Disclaimer acceptance changed to {}", newValue);
        config.setDisclaimerAccepted(newValue);
    }

    private void onSaveConfigurationClicked(ActionEvent event) {
        Main.getConfiguration().save();
    }
}
