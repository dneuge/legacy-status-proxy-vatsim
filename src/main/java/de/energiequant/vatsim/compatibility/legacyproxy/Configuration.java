package de.energiequant.vatsim.compatibility.legacyproxy;

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

public class Configuration {
    private static final Logger LOGGER = LoggerFactory.getLogger(Configuration.class);

    private final File configFile;

    private static final String CURRENT_DISCLAIMER_HASH = DigestUtils.md5Hex(Main.getDisclaimer());

    private static final boolean DEFAULT_DISCLAIMER_ACCEPTED = false;
    private static final boolean DEFAULT_PARSER_LOG = false;
    private static final boolean DEFAULT_QUIRK_LEGACY_DATAFILE_UTF8 = false;
    private static final boolean DEFAULT_UPSTREAM_BASE_URL_OVERRIDDEN = false;
    private static final String DEFAULT_UPSTREAM_BASE_URL = "http://status.vatsim.net";
    private static final String DEFAULT_LOCAL_HOST_NAME = "localhost";
    private static final int DEFAULT_SERVER_PORT = 8080;
    public static final Collection<String> DEFAULT_ALLOWED_IPS = Collections.unmodifiableCollection(Arrays.asList( //
        IPFilter.LOCALHOST_IPV4, //
        IPFilter.LOCALHOST_IPV6 //
    ));

    private final AtomicBoolean isDisclaimerAccepted = new AtomicBoolean(DEFAULT_DISCLAIMER_ACCEPTED);
    private final AtomicBoolean isQuirkLegacyDataFileUtf8Enabled = new AtomicBoolean(
        DEFAULT_QUIRK_LEGACY_DATAFILE_UTF8 //
    );
    private final AtomicBoolean isParserLogEnabled = new AtomicBoolean();
    private final AtomicReference<File> vatSpyBaseDirectory = new AtomicReference<>(null);
    private final AtomicReference<String> upstreamBaseUrlOverride = new AtomicReference<>(DEFAULT_UPSTREAM_BASE_URL);
    private final AtomicBoolean isUpstreamBaseUrlOverridden = new AtomicBoolean(DEFAULT_UPSTREAM_BASE_URL_OVERRIDDEN);
    private final AtomicReference<String> localHostName = new AtomicReference<>(DEFAULT_LOCAL_HOST_NAME);
    private final AtomicInteger serverPort = new AtomicInteger(DEFAULT_SERVER_PORT);
    private final Set<String> allowedIps = Collections.synchronizedSet(new HashSet<>(DEFAULT_ALLOWED_IPS));

    private final Set<Runnable> disclaimerListeners = Collections.synchronizedSet(new HashSet<>());
    private final Set<Runnable> ipFilterListeners = Collections.synchronizedSet(new HashSet<>());

    private static final String KEY_DISCLAIMER_ACCEPTED = "disclaimerAccepted";
    private static final String KEY_LOCAL_HOST_NAME = "localHostName";
    private static final String KEY_PARSER_LOG = "parserLog";
    private static final String KEY_QUIRK_LEGACY_DATAFILE_UTF8 = "quirks.datafile.legacy.UTF8";
    private static final String KEY_SERVER_PORT = "serverPort";
    private static final String KEY_UPSTREAM_BASE_URL_OVERRIDE = "upstreamBaseUrl";
    private static final String KEY_UPSTREAM_BASE_URL_OVERRIDDEN = "upstreamBaseUrl.overrideEnabled";
    private static final String KEY_VATSPY_BASE_DIRECTORY = "vatSpyBaseDirectory";
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
                "The configuration file appears to be located at an unsafe location such as a system directory. " //
                    + "Saving configurations to such locations is not supported and has been disabled. " //
                    + "This can happen if you run the proxy from \"recent files\" or similar on Windows. " //
                    + "Please make sure to run the proxy from a regular working directory or a shortcut." //
            );
        }

        LOGGER.debug("Current disclaimer hash is: {}", CURRENT_DISCLAIMER_HASH);
        load();
    }

    public void load() throws LoadingFailed {
        if (!(configFile.exists() && configFile.canRead())) {
            LOGGER.error("Configuration cannot be loaded from {} because the file is missing or not readable",
                configFile.getAbsolutePath());
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
            readBoolean(properties, KEY_QUIRK_LEGACY_DATAFILE_UTF8, DEFAULT_QUIRK_LEGACY_DATAFILE_UTF8) //
        );
        setVatSpyBaseDirectory(readFile(properties, KEY_VATSPY_BASE_DIRECTORY, ""));
        setParserLogEnabled(
            readBoolean(properties, KEY_PARSER_LOG, DEFAULT_PARSER_LOG) //
        );
        setUpstreamBaseUrl(readString(properties, KEY_UPSTREAM_BASE_URL_OVERRIDE, DEFAULT_UPSTREAM_BASE_URL));
        setLocalHostName(readString(properties, KEY_LOCAL_HOST_NAME, DEFAULT_LOCAL_HOST_NAME));
        setServerPort(readInteger(properties, KEY_SERVER_PORT, DEFAULT_SERVER_PORT));
        setAllowedIps(readStringsFromMultipleKeys(properties, BASEKEY_ALLOWED_IPS, DEFAULT_ALLOWED_IPS));

        // migration: keep custom URL if previously set, guard override if default URL
        // was used before
        setUpstreamBaseUrlOverridden(readBoolean(
            properties,
            KEY_UPSTREAM_BASE_URL_OVERRIDDEN,
            !DEFAULT_UPSTREAM_BASE_URL.equals(getUpstreamBaseUrlOverride()) //
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
        // FIXME: may have issues with spaces or special chars in filenames

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
            isUpstreamBaseUrlOverridden.get() ? "active" : "not used" //
        );
        LOGGER.debug("Configured local host name:     {}", localHostName.get());
        LOGGER.debug("Configured server port:         {}", serverPort.get());
        LOGGER.debug("Configured UTF8 quirk:          {}", isQuirkLegacyDataFileUtf8Enabled.get());
        LOGGER.debug("Configured VAT-Spy base dir:    {}", vatSpyBaseDirectory.get());
        LOGGER.debug("Configured parser log:          {}", isParserLogEnabled.get());
        LOGGER.debug("Configured allowed IPs:         {}", allowedIps);
    }

    public void save() {
        if (!isSaneLocation) {
            LOGGER.warn("Configuration file is at an unsafe location and thus will not be saved: {}",
                configFile.getAbsolutePath());
            return;
        }

        Properties properties = new Properties();
        properties.setProperty(KEY_DISCLAIMER_ACCEPTED, isDisclaimerAccepted.get() ? CURRENT_DISCLAIMER_HASH : "");
        properties.setProperty(KEY_LOCAL_HOST_NAME, localHostName.get());
        properties.setProperty(KEY_PARSER_LOG, Boolean.toString(isParserLogEnabled.get()));
        properties.setProperty(KEY_QUIRK_LEGACY_DATAFILE_UTF8,
            Boolean.toString(isQuirkLegacyDataFileUtf8Enabled.get()) //
        );
        properties.setProperty(KEY_SERVER_PORT, Integer.toString(serverPort.get()));
        properties.setProperty(KEY_UPSTREAM_BASE_URL_OVERRIDE, upstreamBaseUrlOverride.get());
        properties.setProperty(KEY_UPSTREAM_BASE_URL_OVERRIDDEN, Boolean.toString(isUpstreamBaseUrlOverridden.get()));
        properties.setProperty(
            KEY_VATSPY_BASE_DIRECTORY,
            getVatSpyBaseDirectory()
                .map(File::getAbsolutePath)
                .orElse("") //
        );

        int i = 0;
        for (String allowedIp : allowedIps) {
            properties.setProperty(BASEKEY_ALLOWED_IPS + (i++), allowedIp);
        }

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

    public void unsetVatSpyBaseDirectory() {
        vatSpyBaseDirectory.set(null);
    }

    public Set<String> getAllowedIps() {
        return new HashSet<>(allowedIps);
    }

    public boolean isDisclaimerAccepted() {
        return isDisclaimerAccepted.get();
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

    public String getLocalHostName() {
        return localHostName.get();
    }

    public int getServerPort() {
        return serverPort.get();
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
