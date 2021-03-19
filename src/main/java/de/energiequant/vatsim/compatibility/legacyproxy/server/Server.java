package de.energiequant.vatsim.compatibility.legacyproxy.server;

import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Predicate;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.Message;
import org.apache.hc.core5.http.impl.bootstrap.AsyncServerBootstrap;
import org.apache.hc.core5.http.impl.bootstrap.HttpAsyncServer;
import org.apache.hc.core5.http.nio.AsyncServerRequestHandler;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.reactor.IOReactorConfig;
import org.apache.hc.core5.reactor.ListenerEndpoint;
import org.apache.hc.core5.util.TimeValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vatplanner.dataformats.vatsimpublic.export.LegacyNetworkInformationWriter;
import org.vatplanner.dataformats.vatsimpublic.parser.DataFileParserFactory;

import de.energiequant.vatsim.compatibility.legacyproxy.AppConstants;
import de.energiequant.vatsim.compatibility.legacyproxy.ServiceEndpoints;
import de.energiequant.vatsim.compatibility.legacyproxy.fetching.JsonNetworkInformationFetcher;
import de.energiequant.vatsim.compatibility.legacyproxy.fetching.LegacyNetworkInformationFetcher;

public class Server {
    // FIXME: port remains blocked even after proper shutdown?
    // FIXME: server can be stopped twice (second shutdown should be ignored)

    private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);

    private static final Duration NETWORK_INFORMATION_UPDATE_INTERVAL = Duration.ofHours(6);
    private static final Duration NETWORK_INFORMATION_RETRY_INTERVAL = Duration.ofMinutes(5);

    private static final String DISCLAIMER_HEADER = "; YOU ARE ACCESSING THE COMPATIBILITY ADAPTER PROXY FOR VATSIM DATA FILES\n" //
        + ";\n" //
        + "; This proxy server is supposed to be used only in order to establish compatibility between legacy applications and later revisions of status/data files. The intended use does not cover clients used for active participation on the VATSIM network (e.g. pilot/ATC clients).\n" //
        + ";\n" //
        + "; The proxy is inofficial and not supported by VATSIM. Please avoid running pilot or ATC clients using files provided through this proxy.\n" //
        + ";\n" //
        + "; If, even if you do not have connected an active client through this proxy, you experience any issues accessing data or connecting to VATSIM please disable the proxy server and try again.\n" //
        + ";\n" //
        + "; VATSIM data served by this server remains under copyright of VATSIM. Usage of that data remains subject to conditions defined by VATSIM.\n";

    private final LegacyNetworkInformationFetcher legacyNetworkInformationFetcher;
    private final JsonNetworkInformationFetcher jsonNetworkInformationFetcher;
    private final HttpAsyncServer httpServer;

    private final String upstreamBaseUrl = "http://status.vatsim.net";
    private final String localHostname = "localhost";
    private final int localPort = 8080;

    public Server() {
        legacyNetworkInformationFetcher = new LegacyNetworkInformationFetcher(
            upstreamBaseUrl + ServiceEndpoints.NETWORK_INFORMATION_LEGACY,
            NETWORK_INFORMATION_UPDATE_INTERVAL,
            NETWORK_INFORMATION_RETRY_INTERVAL);

        jsonNetworkInformationFetcher = new JsonNetworkInformationFetcher(
            upstreamBaseUrl + ServiceEndpoints.NETWORK_INFORMATION_JSON,
            NETWORK_INFORMATION_UPDATE_INTERVAL,
            NETWORK_INFORMATION_RETRY_INTERVAL);

        LOGGER.info("Starting fetcher threads");
        legacyNetworkInformationFetcher.start();
        jsonNetworkInformationFetcher.start();

        // FIXME: configure
        IPFilter ipFilter = new IPFilter() //
            .allow(IPFilter.LOCALHOST_IPV4) //
            .allow(IPFilter.LOCALHOST_IPV6);

        AsyncServerRequestHandler<Message<HttpRequest, Void>> legacyNetworkInformationRequestHandler = new InjectingLegacyNetworkInformationProxy(
            "http://" + localHostname + ":" + localPort,
            new LegacyNetworkInformationWriter(DISCLAIMER_HEADER),
            legacyNetworkInformationFetcher::getLastFetchedNetworkInformation,
            jsonNetworkInformationFetcher::getLastFetchedNetworkInformation,
            legacyNetworkInformationFetcher::getLastAggregatedStartupMessages);

        httpServer = AsyncServerBootstrap.bootstrap()
            .setIOReactorConfig(IOReactorConfig.DEFAULT)
            .addFilterFirst("ipFilter", ipFilter)
            .register(ServiceEndpoints.DATA_FILE_LEGACY,
                new JsonToLegacyDataFileProxy(
                    new DataFileParserFactory().createDataFileParser(AppConstants.UPSTREAM_DATA_FILE_FORMAT),
                    () -> {
                        // TODO: include URLs from legacy NetworkInformation
                        return jsonNetworkInformationFetcher.getLastFetchedNetworkInformation() //
                            .map(x -> x.getDataFileUrls(AppConstants.UPSTREAM_DATA_FILE_FORMAT)) //
                            .filter(not(List::isEmpty)) //
                            .map(Server::pickRandomItem) //
                            .map(URL::toString) //
                            .orElse(null);
                    } //
                ) //
            )
            .register(ServiceEndpoints.NETWORK_INFORMATION_JSON, new DirectProxy(
                upstreamBaseUrl + ServiceEndpoints.NETWORK_INFORMATION_JSON, //
                StandardCharsets.UTF_8, //
                ContentType.APPLICATION_JSON //
            ))
            .register(ServiceEndpoints.NETWORK_INFORMATION_LEGACY, legacyNetworkInformationRequestHandler)
            .register("/", legacyNetworkInformationRequestHandler)
            .register("*", new SimpleErrorResponse(HttpStatus.SC_NOT_FOUND, "not found"))
            .create();
    }

    public void start() {
        LOGGER.info("Starting server");
        httpServer.start();
        Future<ListenerEndpoint> future = httpServer.listen(new InetSocketAddress(localPort));
        ListenerEndpoint listenerEndpoint;
        try {
            listenerEndpoint = future.get();
            LOGGER.info("Server is listening on {}", listenerEndpoint.getAddress());
        } catch (InterruptedException | ExecutionException ex) {
            LOGGER.error("Failed to start server, shutting down...", ex);
            stop();
        }
    }

    public void stop() {
        LOGGER.info("Stopping fetcher threads");
        legacyNetworkInformationFetcher.stop();
        jsonNetworkInformationFetcher.stop();

        LOGGER.info("Stopping server");
        httpServer.close(CloseMode.IMMEDIATE);

        LOGGER.info("Shutdown complete");
    }

    public void awaitShutdown() {
        try {
            httpServer.awaitShutdown(TimeValue.MAX_VALUE);
        } catch (InterruptedException ex) {
            LOGGER.info("Interrupted, stopping server...", ex);
        }

        stop();
    }

    private static <T> T pickRandomItem(Collection<T> items) {
        List<T> modifiableList = new ArrayList<T>(items);
        Collections.shuffle(modifiableList);
        return modifiableList.iterator().next();
    }

    private static <T> Predicate<T> not(Predicate<T> original) {
        return original.negate();
    }
}
