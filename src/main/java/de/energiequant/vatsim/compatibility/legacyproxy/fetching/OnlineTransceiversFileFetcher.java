package de.energiequant.vatsim.compatibility.legacyproxy.fetching;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vatplanner.dataformats.vatsimpublic.parser.OnlineTransceiversFile;
import org.vatplanner.dataformats.vatsimpublic.parser.json.onlinetransceivers.OnlineTransceiversFileProcessor;

import de.energiequant.common.webdataretrieval.DefaultHttpRetrievalDecoders;
import de.energiequant.common.webdataretrieval.HttpPromiseBuilder;
import de.energiequant.common.webdataretrieval.HttpRetrieval;
import de.energiequant.vatsim.compatibility.legacyproxy.AppConstants;

/**
 * Periodically fetches the {@link OnlineTransceiversFile}.
 * {@link #waitForOnlineTransceiversFile(Duration)} should be used to retrieve
 * the file which will automatically {@link #start()} the fetcher if not already
 * running. Further requests will be served from a periodically updated cache.
 * When not requested for the configured amount of time, fetching will
 * {@link #stop()} automatically until data is requested the next time.
 */
public class OnlineTransceiversFileFetcher extends PeriodicRunnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(OnlineTransceiversFileFetcher.class);

    private final Supplier<String> urlSupplier;
    private final Supplier<Duration> updateIntervalSupplier;
    private final Duration retryInterval;
    private final Duration idleTimeout;
    private final AtomicReference<Instant> lastRequested = new AtomicReference<>(Instant.now());
    private final HttpPromiseBuilder<OnlineTransceiversFile> httpPromiseBuilder;

    private final AtomicReference<OnlineTransceiversFile> lastFile = new AtomicReference<>(null);

    private static final Charset FALLBACK_CHARACTER_SET = StandardCharsets.UTF_8;

    private static final long WAIT_CHECK_INTERVAL_MILLIS = 100;

    public OnlineTransceiversFileFetcher(Supplier<String> urlSupplier, Supplier<Duration> updateIntervalSupplier, Duration retryInterval, Duration idleTimeout) {
        this.urlSupplier = urlSupplier;
        this.updateIntervalSupplier = updateIntervalSupplier;
        this.retryInterval = retryInterval;
        this.idleTimeout = idleTimeout;

        OnlineTransceiversFileProcessor processor = new OnlineTransceiversFileProcessor();

        DefaultHttpRetrievalDecoders decoders = new DefaultHttpRetrievalDecoders();
        httpPromiseBuilder = new HttpPromiseBuilder<>(
            decoders.bodyAsStringWithHeaderCharacterSet(FALLBACK_CHARACTER_SET)
                    .andThen(processor::deserialize)
        ).withConfiguration(
            new HttpRetrieval()
                .setUserAgent(AppConstants.USER_AGENT)
                .setTimeout(AppConstants.EXTERNAL_REQUEST_TIMEOUT)
        );
    }

    @Override
    protected Duration onPeriodicWakeup() {
        boolean isIdleTimeoutExceeded = Instant.now().isAfter(lastRequested.get().plus(idleTimeout));
        if (isIdleTimeoutExceeded) {
            LOGGER.info(
                "Stopping periodic retrieval of online transceivers as no such data has been needed for {} minutes",
                Duration.between(lastRequested.get(), Instant.now()).toMinutes()
            );

            stop();
            return retryInterval;
        }

        OnlineTransceiversFile file = null;
        try {
            String url = urlSupplier.get();
            if (url == null) {
                LOGGER.warn("No URL available, will retry in {}", retryInterval);
                return retryInterval;
            }

            file = httpPromiseBuilder.requestByGet(url).get();
        } catch (InterruptedException | ExecutionException ex) {
            LOGGER.warn("Online transceivers update failed, will retry in {}", retryInterval, ex);
            return retryInterval;
        }

        lastFile.set(file);

        Duration updateInterval = updateIntervalSupplier.get();
        LOGGER.debug("Online transceivers update successful, will update again in {}", updateInterval);
        return updateInterval;
    }

    private Optional<OnlineTransceiversFile> getLastFetchedFile() {
        return Optional.ofNullable(lastFile.get());
    }

    /**
     * Waits blocking for data to become available or the timeout has been reached.
     *
     * @param timeout how long to wait before giving up
     * @return {@link OnlineTransceiversFile} as last retrieved, empty if unavailable after timeout
     */
    public Optional<OnlineTransceiversFile> waitForOnlineTransceiversFile(Duration timeout) {
        lastRequested.set(Instant.now());

        if (!isAlive()) {
            LOGGER.debug("Online transceivers fetcher is not running yet, starting...");
            start();
        }

        Optional<OnlineTransceiversFile> file = getLastFetchedFile();

        Instant wakeupTime = Instant.now().plus(timeout);
        while (!file.isPresent() && Instant.now().isBefore(wakeupTime)) {
            try {
                Thread.sleep(WAIT_CHECK_INTERVAL_MILLIS);
            } catch (InterruptedException ex) {
                LOGGER.debug("sleep got interrupted, abort waiting", ex);
                break;
            }

            file = getLastFetchedFile();
        }

        return file;
    }

    @Override
    public void start() {
        LOGGER.info("Starting periodic retrieval of online transceivers");
        lastFile.set(null);
        super.start();
    }
}
