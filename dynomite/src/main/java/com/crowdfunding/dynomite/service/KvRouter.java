package com.crowdfunding.dynomite.service;

import com.crowdfunding.dynomite.api.dto.KvPutRequest;
import com.crowdfunding.dynomite.client.PeerClient;
import com.crowdfunding.dynomite.ring.RingNode;
import com.crowdfunding.dynomite.store.KeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Service
public class KvRouter {

    private static final Logger log = LoggerFactory.getLogger(KvRouter.class);

    private final ClusterViewPort clusterView;
    private final KeyValueStore store;
    private final PeerClient peerClient;

    public KvRouter(ClusterViewPort clusterView, KeyValueStore store, PeerClient peerClient) {
        this.clusterView = clusterView;
        this.store = store;
        this.peerClient = peerClient;
    }

    public Optional<KvReadResult> get(String key) {
        List<RingNode> replicas = clusterView.replicasForKey(key);
        String localNodeId = clusterView.localNode().id();
        Duration timeout = clusterView.requestTimeout();

        for (RingNode replica : replicas) {
            if (replica.id().equals(localNodeId)) {
                Optional<String> value = store.get(key);
                if (value.isPresent()) {
                    return value.map(found -> new KvReadResult(key, found, replica.id()));
                }
                continue;
            }

            try {
                Optional<String> value = peerClient.getLocalOnly(replica.baseUrl(), key, timeout);
                if (value.isPresent()) {
                    return value.map(found -> new KvReadResult(key, found, replica.id()));
                }
            } catch (Exception ex) {
                log.warn("GET from peer {} for key '{}' failed: {}", replica.id(), key, ex.getMessage());
            }
        }
        return Optional.empty();
    }

    /**
     * Single-pass: write locally on encounter, replicate to remotes in the same loop.
     */
    public void put(String key, KvPutRequest request) {
        List<RingNode> replicas = clusterView.replicasForKey(key);
        String localNodeId = clusterView.localNode().id();
        Duration timeout = clusterView.requestTimeout();

        for (RingNode replica : replicas) {
            if (replica.id().equals(localNodeId)) {
                store.put(key, request.getValue(), request.getTtlSeconds());
                continue;
            }

            try {
                peerClient.putLocalOnlyAsync(replica.baseUrl(), key, request, timeout)
                        .subscribe(
                                unused -> { },
                                err -> log.warn("PUT replication to peer {} for key '{}' failed: {}",
                                        replica.id(), key, err.getMessage())
                        );
            } catch (Exception ex) {
                log.warn("PUT replication to peer {} for key '{}' failed: {}", replica.id(), key, ex.getMessage());
            }
        }
    }

    /**
     * Single-pass: combine local delete and remote deletes into one loop.
     */
    public boolean delete(String key) {
        List<RingNode> replicas = clusterView.replicasForKey(key);
        String localNodeId = clusterView.localNode().id();
        Duration timeout = clusterView.requestTimeout();

        boolean deletedAny = false;
        for (RingNode replica : replicas) {
            if (replica.id().equals(localNodeId)) {
                deletedAny |= store.delete(key);
                continue;
            }
            try {
                deletedAny |= peerClient.deleteLocalOnly(replica.baseUrl(), key, timeout);
            } catch (Exception ex) {
                log.warn("DELETE replication to peer {} for key '{}' failed: {}", replica.id(), key, ex.getMessage());
            }
        }
        return deletedAny;
    }
}
