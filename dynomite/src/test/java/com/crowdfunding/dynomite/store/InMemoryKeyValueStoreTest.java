package com.crowdfunding.dynomite.store;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryKeyValueStoreTest {

    @Test
    void ttlExpiresKeys() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        InMemoryKeyValueStore store = new InMemoryKeyValueStore(clock);

        store.put("k", "v", 1L);
        assertTrue(store.get("k").isPresent());

        clock.setNow(Instant.parse("2026-01-01T00:00:01Z"));
        assertFalse(store.get("k").isPresent());
    }

    private static final class MutableClock extends Clock {
        private final AtomicReference<Instant> now = new AtomicReference<>();

        private MutableClock(Instant initial) {
            now.set(initial);
        }

        void setNow(Instant next) {
            now.set(next);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now.get();
        }
    }
}
