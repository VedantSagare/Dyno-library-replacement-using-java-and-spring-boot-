package com.crowdfunding.dynomite.service;

import com.crowdfunding.dynomite.ring.RingNode;

import java.time.Duration;
import java.util.List;

/**
 * Abstraction over the cluster topology so that routing logic
 * can be tested without a real {@link ClusterView}.
 */
public interface ClusterViewPort {

    RingNode localNode();

    Duration requestTimeout();

    List<RingNode> replicasForKey(String key);
}
