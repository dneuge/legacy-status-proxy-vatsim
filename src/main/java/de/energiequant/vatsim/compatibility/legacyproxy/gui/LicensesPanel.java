package de.energiequant.vatsim.compatibility.legacyproxy.gui;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.ItemEvent;
import java.util.Set;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import de.energiequant.vatsim.compatibility.legacyproxy.Main;
import de.energiequant.vatsim.compatibility.legacyproxy.attribution.License;

public class LicensesPanel extends JPanel {
    // TODO: insets

    private final JComboBox<License> comboBox;
    private final JEditorPane licenseText;
    private final JScrollPane licenseTextScrollPane;

    private static final Rectangle TOP_LEFT = new Rectangle(0, 0, 1, 1);

    public LicensesPanel(Set<License> licenses) {
        super();

        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();

        comboBox = new JComboBox<License>();
        comboBox.setEditable(false);
        licenses.stream().sorted().forEachOrdered(comboBox::addItem);
        comboBox.addItemListener(this::onComboBoxItemStateChanged);
        comboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                License license = (License) value;
                setText(license.getCanonicalName());

                return component;
            }
        });

        licenseText = new JEditorPane();
        licenseText.setEditable(false);
        licenseText.setContentType("text/html");

        licenseTextScrollPane = new JScrollPane(licenseText);

        License programLicense = Main.getEffectiveLicense();
        comboBox.setSelectedItem(programLicense);
        showLicenseText(programLicense);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        add(new JLabel("Display license:"), gbc);

        gbc.gridx++;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(comboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        add(licenseTextScrollPane, gbc);
    }

    private void showLicenseText(License license) {
        licenseText.setText(license.getText());
        licenseText.invalidate();
        licenseTextScrollPane.invalidate();
        EventQueue.invokeLater(this::scrollToStartOfLicense);
    }

    public void onComboBoxItemStateChanged(ItemEvent e) {
        if (e.getStateChange() != ItemEvent.SELECTED) {
            return;
        }

        showLicenseText((License) e.getItem());
    }

    public void selectLicense(License license) {
        comboBox.setSelectedItem(license);
    }

    private void scrollToStartOfLicense() {
        licenseText.scrollRectToVisible(TOP_LEFT);
    }
}
