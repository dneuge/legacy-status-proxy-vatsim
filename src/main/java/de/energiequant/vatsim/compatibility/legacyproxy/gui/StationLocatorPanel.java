package de.energiequant.vatsim.compatibility.legacyproxy.gui;

import static de.energiequant.vatsim.compatibility.legacyproxy.gui.SwingHelper.onChange;
import static de.energiequant.vatsim.compatibility.legacyproxy.gui.SwingHelper.onSelect;
import static de.energiequant.vatsim.compatibility.legacyproxy.gui.SwingHelper.stylePlain;
import static de.energiequant.vatsim.compatibility.legacyproxy.gui.SwingHelper.sumInsets;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vatplanner.dataformats.vatsimpublic.parser.Client;

import de.energiequant.vatsim.compatibility.legacyproxy.AppConstants;
import de.energiequant.vatsim.compatibility.legacyproxy.Configuration;
import de.energiequant.vatsim.compatibility.legacyproxy.Main;
import de.energiequant.vatsim.compatibility.legacyproxy.attribution.VatSpyMetaData;
import de.energiequant.vatsim.compatibility.legacyproxy.server.stationlocator.StationLocator.Strategy;

public class StationLocatorPanel extends JPanel {
    private static final Logger LOGGER = LoggerFactory.getLogger(StationLocatorPanel.class);

    private final VatSpyPanel vatSpyPanel = new VatSpyPanel();
    private final TransceiversPanel transceiversPanel = new TransceiversPanel();

    private static final DateTimeFormatter UTC_HUMAN_READABLE_DATE_FORMATTER = DateTimeFormatter
        .ofPattern("d MMMM YYYY", Locale.US)
        .withZone(ZoneId.of("UTC"));

    private static final Insets NO_INSETS = new Insets(0, 0, 0, 0);
    private static final Insets CHECKBOX_WIDTH_INSETS = new Insets(0, 22, 0, 0);
    private static final Insets BOTTOM_INSETS = new Insets(0, 0, 10, 0);
    private static final Insets TOP_INSETS = new Insets(10, 0, 0, 0);

    private static final String TITLE_POSTFIX_NOT_ACTIVE = " (not used by selected strategy)";

    private final JComboBox<Strategy> strategyComboBox = new JComboBox<>(Strategy.values());
    private final JCheckBox obsCallsignIsObserverCheckBox = new JCheckBox(
        "assume callsigns ending in _OBS to be observers" //
    );
    private final JCheckBox ignorePlaceholderFrequencyCheckBox = new JCheckBox(
        "ignore clients on placeholder frequencies ("
            + (Client.FREQUENCY_KILOHERTZ_PLACEHOLDER_MINIMUM / 1000)
            + "MHz or above)" //
    );
    private final JCheckBox warnAboutUnlocatableATCCheckBox = new JCheckBox(
        "warn about unlocatable ATC stations in log" //
    );
    private final JCheckBox warnAboutUnlocatableObserverCheckBox = new JCheckBox(
        "warn about unlocateable observers in log" //
    );

    public StationLocatorPanel() {
        super();

        onSelect(strategyComboBox, this::onStrategySelected);
        onChange(obsCallsignIsObserverCheckBox, this::onObsCallsignIsObserverChanged);
        onChange(ignorePlaceholderFrequencyCheckBox, this::onIgnorePlaceholderFrequencyChanged);
        onChange(warnAboutUnlocatableATCCheckBox, this::onWarnAboutUnlocatableATCChanged);
        onChange(warnAboutUnlocatableObserverCheckBox, this::onWarnAboutUnlocatableObserverChanged);

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = BOTTOM_INSETS;
        add(
            stylePlain(new JLabel(
                "<html>All changes regarding Station Locator require a restart of the HTTP server to become effective (Run/Stop).</html>" //
            )), //
            gbc //
        );

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        add(new JLabel("Strategy:"), gbc);

        gbc.gridx++;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        strategyComboBox.setEditable(false);
        strategyComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value == null) {
                    return component;
                }

                if (!(component instanceof JLabel)) {
                    LOGGER.warn("DefaultListCellRenderer is not a JLabel");
                } else {
                    ((JLabel) component).setText(((Strategy) value).getDescription());
                }
                return component;
            }
        });
        add(strategyComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.insets = NO_INSETS;
        add(obsCallsignIsObserverCheckBox, gbc);

        gbc.gridy++;
        gbc.insets = CHECKBOX_WIDTH_INSETS;
        add(
            stylePlain(new JLabel(
                "If disabled, only the client's indicated permission level will be taken into account." //
            )),
            gbc //
        );

        gbc.gridy++;
        gbc.insets = NO_INSETS;
        add(ignorePlaceholderFrequencyCheckBox, gbc);

        gbc.gridy++;
        add(warnAboutUnlocatableATCCheckBox, gbc);

        gbc.gridy++;
        add(warnAboutUnlocatableObserverCheckBox, gbc);

        gbc.gridy++;
        gbc.insets = TOP_INSETS;
        add(vatSpyPanel, gbc);

        gbc.gridy++;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        add(transceiversPanel, gbc);

        updateAllOptions();
    }

    private void onStrategySelected() {
        Strategy newValue = (Strategy) strategyComboBox.getSelectedItem();

        Configuration config = Main.getConfiguration();
        Strategy oldValue = config.getStationLocatorStrategy();
        if (oldValue == newValue) {
            return;
        }

        LOGGER.debug("setting station locator strategy to {}", newValue);
        Main.getConfiguration().setStationLocatorStrategy(newValue);

        updateAllOptions();
    }

    private void onObsCallsignIsObserverChanged() {
        boolean newValue = obsCallsignIsObserverCheckBox.isSelected();

        Configuration config = Main.getConfiguration();
        boolean oldValue = config.shouldIdentifyObserverByCallsign();
        if (oldValue == newValue) {
            return;
        }

        LOGGER.debug("setting identification of observers by callsign to {}", newValue);
        Main.getConfiguration().setShouldIdentifyObserverByCallsign(newValue);
    }

    private void onIgnorePlaceholderFrequencyChanged() {
        boolean newValue = ignorePlaceholderFrequencyCheckBox.isSelected();

        Configuration config = Main.getConfiguration();
        boolean oldValue = config.shouldIgnorePlaceholderFrequency();
        if (oldValue == newValue) {
            return;
        }

        LOGGER.debug("setting ignorance of placeholder frequencies to {}", newValue);
        Main.getConfiguration().setShouldIgnorePlaceholderFrequency(newValue);
    }

    private void onWarnAboutUnlocatableATCChanged() {
        boolean newValue = warnAboutUnlocatableATCCheckBox.isSelected();

        Configuration config = Main.getConfiguration();
        boolean oldValue = config.shouldWarnAboutUnlocatableATC();
        if (oldValue == newValue) {
            return;
        }

        LOGGER.debug("setting warning on unlocatable ATC to {}", newValue);
        Main.getConfiguration().setShouldWarnAboutUnlocatableATC(newValue);
    }

    private void onWarnAboutUnlocatableObserverChanged() {
        boolean newValue = warnAboutUnlocatableObserverCheckBox.isSelected();

        Configuration config = Main.getConfiguration();
        boolean oldValue = config.shouldWarnAboutUnlocatableObserver();
        if (oldValue == newValue) {
            return;
        }

        LOGGER.debug("setting warning on unlocatable observer to {}", newValue);
        Main.getConfiguration().setShouldWarnAboutUnlocatableObserver(newValue);
    }

    private void updateAllOptions() {
        Configuration config = Main.getConfiguration();

        strategyComboBox.setSelectedItem(config.getStationLocatorStrategy());
        obsCallsignIsObserverCheckBox.setSelected(config.shouldIdentifyObserverByCallsign());
        ignorePlaceholderFrequencyCheckBox.setSelected(config.shouldIgnorePlaceholderFrequency());
        warnAboutUnlocatableATCCheckBox.setSelected(config.shouldWarnAboutUnlocatableATC());
        warnAboutUnlocatableObserverCheckBox.setSelected(config.shouldWarnAboutUnlocatableObserver());

        vatSpyPanel.updateAllOptions();
        transceiversPanel.updateAllOptions();
    }

    private class VatSpyPanel extends JPanel {
        private final Optional<Instant> includedDate = VatSpyMetaData.getIncludedDataTimestamp();

        private static final String EXTERNAL_CHECK_PREFIX = "Pre-check: ";

        private static final String TITLE = "VAT-Spy data";

        private final JCheckBox warnOnOldDataCheckBox = new JCheckBox(
            "warn if integrated database older than "
                + AppConstants.VATSPY_AGE_WARNING_THRESHOLD.toDays()
                + " days is used" //
        );
        private final JCheckBox useExternalDataCheckBox = new JCheckBox(
            "Use external database:"//
        );

        private final JTextField externalDataField = new JTextField();
        private final JButton externalDataButton = new JButton("Browse");
        private final JLabel externalDataFallbackLabel = new JLabel(
            "Integrated data will still be used if the external database is unavailable." //
        );
        private final JLabel externalDataCheckLabel = new JLabel(EXTERNAL_CHECK_PREFIX + " pending");

        private final JCheckBox aliasUSStationsCheckBox = new JCheckBox(
            "alias US stations to omit ICAO prefix K unless conflicted" //
        );
        private final JCheckBox observerCallsignAsStationCheckBox = new JCheckBox(
            "locate observers by assuming callsign to indicate an ATC station" //
        );

        VatSpyPanel() {
            super();

            onChange(warnOnOldDataCheckBox, this::onWarnOnOldDataChanged);
            onChange(useExternalDataCheckBox, this::onUseExternalDataChanged);
            onChange(aliasUSStationsCheckBox, this::onAliasUSStationsChanged);
            onChange(observerCallsignAsStationCheckBox, this::onObserverCallsignAsStationChanged);

            // FIXME: register filechooser to externalDataButton

            setBorder(BorderFactory.createTitledBorder(TITLE));

            setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();

            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.gridwidth = 3;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = BOTTOM_INSETS;
            add(
                stylePlain(new JLabel(
                    "Integrated database is from "
                        + includedDate
                            .map(UTC_HUMAN_READABLE_DATE_FORMATTER::format)
                            .orElse("unknown date") //
                )),
                gbc //
            );

            gbc.gridy++;
            add(
                stylePlain(new JLabel(
                    "<html>Updated data may be available from https://github.com/vatsimnetwork/vatspy-data-project "
                        + "even if no update for this program is available. "
                        + "VAT-Spy databases installed by other programs can be used as well. Updates to an external "
                        + "database require a restart of the HTTP server (Run/Stop) to become effective."
                        + "</html>" //
                )),
                gbc //
            );

            gbc.gridy++;
            add(warnOnOldDataCheckBox, gbc);

            gbc.gridy++;
            gbc.gridwidth = 1;
            gbc.weightx = 0.0;
            gbc.fill = GridBagConstraints.NONE;
            gbc.insets = NO_INSETS;
            add(useExternalDataCheckBox, gbc);

            gbc.gridx++;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            externalDataField.setEditable(false);
            add(externalDataField, gbc);

            gbc.gridx++;
            gbc.weightx = 0.0;
            gbc.fill = GridBagConstraints.NONE;
            add(externalDataButton, gbc);

            gbc.gridx = 0;
            gbc.gridy++;
            gbc.gridwidth = 3;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = CHECKBOX_WIDTH_INSETS;
            add(
                stylePlain(externalDataFallbackLabel),
                gbc //
            );

            gbc.gridy++;
            gbc.insets = sumInsets(CHECKBOX_WIDTH_INSETS, BOTTOM_INSETS);
            add(stylePlain(externalDataCheckLabel), gbc);

            gbc.gridy++;
            gbc.insets = NO_INSETS;
            add(aliasUSStationsCheckBox, gbc);

            gbc.gridy++;
            add(observerCallsignAsStationCheckBox, gbc);

            gbc.gridy++;
            gbc.insets = CHECKBOX_WIDTH_INSETS;
            add(
                stylePlain(new JLabel(
                    "If disabled, observers can only be located via transceivers." //
                )),
                gbc //
            );

            updateAllOptions();
        }

        private void onWarnOnOldDataChanged() {
            boolean newValue = warnOnOldDataCheckBox.isSelected();

            Configuration config = Main.getConfiguration();
            boolean oldValue = config.shouldWarnAboutOldIntegratedVatSpyDatabase();
            if (oldValue == newValue) {
                return;
            }

            LOGGER.debug("setting warning on outdated integrated VAT-Spy database to {}", newValue);
            Main.getConfiguration().setShouldWarnAboutOldIntegratedVatSpyDatabase(newValue);
        }

        private void onUseExternalDataChanged() {
            boolean newValue = useExternalDataCheckBox.isSelected();

            Configuration config = Main.getConfiguration();
            boolean oldValue = config.isVatSpyBaseDirectoryEnabled();
            if (oldValue == newValue) {
                return;
            }

            LOGGER.debug("setting usage of external VAT-Spy directory to {}", newValue);
            Main.getConfiguration().setVatSpyBaseDirectoryEnabled(newValue);

            updateAllOptions();
        }

        private void onAliasUSStationsChanged() {
            boolean newValue = aliasUSStationsCheckBox.isSelected();

            Configuration config = Main.getConfiguration();
            boolean oldValue = config.shouldVatSpyAliasUSStations();
            if (oldValue == newValue) {
                return;
            }

            LOGGER.debug("setting aliasing of US stations in VAT-Spy data to {}", newValue);
            Main.getConfiguration().setShouldVatSpyAliasUSStations(newValue);
        }

        private void onObserverCallsignAsStationChanged() {
            boolean newValue = observerCallsignAsStationCheckBox.isSelected();

            Configuration config = Main.getConfiguration();
            boolean oldValue = config.shouldLocateObserverByVatSpy();
            if (oldValue == newValue) {
                return;
            }

            LOGGER.debug("setting location of observers via VAT-Spy to {}", newValue);
            Main.getConfiguration().setShouldLocateObserverByVatSpy(newValue);
        }

        private void updateAllOptions() {
            Configuration config = Main.getConfiguration();

            boolean isActive = config.getStationLocatorStrategy().enablesVatSpy();
            if (isActive) {
                setBorder(BorderFactory.createTitledBorder(TITLE));
            } else {
                setBorder(BorderFactory.createTitledBorder(TITLE + TITLE_POSTFIX_NOT_ACTIVE));
            }

            warnOnOldDataCheckBox.setSelected(config.shouldWarnAboutOldIntegratedVatSpyDatabase());

            boolean shouldUseExternalData = config.isVatSpyBaseDirectoryEnabled();
            useExternalDataCheckBox.setSelected(shouldUseExternalData);

            externalDataField.setText(config.getVatSpyBaseDirectory().map(File::getAbsolutePath).orElse(""));

            aliasUSStationsCheckBox.setSelected(config.shouldVatSpyAliasUSStations());
            observerCallsignAsStationCheckBox.setSelected(config.shouldLocateObserverByVatSpy());

            // FIXME: implement pre-check of external database

            externalDataField.setEnabled(shouldUseExternalData);
            externalDataButton.setEnabled(shouldUseExternalData);
            externalDataFallbackLabel.setEnabled(shouldUseExternalData);
            externalDataCheckLabel.setEnabled(shouldUseExternalData);
        }
    }

    private class TransceiversPanel extends JPanel {
        private static final int CACHE_MINIMUM = 0;
        private static final int CACHE_MAXIMUM = 60;
        private static final int CACHE_INTERVAL = 1;

        private static final String TITLE = "Online Transceivers";

        private final JCheckBox locateObserversCheckBox = new JCheckBox("locate observers");

        private final JCheckBox defaultCheckBox = new JCheckBox(
            "use default URL and update interval as announced through network information and data file" //
        );

        private final JLabel cacheLabel = new JLabel("Cache for:");
        private final JSpinner cacheSpinner = new JSpinner(
            new SpinnerNumberModel(15, CACHE_MINIMUM, CACHE_MAXIMUM, CACHE_INTERVAL) //
        );
        private final JLabel cacheUnitLabel = new JLabel("minutes");

        private final JLabel urlLabel = new JLabel("Custom URL:");
        private final JTextField urlField = new JTextField();

        TransceiversPanel() {
            super();

            onChange(locateObserversCheckBox, this::onLocateObserversChanged);
            onChange(defaultCheckBox, this::onDefaultChanged);
            onChange(cacheSpinner, this::onCacheSpinnerChanged);
            onChange(urlField, this::onUrlFieldChanged);

            setBorder(BorderFactory.createTitledBorder(TITLE));

            setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();

            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.gridwidth = 3;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            add(locateObserversCheckBox, gbc);

            gbc.gridy++;
            gbc.insets = CHECKBOX_WIDTH_INSETS;
            add(
                stylePlain(new JLabel(
                    "<html>Observer positions are usually irrelevant and can be left blank. "
                        + "Activate only if needed as this will cause otherwise unnecessary data polls.<html>" //
                )),
                gbc //
            );

            gbc.gridy++;
            gbc.insets = NO_INSETS;
            add(defaultCheckBox, gbc);

            gbc.gridy++;
            gbc.gridwidth = 1;
            gbc.weightx = 0.0;
            gbc.fill = GridBagConstraints.NONE;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.insets = CHECKBOX_WIDTH_INSETS;
            add(urlLabel, gbc);

            gbc.gridx++;
            gbc.gridwidth = 2;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = NO_INSETS;
            add(urlField, gbc);

            gbc.gridx = 0;
            gbc.gridy++;
            gbc.gridwidth = 1;
            gbc.weightx = 0.0;
            gbc.fill = GridBagConstraints.NONE;
            gbc.insets = CHECKBOX_WIDTH_INSETS;
            add(cacheLabel, gbc);

            gbc.gridx++;
            gbc.insets = NO_INSETS;
            add(cacheSpinner, gbc);

            gbc.gridx++;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.WEST;
            add(cacheUnitLabel, gbc);

            gbc.gridx = 0;
            gbc.gridy++;
            gbc.gridwidth = 3;
            gbc.weighty = 1.0;
            gbc.fill = GridBagConstraints.BOTH;
            add(new JPanel(), gbc);

            updateAllOptions();
        }

        private void onLocateObserversChanged() {
            boolean newValue = locateObserversCheckBox.isSelected();

            Configuration config = Main.getConfiguration();
            boolean oldValue = config.shouldLocateObserverByTransceivers();
            if (oldValue == newValue) {
                return;
            }

            LOGGER.debug("setting location of observers via online transceivers to {}", newValue);
            Main.getConfiguration().setShouldLocateObserverByTransceivers(newValue);
        }

        private void onDefaultChanged() {
            boolean newValue = !defaultCheckBox.isSelected();

            Configuration config = Main.getConfiguration();
            boolean oldValue = config.isOnlineTransceiversOverrideEnabled();
            if (oldValue == newValue) {
                return;
            }

            LOGGER.debug("setting override of online transceivers to {}", newValue);
            Main.getConfiguration().setOnlineTransceiversOverrideEnabled(newValue);

            updateAllOptions();
        }

        private void onCacheSpinnerChanged() {
            int newValue = (int) cacheSpinner.getValue();

            Configuration config = Main.getConfiguration();
            int oldValue = config.getOnlineTransceiversOverrideCacheMinutes();
            if (oldValue == newValue) {
                return;
            }

            LOGGER.debug("setting overridden online transceivers cache lifetime to {} minutes", newValue);
            try {
                config.setOnlineTransceiversOverrideCacheMinutes(newValue);
            } catch (IllegalArgumentException ex) {
                LOGGER.warn("invalid overridden online transceivers cache lifetime requested: {}", newValue, ex);
            }

            updateAllOptions();
        }

        private void onUrlFieldChanged() {
            String newValue = urlField.getText();

            Configuration config = Main.getConfiguration();
            String oldValue = config.getOnlineTransceiversOverrideUrl();
            if (oldValue.equals(newValue) || newValue.trim().isEmpty()) {
                return;
            }

            LOGGER.debug("setting overridden online transceivers URL to \"{}\"", newValue);
            try {
                config.setOnlineTransceiversOverrideUrl(newValue);
            } catch (IllegalArgumentException ex) {
                LOGGER.warn("invalid overridden online transceivers URL requested: {}", newValue, ex);
            }

            updateAllOptions();
        }

        private void updateAllOptions() {
            Configuration config = Main.getConfiguration();

            boolean isActive = config.getStationLocatorStrategy().enablesOnlineTransceivers();
            if (isActive) {
                setBorder(BorderFactory.createTitledBorder(TITLE));
            } else {
                setBorder(BorderFactory.createTitledBorder(TITLE + TITLE_POSTFIX_NOT_ACTIVE));
            }

            locateObserversCheckBox.setSelected(config.shouldLocateObserverByTransceivers());

            boolean shouldUseOverride = config.isOnlineTransceiversOverrideEnabled();
            defaultCheckBox.setSelected(!shouldUseOverride);

            urlField.setText(config.getOnlineTransceiversOverrideUrl());
            cacheSpinner.setValue(config.getOnlineTransceiversOverrideCacheMinutes());

            urlLabel.setEnabled(shouldUseOverride);
            urlField.setEnabled(shouldUseOverride);
            cacheLabel.setEnabled(shouldUseOverride);
            cacheSpinner.setEnabled(shouldUseOverride);
            cacheUnitLabel.setEnabled(shouldUseOverride);
        }
    }

}
