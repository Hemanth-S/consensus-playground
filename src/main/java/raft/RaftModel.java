package raft;

import sim.Determinism;

import java.util.*;

/**
 * Represents the complete Raft consensus model.
 * Manages a cluster of Raft nodes and provides high-level operations.
 */
public class RaftModel {
    private final List<RaftNode> nodes = new ArrayList<>();
    private final Map<String, RaftNode> nodeMap = new HashMap<>();
    private final List<String> nodeIds = new ArrayList<>();
    
    /**
     * Create a new Raft model with the specified number of nodes
     */
    public RaftModel(int nodeCount) {
        for (int i = 0; i < nodeCount; i++) {
            String nodeId = "node-" + i;
            nodeIds.add(nodeId);
        }
        
        // Create nodes with peer references
        for (String nodeId : nodeIds) {
            List<String> peers = new ArrayList<>(nodeIds);
            peers.remove(nodeId);
            
            RaftNode node = new RaftNode(nodeId, peers, null); // MessageBus will be set later
            nodes.add(node);
            nodeMap.put(nodeId, node);
        }
    }
    
    /**
     * Set the message bus for all nodes
     */
    public void setMessageBus(sim.MessageBus messageBus) {
        for (RaftNode node : nodes) {
            node.setMessageBus(messageBus);
        }
    }
    
    /**
     * Step all nodes forward by one time unit
     */
    public void step(long currentTime, Determinism determinism) {
        for (RaftNode node : nodes) {
            if (!node.isCrashed()) {
                node.step(currentTime, determinism);
            }
        }
    }
    
    /**
     * Get the current leader (if any)
     */
    public RaftNode getLeader() {
        return nodes.stream()
                   .filter(node -> node.getRole() == RaftRole.LEADER && !node.isCrashed())
                   .findFirst()
                   .orElse(null);
    }
    
    /**
     * Get all followers
     */
    public List<RaftNode> getFollowers() {
        return nodes.stream()
                   .filter(node -> node.getRole() == RaftRole.FOLLOWER && !node.isCrashed())
                   .toList();
    }
    
    /**
     * Get all candidates
     */
    public List<RaftNode> getCandidates() {
        return nodes.stream()
                   .filter(node -> node.getRole() == RaftRole.CANDIDATE && !node.isCrashed())
                   .toList();
    }
    
    /**
     * Get all crashed nodes
     */
    public List<RaftNode> getCrashedNodes() {
        return nodes.stream()
                   .filter(RaftNode::isCrashed)
                   .toList();
    }
    
    /**
     * Get all active (non-crashed) nodes
     */
    public List<RaftNode> getActiveNodes() {
        return nodes.stream()
                   .filter(node -> !node.isCrashed())
                   .toList();
    }
    
    /**
     * Crash a specific node
     */
    public void crashNode(String nodeId) {
        RaftNode node = nodeMap.get(nodeId);
        if (node != null) {
            node.crash();
        }
    }
    
    /**
     * Recover a specific node
     */
    public void recoverNode(String nodeId) {
        RaftNode node = nodeMap.get(nodeId);
        if (node != null) {
            node.recover();
        }
    }
    
    /**
     * Check if the cluster has a majority of active nodes
     */
    public boolean hasMajority() {
        return getActiveNodes().size() > nodes.size() / 2;
    }
    
    /**
     * Check if the cluster is in a consistent state
     */
    public boolean isConsistent() {
        List<RaftNode> activeNodes = getActiveNodes();
        if (activeNodes.isEmpty()) return true;
        
        // Check if all active nodes have the same commit index
        int commitIndex = activeNodes.get(0).getCommitIndex();
        return activeNodes.stream()
                         .allMatch(node -> node.getCommitIndex() == commitIndex);
    }
    
    /**
     * Get the current term of the cluster
     */
    public int getCurrentTerm() {
        return nodes.stream()
                   .mapToInt(RaftNode::getCurrentTerm)
                   .max()
                   .orElse(0);
    }
    
    /**
     * Get a summary of the cluster state
     */
    public ClusterState getClusterState() {
        return new ClusterState(
            getCurrentTerm(),
            getLeader() != null ? getLeader().getNodeId() : null,
            getActiveNodes().size(),
            getCrashedNodes().size(),
            isConsistent()
        );
    }
    
    /**
     * Get all nodes
     */
    public List<RaftNode> getNodes() {
        return new ArrayList<>(nodes);
    }
    
    /**
     * Get a specific node by ID
     */
    public RaftNode getNode(String nodeId) {
        return nodeMap.get(nodeId);
    }
    
    /**
     * Get the number of nodes in the cluster
     */
    public int getNodeCount() {
        return nodes.size();
    }
    
    /**
     * Represents the current state of the cluster
     */
    public static class ClusterState {
        private final int currentTerm;
        private final String leaderId;
        private final int activeNodes;
        private final int crashedNodes;
        private final boolean consistent;
        
        public ClusterState(int currentTerm, String leaderId, int activeNodes, int crashedNodes, boolean consistent) {
            this.currentTerm = currentTerm;
            this.leaderId = leaderId;
            this.activeNodes = activeNodes;
            this.crashedNodes = crashedNodes;
            this.consistent = consistent;
        }
        
        // Getters
        public int getCurrentTerm() { return currentTerm; }
        public String getLeaderId() { return leaderId; }
        public int getActiveNodes() { return activeNodes; }
        public int getCrashedNodes() { return crashedNodes; }
        public boolean isConsistent() { return consistent; }
        
        @Override
        public String toString() {
            return String.format("ClusterState{term=%d, leader='%s', active=%d, crashed=%d, consistent=%s}", 
                               currentTerm, leaderId, activeNodes, crashedNodes, consistent);
        }
    }
}

