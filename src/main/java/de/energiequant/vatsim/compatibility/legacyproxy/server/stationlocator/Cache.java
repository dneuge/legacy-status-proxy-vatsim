package de.energiequant.vatsim.compatibility.legacyproxy.server.stationlocator;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Cache<K, C, M> {
    private static final Logger LOGGER = LoggerFactory.getLogger(Cache.class);

    private final Duration maintenanceInterval;
    private Instant nextMaintenance;

    private final Map<K, Entry<K, C, M>> entriesIndexedByKey = new HashMap<>();

    private final SortedSet<Entry<K, C, M>> entriesSortedByExpiration = new TreeSet<>(
        Comparator.comparing(Entry::getExpiration)
    );

    public static class Entry<K, C, M> {
        private final K indexKey;
        private final C content;
        private final M metaData;
        private final Instant creation;
        private final Instant expiration;

        private Entry(K indexKey, C content, M metaData, Instant expiration) {
            this.indexKey = indexKey;
            this.content = content;
            this.metaData = metaData;
            this.expiration = expiration;

            this.creation = Instant.now();
        }

        public K getIndexKey() {
            return indexKey;
        }

        public C getContent() {
            return content;
        }

        public M getMetaData() {
            return metaData;
        }

        public Instant getCreation() {
            return creation;
        }

        public Instant getExpiration() {
            return expiration;
        }

        private boolean isExpired(Instant now) {
            return expiration.isBefore(now);
        }
    }

    public Cache(Duration maintenanceInterval) {
        this.maintenanceInterval = maintenanceInterval;
        this.nextMaintenance = Instant.now().plus(maintenanceInterval);
    }

    private synchronized void autoMaintain() {
        if (!Instant.now().isAfter(nextMaintenance)) {
            LOGGER.trace("next automated maintenance is due at {}", nextMaintenance);
        } else {
            LOGGER.trace("performing automated maintenance (was due at {})", nextMaintenance);
            maintain();
            nextMaintenance = Instant.now().plus(maintenanceInterval);
        }
    }

    public synchronized void maintain() {
        Instant now = Instant.now();
        Collection<Entry<K, C, M>> removed = new ArrayList<>();

        Iterator<Entry<K, C, M>> it = entriesSortedByExpiration.iterator();
        while (it.hasNext()) {
            Entry<K, C, M> entry = it.next();

            if (entry.isExpired(now)) {
                break;
            }

            LOGGER.trace(
                "maintenance: removing entry \"{}\", timed out at {}",
                entry.getIndexKey(), entry.getExpiration()
            );
            entriesIndexedByKey.remove(entry.getIndexKey(), entry);
            removed.add(entry);
        }

        entriesSortedByExpiration.removeAll(removed);

        LOGGER.trace(
            "maintenance: index size {}, expiration queue size {}",
            entriesIndexedByKey.size(), entriesSortedByExpiration.size()
        );

        this.nextMaintenance = Instant.now().plus(maintenanceInterval);
    }

    public synchronized void add(K key, C content, M metaData, Duration timeout) {
        Entry<K, C, M> entry = new Entry<K, C, M>(key, content, metaData, Instant.now().plus(timeout));

        Entry<K, C, M> previousEntry = entriesIndexedByKey.put(key, entry);
        if (previousEntry != null) {
            LOGGER.trace(
                "adding new entry for key \"{}\" causes removal of old entry from expiration queue, was due at {}",
                key, entry.getExpiration()
            );
            entriesSortedByExpiration.remove(previousEntry);
        }

        entriesSortedByExpiration.add(entry);

        autoMaintain();
    }

    public synchronized Optional<Entry<K, C, M>> get(K key) {
        autoMaintain();

        Entry<K, C, M> entry = entriesIndexedByKey.get(key);
        if ((entry != null) && entry.isExpired(Instant.now())) {
            LOGGER.trace("queried entry for key \"{}\" has expired at {}, removing", key, entry.getExpiration());
            entriesIndexedByKey.remove(entry.getIndexKey(), entry);
            entriesSortedByExpiration.remove(entry);
            entry = null;
        }

        return Optional.ofNullable(entry);
    }

    public synchronized void clear() {
        LOGGER.trace("clearing cache");
        entriesIndexedByKey.clear();
        entriesSortedByExpiration.clear();
        nextMaintenance = Instant.now().plus(maintenanceInterval);
    }
}
