package de.energiequant.vatsim.compatibility.legacyproxy.server.stationlocator;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vatplanner.dataformats.vatsimpublic.parser.OnlineTransceiverStation;
import org.vatplanner.dataformats.vatsimpublic.parser.OnlineTransceiversFile;
import org.vatplanner.dataformats.vatsimpublic.parser.ParserLogEntry;
import org.vatplanner.dataformats.vatsimpublic.parser.ParserLogEntryCollector;
import org.vatplanner.dataformats.vatsimpublic.utils.GeoMath;
import org.vatplanner.dataformats.vatsimpublic.utils.GeoPoint2D;

import de.energiequant.vatsim.compatibility.legacyproxy.Main;
import de.energiequant.vatsim.compatibility.legacyproxy.fetching.OnlineTransceiversFileFetcher;

public class OnlineTransceiversStationLocator {
    private static final Logger LOGGER = LoggerFactory.getLogger(OnlineTransceiversStationLocator.class);

    private final OnlineTransceiversFileFetcher onlineTransceiversFileFetcher;
    private final boolean isParserLogEnabled;

    private static final Duration LOCAL_CACHE_LIFETIME = Duration.ofSeconds(5);
    private static final Duration FETCH_TIMEOUT = Duration.ofSeconds(10);

    private Instant cacheExpiration = Instant.now();
    private Map<String, List<OnlineTransceiverStation>> transceiverStationsByCallsign = new HashMap<>();

    public OnlineTransceiversStationLocator(OnlineTransceiversFileFetcher onlineTransceiversFileFetcher) {
        this.onlineTransceiversFileFetcher = onlineTransceiversFileFetcher;
        isParserLogEnabled = Main.getConfiguration().isParserLogEnabled();
    }

    private synchronized Map<String, List<OnlineTransceiverStation>> getCachedTransceiverStationsByCallsign() {
        maintainLocalCache();

        return transceiverStationsByCallsign;
    }

    private synchronized void maintainLocalCache() {
        if (Instant.now().isBefore(cacheExpiration)) {
            return;
        }

        LOGGER.debug("updating local cache");

        OnlineTransceiversFile file = onlineTransceiversFileFetcher.waitForOnlineTransceiversFile(FETCH_TIMEOUT)
            .orElse(null);

        if (file == null) {
            LOGGER.warn("Online transceivers are currently unavailable.");
        } else {
            logParserMessages(file);

            transceiverStationsByCallsign = file.getStations() //
                .stream() //
                .collect(Collectors.groupingBy(OnlineTransceiverStation::getCallsign));
        }

        cacheExpiration = Instant.now().plus(LOCAL_CACHE_LIFETIME);
    }

    public Optional<Station> locate(String callsign) {
        LOGGER.trace("locating \"{}\"", callsign);

        List<OnlineTransceiverStation> transceiverStations = getCachedTransceiverStationsByCallsign().get(callsign);
        if (transceiverStations == null) {
            LOGGER.trace("no transceivers for \"{}\"", callsign);
            return Optional.empty();
        }

        // first collect all average points per group of transceiver stations reported
        // for the given callsign (stations may be listed multiple times within the same
        // file)
        Set<GeoPoint2D> transceiverCenterPoints = new HashSet<>();

        for (OnlineTransceiverStation transceiverStation : transceiverStations) {
            Set<GeoPoint2D> transceiverPoints = transceiverStation.getTransceivers()
                .stream()
                .map(transceiver -> new GeoPoint2D(transceiver.getLatitude(), transceiver.getLongitude()))
                .collect(Collectors.toSet());

            if (!transceiverPoints.isEmpty()) {
                GeoPoint2D transceiverGroupCenterPoint = GeoMath.average(transceiverPoints);
                transceiverCenterPoints.add(transceiverGroupCenterPoint);

                LOGGER.trace(
                    "calculated center point for {} is {}, source: {}",
                    callsign,
                    transceiverGroupCenterPoint,
                    transceiverPoints //
                );
            }
        }

        if (transceiverCenterPoints.isEmpty()) {
            LOGGER.debug("\"{}\" was listed but actually has no transceiver locations", callsign);
            return Optional.empty();
        }

        // average all group points
        GeoPoint2D stationCenterPoint = GeoMath.average(transceiverCenterPoints);
        if (transceiverCenterPoints.size() > 1) {
            LOGGER.trace(
                "\"{}\" had multiple center points, averaged to {}, input: {}",
                callsign,
                stationCenterPoint,
                transceiverCenterPoints //
            );
        }

        return Optional.of(
            new Station(
                callsign,
                stationCenterPoint.getLatitude(), stationCenterPoint.getLongitude(),
                Source.TRANSCEIVERS //
            ) //
        );
    }

    private void logParserMessages(ParserLogEntryCollector parserLogEntryCollector) {
        // TODO: extract to helper class?

        if (!isParserLogEnabled) {
            return;
        }

        // exceptions/stack traces are not logged as they are only useful for
        // development and would clutter the log in the main window beyond readability
        for (ParserLogEntry entry : parserLogEntryCollector.getParserLogEntries()) {
            LOGGER.warn(
                "Failed to parse{}, section {}, {}: {}", //
                entry.isLineRejected() ? " (rejected)" : "", //
                entry.getSection(), entry.getMessage(), entry.getLineContent() //
            );
        }
    }
}
