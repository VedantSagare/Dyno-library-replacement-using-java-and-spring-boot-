package com.crowdfunding.dynomite.service;

import com.crowdfunding.dynomite.config.DynomiteProperties;
import com.crowdfunding.dynomite.ring.ConsistentHashRing;
import com.crowdfunding.dynomite.ring.RingNode;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ClusterView {

    private final DynomiteProperties properties;
    private final ConsistentHashRing ring;

    public ClusterView(DynomiteProperties properties) {
        this.properties = properties;
        List<RingNode> nodes = properties.getNodes().stream()
                .map(n -> new RingNode(n.getId(), n.getBaseUrl()))
                .toList();
        this.ring = new ConsistentHashRing(nodes, properties.getVirtualNodes());
    }

    public DynomiteProperties getProperties() {
        return properties;
    }

    public RingNode localNode() {
        return properties.getNodes().stream()
                .filter(n -> n.getId().equals(properties.getNodeId()))
                .findFirst()
                .map(n -> new RingNode(n.getId(), n.getBaseUrl()))
                .orElseThrow(() -> new IllegalStateException("dynomite.node-id is not in dynomite.nodes"));
    }

    public List<RingNode> replicasForKey(String key) {
        return ring.getReplicas(key, properties.getReplicationFactor());
    }
}

