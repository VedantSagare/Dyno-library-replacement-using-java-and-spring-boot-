package com.crowdfunding.dynomite.service;

import com.crowdfunding.dynomite.api.dto.KvPutRequest;
import com.crowdfunding.dynomite.client.PeerClient;
import com.crowdfunding.dynomite.ring.RingNode;
import com.crowdfunding.dynomite.store.KeyValueStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class KvRouter {

    private final ClusterView clusterView;
    private final KeyValueStore store;
    private final PeerClient peerClient;

    public KvRouter(ClusterView clusterView, KeyValueStore store, PeerClient peerClient) {
        this.clusterView = clusterView;
        this.store = store;
        this.peerClient = peerClient;
    }

    public Optional<String> get(String key) {
        List<RingNode> replicas = clusterView.replicasForKey(key);
        RingNode local = clusterView.localNode();
        String localNodeId = local.id();
        var timeout = clusterView.requestTimeout();

        for (RingNode replica : replicas) {
            if (replica.id().equals(localNodeId)) {
                Optional<String> value = store.get(key);
                if (value.isPresent()) {
                    return value;
                }
                continue;
            }

            try {
                Optional<String> value = peerClient.getLocalOnly(replica.baseUrl(), key, timeout);
                if (value.isPresent()) {
                    return value;
                }
            } catch (Exception ignored) {
                // Best-effort: continue to next replica
            }
        }
        return Optional.empty();
    }

    public void put(String key, KvPutRequest request) {
        List<RingNode> replicas = clusterView.replicasForKey(key);
        RingNode local = clusterView.localNode();
        String localNodeId = local.id();
        var timeout = clusterView.requestTimeout();

        for (RingNode replica : replicas) {
            if (replica.id().equals(localNodeId)) {
                store.put(key, request.getValue(), request.getTtlSeconds());
                continue;
            }

            try {
                peerClient.putLocalOnly(replica.baseUrl(), key, request, timeout);
            } catch (Exception ignored) {
                // Best-effort replication for the scaffold
            }
        }
    }

    public boolean delete(String key) {
        List<RingNode> replicas = clusterView.replicasForKey(key);
        RingNode local = clusterView.localNode();
        String localNodeId = local.id();
        var timeout = clusterView.requestTimeout();

        boolean deletedAny = false;
        for (RingNode replica : replicas) {
            if (replica.id().equals(localNodeId)) {
                deletedAny |= store.delete(key);
                continue;
            }
            try {
                peerClient.deleteLocalOnly(replica.baseUrl(), key, timeout);
                deletedAny = true;
            } catch (Exception ignored) {
                // Best-effort
            }
        }
        return deletedAny;
    }
}
