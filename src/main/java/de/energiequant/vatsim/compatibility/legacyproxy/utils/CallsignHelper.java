package de.energiequant.vatsim.compatibility.legacyproxy.utils;

import java.util.regex.Pattern;

/**
 * Helper methods to work with callsigns.
 */
public class CallsignHelper {
    private static final Pattern PATTERN_US_CALLSIGN = Pattern.compile(
        "^K[A-Z0-9]{3}(?:[^A-Z0-9].*|)$",
        Pattern.CASE_INSENSITIVE //
    );

    private CallsignHelper() {
        // utility class, hide constructor
    }

    /**
     * Checks if the given callsign indicates a US-based station by ICAO code.
     * US-based stations are recognized based on "K" prefix of a 4-character ICAO
     * code; optional suffixes are allowed after a separator.
     * 
     * @param callsign callsign to check
     * @return true if callsign indicates a US station, false if not
     */
    public static boolean isUsIcaoCallsign(String callsign) {
        return PATTERN_US_CALLSIGN.matcher(callsign).matches();
    }
}
