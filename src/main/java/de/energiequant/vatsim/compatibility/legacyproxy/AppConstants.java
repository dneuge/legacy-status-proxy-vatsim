package de.energiequant.vatsim.compatibility.legacyproxy;

import java.time.Duration;

import org.vatplanner.dataformats.vatsimpublic.parser.DataFileFormat;

public class AppConstants {
    public static final String USER_AGENT = "LegacyStatusProxy/" + Main.getApplicationVersion();
    public static final Duration EXTERNAL_REQUEST_TIMEOUT = Duration.ofSeconds(30);
    public static final DataFileFormat UPSTREAM_DATA_FILE_FORMAT = DataFileFormat.JSON3;
}
