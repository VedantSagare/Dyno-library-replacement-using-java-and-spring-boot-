package com.crowdfunding.dynomite.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PeerClientTest {

    @Test
    void buildInternalKeyUriEncodesReservedCharactersInTheKey() {
        String uri = PeerClient.buildInternalKeyUri("http://127.0.0.1:9091", "folder/item with space");

        assertEquals("http://127.0.0.1:9091/v1/internal/kv/folder%2Fitem%20with%20space", uri);
    }
}
