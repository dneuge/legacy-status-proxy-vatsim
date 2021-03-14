package de.energiequant.vatsim.compatibility.legacyproxy.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import org.apache.hc.core5.http.EndpointDetails;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.message.BasicHttpResponse;
import org.apache.hc.core5.http.nio.AsyncDataConsumer;
import org.apache.hc.core5.http.nio.AsyncEntityProducer;
import org.apache.hc.core5.http.nio.AsyncFilterChain;
import org.apache.hc.core5.http.nio.AsyncFilterHandler;
import org.apache.hc.core5.http.nio.entity.AsyncEntityProducers;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.http.protocol.HttpCoreContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IPFilter implements AsyncFilterHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(IPFilter.class);

    public static final String LOCALHOST_IPV4 = "127.0.0.1";
    public static final String LOCALHOST_IPV6 = "0:0:0:0:0:0:0:1";

    private final Collection<String> allowedAddresses = Collections.synchronizedCollection(new HashSet<String>());

    @Override
    public AsyncDataConsumer handle(HttpRequest request, EntityDetails entityDetails, HttpContext context, org.apache.hc.core5.http.nio.AsyncFilterChain.ResponseTrigger responseTrigger, AsyncFilterChain chain) throws HttpException, IOException {
        EndpointDetails details = (EndpointDetails) context
            .getAttribute(HttpCoreContext.CONNECTION_ENDPOINT);
        InetSocketAddress addr = (InetSocketAddress) details.getRemoteAddress();
        String ip = addr.getAddress().getHostAddress();

        if (allowedAddresses.contains(ip)) {
            LOGGER.debug("allowing connection from {}", ip);
            return chain.proceed(request, entityDetails, context, responseTrigger);
        }

        LOGGER.warn("rejecting connection from {}", ip);

        final HttpResponse unauthorized = new BasicHttpResponse(HttpStatus.SC_FORBIDDEN);
        final AsyncEntityProducer responseContentProducer = AsyncEntityProducers.create("Forbidden");
        responseTrigger.submitResponse(unauthorized, responseContentProducer);

        return null;
    }

    public IPFilter allow(String ip) {
        allowedAddresses.add(ip);
        return this;
    }
}
