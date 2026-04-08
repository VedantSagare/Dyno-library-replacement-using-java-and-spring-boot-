package com.crowdfunding.dynomite.ring;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsistentHashRingTest {

    @Test
    void replicasAreDeterministicAndDistinct() {
        List<RingNode> nodes = List.of(
                new RingNode("a", "http://a"),
                new RingNode("b", "http://b"),
                new RingNode("c", "http://c")
        );
        ConsistentHashRing ring = new ConsistentHashRing(nodes, 16);

        List<RingNode> first = ring.getReplicas("key-1", 2);
        List<RingNode> second = ring.getReplicas("key-1", 2);

        assertEquals(first, second);
        assertEquals(2, first.size());
        assertTrue(first.get(0).id() != null && first.get(1).id() != null);
        assertTrue(!first.get(0).id().equals(first.get(1).id()));
    }
}

