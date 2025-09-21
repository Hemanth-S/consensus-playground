package paxos;

import sim.Determinism;

import java.util.*;

/**
 * Represents the complete Paxos consensus model.
 * This is a placeholder implementation for future development.
 */
public class PaxosModel {
    private final List<PaxosNode> nodes = new ArrayList<>();
    private final Map<String, PaxosNode> nodeMap = new HashMap<>();
    
    /**
     * Create a new Paxos model with the specified number of nodes
     */
    public PaxosModel(int nodeCount) {
        for (int i = 0; i < nodeCount; i++) {
            String nodeId = "node-" + i;
            PaxosNode node = new PaxosNode(nodeId);
            nodes.add(node);
            nodeMap.put(nodeId, node);
        }
    }
    
    /**
     * Step all nodes forward by one time unit
     */
    public void step(long currentTime, Determinism determinism) {
        for (PaxosNode node : nodes) {
            if (!node.isCrashed()) {
                node.step(currentTime, determinism);
            }
        }
    }
    
    /**
     * Crash a specific node
     */
    public void crashNode(String nodeId) {
        PaxosNode node = nodeMap.get(nodeId);
        if (node != null) {
            node.crash();
        }
    }
    
    /**
     * Recover a specific node
     */
    public void recoverNode(String nodeId) {
        PaxosNode node = nodeMap.get(nodeId);
        if (node != null) {
            node.recover();
        }
    }
    
    /**
     * Get all nodes
     */
    public List<PaxosNode> getNodes() {
        return new ArrayList<>(nodes);
    }
    
    /**
     * Get a specific node by ID
     */
    public PaxosNode getNode(String nodeId) {
        return nodeMap.get(nodeId);
    }
    
    /**
     * Get the number of nodes in the cluster
     */
    public int getNodeCount() {
        return nodes.size();
    }
    
    /**
     * Get all active (non-crashed) nodes
     */
    public List<PaxosNode> getActiveNodes() {
        return nodes.stream()
                   .filter(node -> !node.isCrashed())
                   .toList();
    }
}

