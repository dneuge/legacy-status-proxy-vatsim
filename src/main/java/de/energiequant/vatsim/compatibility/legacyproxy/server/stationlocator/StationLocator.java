package de.energiequant.vatsim.compatibility.legacyproxy.server.stationlocator;

import java.io.File;
import java.time.Duration;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vatplanner.dataformats.vatsimpublic.entities.status.ControllerRating;
import org.vatplanner.dataformats.vatsimpublic.parser.Client;
import org.vatplanner.dataformats.vatsimpublic.parser.ClientType;
import org.vatplanner.dataformats.vatsimpublic.parser.DataFile;

import de.energiequant.vatsim.compatibility.legacyproxy.Configuration;
import de.energiequant.vatsim.compatibility.legacyproxy.Main;
import de.energiequant.vatsim.compatibility.legacyproxy.fetching.OnlineTransceiversFileFetcher;
import de.energiequant.vatsim.compatibility.legacyproxy.server.stationlocator.Cache.Entry;

public class StationLocator {
    private static final Logger LOGGER = LoggerFactory.getLogger(StationLocator.class);

    private final Cache<String, Station, Source> cache = new Cache<>(CACHE_MAINTENANCE_INTERVAL);

    private static final Duration CACHE_MAINTENANCE_INTERVAL = Duration.ofMinutes(5);
    private static final Duration POSITIVE_RESULT_TIMEOUT = Duration.ofMinutes(10);

    private final VatSpyStationLocator vatSpyStationLocator;
    private final OnlineTransceiversStationLocator onlineTransceiversStationLocator;

    private final Configuration config = Main.getConfiguration();
    private final Strategy strategy = config.getStationLocatorStrategy();
    private final boolean shouldIdentifyObserverByCallsign = config.shouldIdentifyObserverByCallsign();
    private final boolean shouldWarnAboutUnlocatableATC = config.shouldWarnAboutUnlocatableATC();
    private final boolean shouldWarnAboutUnlocatableObserver = config.shouldWarnAboutUnlocatableObserver();
    private final boolean shouldLocateObserverByVatSpy = config.shouldLocateObserverByVatSpy();
    private final boolean shouldLocateObserverByTransceivers = config.shouldLocateObserverByTransceivers();
    private final boolean shouldLocateObserver = shouldLocateObserverByTransceivers || shouldLocateObserverByVatSpy;
    private final boolean shouldIgnorePlaceholderFrequency = config.shouldIgnorePlaceholderFrequency();

    public static enum Strategy {
        DISABLE(
            "disabled",
            "do not locate stations",
            false,
            false //
        ),
        ONLY_VATSPY(
            "vatspy",
            "locate only from static VAT-Spy data",
            true,
            false //
        ),
        FIRST_VATSPY_THEN_TRANSCEIVERS(
            "vatspyAndTransceivers",
            "locate from static VAT-Spy data, then complete through online transceivers",
            true,
            true //
        ),
        ONLY_TRANSCEIVERS(
            "transceivers",
            "locate only through online transceivers",
            false,
            true //
        );

        private final String configValue;
        private final String description;
        private final boolean enablesVatSpy;
        private final boolean enablesOnlineTransceivers;

        private Strategy(String configValue, String description, boolean enablesVatSpy, boolean enablesOnlineTransceivers) {
            this.configValue = configValue;
            this.description = description;
            this.enablesVatSpy = enablesVatSpy;
            this.enablesOnlineTransceivers = enablesOnlineTransceivers;
        }

        public String getDescription() {
            return description;
        }

        public String getConfigValue() {
            return configValue;
        }

        public static Optional<Strategy> byConfigValue(String configValue) {
            for (Strategy strategy : values()) {
                if (strategy.getConfigValue().equals(configValue)) {
                    return Optional.of(strategy);
                }
            }

            return Optional.empty();
        }

        public boolean enablesOnlineTransceivers() {
            return enablesOnlineTransceivers;
        }

        public boolean enablesVatSpy() {
            return enablesVatSpy;
        }
    }

    public StationLocator(OnlineTransceiversFileFetcher onlineTransceiversFileFetcher) {
        this.vatSpyStationLocator = strategy.enablesVatSpy()
            ? initializeVatSpyStationLocator()
            : null;

        this.onlineTransceiversStationLocator = new OnlineTransceiversStationLocator(onlineTransceiversFileFetcher);
    }

    private VatSpyStationLocator initializeVatSpyStationLocator() {
        Configuration config = Main.getConfiguration();
        boolean isExternalDirectoryEnabled = config.isVatSpyBaseDirectoryEnabled();
        if (isExternalDirectoryEnabled) {
            File vatSpyBaseDir = config.getVatSpyBaseDirectory().orElse(null);
            if (vatSpyBaseDir != null) {
                LOGGER.info("Loading external VAT-Spy data from {}", vatSpyBaseDir.getAbsolutePath());

                try {
                    return new VatSpyStationLocator(vatSpyBaseDir);
                } catch (Exception ex) {
                    LOGGER.warn(
                        "Failed to load external VAT-Spy data from {}, switching to included data",
                        vatSpyBaseDir.getAbsolutePath(), ex //
                    );
                }
            }
        }

        LOGGER.debug("Loading included VAT-Spy data");
        try {
            return new VatSpyStationLocator();
        } catch (Exception ex) {
            LOGGER.warn("Failed to load included VAT-Spy data", ex);
        }

        return null;
    }

    public Optional<Station> locate(String callsign, boolean isObserver) {
        Entry<String, Station, Source> cached = cache.get(callsign).orElse(null);
        if (cached != null) {
            LOGGER.trace("location for \"{}\" was available from cache, source: {}", callsign, cached.getMetaData());
            return Optional.of(cached.getContent());
        }

        Station station = null;

        boolean shouldLocateByVatSpy = (vatSpyStationLocator != null)
            && strategy.enablesVatSpy()
            && (!isObserver || shouldLocateObserverByVatSpy);

        if (shouldLocateByVatSpy) {
            station = vatSpyStationLocator.locate(callsign).orElse(null);
        }

        boolean shouldLocateByTransceivers = strategy.enablesOnlineTransceivers()
            && (!isObserver || shouldLocateObserverByTransceivers);

        if ((station == null) && shouldLocateByTransceivers) {
            station = onlineTransceiversStationLocator.locate(callsign).orElse(null);
        }

        if (station != null) {
            LOGGER.trace("location for \"{}\" was available from {}, caching", callsign, station.getSource());
            cache.add(callsign, station, station.getSource(), POSITIVE_RESULT_TIMEOUT);
            return Optional.of(station);
        }

        // TODO: negative cache?

        return Optional.empty();
    }

    public void injectTo(DataFile dataFile) {
        LOGGER.debug("Injecting station locations...");
        for (Client client : dataFile.getClients()) {
            ClientType clientType = client.getRawClientType();
            boolean isATC = (clientType == ClientType.ATC_CONNECTED) || (clientType == ClientType.ATIS);
            boolean isObserver = (client.getControllerRating() == ControllerRating.OBS);
            boolean hasLocation = !(Double.isNaN(client.getLatitude()) || Double.isNaN(client.getLongitude()));
            boolean hasActiveFrequency = (client
                .getServedFrequencyKilohertz() < Client.FREQUENCY_KILOHERTZ_PLACEHOLDER_MINIMUM //
            );

            if (shouldIdentifyObserverByCallsign && !isObserver) {
                String callsignUpperCase = client.getCallsign().toUpperCase();
                isObserver = callsignUpperCase.endsWith("_OBS") || callsignUpperCase.endsWith("-OBS");
            }

            boolean shouldLocate = isATC
                && (!isObserver || shouldLocateObserver)
                && !hasLocation
                && (hasActiveFrequency || !shouldIgnorePlaceholderFrequency);
            if (!shouldLocate) {
                continue;
            }

            String callsign = client.getCallsign();
            Station station = locate(callsign, isObserver).orElse(null);

            if (station == null) {
                if (isObserver) {
                    if (shouldWarnAboutUnlocatableObserver) {
                        LOGGER.warn("observer station {} could not be located", callsign);
                    }
                } else if (isATC && shouldWarnAboutUnlocatableATC) {
                    LOGGER.warn("ATC station {} could not be located", callsign);
                }
            } else {
                double latitude = station.getLatitude();
                double longitude = station.getLongitude();

                LOGGER.debug("injecting location for {} ({}: {} / {})", callsign, station.getSource(), latitude,
                    longitude);
                client.setLatitude(latitude);
                client.setLongitude(longitude);
            }
        }
    }

    /**
     * Returns true if VAT-Spy data source is used to locate stations, false if not.
     * 
     * @return true if VAT-Spy data source is used to locate stations, false if not
     */
    public boolean usesVatSpySource() {
        return (vatSpyStationLocator != null);
    }

    /**
     * Returns true if VAT-Spy data source is used to locate stations and data has
     * been loaded from an external source, false if either unavailable or internal
     * data is used.
     * 
     * @return true if VAT-Spy data source is used to locate stations and data has
     *         been loaded from an external source, false if either unavailable or
     *         internal data is used
     * @see VatSpyStationLocator#usesExternalDataSource()
     * @see #usesVatSpySource()
     */
    public boolean isVatSpySourceExternal() {
        return (vatSpyStationLocator != null) && vatSpyStationLocator.usesExternalDataSource();
    }
}
