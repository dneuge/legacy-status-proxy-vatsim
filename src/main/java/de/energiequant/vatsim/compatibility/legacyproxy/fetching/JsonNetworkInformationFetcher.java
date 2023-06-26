package de.energiequant.vatsim.compatibility.legacyproxy.fetching;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vatplanner.dataformats.vatsimpublic.parser.NetworkInformation;
import org.vatplanner.dataformats.vatsimpublic.parser.json.NetworkInformationProcessor;

import de.energiequant.common.webdataretrieval.DefaultHttpRetrievalDecoders;
import de.energiequant.common.webdataretrieval.HttpPromiseBuilder;
import de.energiequant.common.webdataretrieval.HttpRetrieval;
import de.energiequant.vatsim.compatibility.legacyproxy.AppConstants;

/**
 * Periodically fetches {@link NetworkInformation} as provided in JSON format.
 * {@link #start()} must be called once. {@link #stop()} should be called when
 * the application shuts down. Already retrieved information is available
 * through {@link #getLastFetchedNetworkInformation()}.
 * <p>
 * The JSON file may not contain all information to construct a legacy
 * {@link NetworkInformation} as it appears to be missing some fields.
 * </p>
 */
public class JsonNetworkInformationFetcher extends PeriodicRunnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonNetworkInformationFetcher.class);

    private final String url;
    private final Duration updateInterval;
    private final Duration retryInterval;
    private final HttpPromiseBuilder<NetworkInformation> httpPromiseBuilder;

    private final AtomicReference<NetworkInformation> lastNetworkInformation = new AtomicReference<>(null);

    private static final Charset FALLBACK_CHARACTER_SET = StandardCharsets.UTF_8;

    public JsonNetworkInformationFetcher(String url, Duration updateInterval, Duration retryInterval) {
        this.url = url;
        this.updateInterval = updateInterval;
        this.retryInterval = retryInterval;

        NetworkInformationProcessor processor = new NetworkInformationProcessor();

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
        NetworkInformation networkInformation = null;
        try {
            networkInformation = httpPromiseBuilder.requestByGet(url).get();
        } catch (InterruptedException | ExecutionException ex) {
            LOGGER.warn("JSON Network Information update failed, will retry in {}", retryInterval, ex);
            return retryInterval;
        }

        lastNetworkInformation.set(networkInformation);

        LOGGER.debug("JSON Network Information update successful, will update again in {}", updateInterval);
        return updateInterval;
    }

    /**
     * Returns the JSON {@link NetworkInformation} as last retrieved.
     *
     * @return JSON {@link NetworkInformation} as last retrieved
     */
    public Optional<NetworkInformation> getLastFetchedNetworkInformation() {
        return Optional.ofNullable(lastNetworkInformation.get());
    }
}
