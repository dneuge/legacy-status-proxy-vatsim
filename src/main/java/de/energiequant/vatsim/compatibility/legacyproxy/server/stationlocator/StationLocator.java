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

    public StationLocator() {
        // FIXME: add options to configuration dialog
        File vatSpyBaseDir = Main.getConfiguration().getVatSpyBaseDirectory().orElse(null);
        if (vatSpyBaseDir == null) {
            LOGGER.debug("VatSpy unavailable, no path set");
            vatSpyStationLocator = null;
        } else {
            vatSpyStationLocator = new VatSpyStationLocator(vatSpyBaseDir);
        }
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
