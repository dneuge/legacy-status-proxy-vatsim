package de.energiequant.vatsim.compatibility.legacyproxy.fetching;

import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vatplanner.dataformats.vatsimpublic.parser.NetworkInformation;
import org.vatplanner.dataformats.vatsimpublic.parser.legacy.NetworkInformationParser;

import de.energiequant.common.webdataretrieval.DefaultHttpRetrievalDecoders;
import de.energiequant.common.webdataretrieval.HttpPromiseBuilder;
import de.energiequant.common.webdataretrieval.HttpRetrieval;
import de.energiequant.vatsim.compatibility.legacyproxy.AppConstants;

/**
 * Periodically fetches {@link NetworkInformation} as provided in legacy format.
 * {@link #start()} must be called once. {@link #stop()} should be called when
 * the application shuts down. Already retrieved information is available
 * through {@link #getLastFetchedNetworkInformation()} and
 * {@link #getLastAggregatedStartupMessages()}.
 */
public class LegacyNetworkInformationFetcher extends PeriodicRunnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(LegacyNetworkInformationFetcher.class);

    private final String initialUrl;
    private final Duration updateInterval;
    private final Duration retryInterval;
    private final HttpPromiseBuilder<NetworkInformation> httpPromiseBuilder;

    private final AtomicReference<NetworkInformation> lastNetworkInformation = new AtomicReference<>(null);
    private final AtomicReference<List<String>> lastStartupMessages = new AtomicReference<>(new ArrayList<>());

    private static final Charset FALLBACK_CHARACTER_SET = StandardCharsets.ISO_8859_1;

    private static final int MAXIMUM_MOVED_TO_REDIRECTS = 10;

    public LegacyNetworkInformationFetcher(String initialUrl, Duration updateInterval, Duration retryInterval) {
        this.initialUrl = initialUrl;
        this.updateInterval = updateInterval;
        this.retryInterval = retryInterval;

        DefaultHttpRetrievalDecoders decoders = new DefaultHttpRetrievalDecoders();
        httpPromiseBuilder = new HttpPromiseBuilder<>(
            decoders.bodyAsStringWithHeaderCharacterSet(FALLBACK_CHARACTER_SET)
                    .andThen(NetworkInformationParser::parse)
        ).withConfiguration(
            new HttpRetrieval()
                .setUserAgent(AppConstants.USER_AGENT)
                .setTimeout(AppConstants.EXTERNAL_REQUEST_TIMEOUT)
        );
    }

    @Override
    protected Duration onPeriodicWakeup() {
        String url = initialUrl;
        NetworkInformation networkInformation = null;

        List<String> allStartupMessages = new ArrayList<String>();

        for (int i = 0; i <= MAXIMUM_MOVED_TO_REDIRECTS; i++) {
            LOGGER.debug("Updating legacy network information from {} in iteration {}", url, i);

            try {
                networkInformation = httpPromiseBuilder.requestByGet(url).get();
            } catch (InterruptedException | ExecutionException ex) {
                LOGGER.warn("Error while fetching legacy network information from {} in iteration {}", url, i, ex);
                networkInformation = null;
                break;
            }

            List<String> startupMessages = networkInformation.getStartupMessages();
            for (String msg : startupMessages) {
                LOGGER.info("Startup message: {}", msg);
            }
            allStartupMessages.addAll(startupMessages);

            List<URL> movedToUrls = new ArrayList<>(networkInformation.getMovedToUrls());
            if (movedToUrls.isEmpty()) {
                break;
            }

            LOGGER.info(
                "Legacy network information retrieved from {} redirects using \"moved to\" to {} (iteration {})", url,
                movedToUrls, i
            );
            Collections.shuffle(movedToUrls);
            url = movedToUrls.iterator().next().toString();
        }

        if ((networkInformation != null) && !networkInformation.getMovedToUrls().isEmpty()) {
            LOGGER.error("Too many movedTo redirects to follow, unable to retrieve final legacy network information");
            networkInformation = null;
        }

        if (networkInformation != null) {
            lastNetworkInformation.set(networkInformation);
            lastStartupMessages.set(Collections.unmodifiableList(allStartupMessages));

            LOGGER.debug("Legacy Network Information update successful, will update again in {}", updateInterval);
            return updateInterval;
        } else {
            LOGGER.warn("Legacy Network Information update failed, will retry in {}", retryInterval);
            return retryInterval;
        }
    }

    /**
     * Returns the legacy {@link NetworkInformation} as last retrieved.
     *
     * @return legacy {@link NetworkInformation} as last retrieved
     */
    public Optional<NetworkInformation> getLastFetchedNetworkInformation() {
        return Optional.ofNullable(lastNetworkInformation.get());
    }

    /**
     * Returns all startup messages collected during last successful fetch process.
     * This is different from {@link NetworkInformation#getStartupMessages()}
     * available through {@link #getLastFetchedNetworkInformation()} as this result
     * also contains all messages encountered in previous the complete chain of
     * {@link NetworkInformation} while following
     * {@link NetworkInformation#getMovedToUrls()}.
     *
     * @return all startup messages collected while following {@link NetworkInformation#getMovedToUrls()}
     */
    public Optional<List<String>> getLastAggregatedStartupMessages() {
        return Optional.ofNullable(lastStartupMessages.get());
    }
}
