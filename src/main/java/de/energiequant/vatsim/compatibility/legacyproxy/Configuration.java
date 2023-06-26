package de.energiequant.vatsim.compatibility.legacyproxy;

import static de.energiequant.vatsim.compatibility.legacyproxy.utils.ArgumentChecks.requireAtLeast;
import static de.energiequant.vatsim.compatibility.legacyproxy.utils.ArgumentChecks.requireData;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.energiequant.vatsim.compatibility.legacyproxy.server.IPFilter;
import de.energiequant.vatsim.compatibility.legacyproxy.server.stationlocator.StationLocator.Strategy;

public class Configuration {
    // TODO: switch from File to Path?

    private static final Logger LOGGER = LoggerFactory.getLogger(Configuration.class);

    private final File configFile;

    private static final String CURRENT_DISCLAIMER_HASH = DigestUtils.md5Hex(Main.getDisclaimer());

    private static final boolean DEFAULT_DISCLAIMER_ACCEPTED = false;
    private static final boolean DEFAULT_PARSER_LOG = false;
    private static final boolean DEFAULT_QUIRK_LEGACY_DATAFILE_UTF8 = false;
    private static final boolean DEFAULT_UPSTREAM_BASE_URL_OVERRIDDEN = false;
    private static final String DEFAULT_UPSTREAM_BASE_URL = "http://status.vatsim.net";
    private static final String DEFAULT_LOCAL_HOST_NAME = "localhost";
    public static final int DEFAULT_ONLINE_TRANSCEIVERS_OVERRIDE_CACHE_MINUTES = 15;
    private static final boolean DEFAULT_ONLINE_TRANSCEIVERS_OVERRIDE_ENABLED = false;
    private static final String DEFAULT_ONLINE_TRANSCEIVERS_OVERRIDE_URL = "https://data.vatsim.net/v3/transceivers-data.json";
    private static final int DEFAULT_SERVER_PORT = 8080;
    private static final boolean DEFAULT_STATION_LOCATOR_IGNORE_PLACEHOLDER_FREQUENCY = true;
    private static final boolean DEFAULT_STATION_LOCATOR_LOCATE_OBS_BY_TRANSCEIVERS = false;
    private static final boolean DEFAULT_STATION_LOCATOR_LOCATE_OBS_BY_VATSPY = true;
    private static final boolean DEFAULT_STATION_LOCATOR_OBSERVER_BY_CALLSIGN = true;
    private static final Strategy DEFAULT_STATION_LOCATOR_STRATEGY = Strategy.FIRST_VATSPY_THEN_TRANSCEIVERS;
    private static final boolean DEFAULT_STATION_LOCATOR_VATSPY_ALIAS_US = true;
    private static final boolean DEFAULT_STATION_LOCATOR_WARN_UNLOCATABLE_ATC = false;
    private static final boolean DEFAULT_STATION_LOCATOR_WARN_UNLOCATABLE_OBS = false;
    private static final boolean DEFAULT_VATSPY_BASE_DIRECTORY_ENABLED = false;
    private static final boolean DEFAULT_VATSPY_WARN_OLD_INTEGRATED_DB = true;
    public static final Collection<String> DEFAULT_ALLOWED_IPS = Collections.unmodifiableCollection(Arrays.asList(
        IPFilter.LOCALHOST_IPV4,
        IPFilter.LOCALHOST_IPV6
    ));

    private final AtomicBoolean isDisclaimerAccepted = new AtomicBoolean(DEFAULT_DISCLAIMER_ACCEPTED);
    private final AtomicBoolean isQuirkLegacyDataFileUtf8Enabled = new AtomicBoolean(
        DEFAULT_QUIRK_LEGACY_DATAFILE_UTF8
    );
    private final AtomicBoolean isParserLogEnabled = new AtomicBoolean();
    private final AtomicReference<File> vatSpyBaseDirectory = new AtomicReference<>(null);
    private final AtomicBoolean isVatSpyBaseDirectoryEnabled = new AtomicBoolean(DEFAULT_VATSPY_BASE_DIRECTORY_ENABLED);
    private final AtomicReference<String> upstreamBaseUrlOverride = new AtomicReference<>(DEFAULT_UPSTREAM_BASE_URL);
    private final AtomicBoolean isUpstreamBaseUrlOverridden = new AtomicBoolean(DEFAULT_UPSTREAM_BASE_URL_OVERRIDDEN);
    private final AtomicReference<String> localHostName = new AtomicReference<>(DEFAULT_LOCAL_HOST_NAME);
    private final AtomicInteger serverPort = new AtomicInteger(DEFAULT_SERVER_PORT);
    private final Set<String> allowedIps = Collections.synchronizedSet(new HashSet<>(DEFAULT_ALLOWED_IPS));

    private final Set<Runnable> disclaimerListeners = Collections.synchronizedSet(new HashSet<>());
    private final Set<Runnable> ipFilterListeners = Collections.synchronizedSet(new HashSet<>());

    private final AtomicInteger onlineTransceiversOverrideCacheMinutes = new AtomicInteger(
        DEFAULT_ONLINE_TRANSCEIVERS_OVERRIDE_CACHE_MINUTES
    );
    private final AtomicBoolean isOnlineTransceiversOverrideEnabled = new AtomicBoolean(
        DEFAULT_ONLINE_TRANSCEIVERS_OVERRIDE_ENABLED
    );
    private final AtomicReference<String> onlineTransceiversOverrideUrl = new AtomicReference<>(
        DEFAULT_ONLINE_TRANSCEIVERS_OVERRIDE_URL
    );

    private final AtomicReference<Strategy> stationLocatorStrategy = new AtomicReference<>(
        DEFAULT_STATION_LOCATOR_STRATEGY
    );
    private final AtomicBoolean shouldIgnorePlaceholderFrequency = new AtomicBoolean(
        DEFAULT_STATION_LOCATOR_IGNORE_PLACEHOLDER_FREQUENCY
    );
    private final AtomicBoolean shouldIdentifyObserverByCallsign = new AtomicBoolean(
        DEFAULT_STATION_LOCATOR_OBSERVER_BY_CALLSIGN
    );
    private final AtomicBoolean shouldWarnAboutOldIntegratedVatSpyDatabase = new AtomicBoolean(
        DEFAULT_VATSPY_WARN_OLD_INTEGRATED_DB
    );
    private final AtomicBoolean shouldLocateObserverByTransceivers = new AtomicBoolean(
        DEFAULT_STATION_LOCATOR_LOCATE_OBS_BY_TRANSCEIVERS
    );
    private final AtomicBoolean shouldLocateObserverByVatSpy = new AtomicBoolean(
        DEFAULT_STATION_LOCATOR_LOCATE_OBS_BY_VATSPY
    );
    private final AtomicBoolean shouldVatSpyAliasUSStations = new AtomicBoolean(
        DEFAULT_STATION_LOCATOR_VATSPY_ALIAS_US
    );
    private final AtomicBoolean shouldWarnAboutUnlocatableATC = new AtomicBoolean(
        DEFAULT_STATION_LOCATOR_WARN_UNLOCATABLE_ATC
    );
    private final AtomicBoolean shouldWarnAboutUnlocatableObserver = new AtomicBoolean(
        DEFAULT_STATION_LOCATOR_WARN_UNLOCATABLE_OBS
    );

    private static final String KEY_DISCLAIMER_ACCEPTED = "disclaimerAccepted";
    private static final String KEY_LOCAL_HOST_NAME = "localHostName";
    private static final String KEY_PARSER_LOG = "parserLog";
    private static final String KEY_QUIRK_LEGACY_DATAFILE_UTF8 = "quirks.datafile.legacy.UTF8";
    private static final String KEY_ONLINE_TRANSCEIVERS_OVERRIDE_CACHE_MINUTES = "onlineTransceivers.override.cacheMinutes";
    private static final String KEY_ONLINE_TRANSCEIVERS_OVERRIDE_ENABLED = "onlineTransceivers.override.enabled";
    private static final String KEY_ONLINE_TRANSCEIVERS_OVERRIDE_URL = "onlineTransceivers.override.url";
    private static final String KEY_SERVER_PORT = "serverPort";
    private static final String KEY_STATION_LOCATOR_IGNORE_PLACEHOLDER_FREQUENCY = "stationLocator.ignorePlaceholderFrequency";
    private static final String KEY_STATION_LOCATOR_LOCATE_OBS_BY_TRANSCEIVERS = "stationLocator.locateOBS.onlineTransceivers";
    private static final String KEY_STATION_LOCATOR_LOCATE_OBS_BY_VATSPY = "stationLocator.locateOBS.vatSpy";
    private static final String KEY_STATION_LOCATOR_OBSERVER_BY_CALLSIGN = "stationLocator.assumeObserverByCallsign";
    private static final String KEY_STATION_LOCATOR_STRATEGY = "stationLocator.strategy";
    private static final String KEY_STATION_LOCATOR_VATSPY_ALIAS_US = "stationLocator.vatSpy.aliasUS";
    private static final String KEY_STATION_LOCATOR_WARN_UNLOCATABLE_ATC = "stationLocator.warnUnlocatableATC";
    private static final String KEY_STATION_LOCATOR_WARN_UNLOCATABLE_OBS = "stationLocator.warnUnlocatableOBS";
    private static final String KEY_UPSTREAM_BASE_URL_OVERRIDE = "upstreamBaseUrl";
    private static final String KEY_UPSTREAM_BASE_URL_OVERRIDDEN = "upstreamBaseUrl.overrideEnabled";
    private static final String KEY_VATSPY_BASE_DIRECTORY = "vatSpy.external.directory";
    private static final String KEY_VATSPY_BASE_DIRECTORY_ENABLED = "vatSpy.external.enabled";
    private static final String KEY_VATSPY_WARN_OLD_INTEGRATED_DB = "vatSpy.warnOldIntegratedDB";
    private static final String BASEKEY_ALLOWED_IPS = "allowedIps.";

    public static final int SERVER_PORT_MINIMUM = 1;
    public static final int SERVER_PORT_MAXIMUM = 65535;

    private final boolean isSaneLocation;

    public static class LoadingFailed extends Exception {
        private LoadingFailed(File configFile, Throwable cause) {
            super("Configuration could not be loaded from " + configFile.getAbsolutePath(), cause);
        }
    }

    public Configuration(File configFile) throws LoadingFailed {
        this.configFile = configFile;
        this.isSaneLocation = !isSystemPath(configFile);

        if (!isSaneLocation) {
            LOGGER.warn(
                "The configuration file appears to be located at an unsafe location such as a system directory. "
                    + "Saving configurations to such locations is not supported and has been disabled. "
                    + "This can happen if you run the proxy from \"recent files\" or similar on Windows. "
                    + "Please make sure to run the proxy from a regular working directory or a shortcut."
            );
        }

        LOGGER.debug("Current disclaimer hash is: {}", CURRENT_DISCLAIMER_HASH);
        load();
    }

    public void load() throws LoadingFailed {
        if (!(configFile.exists() && configFile.canRead())) {
            LOGGER.error("Configuration cannot be loaded from {} because the file is missing or not readable",
                         configFile.getAbsolutePath()
            );
            return;
        }

        LOGGER.info("Loading configuration from {}", configFile.getAbsolutePath());
        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream(configFile)) {
            properties.load(fis);
        } catch (IOException ex) {
            throw new LoadingFailed(configFile, ex);
        }

        setDisclaimerAccepted(CURRENT_DISCLAIMER_HASH.equals(readString(properties, KEY_DISCLAIMER_ACCEPTED, "")));
        setQuirkLegacyDataFileUtf8Enabled(
            readBoolean(properties, KEY_QUIRK_LEGACY_DATAFILE_UTF8, DEFAULT_QUIRK_LEGACY_DATAFILE_UTF8)
        );
        setParserLogEnabled(
            readBoolean(properties, KEY_PARSER_LOG, DEFAULT_PARSER_LOG)
        );
        setUpstreamBaseUrl(readString(properties, KEY_UPSTREAM_BASE_URL_OVERRIDE, DEFAULT_UPSTREAM_BASE_URL));
        setLocalHostName(readString(properties, KEY_LOCAL_HOST_NAME, DEFAULT_LOCAL_HOST_NAME));
        setServerPort(readInteger(properties, KEY_SERVER_PORT, DEFAULT_SERVER_PORT));
        setAllowedIps(readStringsFromMultipleKeys(properties, BASEKEY_ALLOWED_IPS, DEFAULT_ALLOWED_IPS));

        setVatSpyBaseDirectory(readFile(properties, KEY_VATSPY_BASE_DIRECTORY, ""));
        setVatSpyBaseDirectoryEnabled(
            readBoolean(properties, KEY_VATSPY_BASE_DIRECTORY_ENABLED, DEFAULT_VATSPY_BASE_DIRECTORY_ENABLED)
        );
        setShouldWarnAboutOldIntegratedVatSpyDatabase(readBoolean(
            properties,
            KEY_VATSPY_WARN_OLD_INTEGRATED_DB,
            DEFAULT_VATSPY_WARN_OLD_INTEGRATED_DB
        ));

        setOnlineTransceiversOverrideEnabled(readBoolean(
            properties,
            KEY_ONLINE_TRANSCEIVERS_OVERRIDE_ENABLED,
            DEFAULT_ONLINE_TRANSCEIVERS_OVERRIDE_ENABLED
        ));
        setOnlineTransceiversOverrideCacheMinutes(readInteger(
            properties,
            KEY_ONLINE_TRANSCEIVERS_OVERRIDE_CACHE_MINUTES,
            DEFAULT_ONLINE_TRANSCEIVERS_OVERRIDE_CACHE_MINUTES
        ));
        setOnlineTransceiversOverrideUrl(readString(
            properties,
            KEY_ONLINE_TRANSCEIVERS_OVERRIDE_URL,
            DEFAULT_ONLINE_TRANSCEIVERS_OVERRIDE_URL
        ));

        setStationLocatorStrategy(
            Strategy.byConfigValue(readString(
                properties,
                KEY_STATION_LOCATOR_STRATEGY,
                DEFAULT_STATION_LOCATOR_STRATEGY.getConfigValue()
            )).orElseThrow(() -> new IllegalArgumentException(
                "invalid station locator strategy: \"" +
                    readString(
                        properties,
                        KEY_STATION_LOCATOR_STRATEGY,
                        ""
                    ) +
                    "\""
            ))
        );

        setShouldIgnorePlaceholderFrequency(readBoolean(
            properties,
            KEY_STATION_LOCATOR_IGNORE_PLACEHOLDER_FREQUENCY,
            DEFAULT_STATION_LOCATOR_IGNORE_PLACEHOLDER_FREQUENCY
        ));
        setShouldIdentifyObserverByCallsign(readBoolean(
            properties,
            KEY_STATION_LOCATOR_OBSERVER_BY_CALLSIGN,
            DEFAULT_STATION_LOCATOR_OBSERVER_BY_CALLSIGN
        ));
        setShouldLocateObserverByTransceivers(readBoolean(
            properties,
            KEY_STATION_LOCATOR_LOCATE_OBS_BY_TRANSCEIVERS,
            DEFAULT_STATION_LOCATOR_LOCATE_OBS_BY_TRANSCEIVERS
        ));
        setShouldLocateObserverByVatSpy(readBoolean(
            properties,
            KEY_STATION_LOCATOR_LOCATE_OBS_BY_VATSPY,
            DEFAULT_STATION_LOCATOR_LOCATE_OBS_BY_VATSPY
        ));
        setShouldVatSpyAliasUSStations(readBoolean(
            properties,
            KEY_STATION_LOCATOR_VATSPY_ALIAS_US,
            DEFAULT_STATION_LOCATOR_VATSPY_ALIAS_US
        ));
        setShouldWarnAboutUnlocatableATC(readBoolean(
            properties,
            KEY_STATION_LOCATOR_WARN_UNLOCATABLE_ATC,
            DEFAULT_STATION_LOCATOR_WARN_UNLOCATABLE_ATC
        ));
        setShouldWarnAboutUnlocatableObserver(readBoolean(
            properties,
            KEY_STATION_LOCATOR_WARN_UNLOCATABLE_OBS,
            DEFAULT_STATION_LOCATOR_WARN_UNLOCATABLE_OBS
        ));

        // migration: keep custom URL if previously set, guard override if default URL
        // was used before
        setUpstreamBaseUrlOverridden(readBoolean(
            properties,
            KEY_UPSTREAM_BASE_URL_OVERRIDDEN,
            !DEFAULT_UPSTREAM_BASE_URL.equals(getUpstreamBaseUrlOverride())
        ));

        logConfig();
    }

    private boolean isSystemPath(File file) {
        String sysRoot = System.getenv("SystemRoot");
        if ((sysRoot == null) || sysRoot.isEmpty()) {
            return false;
        }

        sysRoot = sysRoot.toLowerCase();
        if (!sysRoot.endsWith(File.separator)) {
            sysRoot += File.separator;
        }

        return file.getAbsolutePath().toLowerCase().startsWith(sysRoot);
    }

    private Collection<String> readStringsFromMultipleKeys(Properties properties, String baseKey, Collection<String> defaultValues) {
        Collection<String> values = new ArrayList<>();
        for (Entry<Object, Object> entry : properties.entrySet()) {
            if (!((String) entry.getKey()).startsWith(baseKey)) {
                continue;
            }

            values.add((String) entry.getValue());
        }

        if (values.isEmpty()) {
            values = new ArrayList<>(defaultValues);
        }

        return values;
    }

    private boolean readBoolean(Properties properties, String key, boolean defaultValue) {
        return Boolean.parseBoolean(readString(properties, key, Boolean.toString(defaultValue)));
    }

    private int readInteger(Properties properties, String key, int defaultValue) {
        return Integer.parseInt(readString(properties, key, Integer.toString(defaultValue)));
    }

    private String readString(Properties properties, String key, String defaultValue) {
        return (String) properties.getOrDefault(key, defaultValue);
    }

    private File readFile(Properties properties, String key, String defaultValue) {
        String path = readString(properties, key, defaultValue);
        if (path.trim().isEmpty()) {
            return null;
        }

        return new File(path);
    }

    private void logConfig() {
        LOGGER.debug("Configuration file path:        {}", configFile.getAbsolutePath());
        LOGGER.debug("Configured disclaimer accepted: {}", isDisclaimerAccepted.get());
        LOGGER.debug(
            "Configured upstream base URL:   {} ({})",
            upstreamBaseUrlOverride.get(),
            isUpstreamBaseUrlOverridden.get() ? "active" : "not used"
        );
        LOGGER.debug("Configured local host name:     {}", localHostName.get());
        LOGGER.debug("Configured server port:         {}", serverPort.get());
        LOGGER.debug("Configured UTF8 quirk:          {}", isQuirkLegacyDataFileUtf8Enabled.get());
        LOGGER.debug(
            "Configured VAT-Spy ext basedir: {} ({})",
            vatSpyBaseDirectory.get(),
            isVatSpyBaseDirectoryEnabled.get() ? "enabled" : "not used"
        );
        LOGGER.debug(
            "Configured VAT-Spy int log old: {}",
            shouldWarnAboutOldIntegratedVatSpyDatabase.get()
        );
        LOGGER.debug("Configured parser log:          {}", isParserLogEnabled.get());
        LOGGER.debug("Configured allowed IPs:         {}", allowedIps);

        LOGGER.debug(
            "Configured xcvr URL:            {} ({})",
            onlineTransceiversOverrideUrl.get(),
            isOnlineTransceiversOverrideEnabled.get() ? "active" : "not used"
        );
        LOGGER.debug(
            "Configured xcvr cache lifetime: {} minutes ({})",
            onlineTransceiversOverrideCacheMinutes.get(),
            isOnlineTransceiversOverrideEnabled.get() ? "active" : "not used"
        );

        LOGGER.debug(
            "Configured SL strategy:         {}",
            stationLocatorStrategy.get().getDescription()
        );
        LOGGER.debug(
            "Configured SL ignore dummyfreq: {}",
            shouldIgnorePlaceholderFrequency.get()
        );
        LOGGER.debug(
            "Configured SL alias US/VAT-Spy: {}",
            shouldVatSpyAliasUSStations.get()
        );
        LOGGER.debug(
            "Configured SL OBS by callsign:  {}",
            shouldIdentifyObserverByCallsign.get()
        );
        LOGGER.debug(
            "Configured SL loc OBS/VAT-Spy:  {}",
            shouldLocateObserverByVatSpy.get()
        );
        LOGGER.debug(
            "Configured SL loc OBS/xcvr:     {}",
            shouldLocateObserverByTransceivers.get()
        );
        LOGGER.debug(
            "Configured SL warn unloc ATC:   {}",
            shouldWarnAboutUnlocatableATC.get()
        );
        LOGGER.debug(
            "Configured SL warn unloc OBS:   {}",
            shouldWarnAboutUnlocatableObserver.get()
        );
    }

    public void save() {
        if (!isSaneLocation) {
            LOGGER.warn("Configuration file is at an unsafe location and thus will not be saved: {}",
                        configFile.getAbsolutePath()
            );
            return;
        }

        Properties properties = new Properties();
        properties.setProperty(KEY_DISCLAIMER_ACCEPTED, isDisclaimerAccepted.get() ? CURRENT_DISCLAIMER_HASH : "");
        properties.setProperty(KEY_LOCAL_HOST_NAME, localHostName.get());
        properties.setProperty(KEY_PARSER_LOG, Boolean.toString(isParserLogEnabled.get()));
        properties.setProperty(KEY_QUIRK_LEGACY_DATAFILE_UTF8,
                               Boolean.toString(isQuirkLegacyDataFileUtf8Enabled.get())
        );
        properties.setProperty(KEY_SERVER_PORT, Integer.toString(serverPort.get()));
        properties.setProperty(KEY_UPSTREAM_BASE_URL_OVERRIDE, upstreamBaseUrlOverride.get());
        properties.setProperty(KEY_UPSTREAM_BASE_URL_OVERRIDDEN, Boolean.toString(isUpstreamBaseUrlOverridden.get()));

        properties.setProperty(
            KEY_VATSPY_BASE_DIRECTORY,
            getVatSpyBaseDirectory()
                .map(File::getAbsolutePath)
                .orElse("")
        );
        properties.setProperty(KEY_VATSPY_BASE_DIRECTORY_ENABLED, Boolean.toString(isVatSpyBaseDirectoryEnabled.get()));
        properties.setProperty(
            KEY_VATSPY_WARN_OLD_INTEGRATED_DB,
            Boolean.toString(shouldWarnAboutOldIntegratedVatSpyDatabase.get())
        );

        int i = 0;
        for (String allowedIp : allowedIps) {
            properties.setProperty(BASEKEY_ALLOWED_IPS + (i++), allowedIp);
        }

        properties.setProperty(
            KEY_ONLINE_TRANSCEIVERS_OVERRIDE_ENABLED,
            Boolean.toString(isOnlineTransceiversOverrideEnabled.get())
        );
        properties.setProperty(
            KEY_ONLINE_TRANSCEIVERS_OVERRIDE_CACHE_MINUTES,
            Integer.toString(onlineTransceiversOverrideCacheMinutes.get())
        );
        properties.setProperty(
            KEY_ONLINE_TRANSCEIVERS_OVERRIDE_URL,
            onlineTransceiversOverrideUrl.get()
        );

        properties.setProperty(
            KEY_STATION_LOCATOR_STRATEGY,
            stationLocatorStrategy.get().getConfigValue()
        );
        properties.setProperty(
            KEY_STATION_LOCATOR_IGNORE_PLACEHOLDER_FREQUENCY,
            Boolean.toString(shouldIgnorePlaceholderFrequency.get())
        );
        properties.setProperty(
            KEY_STATION_LOCATOR_OBSERVER_BY_CALLSIGN,
            Boolean.toString(shouldIdentifyObserverByCallsign.get())
        );
        properties.setProperty(
            KEY_STATION_LOCATOR_LOCATE_OBS_BY_TRANSCEIVERS,
            Boolean.toString(shouldLocateObserverByTransceivers.get())
        );
        properties.setProperty(
            KEY_STATION_LOCATOR_LOCATE_OBS_BY_VATSPY,
            Boolean.toString(shouldLocateObserverByVatSpy.get())
        );
        properties.setProperty(
            KEY_STATION_LOCATOR_VATSPY_ALIAS_US,
            Boolean.toString(shouldVatSpyAliasUSStations.get())
        );
        properties.setProperty(
            KEY_STATION_LOCATOR_WARN_UNLOCATABLE_ATC,
            Boolean.toString(shouldWarnAboutUnlocatableATC.get())
        );
        properties.setProperty(
            KEY_STATION_LOCATOR_WARN_UNLOCATABLE_OBS,
            Boolean.toString(shouldWarnAboutUnlocatableObserver.get())
        );

        if (configFile.exists() && !configFile.canWrite()) {
            LOGGER.error("Configuration cannot be saved because {} is not writable", configFile.getAbsolutePath());
            return;
        }
        try (FileOutputStream fos = new FileOutputStream(configFile)) {
            properties.store(fos, "");
        } catch (IOException ex) {
            LOGGER.error("Configuration could not be saved to {}", configFile.getAbsolutePath(), ex);
            return;
        }
        LOGGER.info("Configuration was saved to {}", configFile.getAbsolutePath());
    }

    public void setAllowedIps(Collection<String> allowedIps) {
        this.allowedIps.retainAll(allowedIps);
        this.allowedIps.addAll(allowedIps);

        notifyListeners(ipFilterListeners);
    }

    public void setDisclaimerAccepted(boolean isDisclaimerAccepted) {
        boolean previous = this.isDisclaimerAccepted.getAndSet(isDisclaimerAccepted);
        if (previous != isDisclaimerAccepted) {
            notifyListeners(disclaimerListeners);
        }
    }

    public void setOnlineTransceiversOverrideCacheMinutes(int minutes) {
        requireAtLeast("override online transceiver cache lifetime in minutes", minutes, 0);
        onlineTransceiversOverrideCacheMinutes.set(minutes);
    }

    public void setOnlineTransceiversOverrideEnabled(boolean enabled) {
        isOnlineTransceiversOverrideEnabled.set(enabled);
    }

    public void setOnlineTransceiversOverrideUrl(String url) {
        requireData("override online transceivers URL", url);
        onlineTransceiversOverrideUrl.set(url);
    }

    public void setParserLogEnabled(boolean isParserLogEnabled) {
        this.isParserLogEnabled.set(isParserLogEnabled);
    }

    public void setQuirkLegacyDataFileUtf8Enabled(boolean isQuirkLegacyDataFileUtf8Enabled) {
        this.isQuirkLegacyDataFileUtf8Enabled.set(isQuirkLegacyDataFileUtf8Enabled);
    }

    public void setLocalHostName(String localHostName) {
        requireData("local host name", localHostName);
        this.localHostName.set(localHostName);
    }

    public void setServerPort(int serverPort) {
        if ((serverPort < SERVER_PORT_MINIMUM) || (serverPort > SERVER_PORT_MAXIMUM)) {
            throw new IllegalArgumentException("Invalid server port: " + serverPort);
        }

        this.serverPort.set(serverPort);
    }

    public void setShouldIgnorePlaceholderFrequency(boolean shouldIgnore) {
        shouldIgnorePlaceholderFrequency.set(shouldIgnore);
    }

    public void setShouldIdentifyObserverByCallsign(boolean shouldIdentifyByCallsign) {
        shouldIdentifyObserverByCallsign.set(shouldIdentifyByCallsign);
    }

    public void setShouldLocateObserverByTransceivers(boolean shouldLocateByTransceivers) {
        shouldLocateObserverByTransceivers.set(shouldLocateByTransceivers);
    }

    public void setShouldLocateObserverByVatSpy(boolean shouldLocateByVatSpy) {
        shouldLocateObserverByVatSpy.set(shouldLocateByVatSpy);
    }

    public void setShouldVatSpyAliasUSStations(boolean shouldAlias) {
        shouldVatSpyAliasUSStations.set(shouldAlias);
    }

    public void setShouldWarnAboutOldIntegratedVatSpyDatabase(boolean shouldWarn) {
        shouldWarnAboutOldIntegratedVatSpyDatabase.set(shouldWarn);
    }

    public void setShouldWarnAboutUnlocatableATC(boolean shouldWarn) {
        shouldWarnAboutUnlocatableATC.set(shouldWarn);
    }

    public void setShouldWarnAboutUnlocatableObserver(boolean shouldWarn) {
        shouldWarnAboutUnlocatableObserver.set(shouldWarn);
    }

    public void setStationLocatorStrategy(Strategy strategy) {
        requireData("station locator strategy", strategy);
        stationLocatorStrategy.set(strategy);
    }

    public void setUpstreamBaseUrl(String upstreamBaseUrl) {
        upstreamBaseUrl = removeTrailingSlashes(upstreamBaseUrl);
        requireData("upstream base URL", upstreamBaseUrl);

        this.upstreamBaseUrlOverride.set(upstreamBaseUrl);
    }

    public void setUpstreamBaseUrlOverridden(boolean isUpstreamBaseUrlOverridden) {
        this.isUpstreamBaseUrlOverridden.set(isUpstreamBaseUrlOverridden);
    }

    public void setVatSpyBaseDirectory(File directory) {
        vatSpyBaseDirectory.set(directory);
    }

    public void setVatSpyBaseDirectoryEnabled(boolean enabled) {
        isVatSpyBaseDirectoryEnabled.set(enabled);
    }

    public void unsetVatSpyBaseDirectory() {
        vatSpyBaseDirectory.set(null);
    }

    public Set<String> getAllowedIps() {
        return new HashSet<>(allowedIps);
    }

    public boolean isDisclaimerAccepted() {
        return isDisclaimerAccepted.get();
    }

    public boolean isOnlineTransceiversOverrideEnabled() {
        return isOnlineTransceiversOverrideEnabled.get();
    }

    public boolean isParserLogEnabled() {
        return isParserLogEnabled.get();
    }

    public boolean isQuirkLegacyDataFileUtf8Enabled() {
        return isQuirkLegacyDataFileUtf8Enabled.get();
    }

    public boolean isUpstreamBaseUrlOverridden() {
        return isUpstreamBaseUrlOverridden.get();
    }

    public boolean isVatSpyBaseDirectoryEnabled() {
        return isVatSpyBaseDirectoryEnabled.get();
    }

    public String getLocalHostName() {
        return localHostName.get();
    }

    public int getOnlineTransceiversOverrideCacheMinutes() {
        return onlineTransceiversOverrideCacheMinutes.get();
    }

    public String getOnlineTransceiversOverrideUrl() {
        return onlineTransceiversOverrideUrl.get();
    }

    public int getServerPort() {
        return serverPort.get();
    }

    public Strategy getStationLocatorStrategy() {
        return stationLocatorStrategy.get();
    }

    public String getUpstreamBaseUrlOverride() {
        return upstreamBaseUrlOverride.get();
    }

    public String getUpstreamBaseUrl() {
        return isUpstreamBaseUrlOverridden() ? getUpstreamBaseUrlOverride() : DEFAULT_UPSTREAM_BASE_URL;
    }

    public Optional<File> getVatSpyBaseDirectory() {
        return Optional.ofNullable(vatSpyBaseDirectory.get());
    }

    public boolean shouldIgnorePlaceholderFrequency() {
        return shouldIgnorePlaceholderFrequency.get();
    }

    public boolean shouldIdentifyObserverByCallsign() {
        return shouldIdentifyObserverByCallsign.get();
    }

    public boolean shouldLocateObserverByTransceivers() {
        return shouldLocateObserverByTransceivers.get();
    }

    public boolean shouldLocateObserverByVatSpy() {
        return shouldLocateObserverByVatSpy.get();
    }

    public boolean shouldVatSpyAliasUSStations() {
        return shouldVatSpyAliasUSStations.get();
    }

    public boolean shouldWarnAboutOldIntegratedVatSpyDatabase() {
        return shouldWarnAboutOldIntegratedVatSpyDatabase.get();
    }

    public boolean shouldWarnAboutUnlocatableATC() {
        return shouldWarnAboutUnlocatableATC.get();
    }

    public boolean shouldWarnAboutUnlocatableObserver() {
        return shouldWarnAboutUnlocatableObserver.get();
    }

    private String removeTrailingSlashes(String s) {
        return (s != null) ? s.replaceAll("/*$", "") : null;
    }

    public void addDisclaimerListener(Runnable listener) {
        disclaimerListeners.add(listener);
    }

    public void addIPFilterListener(Runnable listener) {
        ipFilterListeners.add(listener);
    }

    private void notifyListeners(Set<Runnable> listeners) {
        Collection<Runnable> copy = new ArrayList<>(listeners);
        for (Runnable listener : copy) {
            try {
                listener.run();
            } catch (Exception ex) {
                LOGGER.warn("Notification of configuration listener failed", ex);
            }
        }
    }

    public boolean isSaneLocation() {
        return isSaneLocation;
    }
}
