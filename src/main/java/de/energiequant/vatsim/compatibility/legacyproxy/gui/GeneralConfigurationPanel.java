package de.energiequant.vatsim.compatibility.legacyproxy.gui;

import static de.energiequant.vatsim.compatibility.legacyproxy.gui.SwingHelper.onChange;
import static de.energiequant.vatsim.compatibility.legacyproxy.gui.SwingHelper.stylePlain;
import static de.energiequant.vatsim.compatibility.legacyproxy.gui.SwingHelper.unformattedNumericSpinner;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.util.Set;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.energiequant.vatsim.compatibility.legacyproxy.Configuration;
import de.energiequant.vatsim.compatibility.legacyproxy.Main;

public class GeneralConfigurationPanel extends JPanel {
    private static final Logger LOGGER = LoggerFactory.getLogger(GeneralConfigurationPanel.class);

    private final UpstreamPanel upstreamPanel = new UpstreamPanel();
    private final HttpServerPanel httpServerPanel = new HttpServerPanel();
    private final AccessPanel accessPanel = new AccessPanel();

    public GeneralConfigurationPanel() {
        super();

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(upstreamPanel, gbc);

        gbc.gridy++;
        add(httpServerPanel, gbc);

        gbc.gridy++;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        add(accessPanel, gbc);

        updateAllOptions();
    }

    private void updateAllOptions() {
        upstreamPanel.updateAllOptions();
        httpServerPanel.updateAllOptions();
        accessPanel.updateAddressList();
    }

    private class UpstreamPanel extends JPanel {
        final JCheckBox upstreamBaseUrlDefaultCheckBox = new JCheckBox("Use VATSIM default URL");
        final JTextField upstreamBaseUrlField = new JTextField();

        public UpstreamPanel() {
            super();

            onChange(upstreamBaseUrlField, this::onUpstreamBaseUrlFieldAction);
            onChange(upstreamBaseUrlDefaultCheckBox, this::onUpstreamBaseUrlOverrideChanged);

            setBorder(BorderFactory.createTitledBorder("Upstream connection (VATSIM data source)"));

            setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();

            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.gridwidth = 2;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.WEST;
            add(stylePlain(new JLabel(
                "<html>Usually no changes to upstream connection should be required. The base URL is appended with further paths and cannot end with a slash. Changes to the upstream connection require a full restart of the proxy application (run/stop is insufficient).</html>")),
                gbc);

            gbc.gridy++;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridwidth = 2;
            add(upstreamBaseUrlDefaultCheckBox, gbc);

            gbc.gridy++;
            gbc.weightx = 0.0;
            gbc.fill = GridBagConstraints.NONE;
            gbc.gridwidth = 1;
            JLabel upstreamBaseUrlLabel = new JLabel("Custom base URL:");
            add(upstreamBaseUrlLabel, gbc);

            gbc.gridx++;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            add(upstreamBaseUrlField, gbc);
        }

        private void updateAllOptions() {
            Configuration config = Main.getConfiguration();
            boolean isUpstreamBaseUrlOverridden = config.isUpstreamBaseUrlOverridden();

            upstreamBaseUrlDefaultCheckBox.setSelected(!isUpstreamBaseUrlOverridden);
            upstreamBaseUrlField.setText(config.getUpstreamBaseUrlOverride());
            upstreamBaseUrlField.setEnabled(isUpstreamBaseUrlOverridden);
        }

        private void onUpstreamBaseUrlOverrideChanged() {
            Main.getConfiguration().setUpstreamBaseUrlOverridden(!upstreamBaseUrlDefaultCheckBox.isSelected());
            updateAllOptions();
        }

        private void onUpstreamBaseUrlFieldAction() {
            String newValue = upstreamBaseUrlField.getText();

            Configuration config = Main.getConfiguration();
            String oldValue = config.getLocalHostName();
            if (oldValue.equals(newValue) || newValue.trim().isEmpty()) {
                return;
            }

            LOGGER.debug("setting upstream base URL to \"{}\"", newValue);
            try {
                config.setUpstreamBaseUrl(newValue);
            } catch (IllegalArgumentException ex) {
                LOGGER.warn("invalid upstream base URL requested: {}", newValue, ex);
            }
        }
    }

    private class HttpServerPanel extends JPanel {
        final JTextField localHostNameField = new JTextField();
        final JSpinner serverPortField = unformattedNumericSpinner(new JSpinner(
            new SpinnerNumberModel(1, Configuration.SERVER_PORT_MINIMUM, Configuration.SERVER_PORT_MAXIMUM, 1) //
        ));
        final JCheckBox quirkUtf8CheckBox = new JCheckBox("encode data file in UTF-8 instead of ISO-8859-1");
        final JCheckBox enableParserLogCheckBox = new JCheckBox("log parser errors (upstream data)");

        public HttpServerPanel() {
            super();

            onChange(localHostNameField, this::onLocalHostNameFieldAction);
            onChange(serverPortField, this::onServerPortFieldChanged);
            onChange(quirkUtf8CheckBox, this::onQuirkUtf8CheckBoxChanged);
            onChange(enableParserLogCheckBox, this::onEnableParserLogCheckBoxChanged);

            styleSpinner(serverPortField);

            setBorder(BorderFactory.createTitledBorder("HTTP Server"));

            setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();

            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.gridwidth = 2;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.WEST;
            add(stylePlain(new JLabel(
                "<html>The host name must be reachable by all clients, an IPv4 address is also possible. Changes to the HTTP server configuration require a restart (use the run/stop button on main window).</html>")),
                gbc);

            gbc.gridy++;
            gbc.gridwidth = 1;
            gbc.weightx = 0.0;
            gbc.fill = GridBagConstraints.NONE;
            add(new JLabel("Host name:"), gbc);

            gbc.gridx++;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            add(localHostNameField, gbc);

            gbc.gridx = 0;
            gbc.gridy++;
            gbc.weightx = 0.0;
            gbc.gridwidth = 1;
            gbc.fill = GridBagConstraints.NONE;
            add(new JLabel("Port:"), gbc);

            gbc.gridx++;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            add(serverPortField, gbc);

            gbc.gridy++;
            gbc.gridwidth = 1;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            add(quirkUtf8CheckBox, gbc);

            gbc.gridy++;
            gbc.gridwidth = 1;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            add(enableParserLogCheckBox, gbc);
        }

        private void styleSpinner(JSpinner spinner) {
            JComponent editor = spinner.getEditor();
            if (editor instanceof JSpinner.DefaultEditor) {
                JFormattedTextField textField = ((JSpinner.DefaultEditor) editor).getTextField();
                textField.setHorizontalAlignment(JTextField.LEFT);
                textField.setFont(spinner.getFont().deriveFont(Font.PLAIN));
            }
        }

        private void updateAllOptions() {
            Configuration config = Main.getConfiguration();
            localHostNameField.setText(config.getLocalHostName());
            serverPortField.setValue(config.getServerPort());
            quirkUtf8CheckBox.setSelected(config.isQuirkLegacyDataFileUtf8Enabled());
            enableParserLogCheckBox.setSelected(config.isParserLogEnabled());
        }

        private void onLocalHostNameFieldAction() {
            String newValue = localHostNameField.getText();

            Configuration config = Main.getConfiguration();
            String oldValue = config.getLocalHostName();
            if (oldValue.equals(newValue) || newValue.trim().isEmpty()) {
                return;
            }

            LOGGER.debug("setting local host name to \"{}\"", newValue);
            try {
                config.setLocalHostName(newValue);
            } catch (IllegalArgumentException ex) {
                LOGGER.warn("invalid local host name requested: {}", newValue, ex);
            }

            updateAllOptions();
        }

        private void onServerPortFieldChanged() {
            int newValue = (int) serverPortField.getValue();

            Configuration config = Main.getConfiguration();
            int oldValue = config.getServerPort();
            if (oldValue == newValue) {
                return;
            }

            LOGGER.debug("setting server port to {}", newValue);
            try {
                config.setServerPort(newValue);
            } catch (IllegalArgumentException ex) {
                LOGGER.warn("invalid server port requested: {}", newValue, ex);
            }

            updateAllOptions();
        }

        private void onQuirkUtf8CheckBoxChanged() {
            boolean newValue = quirkUtf8CheckBox.isSelected();

            Configuration config = Main.getConfiguration();
            boolean oldValue = config.isQuirkLegacyDataFileUtf8Enabled();
            if (oldValue == newValue) {
                return;
            }

            LOGGER.debug("setting UTF8 quirk to {}", newValue);
            config.setQuirkLegacyDataFileUtf8Enabled(newValue);
        }

        private void onEnableParserLogCheckBoxChanged() {
            boolean newValue = enableParserLogCheckBox.isSelected();

            Configuration config = Main.getConfiguration();
            boolean oldValue = config.isParserLogEnabled();
            if (oldValue == newValue) {
                return;
            }

            LOGGER.debug("setting parser logging to {}", newValue);
            config.setParserLogEnabled(newValue);
        }
    }

    private class AccessPanel extends JPanel {
        final JTextField ipAddressField = new JTextField();
        final DefaultListModel<String> addressListModel = new DefaultListModel<>();
        final JList<String> addressList = new JList<String>(addressListModel);

        public AccessPanel() {
            super();

            setBorder(BorderFactory.createTitledBorder("Access (IP Filter)"));

            setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();

            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.gridwidth = 2;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.WEST;
            add(stylePlain(new JLabel(
                "<html>Only the listed IP addresses are served data by the proxy server. Addresses need to be entered exactly as shown in the log. Changes are applied immediately.</html>")),
                gbc);

            gbc.gridy++;
            gbc.gridwidth = 1;
            gbc.weightx = 0.0;
            gbc.fill = GridBagConstraints.NONE;
            add(new JLabel("IP Address:"), gbc);

            gbc.gridx++;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            add(ipAddressField, gbc);

            gbc.gridx++;
            gbc.weightx = 0.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            JButton addButton = new JButton("Add");
            addButton.addActionListener(this::onAddClicked);
            add(addButton, gbc);

            gbc.gridx = 0;
            gbc.gridy++;
            gbc.weightx = 1.0;
            gbc.weighty = 1.0;
            gbc.gridwidth = 2;
            gbc.gridheight = 3;
            gbc.fill = GridBagConstraints.BOTH;
            add(new JScrollPane(addressList), gbc);

            gbc.gridx += 2;
            gbc.weightx = 0.0;
            gbc.weighty = 0.0;
            gbc.gridwidth = 1;
            gbc.gridheight = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            JButton removeButton = new JButton("Remove");
            removeButton.addActionListener(this::onRemoveClicked);
            add(removeButton, gbc);

            gbc.gridy += 2;
            gbc.anchor = GridBagConstraints.SOUTH;
            JButton resetButton = new JButton("Reset");
            resetButton.addActionListener(this::onResetClicked);
            add(resetButton, gbc);

            updateAddressList();
        }

        private void updateAddressList() {
            addressListModel.clear();

            Main.getConfiguration() //
                .getAllowedIps() //
                .stream() //
                .sorted() //
                .forEachOrdered(addressListModel::addElement);
        }

        private void manipulateAllowedIps(Consumer<Set<String>> manipulator) {
            Configuration config = Main.getConfiguration();
            Set<String> allowedIps = config.getAllowedIps();
            manipulator.accept(allowedIps);
            config.setAllowedIps(allowedIps);

            updateAddressList();
        }

        private void onAddClicked(ActionEvent event) {
            String newAddress = ipAddressField.getText().trim();
            if (newAddress.isEmpty()) {
                return;
            }

            LOGGER.debug("adding \"{}\" to allowed IPs", newAddress);
            manipulateAllowedIps(allowedIps -> allowedIps.add(newAddress));
        }

        private void onRemoveClicked(ActionEvent event) {
            String address = addressList.getSelectedValue();
            if (address == null) {
                return;
            }

            LOGGER.debug("removing \"{}\" from allowed IPs", address);
            manipulateAllowedIps(allowedIps -> allowedIps.remove(address));
        }

        private void onResetClicked(ActionEvent event) {
            int result = JOptionPane.showConfirmDialog(this,
                "You are about to reset the list of allowed IP addresses to the default of localhost IPv4 (127.0.0.1) and IPv6 addresses (::1). Are you sure? All other addresses will be removed.",
                "Confirm reset", JOptionPane.OK_CANCEL_OPTION);
            if (result != JOptionPane.OK_OPTION) {
                LOGGER.debug("request to reset allowed IPs was canceled");
                return;
            }

            LOGGER.debug("resetting allowed IPs to default");
            manipulateAllowedIps(allowedIps -> {
                allowedIps.clear();
                allowedIps.addAll(Configuration.DEFAULT_ALLOWED_IPS);
            });
        }
    }

}
