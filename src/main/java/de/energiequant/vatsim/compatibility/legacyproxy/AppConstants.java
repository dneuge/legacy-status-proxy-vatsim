package de.energiequant.vatsim.compatibility.legacyproxy;

import java.time.Duration;

import org.vatplanner.dataformats.vatsimpublic.parser.DataFileFormat;

public class AppConstants {
    public static final String USER_AGENT = "LegacyStatusProxy/" + Main.getApplicationVersion();
    public static final Duration EXTERNAL_REQUEST_TIMEOUT = Duration.ofSeconds(30);
    public static final DataFileFormat UPSTREAM_DATA_FILE_FORMAT = DataFileFormat.JSON3;
    public static final Duration VATSPY_AGE_WARNING_THRESHOLD = Duration.ofDays(270);

    public static final String DEPENDENCIES_CAPTION = "This software includes the following runtime dependencies.\nFor full author information please refer to their individual websites.\nIn alphabetical order:";
    public static final String PROJECT_DEPENDENCY_LICENSE_INTRO = "made available under ";
    public static final String SAVING_DISABLED_TOOLTIP = "Saving has been disabled due to an unsafe file location.";
}
