package de.energiequant.vatsim.compatibility.legacyproxy.server;

import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import org.apache.hc.core5.http.ConnectionReuseStrategy;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.Message;
import org.apache.hc.core5.http.URIScheme;
import org.apache.hc.core5.http.impl.bootstrap.AsyncServerBootstrap;
import org.apache.hc.core5.http.impl.bootstrap.HttpAsyncServer;
import org.apache.hc.core5.http.nio.AsyncServerRequestHandler;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.reactor.IOReactorConfig;
import org.apache.hc.core5.reactor.ListenerEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vatplanner.dataformats.vatsimpublic.export.LegacyNetworkInformationWriter;
import org.vatplanner.dataformats.vatsimpublic.parser.DataFileParserFactory;
import org.vatplanner.dataformats.vatsimpublic.parser.OnlineTransceiversFile;

import de.energiequant.vatsim.compatibility.legacyproxy.AppConstants;
import de.energiequant.vatsim.compatibility.legacyproxy.Configuration;
import de.energiequant.vatsim.compatibility.legacyproxy.Main;
import de.energiequant.vatsim.compatibility.legacyproxy.ServiceEndpoints;
import de.energiequant.vatsim.compatibility.legacyproxy.fetching.JsonNetworkInformationFetcher;
import de.energiequant.vatsim.compatibility.legacyproxy.fetching.LegacyNetworkInformationFetcher;
import de.energiequant.vatsim.compatibility.legacyproxy.fetching.OnlineTransceiversFileFetcher;

public class Server {
    private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);

    private static final Duration NETWORK_INFORMATION_UPDATE_INTERVAL = Duration.ofHours(6);
    private static final Duration NETWORK_INFORMATION_RETRY_INTERVAL = Duration.ofMinutes(5);

    private static final Duration ONLINE_TRANSCEIVERS_MINIMUM_ALLOWED_UPDATE_INTERVAL = Duration.ofMinutes(1);
    private static final Duration ONLINE_TRANSCEIVERS_RETRY_INTERVAL = Duration.ofMinutes(1);
    private static final Duration ONLINE_TRANSCEIVERS_IDLE_TIMEOUT = Duration.ofMinutes(5);

    /**
     * Default interval until we got at least one data file which tells us the
     * actual data retrieval interval requested by upstream.
     */
    private static final Duration ASSUMED_DEFAULT_MINIMUM_DATA_UPDATE_INTERVAL = Duration.ofMinutes(5);

    private final LegacyNetworkInformationFetcher legacyNetworkInformationFetcher;
    private final JsonNetworkInformationFetcher jsonNetworkInformationFetcher;
    private final OnlineTransceiversFileFetcher onlineTransceiversFileFetcher;
    private final AtomicReference<HttpAsyncServer> httpServer = new AtomicReference<>();
    private final IPFilter ipFilter = new IPFilter();

    private static final ConnectionReuseStrategy NEVER_REUSE_CONNECTIONS = (request, response, context) -> false;

    private final String upstreamBaseUrl = Main.getConfiguration().getUpstreamBaseUrl();
    private final AtomicReference<Duration> authoritativeMinimumDataUpdateInterval = new AtomicReference<>(
        ASSUMED_DEFAULT_MINIMUM_DATA_UPDATE_INTERVAL);

    private final boolean shouldShutdownOnStartFailure;
    private final AtomicReference<State> state = new AtomicReference<>(State.INITIAL);
    private final Set<Runnable> stateChangeListeners = Collections.synchronizedSet(new HashSet<>());

    private static final long COMMAND_TIMEOUT = 2000;
    private final Deque<Command> commandQueue = new LinkedList<>();
    private final Thread commandThread = new Thread(() -> {
        while (state.get() != State.FULLY_STOPPED) {
            Command command = null;
            synchronized (commandQueue) {
                if (commandQueue.isEmpty()) {
                    try {
                        commandQueue.wait(COMMAND_TIMEOUT);
                    } catch (InterruptedException ex) {
                        // ignore, expected
                    }
                }

                command = commandQueue.pollFirst();
            }

            if (command != null) {
                onCommand(command);
            }
        }
    });

    public static enum State {
        INITIAL,
        BLOCKED_BY_DISCLAIMER,
        RUNNING,
        HTTP_SERVER_STOPPED,
        FULLY_STOPPED;
    }

    private static enum Command {
        START_HTTP_SERVER,
        STOP_HTTP_SERVER,
        STOP_ALL,
        NOTIFY_STATE_CHANGE_LISTENERS;
    }

    public Server(boolean shouldShutdownOnStartFailure) {
        this.shouldShutdownOnStartFailure = shouldShutdownOnStartFailure;

        legacyNetworkInformationFetcher = new LegacyNetworkInformationFetcher(
            upstreamBaseUrl + ServiceEndpoints.NETWORK_INFORMATION_LEGACY,
            NETWORK_INFORMATION_UPDATE_INTERVAL,
            NETWORK_INFORMATION_RETRY_INTERVAL
        );

        jsonNetworkInformationFetcher = new JsonNetworkInformationFetcher(
            upstreamBaseUrl + ServiceEndpoints.NETWORK_INFORMATION_JSON,
            NETWORK_INFORMATION_UPDATE_INTERVAL,
            NETWORK_INFORMATION_RETRY_INTERVAL
        );

        onlineTransceiversFileFetcher = new OnlineTransceiversFileFetcher(
            () -> {
                Configuration config = Main.getConfiguration();
                if (config.isOnlineTransceiversOverrideEnabled()) {
                    return config.getOnlineTransceiversOverrideUrl();
                }

                List<URL> transceiversUrls = jsonNetworkInformationFetcher.getLastFetchedNetworkInformation()
                                                                          .map(x -> x.getDataUrls(OnlineTransceiversFile.Format.INITIAL))
                                                                          .orElse(new ArrayList<>());

                if (transceiversUrls.isEmpty()) {
                    return null;
                }

                return pickRandomItem(transceiversUrls).toString();
            },
            () -> {
                Configuration config = Main.getConfiguration();

                if (config.isOnlineTransceiversOverrideEnabled()) {
                    return Duration.ofMinutes(config.getOnlineTransceiversOverrideCacheMinutes());
                }

                return max(authoritativeMinimumDataUpdateInterval.get(),
                           ONLINE_TRANSCEIVERS_MINIMUM_ALLOWED_UPDATE_INTERVAL
                );
            },
            ONLINE_TRANSCEIVERS_RETRY_INTERVAL,
            ONLINE_TRANSCEIVERS_IDLE_TIMEOUT
        );

        LOGGER.info("Starting NetworkInformation fetcher threads");
        legacyNetworkInformationFetcher.start();
        jsonNetworkInformationFetcher.start();

        Main.getConfiguration().addIPFilterListener(this::updateIPFilter);
        updateIPFilter();

        commandThread.start();
    }

    private void updateIPFilter() {
        ipFilter.allowOnly(Main.getConfiguration().getAllowedIps());
    }

    public void startHttpServer() {
        queueCommand(Command.START_HTTP_SERVER);
    }

    private void _startHttpServer() {
        if (state.get() == State.FULLY_STOPPED) {
            LOGGER.error("Server has completely stopped and cannot be restarted");
            return;
        } else if (state.get() == State.RUNNING) {
            LOGGER.info("Server is already running, not starting again");
            return;
        }

        if (!Main.getDisclaimerState().isAccepted()) {
            LOGGER.warn("Server can only be started after accepting the disclaimer");
            setState(State.BLOCKED_BY_DISCLAIMER);
            return;
        }

        LOGGER.info("Starting HTTP server");

        Configuration config = Main.getConfiguration();
        String localHostname = config.getLocalHostName();
        int localPort = config.getServerPort();

        AsyncServerRequestHandler<Message<HttpRequest, Void>> legacyNetworkInformationRequestHandler = new InjectingLegacyNetworkInformationProxy(
            "http://" + localHostname + ":" + localPort,
            new LegacyNetworkInformationWriter(AppConstants.SERVER_DISCLAIMER_HEADER),
            legacyNetworkInformationFetcher::getLastFetchedNetworkInformation,
            jsonNetworkInformationFetcher::getLastFetchedNetworkInformation,
            legacyNetworkInformationFetcher::getLastAggregatedStartupMessages
        );

        HttpAsyncServer myHttpServer = AsyncServerBootstrap
            .bootstrap()
            .setIOReactorConfig(IOReactorConfig.custom()
                                               .setSoReuseAddress(true)
                                               .build()
            )
            .addFilterFirst("ipFilter", ipFilter)
            .setCanonicalHostName(localHostname)
            .setConnectionReuseStrategy(NEVER_REUSE_CONNECTIONS)
            .register(
                ServiceEndpoints.DATA_FILE_LEGACY,
                new JsonToLegacyDataFileProxy(
                    new DataFileParserFactory().createDataFileParser(AppConstants.UPSTREAM_DATA_FILE_FORMAT),
                    onlineTransceiversFileFetcher,
                    () -> {
                        Set<URL> combined = new HashSet<>();
                        combined.addAll(
                            jsonNetworkInformationFetcher.getLastFetchedNetworkInformation()
                                                         .map(x -> x.getDataUrls(AppConstants.UPSTREAM_DATA_FILE_FORMAT))
                                                         .orElse(new ArrayList<>())
                        );
                        combined.addAll(
                            legacyNetworkInformationFetcher.getLastFetchedNetworkInformation()
                                                           .map(x -> x.getDataUrls(AppConstants.UPSTREAM_DATA_FILE_FORMAT))
                                                           .orElse(new ArrayList<>())
                        );

                        if (combined.isEmpty()) {
                            return null;
                        }

                        return pickRandomItem(combined).toString();
                    },
                    this::setAuthoritativeMinimumDataUpdateInterval
                )
            )
            .register(
                ServiceEndpoints.NETWORK_INFORMATION_JSON,
                new DirectProxy(
                    upstreamBaseUrl + ServiceEndpoints.NETWORK_INFORMATION_JSON,
                    StandardCharsets.UTF_8,
                    ContentType.APPLICATION_JSON
                )
            )
            .register(ServiceEndpoints.NETWORK_INFORMATION_LEGACY, legacyNetworkInformationRequestHandler)
            .register("/", legacyNetworkInformationRequestHandler)
            .register("*", new SimpleErrorResponse(HttpStatus.SC_NOT_FOUND, "not found"))
            .create();

        httpServer.set(myHttpServer);

        myHttpServer.start();
        Future<ListenerEndpoint> future = myHttpServer.listen(new InetSocketAddress(localPort), URIScheme.HTTP);
        setState(State.RUNNING);
        ListenerEndpoint listenerEndpoint;
        try {
            listenerEndpoint = future.get();
            LOGGER.info("HTTP server is listening on {}", listenerEndpoint.getAddress());
        } catch (InterruptedException | ExecutionException ex) {
            if (shouldShutdownOnStartFailure) {
                LOGGER.error("Failed to start HTTP server, shutting down...", ex);
                stopAll();
            } else {
                LOGGER.error("Failed to start HTTP server", ex);
                stopHttpServer();
            }
        }
    }

    public void stopHttpServer() {
        queueCommand(Command.STOP_HTTP_SERVER);
    }

    private void _stopHttpServer() {
        if (state.get() != State.RUNNING) {
            LOGGER.info("HTTP server is not running");
            return;
        }

        LOGGER.info("Stopping HTTP server");
        httpServer.get().close(CloseMode.IMMEDIATE);

        LOGGER.info("Stopped HTTP server");
        setState(State.HTTP_SERVER_STOPPED);
    }

    public void stopAll() {
        queueCommand(Command.STOP_ALL);
    }

    private void _stopAll() {
        LOGGER.info("Stopping fetcher threads");
        legacyNetworkInformationFetcher.stop();
        jsonNetworkInformationFetcher.stop();
        onlineTransceiversFileFetcher.stop();

        _stopHttpServer();

        LOGGER.info("Shutdown complete");
        setState(State.FULLY_STOPPED);
    }

    private static <T> T pickRandomItem(Collection<T> items) {
        List<T> modifiableList = new ArrayList<T>(items);
        Collections.shuffle(modifiableList);
        return modifiableList.iterator().next();
    }

    private static <T> Predicate<T> not(Predicate<T> original) {
        return original.negate();
    }

    public State getState() {
        return state.get();
    }

    private void queueCommand(Command command) {
        synchronized (commandQueue) {
            commandQueue.add(command);
            commandQueue.notifyAll();
        }
    }

    public void addStateChangeListener(Runnable listener) {
        stateChangeListeners.add(listener);
    }

    private void notifyStateChangeListeners() {
        Collection<Runnable> copy = new ArrayList<>(stateChangeListeners);
        for (Runnable listener : copy) {
            try {
                listener.run();
            } catch (Exception ex) {
                LOGGER.warn("Failed to notify server state change listener", ex);
            }
        }
    }

    private void setState(State newState) {
        state.set(newState);
        queueCommand(Command.NOTIFY_STATE_CHANGE_LISTENERS);
    }

    private void onCommand(Command command) {
        switch (command) {
            case START_HTTP_SERVER:
                _startHttpServer();
                break;

            case STOP_HTTP_SERVER:
                _stopHttpServer();
                break;

            case STOP_ALL:
                _stopAll();
                break;

            case NOTIFY_STATE_CHANGE_LISTENERS:
                notifyStateChangeListeners();
                break;

            default:
                throw new IllegalArgumentException("Unsupported command: " + command);
        }
    }

    private void setAuthoritativeMinimumDataUpdateInterval(Duration interval) {
        Duration previous = authoritativeMinimumDataUpdateInterval.getAndSet(interval);
        if (!previous.equals(interval)) {
            LOGGER.debug("new authoritative minimum data retrieval interval: {}", interval);
        }
    }

    private Duration max(Duration a, Duration b) {
        if (a.compareTo(b) > 0) {
            return a;
        } else {
            return b;
        }
    }
}
