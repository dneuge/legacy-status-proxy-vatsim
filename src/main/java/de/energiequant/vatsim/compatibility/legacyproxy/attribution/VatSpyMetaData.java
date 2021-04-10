package de.energiequant.vatsim.compatibility.legacyproxy.attribution;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.energiequant.vatsim.compatibility.legacyproxy.utils.ResourceUtils;

public class VatSpyMetaData {
    private static final Logger LOGGER = LoggerFactory.getLogger(VatSpyMetaData.class);

    private static final String INTERNAL_DATE_RESOURCE = "com/github/vatsimnetwork/vatspy-data-project/.git_commit_date";
    private static final Instant INCLUDED_DATA_TIMESTAMP = loadDataTimestamp();

    private static Instant loadDataTimestamp() {
        Instant timestamp = null;
        try {
            timestamp = ResourceUtils.getAbsoluteResourceContentAsString(
                VatSpyMetaData.class,
                INTERNAL_DATE_RESOURCE,
                StandardCharsets.UTF_8 //
            )
                .map(String::trim)
                .map(ZonedDateTime::parse)
                .map(ZonedDateTime::toInstant)
                .orElse(null);
        } catch (Exception ex) {
            LOGGER.warn("Failed to load date information for included VAT-Spy data.", ex);
            return null;
        }

        if (timestamp == null) {
            LOGGER.warn("Failed to load date information for included VAT-Spy data.");
        } else {
            LOGGER.debug("Included VAT-Spy data: {}", timestamp);
        }

        return timestamp;
    }

    public static Optional<Instant> getIncludedDataTimestamp() {
        return Optional.ofNullable(INCLUDED_DATA_TIMESTAMP);
    }
}
