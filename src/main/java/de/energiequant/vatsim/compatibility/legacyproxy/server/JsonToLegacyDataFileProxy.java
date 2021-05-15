package de.energiequant.vatsim.compatibility.legacyproxy.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.Message;
import org.apache.hc.core5.http.nio.entity.AsyncEntityProducers;
import org.apache.hc.core5.http.nio.support.AsyncResponseBuilder;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vatplanner.dataformats.vatsimpublic.export.LegacyDataFileWriter;
import org.vatplanner.dataformats.vatsimpublic.export.Writer;
import org.vatplanner.dataformats.vatsimpublic.parser.DataFile;
import org.vatplanner.dataformats.vatsimpublic.parser.Parser;
import org.vatplanner.dataformats.vatsimpublic.parser.ParserLogEntry;

import de.energiequant.common.webdataretrieval.DefaultHttpRetrievalDecoders;
import de.energiequant.common.webdataretrieval.HttpPromiseBuilder;
import de.energiequant.common.webdataretrieval.HttpRetrieval;
import de.energiequant.vatsim.compatibility.legacyproxy.AppConstants;
import de.energiequant.vatsim.compatibility.legacyproxy.Configuration;
import de.energiequant.vatsim.compatibility.legacyproxy.Main;
import de.energiequant.vatsim.compatibility.legacyproxy.server.stationlocator.StationLocator;
import de.energiequant.vatsim.compatibility.legacyproxy.server.stationlocator.StationLocator.Strategy;

public class JsonToLegacyDataFileProxy extends GetOnlyRequestHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonToLegacyDataFileProxy.class);

    private final Supplier<String> jsonUrlSupplier;
    private final HttpPromiseBuilder<DataFile> promiseBuilder;

    private final StationLocator stationLocator = new StationLocator();

    private final Configuration config = Main.getConfiguration();
    private final boolean isParserLogEnabled = config.isParserLogEnabled();
    private final boolean isQuirkUtf8Enabled = config.isQuirkLegacyDataFileUtf8Enabled();
    private final boolean shouldInjectLocations = config.getStationLocatorStrategy() != Strategy.DISABLE;

    private static final Charset FALLBACK_CHARACTER_SET = StandardCharsets.UTF_8;

    private final String header = stationLocator.usesVatSpySource()
        ? AppConstants.SERVER_DISCLAIMER_HEADER //
            + (stationLocator.isVatSpySourceExternal()
                ? AppConstants.SERVER_VAT_SPY_EXTERNAL_HEADER
                : AppConstants.SERVER_VAT_SPY_INTERNAL_HEADER) //
        : AppConstants.SERVER_DISCLAIMER_HEADER;

    public JsonToLegacyDataFileProxy(Parser<DataFile> parser, Supplier<String> jsonUrlSupplier) {
        this.jsonUrlSupplier = jsonUrlSupplier;

        DefaultHttpRetrievalDecoders decoders = new DefaultHttpRetrievalDecoders();
        promiseBuilder = new HttpPromiseBuilder<>(
            decoders.bodyAsStringWithHeaderCharacterSet(FALLBACK_CHARACTER_SET) //
                .andThen(parser::deserialize) //
        ).withConfiguration(
            new HttpRetrieval()
                .setUserAgent(AppConstants.USER_AGENT)
                .setTimeout(AppConstants.EXTERNAL_REQUEST_TIMEOUT) //
        );
    }

    @Override
    protected void handleGet(final Message<HttpRequest, Void> message, final ResponseTrigger responseTrigger, final HttpContext context) throws HttpException, IOException {
        LOGGER.debug("Processing request for legacy data file");

        DataFile dataFile = null;
        String url = jsonUrlSupplier.get();
        if (url == null) {
            LOGGER.warn("No upstream JSON URL is available; unable to serve request for data file");

            responseTrigger.submitResponse(
                AsyncResponseBuilder.create(HttpStatus.SC_SERVICE_UNAVAILABLE)
                    .setEntity(AsyncEntityProducers.create("No matching upstream JSON URLs known",
                        ContentType.TEXT_PLAIN))
                    .build(),
                context);
            return;
        }

        try {
            LOGGER.debug("Retrieving JSON data file from {}", url);
            dataFile = promiseBuilder.requestByGet(url).get();
            logParserMessages(dataFile);
        } catch (InterruptedException | ExecutionException ex) {
            LOGGER.warn("Failed to retrieve JSON data file from {}", url, ex);

            responseTrigger.submitResponse(
                AsyncResponseBuilder.create(HttpStatus.SC_SERVICE_UNAVAILABLE)
                    .setEntity(AsyncEntityProducers.create("Failed to retrieve JSON data file",
                        ContentType.TEXT_PLAIN))
                    .build(),
                context);
            return;
        }

        if (shouldInjectLocations) {
            stationLocator.injectTo(dataFile);
        }

        LOGGER.debug("Encoding legacy data file");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Writer<DataFile> writer = new LegacyDataFileWriter(header);
        writer.serialize(dataFile, baos);
        baos.flush();
        byte[] bytes = baos.toByteArray();
        baos = null;

        if (isQuirkUtf8Enabled) {
            bytes = recodeLatinToUTF8(bytes);
        }

        responseTrigger.submitResponse(
            AsyncResponseBuilder.create(HttpStatus.SC_OK)
                .setEntity(AsyncEntityProducers.create(bytes, ContentType.TEXT_PLAIN))
                .build(),
            context);
    }

    private void logParserMessages(DataFile dataFile) {
        if (!isParserLogEnabled) {
            return;
        }

        // exceptions/stack traces are not logged as they are only useful for
        // development and would clutter the log in the main window beyond readability
        for (ParserLogEntry entry : dataFile.getParserLogEntries()) {
            LOGGER.warn(
                "Failed to parse{}, section {}, {}: {}", //
                entry.isLineRejected() ? " (rejected)" : "", //
                entry.getSection(), entry.getMessage(), entry.getLineContent() //
            );
        }
    }

    private byte[] recodeLatinToUTF8(byte[] bytes) {
        String s = new String(bytes, StandardCharsets.ISO_8859_1);
        return toByteArray(StandardCharsets.UTF_8.encode(CharBuffer.wrap(s)));
    }

    private byte[] toByteArray(ByteBuffer buffer) {
        if (!buffer.hasArray()) {
            throw new IllegalArgumentException("buffer must be backed by an array");
        }

        int offset = buffer.arrayOffset();
        return Arrays.copyOfRange(buffer.array(), offset, offset + buffer.limit());
    }
}
