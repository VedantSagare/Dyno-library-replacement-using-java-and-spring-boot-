package com.crowdfunding.dynomite.service;

import com.crowdfunding.dynomite.api.dto.KvPutRequest;
import com.crowdfunding.dynomite.client.PeerClient;
import com.crowdfunding.dynomite.config.DynomiteProperties;
import com.crowdfunding.dynomite.ring.RingNode;
import com.crowdfunding.dynomite.store.KeyValueStore;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KvRouterTest {

    @Test
    void getReportsTheReplicaThatServedTheValue() {
        RingNode local = new RingNode("node1", "http://node1");
        RingNode remote = new RingNode("node2", "http://node2");
        RecordingStore store = new RecordingStore(new ArrayList<>());
        RecordingPeerClient peerClient = new RecordingPeerClient(new ArrayList<>());
        peerClient.readResults.put("http://node2|alpha", Optional.of("beta"));
        KvRouter router = new KvRouter(new FixedClusterView(local, List.of(local, remote)), store, peerClient);

        Optional<KvReadResult> result = router.get("alpha");

        assertTrue(result.isPresent());
        assertEquals("alpha", result.get().key());
        assertEquals("beta", result.get().value());
        assertEquals("node2", result.get().servedBy());
    }

    @Test
    void putWritesToTheLocalReplicaBeforeReplicatingRemotely() {
        RingNode remote = new RingNode("node2", "http://node2");
        RingNode local = new RingNode("node1", "http://node1");
        List<String> events = new ArrayList<>();
        RecordingStore store = new RecordingStore(events);
        RecordingPeerClient peerClient = new RecordingPeerClient(events);
        KvRouter router = new KvRouter(new FixedClusterView(local, List.of(remote, local)), store, peerClient);
        KvPutRequest request = new KvPutRequest();
        request.setValue("beta");
        request.setTtlSeconds(30L);

        router.put("alpha", request);

        assertEquals(List.of("local-put:alpha", "remote-put:http://node2"), events);
    }

    @Test
    void deleteReturnsFalseWhenNoReplicaActuallyDeletesAnything() {
        RingNode local = new RingNode("node1", "http://node1");
        RingNode remote = new RingNode("node2", "http://node2");
        RecordingStore store = new RecordingStore(new ArrayList<>());
        RecordingPeerClient peerClient = new RecordingPeerClient(new ArrayList<>());
        peerClient.deleteResults.put("http://node2|alpha", false);
        KvRouter router = new KvRouter(new FixedClusterView(local, List.of(local, remote)), store, peerClient);

        boolean deleted = router.delete("alpha");

        assertFalse(deleted);
    }

    private static final class FixedClusterView extends ClusterView {
        private static final Duration REQUEST_TIMEOUT = Duration.ofMillis(250);

        private final RingNode localNode;
        private final List<RingNode> replicas;

        private FixedClusterView(RingNode localNode, List<RingNode> replicas) {
            super(testProperties(localNode, replicas));
            this.localNode = localNode;
            this.replicas = List.copyOf(replicas);
        }

        @Override
        public RingNode localNode() {
            return localNode;
        }

        @Override
        public Duration requestTimeout() {
            return REQUEST_TIMEOUT;
        }

        @Override
        public List<RingNode> replicasForKey(String key) {
            return replicas;
        }

        private static DynomiteProperties testProperties(RingNode localNode, List<RingNode> replicas) {
            DynomiteProperties properties = new DynomiteProperties();
            properties.setNodeId(localNode.id());

            List<DynomiteProperties.Node> nodes = new ArrayList<>();
            for (RingNode node : new LinkedHashSet<>(replicas)) {
                DynomiteProperties.Node propertyNode = new DynomiteProperties.Node();
                propertyNode.setId(node.id());
                propertyNode.setBaseUrl(node.baseUrl());
                nodes.add(propertyNode);
            }

            if (nodes.stream().noneMatch(node -> node.getId().equals(localNode.id()))) {
                DynomiteProperties.Node propertyNode = new DynomiteProperties.Node();
                propertyNode.setId(localNode.id());
                propertyNode.setBaseUrl(localNode.baseUrl());
                nodes.add(propertyNode);
            }

            properties.setNodes(nodes);
            properties.setReplicationFactor(Math.max(1, replicas.size()));
            properties.setVirtualNodes(16);
            properties.setRequestTimeoutMillis((int) REQUEST_TIMEOUT.toMillis());
            return properties;
        }
    }

    private static final class RecordingStore implements KeyValueStore {
        private final Map<String, String> values = new LinkedHashMap<>();
        private final List<String> events;

        private RecordingStore(List<String> events) {
            this.events = events;
        }

        @Override
        public void put(String key, String value, Long ttlSeconds) {
            events.add("local-put:" + key);
            values.put(key, value);
        }

        @Override
        public Optional<String> get(String key) {
            return Optional.ofNullable(values.get(key));
        }

        @Override
        public boolean delete(String key) {
            return values.remove(key) != null;
        }
    }

    private static final class RecordingPeerClient extends PeerClient {
        private final Map<String, Optional<String>> readResults = new LinkedHashMap<>();
        private final Map<String, Boolean> deleteResults = new LinkedHashMap<>();
        private final List<String> events;

        private RecordingPeerClient(List<String> events) {
            super(WebClient.builder());
            this.events = events;
        }

        @Override
        public Optional<String> getLocalOnly(String baseUrl, String key, Duration timeout) {
            return readResults.getOrDefault(baseUrl + "|" + key, Optional.empty());
        }

        @Override
        public void putLocalOnly(String baseUrl, String key, KvPutRequest request, Duration timeout) {
            events.add("remote-put:" + baseUrl);
        }

        @Override
        public boolean deleteLocalOnly(String baseUrl, String key, Duration timeout) {
            return deleteResults.getOrDefault(baseUrl + "|" + key, false);
        }
    }
}
