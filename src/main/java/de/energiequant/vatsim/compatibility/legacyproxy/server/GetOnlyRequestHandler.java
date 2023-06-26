package de.energiequant.vatsim.compatibility.legacyproxy.server;

import java.io.IOException;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.Message;
import org.apache.hc.core5.http.nio.AsyncRequestConsumer;
import org.apache.hc.core5.http.nio.AsyncServerRequestHandler;
import org.apache.hc.core5.http.nio.entity.AsyncEntityProducers;
import org.apache.hc.core5.http.nio.entity.NoopEntityConsumer;
import org.apache.hc.core5.http.nio.support.AsyncResponseBuilder;
import org.apache.hc.core5.http.nio.support.BasicRequestConsumer;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class GetOnlyRequestHandler implements AsyncServerRequestHandler<Message<HttpRequest, Void>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetOnlyRequestHandler.class);

    @Override
    public AsyncRequestConsumer<Message<HttpRequest, Void>> prepare(final HttpRequest request, final EntityDetails entityDetails, final HttpContext context) throws HttpException {
        return new BasicRequestConsumer<>(entityDetails != null ? new NoopEntityConsumer() : null);
    }

    @Override
    public void handle(final Message<HttpRequest, Void> message, final ResponseTrigger responseTrigger, final HttpContext context) throws HttpException, IOException {
        final HttpRequest request = message.getHead();
        String method = request.getMethod();
        if (!"GET".equals(method)) {
            LOGGER.warn("Client requested unsupported method {}", method);

            responseTrigger.submitResponse(
                AsyncResponseBuilder.create(HttpStatus.SC_METHOD_NOT_ALLOWED)
                                    .setEntity(AsyncEntityProducers.create("Method not allowed", ContentType.TEXT_PLAIN))
                                    .build(),
                context
            );
            return;
        }

        handleGet(message, responseTrigger, context);
    }

    protected abstract void handleGet(final Message<HttpRequest, Void> message, final ResponseTrigger responseTrigger, final HttpContext context) throws HttpException, IOException;
}
