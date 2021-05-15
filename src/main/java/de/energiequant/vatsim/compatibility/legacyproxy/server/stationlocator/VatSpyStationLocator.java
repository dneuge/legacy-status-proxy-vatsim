package de.energiequant.vatsim.compatibility.legacyproxy.server.stationlocator;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
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
import org.vatplanner.dataformats.vatsimpublic.parser.vatspy.UpperInformationRegion;
import org.vatplanner.dataformats.vatsimpublic.parser.vatspy.VatSpyFile;
import org.vatplanner.dataformats.vatsimpublic.parser.vatspy.VatSpyFileParser;
import org.vatplanner.dataformats.vatsimpublic.utils.GeoMath;
import org.vatplanner.dataformats.vatsimpublic.utils.GeoPoint2D;

import de.energiequant.vatsim.compatibility.legacyproxy.AppConstants;
import de.energiequant.vatsim.compatibility.legacyproxy.Configuration;
import de.energiequant.vatsim.compatibility.legacyproxy.Main;
import de.energiequant.vatsim.compatibility.legacyproxy.attribution.VatSpyMetaData;
import de.energiequant.vatsim.compatibility.legacyproxy.utils.CallsignHelper;
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

    private final boolean isLoggingAllowed;
    private final boolean usesExternalDataSource;

    private static final int CHECK_MINIMUM_CALLSIGNS = 10000;

    public static class LoadingFailed extends Exception {
        private LoadingFailed(String message) {
            super(message);
        }

        private LoadingFailed(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public VatSpyStationLocator(File baseDirectory) throws LoadingFailed {
        this(baseDirectory, true);
    }

    private VatSpyStationLocator(File baseDirectory, boolean isLoggingAllowed) throws LoadingFailed {
        this.isLoggingAllowed = isLoggingAllowed;
        this.usesExternalDataSource = true;

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
        this.isLoggingAllowed = true;
        this.usesExternalDataSource = false;
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

    /**
     * Checks if data can be loaded from given base directory without any obvious
     * issues.
     * 
     * @param baseDirectory directory containing VAT-Spy data to be checked
     * @return message describing possible error, empty if data seems usable
     */
    public static Optional<String> check(File baseDirectory) {
        LOGGER.debug("Checking {}", baseDirectory);

        VatSpyStationLocator locator = null;
        try {
            locator = new VatSpyStationLocator(baseDirectory, false);
        } catch (Exception ex) {
            return Optional.of(ex.getMessage());
        }

        int numCallsigns = locator.centerPointsByCallsignPrefix.size();
        if (numCallsigns < CHECK_MINIMUM_CALLSIGNS) {
            return Optional.of(
                "low number of callsigns after import (found " + numCallsigns
                    + ", expected at least " + CHECK_MINIMUM_CALLSIGNS + ")" //
            );
        }

        return Optional.empty();
    }

    private void load(VatSpyFile vatSpyFile, FIRBoundaryFile firBoundaryFile) {
        if (isLoggingAllowed && Main.getConfiguration().isParserLogEnabled()) {
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
        Map<String, Set<GeoPoint2D>> multipleCenterPointsByCallsignPrefix = new HashMap<>();

        Map<String, Set<GeoPoint2D>> multipleAirportCenterPointsByFirId = new HashMap<>();
        for (Airport airport : vatSpyFile.getAirports()) {
            String firId = airport.getFlightInformationRegionId();
            multipleAirportCenterPointsByFirId.computeIfAbsent(firId, x -> new HashSet<>())
                .add(airport.getLocation());
        }

        Map<String, Set<GeoPoint2D>> centerPointsByFirId = new HashMap<>();
        for (FlightInformationRegion fir : vatSpyFile.getFlightInformationRegions()) {
            String boundaryId = fir.getBoundaryId().orElseGet(fir::getId);

            GeoPoint2D centerPoint = centerPointsByBoundaryId.get(boundaryId);
            if (centerPoint == null) {
                Set<GeoPoint2D> airportCenterPoints = multipleAirportCenterPointsByFirId.get(fir.getId());
                if (airportCenterPoints == null) {
                    warn(
                        "Missing center point for FIR \"{}\", boundary ID \"{}\"; no airports covered by FIR",
                        fir.getId(),
                        boundaryId //
                    );

                    continue;
                }

                centerPoint = GeoMath.average(airportCenterPoints);
                debug(
                    "Missing center point for FIR \"{}\", boundary ID \"{}\" has been substituted using covered airports: {}",
                    fir.getId(),
                    boundaryId,
                    centerPoint //
                );
            }

            centerPointsByFirId
                .computeIfAbsent(fir.getId(), x -> new HashSet<>())
                .add(centerPoint);

            // even if callsign prefixes are configured, some stations still log in with
            // what is set as IDs instead, so both need to be registered (the prefix is
            // optional)
            String callsignPrefix = fir.getCallsignPrefix().map(this::unifyCallsign).orElse(null);
            if (callsignPrefix != null) {
                multipleCenterPointsByCallsignPrefix
                    .computeIfAbsent(callsignPrefix, x -> new HashSet<>())
                    .add(centerPoint);
            }

            String idAsCallsign = unifyCallsign(fir.getId());
            if (!idAsCallsign.equals(callsignPrefix)) {
                multipleCenterPointsByCallsignPrefix
                    .computeIfAbsent(idAsCallsign, x -> new HashSet<>())
                    .add(centerPoint);
            }
        }

        for (UpperInformationRegion uir : vatSpyFile.getUpperInformationRegions()) {
            String callsignPrefix = unifyCallsign(uir.getId());

            Set<GeoPoint2D> centerPoints = new HashSet<>();
            for (String firId : uir.getFlightInformationRegionIds()) {
                Set<GeoPoint2D> firCenterPoints = centerPointsByFirId.get(firId);
                if ((firCenterPoints == null) || firCenterPoints.isEmpty()) {
                    warn(
                        "Missing center points for FIR \"{}\" referenced by UIR \"{}\"",
                        firId, uir.getId() //
                    );
                }
                centerPoints.addAll(firCenterPoints);
            }

            if (centerPoints.isEmpty()) {
                warn(
                    "Missing center points for UIR \"{}\"",
                    uir.getId() //
                );
                continue;
            }

            multipleCenterPointsByCallsignPrefix
                .computeIfAbsent(callsignPrefix, x -> new HashSet<>())
                .addAll(centerPoints);
        }

        for (Airport airport : vatSpyFile.getAirports()) {
            String airportIcaoCallsignPrefix = unifyCallsign(airport.getIcaoCode());
            multipleCenterPointsByCallsignPrefix
                .computeIfAbsent(airportIcaoCallsignPrefix, x -> new HashSet<>())
                .add(airport.getLocation());

            String airportAlternativeCodeCallsignPrefix = airport.getAlternativeCode()
                .map(this::unifyCallsign)
                .orElse(null);
            if ((airportAlternativeCodeCallsignPrefix != null)
                && !airportIcaoCallsignPrefix.equals(airportAlternativeCodeCallsignPrefix)) {

                multipleCenterPointsByCallsignPrefix
                    .computeIfAbsent(airportAlternativeCodeCallsignPrefix, x -> new HashSet<>())
                    .add(airport.getLocation());
            }
        }

        Map<String, GeoPoint2D> singleCenterPointsByCallsignPrefix = new HashMap<>();
        for (Entry<String, Set<GeoPoint2D>> entry : multipleCenterPointsByCallsignPrefix.entrySet()) {
            String callsign = entry.getKey();
            Set<GeoPoint2D> centerPoints = entry.getValue();
            GeoPoint2D averageCenterPoint = GeoMath.average(centerPoints);

            trace(
                "calculated center point for {} is {}, source: {}",
                callsign,
                averageCenterPoint,
                centerPoints //
            );

            singleCenterPointsByCallsignPrefix.put(callsign, averageCenterPoint);
        }

        if (shouldAliasUSStations) {
            Map<String, GeoPoint2D> aliasedCenterPoints = aliasUSStations(singleCenterPointsByCallsignPrefix);
            singleCenterPointsByCallsignPrefix.putAll(aliasedCenterPoints);
        }

        return singleCenterPointsByCallsignPrefix;
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
            trace(
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
            .distinct()
            .collect(Collectors.toList());

        GeoPoint2D calculated = GeoMath.average(points);

        if (LOGGER.isTraceEnabled()) {
            trace(
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
            warn(
                "Failed to parse from {}{}, section {}, {}: {}", //
                fileName, //
                entry.isLineRejected() ? " (rejected)" : "", //
                entry.getSection(), entry.getMessage(), entry.getLineContent() //
            );
        }
    }

    public Optional<Station> locate(String callsign) {
        trace("locating \"{}\"", callsign);

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
            trace("no match for callsign prefix \"{}\"", callsign);
            return null;
        } else {
            trace("callsign prefix \"{}\" matched", callsign);
            return new Station(callsign, centerPoint.getLatitude(), centerPoint.getLongitude());
        }
    }

    private String[] splitCallsignUppercased(String callsign) {
        return callsign.toUpperCase().split("[^A-Z0-9]");
    }

    private String unifyCallsign(String callsign) {
        return String.join(CALLSIGN_DELIMITER, splitCallsignUppercased(callsign));
    }

    /**
     * Returns true if locations are provided from an external data source, false if
     * included data source is used.
     * 
     * @return true if locations are provided from an external data source, false if
     *         included data source is used
     */
    public boolean usesExternalDataSource() {
        return usesExternalDataSource;
    }

    private void warn(String format, Object arg) {
        if (isLoggingAllowed) {
            LOGGER.warn(format, arg);
        }
    }

    private void warn(String format, Object... arguments) {
        if (isLoggingAllowed) {
            LOGGER.warn(format, arguments);
        }
    }

    private void debug(String format, Object... arguments) {
        if (isLoggingAllowed) {
            LOGGER.debug(format, arguments);
        }
    }

    private void trace(String format, Object arg) {
        if (isLoggingAllowed) {
            LOGGER.trace(format, arg);
        }
    }

    private void trace(String format, Object... arguments) {
        if (isLoggingAllowed) {
            LOGGER.trace(format, arguments);
        }
    }
}
