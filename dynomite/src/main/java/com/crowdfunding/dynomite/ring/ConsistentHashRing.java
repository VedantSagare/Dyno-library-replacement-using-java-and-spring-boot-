package com.crowdfunding.dynomite.ring;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

public final class ConsistentHashRing {

    private final NavigableMap<Long, RingNode> ring;
    private final List<RingNode> uniqueNodes;

    public ConsistentHashRing(List<RingNode> nodes, int virtualNodes) {
        Objects.requireNonNull(nodes, "nodes");
        if (nodes.isEmpty()) {
            throw new IllegalArgumentException("nodes must not be empty");
        }
        if (virtualNodes < 1) {
            throw new IllegalArgumentException("virtualNodes must be >= 1");
        }

        this.uniqueNodes = nodes.stream()
                .sorted(Comparator.comparing(RingNode::id))
                .toList();

        TreeMap<Long, RingNode> map = new TreeMap<>();
        for (RingNode node : uniqueNodes) {
            for (int i = 0; i < virtualNodes; i++) {
                long hash = Hashing.hash64(node.id() + "#" + i);
                map.put(hash, node);
            }
        }
        this.ring = Collections.unmodifiableNavigableMap(map);
    }

    public List<RingNode> getReplicas(String key, int replicationFactor) {
        Objects.requireNonNull(key, "key");
        if (replicationFactor < 1) {
            throw new IllegalArgumentException("replicationFactor must be >= 1");
        }

        int maxReplicas = Math.min(replicationFactor, uniqueNodes.size());

        long keyHash = Hashing.hash64(key);
        LinkedHashSet<RingNode> results = new LinkedHashSet<>(maxReplicas);

        // Walk clockwise from the key hash, wrapping around.
        for (RingNode node : valuesStartingAt(keyHash)) {
            results.add(node);
            if (results.size() == maxReplicas) {
                break;
            }
        }

        // Extremely small rings could theoretically collide to fewer nodes; fall back to unique list.
        if (results.size() < maxReplicas) {
            for (RingNode node : uniqueNodes) {
                results.add(node);
                if (results.size() == maxReplicas) {
                    break;
                }
            }
        }

        return new ArrayList<>(results);
    }

    private Iterable<RingNode> valuesStartingAt(long hash) {
        List<RingNode> values = new ArrayList<>(ring.size());
        values.addAll(ring.tailMap(hash, true).values());
        values.addAll(ring.headMap(hash, false).values());
        return values;
    }
}

