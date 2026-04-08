package com.crowdfunding.dynomite.store;

import java.util.Optional;

public interface KeyValueStore {

    void put(String key, String value, Long ttlSeconds);

    Optional<String> get(String key);

    boolean delete(String key);
}

