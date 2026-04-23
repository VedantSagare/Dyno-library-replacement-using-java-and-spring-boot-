package com.crowdfunding.dynomite.store;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class InMemoryKeyValueStore implements KeyValueStore {

    private static final Logger log = LoggerFactory.getLogger(InMemoryKeyValueStore.class);
    private static final long REAPER_INTERVAL_SECONDS = 60;

    private final ConcurrentHashMap<String, Record> map = new ConcurrentHashMap<>();
    private final Clock clock;
    private final ScheduledExecutorService reaper;

    public InMemoryKeyValueStore() {
        this(Clock.systemUTC());
    }

    InMemoryKeyValueStore(Clock clock) {
        this.clock = clock;
        this.reaper = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "kv-ttl-reaper");
            t.setDaemon(true);
            return t;
        });
        this.reaper.scheduleAtFixedRate(this::evictExpired,
                REAPER_INTERVAL_SECONDS, REAPER_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    public void put(String key, String value, Long ttlSeconds) {
        Instant expiresAt = null;
        if (ttlSeconds != null && ttlSeconds > 0) {
            expiresAt = clock.instant().plusSeconds(ttlSeconds);
        }
        map.put(key, new Record(value, expiresAt));
    }

    @Override
    public Optional<String> get(String key) {
        Record record = map.get(key);
        if (record == null) {
            return Optional.empty();
        }
        Instant now = clock.instant();
        if (record.isExpired(now)) {
            map.remove(key, record);
            return Optional.empty();
        }
        return Optional.of(record.value());
    }

    @Override
    public boolean delete(String key) {
        return map.remove(key) != null;
    }

    /**
     * Returns the current number of entries (including potentially-expired ones
     * that have not yet been reaped). Useful for monitoring / observability.
     */
    public int size() {
        return map.size();
    }

    @PreDestroy
    void shutdown() {
        reaper.shutdownNow();
    }

    /**
     * Periodic background task that removes expired entries so they
     * don't leak memory when never read again after TTL expiry.
     */
    private void evictExpired() {
        Instant now = clock.instant();
        int evicted = 0;
        for (var entry : map.entrySet()) {
            if (entry.getValue().isExpired(now)) {
                if (map.remove(entry.getKey(), entry.getValue())) {
                    evicted++;
                }
            }
        }
        if (evicted > 0) {
            log.debug("TTL reaper evicted {} expired entries", evicted);
        }
    }

    private record Record(String value, Instant expiresAt) {
        boolean isExpired(Instant now) {
            return expiresAt != null && !now.isBefore(expiresAt);
        }
    }
}
