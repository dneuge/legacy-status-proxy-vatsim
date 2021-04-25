package de.energiequant.vatsim.compatibility.legacyproxy.server.stationlocator;

import java.io.File;
import java.time.Duration;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.energiequant.vatsim.compatibility.legacyproxy.Main;
import de.energiequant.vatsim.compatibility.legacyproxy.server.stationlocator.Cache.Entry;

public class StationLocator {
    private static final Logger LOGGER = LoggerFactory.getLogger(StationLocator.class);

    private final Cache<String, Station, Source> cache = new Cache<>(CACHE_MAINTENANCE_INTERVAL);

    private static final Duration CACHE_MAINTENANCE_INTERVAL = Duration.ofMinutes(5);
    private static final Duration POSITIVE_RESULT_TIMEOUT = Duration.ofMinutes(10);

    private final VatSpyStationLocator vatSpyStationLocator;

    public static enum Source {
        VATSPY,
        NONE;
    }

    public static enum Strategy {
        DISABLE(
            "disabled",
            "do not locate stations" //
        ),
        ONLY_VATSPY(
            "vatspy",
            "locate only from static VAT-Spy data" //
        ),
        FIRST_VATSPY_THEN_TRANSCEIVERS(
            "vatspyAndTransceivers",
            "locate from static VAT-Spy data, then complete through online transceivers" //
        ),
        ONLY_TRANSCEIVERS(
            "transceivers",
            "locate only through online transceivers" //
        );

        private final String configValue;
        private final String description;

        private Strategy(String configValue, String description) {
            this.configValue = configValue;
            this.description = description;
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
    }

    public StationLocator() {
        this.vatSpyStationLocator = initializeVatSpyStationLocator();
    }

    private VatSpyStationLocator initializeVatSpyStationLocator() {
        // FIXME: add option to disable VAT-Spy lookup
        // FIXME: add options to configuration dialog
        File vatSpyBaseDir = Main.getConfiguration().getVatSpyBaseDirectory().orElse(null);
        boolean shouldFailover = true; // FIXME: configure

        VatSpyStationLocator vatSpyStationLocator = null;
        if (vatSpyBaseDir != null) {
            LOGGER.info("Loading external VAT-Spy data from {}", vatSpyBaseDir.getAbsolutePath());

            try {
                return new VatSpyStationLocator(vatSpyBaseDir);
            } catch (Exception ex) {
                LOGGER.warn("Failed to load external VAT-Spy data from {}", vatSpyBaseDir.getAbsolutePath(), ex);
            }

            if (!shouldFailover) {
                LOGGER.warn("Failover to included VAT-Spy data has been disabled, VAT-Spy data will not be used.");
                return null;
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

    public Optional<Station> locate(String callsign) {
        Entry<String, Station, Source> cached = cache.get(callsign).orElse(null);
        if (cached != null) {
            LOGGER.trace("location for \"{}\" was available from cache, source: {}", callsign, cached.getMetaData());
            return Optional.of(cached.getContent());
        }

        Station station = null;

        if (vatSpyStationLocator != null) {
            station = vatSpyStationLocator.locate(callsign).orElse(null);
            if (station != null) {
                LOGGER.trace("location for \"{}\" was available from VATSpy data, caching", callsign);
                cache.add(callsign, station, Source.VATSPY, POSITIVE_RESULT_TIMEOUT);
                return Optional.of(station);
            }
        }

        // TODO: negative cache?

        return Optional.empty();
    }
}
