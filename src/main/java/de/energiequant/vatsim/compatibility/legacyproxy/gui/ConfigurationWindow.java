package de.energiequant.vatsim.compatibility.legacyproxy.gui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.energiequant.vatsim.compatibility.legacyproxy.AppConstants;
import de.energiequant.vatsim.compatibility.legacyproxy.Main;

public class ConfigurationWindow extends JFrame {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationWindow.class);

    public ConfigurationWindow() {
        super("Configuration");

        setSize(new Dimension(640, 480));
        setMinimumSize(new Dimension(640, 480));

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("General", new GeneralConfigurationPanel());
        add(tabbedPane, gbc);

        gbc.gridy++;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.insets = new Insets(3, 5, 3, 5);
        JButton saveButton = new JButton("Save configuration");
        saveButton.addActionListener(this::onSaveClicked);
        if (!Main.getConfiguration().isSaneLocation()) {
            saveButton.setEnabled(false);
            saveButton.setToolTipText(AppConstants.SAVING_DISABLED_TOOLTIP);
        }
        add(saveButton, gbc);
    }

    private void onSaveClicked(ActionEvent event) {
        Main.getConfiguration().save();
    }
}
