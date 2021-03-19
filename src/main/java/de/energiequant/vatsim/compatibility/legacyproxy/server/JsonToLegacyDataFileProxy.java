package de.energiequant.vatsim.compatibility.legacyproxy.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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

import de.energiequant.common.webdataretrieval.DefaultHttpRetrievalDecoders;
import de.energiequant.common.webdataretrieval.HttpPromiseBuilder;
import de.energiequant.common.webdataretrieval.HttpRetrieval;
import de.energiequant.vatsim.compatibility.legacyproxy.AppConstants;

public class JsonToLegacyDataFileProxy extends GetOnlyRequestHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonToLegacyDataFileProxy.class);

    private final Supplier<String> jsonUrlSupplier;
    private final HttpPromiseBuilder<DataFile> promiseBuilder;

    // TODO: configure, depends on status client version
    private boolean isQuirkUtf8Enabled = false;

    private static final Charset FALLBACK_CHARACTER_SET = StandardCharsets.UTF_8;

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

        LOGGER.debug("Encoding legacy data file");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Writer<DataFile> writer = new LegacyDataFileWriter();
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

    private byte[] recodeLatinToUTF8(byte[] bytes) {
        String s = new String(bytes, StandardCharsets.ISO_8859_1);
        return StandardCharsets.UTF_8.encode(CharBuffer.wrap(s)).array();
    }
}
