package com.crowdfunding.dynomite.ring;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
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
                .toList();  // already unmodifiable

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
        Iterator<RingNode> tailIterator = ring.tailMap(keyHash, true).values().iterator();
        while (tailIterator.hasNext() && results.size() < maxReplicas) {
            results.add(tailIterator.next());
        }
        Iterator<RingNode> headIterator = ring.headMap(keyHash, false).values().iterator();
        while (headIterator.hasNext() && results.size() < maxReplicas) {
            results.add(headIterator.next());
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

        // Return an immutable, compact list instead of a mutable ArrayList.
        return List.copyOf(results);
    }
}
