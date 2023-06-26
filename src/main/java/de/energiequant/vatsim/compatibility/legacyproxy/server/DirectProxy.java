package de.energiequant.vatsim.compatibility.legacyproxy.server;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;

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

import de.energiequant.common.webdataretrieval.DefaultHttpRetrievalDecoders;
import de.energiequant.common.webdataretrieval.HttpPromiseBuilder;
import de.energiequant.common.webdataretrieval.HttpRetrieval;
import de.energiequant.common.webdataretrieval.RetrievedData;
import de.energiequant.vatsim.compatibility.legacyproxy.AppConstants;

public class DirectProxy extends GetOnlyRequestHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(DirectProxy.class);

    private final String url;
    private final Charset outputCharacterSet;
    private final ContentType outputContentType;
    private final HttpPromiseBuilder<RetrievedData<String>> promiseBuilder;

    private static final Charset FALLBACK_CHARACTER_SET = StandardCharsets.ISO_8859_1;

    public DirectProxy(String url, Charset outputCharacterSet, ContentType outputContentType) {
        this.url = url;
        this.outputCharacterSet = outputCharacterSet;
        this.outputContentType = outputContentType;

        DefaultHttpRetrievalDecoders decoders = new DefaultHttpRetrievalDecoders();
        promiseBuilder = new HttpPromiseBuilder<>(
            decoders.withMetaData(
                decoders.bodyAsStringWithHeaderCharacterSet(FALLBACK_CHARACTER_SET)
            )
        ).withConfiguration(
            new HttpRetrieval()
                .setUserAgent(AppConstants.USER_AGENT)
                .setTimeout(AppConstants.EXTERNAL_REQUEST_TIMEOUT)
        );
    }

    @Override
    protected void handleGet(final Message<HttpRequest, Void> message, final ResponseTrigger responseTrigger, final HttpContext context) throws HttpException, IOException {
        LOGGER.debug("Processing request to directly proxy {}", url);

        RetrievedData<String> retrievedData = null;
        try {
            retrievedData = promiseBuilder.requestByGet(url).get();
        } catch (InterruptedException | ExecutionException ex) {
            LOGGER.warn("Failed to retrieve data from {}", url, ex);
        }

        if (retrievedData == null) {
            responseTrigger.submitResponse(
                AsyncResponseBuilder.create(HttpStatus.SC_BAD_GATEWAY)
                                    .setEntity(AsyncEntityProducers.create("Request to upstream server failed", ContentType.TEXT_PLAIN))
                                    .build(),
                context
            );
            return;
        }

        responseTrigger.submitResponse(
            AsyncResponseBuilder.create(HttpStatus.SC_OK)
                                .setEntity(AsyncEntityProducers.create(retrievedData.getData().getBytes(outputCharacterSet),
                                                                       outputContentType
                                ))
                                .build(),
            context
        );
    }
}
