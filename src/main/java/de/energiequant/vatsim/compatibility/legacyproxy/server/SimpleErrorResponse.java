package de.energiequant.vatsim.compatibility.legacyproxy.server;

import java.io.IOException;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.Message;
import org.apache.hc.core5.http.nio.AsyncRequestConsumer;
import org.apache.hc.core5.http.nio.AsyncServerRequestHandler;
import org.apache.hc.core5.http.nio.entity.AsyncEntityProducers;
import org.apache.hc.core5.http.nio.entity.NoopEntityConsumer;
import org.apache.hc.core5.http.nio.support.AsyncResponseBuilder;
import org.apache.hc.core5.http.nio.support.BasicRequestConsumer;
import org.apache.hc.core5.http.protocol.HttpContext;

public class SimpleErrorResponse implements AsyncServerRequestHandler<Message<HttpRequest, Void>> {
    private final int statusCode;
    private final String responseText;

    public SimpleErrorResponse(int statusCode, String responseText) {
        this.statusCode = statusCode;
        this.responseText = responseText;
    }

    @Override
    public AsyncRequestConsumer<Message<HttpRequest, Void>> prepare(HttpRequest request, EntityDetails entityDetails, HttpContext context) throws HttpException {
        return new BasicRequestConsumer<>(entityDetails != null ? new NoopEntityConsumer() : null);
    }

    @Override
    public void handle(final Message<HttpRequest, Void> message, final ResponseTrigger responseTrigger, final HttpContext context) throws HttpException, IOException {
        responseTrigger.submitResponse(
            AsyncResponseBuilder.create(statusCode)
                .setEntity(AsyncEntityProducers.create(responseText, ContentType.TEXT_PLAIN))
                .build(),
            context);
    }
}
