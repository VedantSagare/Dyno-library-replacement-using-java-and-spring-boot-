package com.crowdfunding.dynomite.service;

import com.crowdfunding.dynomite.config.DynomiteProperties;
import com.crowdfunding.dynomite.ring.ConsistentHashRing;
import com.crowdfunding.dynomite.ring.RingNode;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
public class ClusterView implements ClusterViewPort {

    private final DynomiteProperties properties;
    private final ConsistentHashRing ring;
    private final RingNode localNode;
    private final Duration requestTimeout;

    public ClusterView(DynomiteProperties properties) {
        this.properties = properties;
        List<RingNode> nodes = properties.getNodes().stream()
                .map(n -> new RingNode(n.getId(), n.getBaseUrl()))
                .toList();
        this.ring = new ConsistentHashRing(nodes, properties.getVirtualNodes());
        this.localNode = nodes.stream()
                .filter(node -> node.id().equals(properties.getNodeId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("dynomite.node-id is not in dynomite.nodes"));
        this.requestTimeout = Duration.ofMillis(properties.getRequestTimeoutMillis());
    }

    public DynomiteProperties getProperties() {
        return properties;
    }

    @Override
    public RingNode localNode() {
        return localNode;
    }

    @Override
    public Duration requestTimeout() {
        return requestTimeout;
    }

    @Override
    public List<RingNode> replicasForKey(String key) {
        return ring.getReplicas(key, properties.getReplicationFactor());
    }
}
