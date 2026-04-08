package com.crowdfunding.dynomite.store;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryKeyValueStore implements KeyValueStore {

    private final ConcurrentHashMap<String, Record> map = new ConcurrentHashMap<>();
    private final Clock clock;

    public InMemoryKeyValueStore() {
        this(Clock.systemUTC());
    }

    InMemoryKeyValueStore(Clock clock) {
        this.clock = clock;
    }

    @Override
    public void put(String key, String value, Long ttlSeconds) {
        Instant expiresAt = null;
        if (ttlSeconds != null && ttlSeconds > 0) {
            expiresAt = Instant.now(clock).plusSeconds(ttlSeconds);
        }
        map.put(key, new Record(value, expiresAt));
    }

    @Override
    public Optional<String> get(String key) {
        Record record = map.get(key);
        if (record == null) {
            return Optional.empty();
        }
        if (record.isExpired(clock)) {
            map.remove(key, record);
            return Optional.empty();
        }
        return Optional.of(record.value());
    }

    @Override
    public boolean delete(String key) {
        return map.remove(key) != null;
    }

    private record Record(String value, Instant expiresAt) {
        boolean isExpired(Clock clock) {
            return expiresAt != null && !Instant.now(clock).isBefore(expiresAt);
        }
    }
}

