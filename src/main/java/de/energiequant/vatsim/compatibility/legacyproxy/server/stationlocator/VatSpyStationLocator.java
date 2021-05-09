package de.energiequant.vatsim.compatibility.legacyproxy.server.stationlocator;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vatplanner.dataformats.vatsimpublic.parser.Parser;
import org.vatplanner.dataformats.vatsimpublic.parser.ParserLogEntry;
import org.vatplanner.dataformats.vatsimpublic.parser.ParserLogEntryCollector;
import org.vatplanner.dataformats.vatsimpublic.parser.vatspy.Airport;
import org.vatplanner.dataformats.vatsimpublic.parser.vatspy.FIRBoundary;
import org.vatplanner.dataformats.vatsimpublic.parser.vatspy.FIRBoundaryFile;
import org.vatplanner.dataformats.vatsimpublic.parser.vatspy.FIRBoundaryFileParser;
import org.vatplanner.dataformats.vatsimpublic.parser.vatspy.FlightInformationRegion;
import org.vatplanner.dataformats.vatsimpublic.parser.vatspy.GeoPoint2D;
import org.vatplanner.dataformats.vatsimpublic.parser.vatspy.UpperInformationRegion;
import org.vatplanner.dataformats.vatsimpublic.parser.vatspy.VatSpyFile;
import org.vatplanner.dataformats.vatsimpublic.parser.vatspy.VatSpyFileParser;

import de.energiequant.vatsim.compatibility.legacyproxy.AppConstants;
import de.energiequant.vatsim.compatibility.legacyproxy.Configuration;
import de.energiequant.vatsim.compatibility.legacyproxy.Main;
import de.energiequant.vatsim.compatibility.legacyproxy.attribution.VatSpyMetaData;
import de.energiequant.vatsim.compatibility.legacyproxy.utils.CallsignHelper;
import de.energiequant.vatsim.compatibility.legacyproxy.utils.GeoMath;
import de.energiequant.vatsim.compatibility.legacyproxy.utils.ResourceUtils;

public class VatSpyStationLocator {
    private static final Logger LOGGER = LoggerFactory.getLogger(VatSpyStationLocator.class);

    private static final String EXPECTED_FILE_NAME_VATSPY_DAT = "VATSpy.dat";
    private static final String EXPECTED_FILE_NAME_FIR_BOUNDARIES_DAT = "FIRBoundaries.dat";

    private static final String CALLSIGN_DELIMITER = "_";

    private static final String INCLUDED_VAT_SPY_DAT_PATH = "com/github/vatsimnetwork/vatspy-data-project/"
        + EXPECTED_FILE_NAME_VATSPY_DAT;
    private static final String INCLUDED_FIR_BOUNDARIES_DAT_PATH = "com/github/vatsimnetwork/vatspy-data-project/"
        + EXPECTED_FILE_NAME_FIR_BOUNDARIES_DAT;

    private final Configuration config = Main.getConfiguration();
    private final boolean shouldAliasUSStations = config.shouldVatSpyAliasUSStations();

    private final Map<String, GeoPoint2D> centerPointsByCallsignPrefix = new HashMap<>();

    public static class LoadingFailed extends Exception {
        private LoadingFailed(String message) {
            super(message);
        }

        private LoadingFailed(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public VatSpyStationLocator(File baseDirectory) throws LoadingFailed {
        if (!baseDirectory.isDirectory()) {
            throw new IllegalArgumentException(baseDirectory.getAbsolutePath() + " is not a directory");
        }

        VatSpyFile vatSpyFile = parse(
            findFile(baseDirectory, EXPECTED_FILE_NAME_VATSPY_DAT),
            new VatSpyFileParser() //
        );

        FIRBoundaryFile firBoundaryFile = parse(
            findFile(baseDirectory, EXPECTED_FILE_NAME_FIR_BOUNDARIES_DAT),
            new FIRBoundaryFileParser() //
        );

        load(vatSpyFile, firBoundaryFile);
    }

    public VatSpyStationLocator() throws LoadingFailed {
        warnAboutOldVatSpyData();

        VatSpyFile vatSpyFile = parse(
            ResourceUtils.getAbsoluteResourceContentAsString(
                VatSpyStationLocator.class,
                INCLUDED_VAT_SPY_DAT_PATH,
                StandardCharsets.UTF_8 //
            ).orElseThrow(() -> new LoadingFailed("Resource unavailable: " + INCLUDED_VAT_SPY_DAT_PATH)),
            new VatSpyFileParser() //
        );

        FIRBoundaryFile firBoundaryFile = parse(
            ResourceUtils.getAbsoluteResourceContentAsString(
                VatSpyStationLocator.class,
                INCLUDED_FIR_BOUNDARIES_DAT_PATH,
                StandardCharsets.UTF_8 //
            ).orElseThrow(() -> new LoadingFailed("Resource unavailable: " + EXPECTED_FILE_NAME_FIR_BOUNDARIES_DAT)),
            new FIRBoundaryFileParser() //
        );

        load(vatSpyFile, firBoundaryFile);
    }

    private void load(VatSpyFile vatSpyFile, FIRBoundaryFile firBoundaryFile) {
        if (Main.getConfiguration().isParserLogEnabled()) {
            logParserMessages(EXPECTED_FILE_NAME_VATSPY_DAT, vatSpyFile);
            logParserMessages(EXPECTED_FILE_NAME_FIR_BOUNDARIES_DAT, firBoundaryFile);
        }

        Map<String, GeoPoint2D> centerPointsByBoundaryId = indexCenterPointsByBoundaryId(firBoundaryFile);
        centerPointsByCallsignPrefix.putAll(indexCenterPointsByCallsignPrefix(vatSpyFile, centerPointsByBoundaryId));
    }

    private static void warnAboutOldVatSpyData() {
        if (!Main.getConfiguration().shouldWarnAboutOldIntegratedVatSpyDatabase()) {
            return;
        }

        if (VatSpyMetaData.isOlderThan(AppConstants.VATSPY_AGE_WARNING_THRESHOLD)) {
            LOGGER.warn(
                "VAT-Spy data is {} days old and may be outdated. Please check for updates.",
                VatSpyMetaData.getAge() //
                    .map(age -> Long.toString(age.toHours() / 24))
                    .orElse("?") //
            );
        }
    }

    private Map<String, GeoPoint2D> indexCenterPointsByCallsignPrefix(VatSpyFile vatSpyFile, Map<String, GeoPoint2D> centerPointsByBoundaryId) {
        // TODO: refactor to simplify
        // FIXME: change to collect all points, then average instead of overwriting
        Map<String, GeoPoint2D> centerPointsByCallsignPrefix = new HashMap<>();

        Map<String, List<GeoPoint2D>> centerPointsByFirId = new HashMap<>();
        for (FlightInformationRegion fir : vatSpyFile.getFlightInformationRegions()) {
            String boundaryId = fir.getBoundaryId().orElseGet(fir::getId);

            GeoPoint2D centerPoint = centerPointsByBoundaryId.get(boundaryId);
            if (centerPoint == null) {
                LOGGER.warn(
                    "Missing center point for FIR \"{}\", boundary ID \"{}\"",
                    fir.getId(),
                    boundaryId //
                );
                continue;
            }

            centerPointsByFirId.computeIfAbsent(fir.getId(), x -> new ArrayList<>())
                .add(centerPoint);

            // even if callsign prefixes are configured, some stations still log in with
            // what is set as IDs instead, so both need to be registered (the prefix is
            // optional)
            String callsignPrefix = fir.getCallsignPrefix().map(this::unifyCallsign).orElse(null);
            if (callsignPrefix != null) {
                GeoPoint2D previousCenterPoint = centerPointsByCallsignPrefix.put(callsignPrefix, centerPoint);
                if (previousCenterPoint != null) {
                    LOGGER.warn(
                        "Multiple center points with callsign prefix \"{}\", was {}, is now {}",
                        callsignPrefix,
                        previousCenterPoint,
                        centerPoint //
                    );
                }
            }

            String idAsCallsign = unifyCallsign(fir.getId());
            if (!idAsCallsign.equals(callsignPrefix)) {
                GeoPoint2D previousCenterPoint = centerPointsByCallsignPrefix.put(idAsCallsign, centerPoint);
                if (previousCenterPoint != null) {
                    LOGGER.warn(
                        "Multiple center points with callsign prefix \"{}\", was {}, is now {}",
                        idAsCallsign,
                        previousCenterPoint,
                        centerPoint //
                    );
                }
            }
        }

        for (UpperInformationRegion uir : vatSpyFile.getUpperInformationRegions()) {
            String callsignPrefix = unifyCallsign(uir.getId());

            Collection<GeoPoint2D> centerPoints = new ArrayList<>();
            for (String firId : uir.getFlightInformationRegionIds()) {
                List<GeoPoint2D> firCenterPoints = centerPointsByFirId.get(firId);
                if (firCenterPoints == null) {
                    LOGGER.warn(
                        "Missing center points for FIR \"{}\" referenced by UIR \"{}\"",
                        firId, uir.getId() //
                    );
                }
                centerPoints.addAll(firCenterPoints);
            }

            if (centerPoints.isEmpty()) {
                LOGGER.warn(
                    "Missing center points for UIR \"{}\"",
                    uir.getId() //
                );
                continue;
            }

            GeoPoint2D centerPoint = GeoMath.average(centerPoints);
            LOGGER.trace(
                "Center point for UIR \"{}\" has been calculated to {}",
                uir.getId(), centerPoint //
            );

            GeoPoint2D previousCenterPoint = centerPointsByCallsignPrefix.put(callsignPrefix, centerPoint);
            if (previousCenterPoint != null) {
                LOGGER.warn(
                    "Multiple center points with callsign prefix \"{}\", was {}, is now {}",
                    callsignPrefix,
                    previousCenterPoint,
                    centerPoint //
                );
            }
        }

        for (Airport airport : vatSpyFile.getAirports()) {
            String airportIcaoCallsignPrefix = unifyCallsign(airport.getIcaoCode());
            if (centerPointsByCallsignPrefix.containsKey(airportIcaoCallsignPrefix)) {
                LOGGER.warn(
                    "Center point for callsign prefix \"{}\" is already set, not adding location for airport {}",
                    airportIcaoCallsignPrefix,
                    airport.getIcaoCode() //
                );
            } else {
                centerPointsByCallsignPrefix.put(
                    airportIcaoCallsignPrefix,
                    airport.getLocation() //
                );
            }

            String airportAlternativeCodeCallsignPrefix = airport.getAlternativeCode()
                .map(this::unifyCallsign)
                .orElse(null);
            if ((airportAlternativeCodeCallsignPrefix != null)
                && !airportIcaoCallsignPrefix.equals(airportAlternativeCodeCallsignPrefix)) {
                if (centerPointsByCallsignPrefix.containsKey(airportAlternativeCodeCallsignPrefix)) {
                    LOGGER.warn(
                        "Center point for callsign prefix \"{}\" is already set, not adding location for airport {} (alternative code {})",
                        airportAlternativeCodeCallsignPrefix,
                        airport.getIcaoCode(),
                        airport.getAlternativeCode().orElse(null) //
                    );
                } else {
                    centerPointsByCallsignPrefix.put(
                        airportAlternativeCodeCallsignPrefix,
                        airport.getLocation() //
                    );
                }
            }
        }

        if (shouldAliasUSStations) {
            Map<String, GeoPoint2D> aliasedCenterPoints = aliasUSStations(centerPointsByCallsignPrefix);
            centerPointsByCallsignPrefix.putAll(aliasedCenterPoints);
        }

        return centerPointsByCallsignPrefix;
    }

    /**
     * US stations (ICAO prefix K) often log in without the ICAO prefix and
     * sometimes do not match any other registered prefix either. This is a
     * well-known issue with a good chance of it just putting a K in front matching
     * the correct station, so we alias all US stations to omit their K prefix if no
     * other call sign collides to increase our chance to provide as many locations
     * as possible from VAT-Spy data.
     * 
     * @param centerPointsByCallsignPrefix all locations indexed by callsign prefix
     *        so far
     * @return stations to be aliased, collision-free
     */
    private Map<String, GeoPoint2D> aliasUSStations(Map<String, GeoPoint2D> centerPointsByCallsignPrefix) {
        Map<String, GeoPoint2D> aliasedCenterPoints = new HashMap<>();
        for (Map.Entry<String, GeoPoint2D> entry : centerPointsByCallsignPrefix.entrySet()) {
            String originalCallsignPrefix = entry.getKey();

            if (!originalCallsignPrefix.startsWith("K") || !CallsignHelper.isUsIcaoCallsign(originalCallsignPrefix)) {
                continue;
            }

            String alias = originalCallsignPrefix.substring(1);
            if (centerPointsByCallsignPrefix.containsKey(alias)) {
                continue;
            }

            GeoPoint2D centerPoint = entry.getValue();
            LOGGER.trace(
                "Aliasing \"{}\" to \"{}\" at {}",
                alias,
                originalCallsignPrefix,
                centerPoint //
            );

            aliasedCenterPoints.put(alias, centerPoint);
        }
        return aliasedCenterPoints;
    }

    private Map<String, GeoPoint2D> indexCenterPointsByBoundaryId(FIRBoundaryFile firBoundaryFile) {
        Map<String, GeoPoint2D> centerPointsByBoundaryId = new HashMap<>();
        Collection<List<FIRBoundary>> boundariesPerId = firBoundaryFile.getBoundaries()
            .stream()
            .collect(Collectors.groupingBy(FIRBoundary::getId))
            .values();
        for (List<FIRBoundary> boundaries : boundariesPerId) {
            centerPointsByBoundaryId.put(
                boundaries.iterator().next().getId(),
                calculateCenterPoint(boundaries) //
            );
        }
        return centerPointsByBoundaryId;
    }

    private GeoPoint2D calculateCenterPoint(Collection<FIRBoundary> boundaries) {
        int numBoundaries = boundaries.size();
        if (numBoundaries == 1) {
            return boundaries.iterator().next().getCenterPoint();
        }

        List<GeoPoint2D> points = boundaries.stream()
            .map(FIRBoundary::getCenterPoint)
            .collect(Collectors.toList());

        GeoPoint2D calculated = GeoMath.average(points);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(
                "Calculated center point for {} as {} from {} boundaries: {}",
                boundaries.iterator().next().getId(),
                calculated,
                numBoundaries,
                points.stream()
                    .map(GeoPoint2D::toString)
                    .collect(Collectors.joining(", ")) //
            );
        }

        return calculated;
    }

    private File findFile(File directory, String expectedFileName) {
        return Arrays.stream(directory.listFiles())
            .filter(f -> f.isFile() && expectedFileName.equalsIgnoreCase(f.getName()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                expectedFileName + " could not be found in " + directory.getAbsolutePath() //
            ));
    }

    private <T> T parse(File file, Parser<T> parser) throws LoadingFailed {
        try (Reader r = new FileReader(file)) {
            return parser.deserialize(r);
        } catch (Exception ex) {
            throw new LoadingFailed("Failed to parse " + file.getAbsolutePath(), ex);
        }
    }

    private <T> T parse(String content, Parser<T> parser) throws LoadingFailed {
        try {
            return parser.deserialize(content);
        } catch (Exception ex) {
            throw new LoadingFailed("Failed to parse", ex);
        }
    }

    private void logParserMessages(String fileName, ParserLogEntryCollector logCollector) {
        // exceptions/stack traces are not logged as they are only useful for
        // development and would clutter the log in the main window beyond readability
        for (ParserLogEntry entry : logCollector.getParserLogEntries()) {
            LOGGER.warn(
                "Failed to parse from {}{}, section {}, {}: {}", //
                fileName, //
                entry.isLineRejected() ? " (rejected)" : "", //
                entry.getSection(), entry.getMessage(), entry.getLineContent() //
            );
        }
    }

    public Optional<Station> locate(String callsign) {
        LOGGER.trace("locating \"{}\"", callsign);

        String[] callsignSegments = splitCallsignUppercased(callsign);
        for (int usedSegments = callsignSegments.length; usedSegments > 0; usedSegments--) {
            Station station = locate(Arrays.copyOf(callsignSegments, usedSegments));
            if (station != null) {
                return Optional.of(station);
            }

        }

        return Optional.empty();
    }

    private Station locate(String[] callsignSegments) {
        String callsign = String.join(CALLSIGN_DELIMITER, callsignSegments);

        GeoPoint2D centerPoint = centerPointsByCallsignPrefix.get(callsign);
        if (centerPoint == null) {
            LOGGER.trace("no match for callsign prefix \"{}\"", callsign);
            return null;
        } else {
            LOGGER.trace("callsign prefix \"{}\" matched", callsign);
            return new Station(callsign, centerPoint.getLatitude(), centerPoint.getLongitude());
        }
    }

    private String[] splitCallsignUppercased(String callsign) {
        return callsign.toUpperCase().split("[^A-Z0-9]");
    }

    private String unifyCallsign(String callsign) {
        return String.join(CALLSIGN_DELIMITER, splitCallsignUppercased(callsign));
    }
}
