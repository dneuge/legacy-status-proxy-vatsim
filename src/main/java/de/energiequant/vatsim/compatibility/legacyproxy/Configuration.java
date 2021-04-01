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
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.energiequant.vatsim.compatibility.legacyproxy.server.IPFilter;

public class Configuration {
    private static final Logger LOGGER = LoggerFactory.getLogger(Configuration.class);

    private final File configFile;

    private static final boolean DEFAULT_DISCLAIMER_ACCEPTED = false;
    private static final boolean DEFAULT_QUIRK_LEGACY_DATAFILE_UTF8 = false;
    private static final String DEFAULT_UPSTREAM_BASE_URL = "http://status.vatsim.net";
    private static final String DEFAULT_LOCAL_HOST_NAME = "localhost";
    private static final int DEFAULT_SERVER_PORT = 8080;
    private static final Collection<String> DEFAULT_ALLOWED_IPS = Arrays.asList( //
        IPFilter.LOCALHOST_IPV4, //
        IPFilter.LOCALHOST_IPV6 //
    );

    private final AtomicBoolean isDisclaimerAccepted = new AtomicBoolean(DEFAULT_DISCLAIMER_ACCEPTED);
    private final AtomicBoolean isQuirkLegacyDataFileUtf8Enabled = new AtomicBoolean(
        DEFAULT_QUIRK_LEGACY_DATAFILE_UTF8 //
    );
    private final AtomicReference<String> upstreamBaseUrl = new AtomicReference<>(DEFAULT_UPSTREAM_BASE_URL);
    private final AtomicReference<String> localHostName = new AtomicReference<>(DEFAULT_LOCAL_HOST_NAME);
    private final AtomicInteger serverPort = new AtomicInteger(DEFAULT_SERVER_PORT);
    private final Set<String> allowedIps = Collections.synchronizedSet(new HashSet<>(DEFAULT_ALLOWED_IPS));

    private final Set<Runnable> disclaimerListeners = Collections.synchronizedSet(new HashSet<>());

    private static final String KEY_DISCLAIMER_ACCEPTED = "disclaimerAccepted";
    private static final String KEY_LOCAL_HOST_NAME = "localHostName";
    private static final String KEY_QUIRK_LEGACY_DATAFILE_UTF8 = "quirks.datafile.legacy.UTF8";
    private static final String KEY_SERVER_PORT = "serverPort";
    private static final String KEY_UPSTREAM_BASE_URL = "upstreamBaseUrl";
    private static final String BASEKEY_ALLOWED_IPS = "allowedIps.";

    public static class LoadingFailed extends Exception {
        private LoadingFailed(File configFile, Throwable cause) {
            super("Configuration could not be loaded from " + configFile.getAbsolutePath(), cause);
        }
    }

    public Configuration(File configFile) throws LoadingFailed {
        this.configFile = configFile;
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

        setDisclaimerAccepted(readBoolean(properties, KEY_DISCLAIMER_ACCEPTED, DEFAULT_DISCLAIMER_ACCEPTED));
        setQuirkLegacyDataFileUtf8Enabled(
            readBoolean(properties, KEY_QUIRK_LEGACY_DATAFILE_UTF8, DEFAULT_QUIRK_LEGACY_DATAFILE_UTF8) //
        );
        setUpstreamBaseUrl(readString(properties, KEY_UPSTREAM_BASE_URL, DEFAULT_UPSTREAM_BASE_URL));
        setLocalHostName(readString(properties, KEY_LOCAL_HOST_NAME, DEFAULT_LOCAL_HOST_NAME));
        setServerPort(readInteger(properties, KEY_SERVER_PORT, DEFAULT_SERVER_PORT));
        setAllowedIps(readStringsFromMultipleKeys(properties, BASEKEY_ALLOWED_IPS, DEFAULT_ALLOWED_IPS));

        logConfig();
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

    private void logConfig() {
        LOGGER.debug("Configuration file path: {}", configFile.getAbsolutePath());
        LOGGER.debug("Configured disclaimer accepted: {}", isDisclaimerAccepted.get());
        LOGGER.debug("Configured upstream base URL: {}", upstreamBaseUrl.get());
        LOGGER.debug("Configured local host name: {}", localHostName.get());
        LOGGER.debug("Configured quirk to enable UTF8 for legacy datafile: {}", isQuirkLegacyDataFileUtf8Enabled.get());
        LOGGER.debug("Configured server port: {}", serverPort.get());
        LOGGER.debug("Configured allowed IPs:\n{}", allowedIps.stream().collect(Collectors.joining("\n")));
    }

    public void save() {
        Properties properties = new Properties();
        properties.setProperty(KEY_DISCLAIMER_ACCEPTED, Boolean.toString(isDisclaimerAccepted.get()));
        properties.setProperty(KEY_LOCAL_HOST_NAME, localHostName.get());
        properties.setProperty(KEY_QUIRK_LEGACY_DATAFILE_UTF8,
            Boolean.toString(isQuirkLegacyDataFileUtf8Enabled.get()) //
        );
        properties.setProperty(KEY_SERVER_PORT, Integer.toString(serverPort.get()));
        properties.setProperty(KEY_UPSTREAM_BASE_URL, upstreamBaseUrl.get());

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
    }

    public void setDisclaimerAccepted(boolean isDisclaimerAccepted) {
        boolean previous = this.isDisclaimerAccepted.getAndSet(isDisclaimerAccepted);
        if (previous != isDisclaimerAccepted) {
            notifyListeners(disclaimerListeners);
        }
    }

    public void setQuirkLegacyDataFileUtf8Enabled(boolean isQuirkLegacyDataFileUtf8Enabled) {
        this.isQuirkLegacyDataFileUtf8Enabled.set(isQuirkLegacyDataFileUtf8Enabled);
    }

    public void setLocalHostName(String localHostName) {
        requireData("local host name", localHostName);
        this.localHostName.set(localHostName);
    }

    public void setServerPort(int serverPort) {
        if (serverPort <= 0) {
            throw new IllegalArgumentException("Invalid server port: " + serverPort);
        }

        this.serverPort.set(serverPort);
    }

    public void setUpstreamBaseUrl(String upstreamBaseUrl) {
        upstreamBaseUrl = removeTrailingSlashes(upstreamBaseUrl);
        requireData("upstream base URL", upstreamBaseUrl);

        this.upstreamBaseUrl.set(upstreamBaseUrl);
    }

    public Set<String> getAllowedIps() {
        return new HashSet<>(allowedIps);
    }

    public boolean isDisclaimerAccepted() {
        return isDisclaimerAccepted.get();
    }

    public boolean isQuirkLegacyDataFileUtf8Enabled() {
        return isQuirkLegacyDataFileUtf8Enabled.get();
    }

    public String getLocalHostName() {
        return localHostName.get();
    }

    public int getServerPort() {
        return serverPort.get();
    }

    public String getUpstreamBaseUrl() {
        return upstreamBaseUrl.get();
    }

    private String removeTrailingSlashes(String s) {
        return (s != null) ? s.replaceAll("/*$", "") : null;
    }

    public void addDisclaimerListener(Runnable listener) {
        disclaimerListeners.add(listener);
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

}
