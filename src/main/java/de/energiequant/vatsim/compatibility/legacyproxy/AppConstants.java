package de.energiequant.vatsim.compatibility.legacyproxy;

import java.time.Duration;

import org.vatplanner.dataformats.vatsimpublic.parser.DataFileFormat;

public class AppConstants {
    // TODO: set application meta data automatically during build
    public static final String APPLICATION_JAR_NAME = "legacy-status-proxy-vatsim.jar";
    public static final String APPLICATION_NAME = Launcher.APPLICATION_NAME;
    public static final String APPLICATION_VERSION = "0.90.3";
    public static final String APPLICATION_URL = "https://github.com/dneuge/legacy-status-proxy-vatsim";
    public static final String APPLICATION_COPYRIGHT = "Copyright (c) 2021 Daniel Neugebauer";

    // TODO: Main#getApplicationVersion only returns the constant from this class
    public static final String USER_AGENT = "LegacyStatusProxy/" + Main.getApplicationVersion();
    public static final Duration EXTERNAL_REQUEST_TIMEOUT = Duration.ofSeconds(30);
    public static final DataFileFormat UPSTREAM_DATA_FILE_FORMAT = DataFileFormat.JSON3;
    public static final Duration VATSPY_AGE_WARNING_THRESHOLD = Duration.ofDays(270);

    public static final String DEPENDENCIES_CAPTION = "This software includes the following runtime dependencies.\nFor full author information please refer to their individual websites.\nIn alphabetical order:";
    public static final String PROJECT_DEPENDENCY_LICENSE_INTRO = "made available under ";
    public static final String SAVING_DISABLED_TOOLTIP = "Saving has been disabled due to an unsafe file location.";

    // TODO: incorporate constants for name and URL currently present in Main
    public static final String SERVER_DISCLAIMER_HEADER = "YOU ARE ACCESSING THE COMPATIBILITY ADAPTER PROXY FOR VATSIM DATA FILES\n" //
        + "\n"
        + "This proxy server is supposed to be used only in order to establish compatibility\n"
        + "between legacy applications and later revisions of status/data files.\n"
        + "\n"
        + "The intended use does not cover clients used for active participation on the\n"
        + "VATSIM network (e.g. pilot/ATC clients).\n"
        + "\n"
        + "The proxy is inofficial and not supported by VATSIM. Please avoid running pilot\n"
        + "or ATC clients using files provided through this proxy.\n"
        + "\n"
        + "If, even if you do not have connected an active client through this proxy, you\n"
        + "experience any issues accessing data or connecting to VATSIM please disable the\n"
        + "proxy server and try again.\n"
        + "\n" //
        + "VATSIM data served by this server remains under copyright of VATSIM.\n"
        + "Usage of that data remains subject to conditions defined by VATSIM.\n";

    public static final String SERVER_VAT_SPY_INTERNAL_HEADER = "\n"
        + "Information from VAT-Spy Client Data Update Project is used to locate stations.\n"
        + "That part of data included in this file is:\n"
        + "\n"
        + "  Copyright (c) 2019-2021 Néstor Pérez, Niels Voogd, Adrian Bjerke, Alex Long and contributors\n"
        + "  VAT-Spy is being developed by Ross Carlson\n"
        + "\n"
        + "See https://github.com/vatsimnetwork/vatspy-data-project for more information.\n"
        + "\n"
        + "The used VAT-Spy data has been made available under CC-BY-SA-4.0 license.\n"
        + "See: https://creativecommons.org/licenses/by-sa/4.0/\n"
        + "\n"
        + "Note that despite CC-BY-SA-4.0 licensed information has been used to enrich\n"
        + "this output, \"ShareAlike\" must not be extended to any information other than\n"
        + "the station locations calculated from that data as all other data remains\n"
        + "the sole property of VATSIM as stated above and cannot be sublicensed.\n"
        + "\n"
        + "In general, if you want to use station location data, it makes much more sense\n"
        + "to simply use the URL shown above to download an original copy of the full\n"
        + "VAT-Spy data base and maybe have a look at the implementation of this server.\n";

    public static final String SERVER_VAT_SPY_EXTERNAL_HEADER = "\n"
        + "Information read from a VAT-Spy data source is used to locate stations.\n"
        + "Copyright information for that data source cannot be determined automatically.\n"
        + "Please ask the server operator for information on exact data source, copyright,\n"
        + "license and conditions.\n"
        + "\n"
        + "Note that any license of the data source used to enrich this output must\n"
        + "not extend to any information other than the station locations calculated\n"
        + "from that data as all other data remains the sole property of VATSIM as stated\n"
        + "above and cannot be sublicensed.\n";
}
