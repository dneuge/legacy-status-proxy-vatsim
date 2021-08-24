package de.energiequant.vatsim.compatibility.legacyproxy.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
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
import org.vatplanner.dataformats.vatsimpublic.export.Writer;
import org.vatplanner.dataformats.vatsimpublic.parser.DataFileFormat;
import org.vatplanner.dataformats.vatsimpublic.parser.NetworkInformation;

import de.energiequant.vatsim.compatibility.legacyproxy.ServiceEndpoints;

public class InjectingLegacyNetworkInformationProxy extends GetOnlyRequestHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(InjectingLegacyNetworkInformationProxy.class);

    private final String localBaseUrl;
    private final Writer<NetworkInformation> writer;
    private final Supplier<Optional<NetworkInformation>> legacyNetworkInformationSupplier;
    private final Supplier<Optional<NetworkInformation>> jsonNetworkInformationSupplier;
    private final Supplier<Optional<List<String>>> startupMessagesSupplier;

    public InjectingLegacyNetworkInformationProxy(String localBaseUrl, Writer<NetworkInformation> writer, Supplier<Optional<NetworkInformation>> legacyNetworkInformationSupplier, Supplier<Optional<NetworkInformation>> jsonNetworkInformationSupplier, Supplier<Optional<List<String>>> startupMessagesSupplier) {
        this.localBaseUrl = localBaseUrl;
        this.writer = writer;
        this.legacyNetworkInformationSupplier = legacyNetworkInformationSupplier;
        this.jsonNetworkInformationSupplier = jsonNetworkInformationSupplier;
        this.startupMessagesSupplier = startupMessagesSupplier;
    }

    @Override
    protected void handleGet(final Message<HttpRequest, Void> message, final ResponseTrigger responseTrigger, final HttpContext context) throws HttpException, IOException {
        LOGGER.debug("Processing request for legacy network information");

        NetworkInformation legacy = legacyNetworkInformationSupplier.get().orElse(null);
        if (legacy == null) {
            LOGGER.warn("upstream legacy network information is unavailable; unable to serve request");

            responseTrigger.submitResponse(
                AsyncResponseBuilder.create(HttpStatus.SC_SERVICE_UNAVAILABLE)
                    .setEntity(AsyncEntityProducers.create("upstream legacy network information is unavailable",
                        ContentType.TEXT_PLAIN))
                    .build(),
                context);
            return;
        }

        NetworkInformation json = jsonNetworkInformationSupplier.get().orElse(null);
        if (json == null) {
            LOGGER.warn("upstream JSON network information is unavailable; served information may be incomplete");
            json = new NetworkInformation();
        }

        NetworkInformation merged = new NetworkInformation().addAll(legacy).addAll(json);

        NetworkInformation out = new NetworkInformation();
        out.setWhazzUpString(merged.getWhazzUpString());

        Map<String, List<URL>> urlsByJsonKey = merged.getAllUrlsByDataKey();
        for (Entry<String, List<URL>> entry : urlsByJsonKey.entrySet()) {
            String jsonKey = entry.getKey();

            // skip legacy URLs, those are to be replaced by the proxy
            if (DataFileFormat.Constants.LEGACY_JSON_KEY.equals(jsonKey)) {
                continue;
            }

            Set<URL> deduplicatedUrls = new HashSet<>(entry.getValue());
            for (URL url : deduplicatedUrls) {
                out.addAsDataUrl(jsonKey, url.toString());
            }
        }

        out.addAsDataUrl(
            DataFileFormat.LEGACY.getNetworkInformationDataKey(), //
            localBaseUrl + ServiceEndpoints.DATA_FILE_LEGACY //
        );

        copy(merged.getMetarUrls(), out, NetworkInformation.PARAMETER_KEY_URL_METAR);
        copy(merged.getServersFileUrls(), out, NetworkInformation.PARAMETER_KEY_URL_SERVERS_FILE);
        copy(merged.getUserStatisticsUrls(), out, NetworkInformation.PARAMETER_KEY_URL_USER_STATISTICS);

        // startup messages are not used from merged NetworkInformation directly but
        // instead taken from aggregated list collected while following moveTo redirects
        startupMessagesSupplier.get() //
            .orElse(new ArrayList<>()) //
            .forEach(out::addStartupMessage);

        LOGGER.debug("Encoding legacy network information");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writer.serialize(out, baos);
        baos.flush();
        byte[] bytes = baos.toByteArray();
        baos = null;

        responseTrigger.submitResponse(
            AsyncResponseBuilder.create(HttpStatus.SC_OK)
                .setEntity(AsyncEntityProducers.create(bytes, ContentType.TEXT_PLAIN))
                .build(),
            context);
    }

    private void copy(List<URL> urls, NetworkInformation out, String key) {
        if (urls == null) {
            return;
        }

        HashSet<URL> deduplicatedUrls = new HashSet<URL>(urls);
        for (URL url : deduplicatedUrls) {
            out.addAsUrl(key, url.toString());
        }
    }
}