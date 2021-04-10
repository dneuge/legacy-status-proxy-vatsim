package de.energiequant.vatsim.compatibility.legacyproxy.server.stationlocator;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
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

import de.energiequant.vatsim.compatibility.legacyproxy.Main;

public class VatSpyStationLocator {
    private static final Logger LOGGER = LoggerFactory.getLogger(VatSpyStationLocator.class);

    private static final String EXPECTED_FILE_NAME_VATSPY_DAT = "VATSpy.dat";
    private static final String EXPECTED_FILE_NAME_FIR_BOUNDARIES_DAT = "FIRBoundaries.dat";

    private static final String CALLSIGN_DELIMITER = "_";

    private final Map<String, GeoPoint2D> centerPointsByCallsignPrefix;

    public static class LoadingFailed extends RuntimeException {
        private LoadingFailed(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public VatSpyStationLocator(File baseDirectory) {
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

        if (Main.getConfiguration().isParserLogEnabled()) {
            logParserMessages(EXPECTED_FILE_NAME_VATSPY_DAT, vatSpyFile);
            logParserMessages(EXPECTED_FILE_NAME_FIR_BOUNDARIES_DAT, firBoundaryFile);
        }

        Map<String, GeoPoint2D> centerPointsByBoundaryId = indexCenterPointsByBoundaryId(firBoundaryFile);
        centerPointsByCallsignPrefix = indexCenterPointsByCallsignPrefix(vatSpyFile, centerPointsByBoundaryId);
    }

    private Map<String, GeoPoint2D> indexCenterPointsByCallsignPrefix(VatSpyFile vatSpyFile, Map<String, GeoPoint2D> centerPointsByBoundaryId) {
        // TODO: refactor to simplify
        Map<String, GeoPoint2D> centerPointsByCallsignPrefix = new HashMap<>();

        for (Airport airport : vatSpyFile.getAirports()) {
            centerPointsByCallsignPrefix.put(
                unifyCallsign(airport.getIcaoCode()),
                airport.getLocation() //
            );

            // TODO: add by alternative codes as well?
        }

        Map<String, List<GeoPoint2D>> centerPointsByFirId = new HashMap<>();
        for (FlightInformationRegion fir : vatSpyFile.getFlightInformationRegions()) {
            String callsignPrefix = unifyCallsign(fir.getCallsignPrefix().orElseGet(fir::getId));
            String boundaryId = fir.getBoundaryId().orElseGet(fir::getId);

            GeoPoint2D centerPoint = centerPointsByBoundaryId.get(boundaryId);
            if (centerPoint == null) {
                LOGGER.warn(
                    "Missing center point for FIR \"{}\", callsign prefix \"{}\", boundary ID \"{}\"",
                    fir.getId(),
                    callsignPrefix,
                    boundaryId //
                );
                continue;
            }

            centerPointsByFirId.computeIfAbsent(fir.getId(), x -> new ArrayList<>())
                .add(centerPoint);

            GeoPoint2D previousCenterPoint = centerPointsByCallsignPrefix.put(callsignPrefix, centerPoint);
            if (previousCenterPoint != null) {
                // TODO: don't warn if points are equal
                LOGGER.warn(
                    "Multiple center points with callsign prefix \"{}\", was {}, is now {}",
                    callsignPrefix,
                    previousCenterPoint,
                    centerPoint //
                );
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

            GeoPoint2D centerPoint = average(centerPoints);
            LOGGER.trace(
                "Center point for UIR \"{}\" has been calculated to {}",
                uir.getId(), centerPoint //
            );

            GeoPoint2D previousCenterPoint = centerPointsByCallsignPrefix.put(callsignPrefix, centerPoint);
            if (previousCenterPoint != null) {
                // TODO: don't warn if points are equal
                LOGGER.warn(
                    "Multiple center points with callsign prefix \"{}\", was {}, is now {}",
                    callsignPrefix,
                    previousCenterPoint,
                    centerPoint //
                );
            }
        }

        return centerPointsByCallsignPrefix;
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

        double sumLatitudes = 0.0;
        double sumLongitudes = 0.0;

        for (FIRBoundary boundary : boundaries) {
            GeoPoint2D singleCenter = boundary.getCenterPoint();
            sumLatitudes += singleCenter.getLatitude();
            sumLongitudes += singleCenter.getLongitude();
        }

        GeoPoint2D calculated = new GeoPoint2D(
            sumLatitudes / numBoundaries,
            sumLongitudes / numBoundaries //
        );

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(
                "Calculated center point for {} as {} from {} boundaries: {}",
                boundaries.iterator().next().getId(),
                calculated,
                numBoundaries,
                boundaries.stream()
                    .map(FIRBoundary::getCenterPoint)
                    .map(GeoPoint2D::toString)
                    .collect(Collectors.joining(", ")) //
            );
        }

        return calculated;
    }

    private GeoPoint2D average(Collection<GeoPoint2D> points) {
        // FIXME: combine with calculateCenterPoint

        int numPoints = points.size();
        if (numPoints == 1) {
            return points.iterator().next();
        }

        double sumLatitudes = 0.0;
        double sumLongitudes = 0.0;

        for (GeoPoint2D point : points) {
            sumLatitudes += point.getLatitude();
            sumLongitudes += point.getLongitude();
        }

        GeoPoint2D calculated = new GeoPoint2D(
            sumLatitudes / numPoints,
            sumLongitudes / numPoints //
        );

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

    private <T> T parse(File file, Parser<T> parser) {
        try (Reader r = new FileReader(file)) {
            return parser.deserialize(r);
        } catch (Exception ex) {
            throw new LoadingFailed("Failed to parse " + file.getAbsolutePath(), ex);
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
