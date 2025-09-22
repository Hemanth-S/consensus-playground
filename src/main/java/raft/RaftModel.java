package raft;

import sim.Cluster;
import sim.Determinism;
import sim.Message;
import sim.MessageBus;
import sim.NetworkRule;

import java.util.*;
import java.util.stream.Collectors;

/**
 * High-level abstraction for Raft consensus simulation.
 * Wires a Cluster of RaftNodes and exposes operations for CLI/scenarios.
 * 
 * This class serves as the abstraction boundary, allowing the same CLI/scenario
 * code to work with different consensus algorithms (Raft, Paxos, etc.).
 */
public class RaftModel {
    private final Cluster cluster;
    private final Map<String, RaftNode> nodesById;
    private final List<String> nodeIds;
    
    /**
     * Create a new RaftModel with the specified nodes and random seed.
     * 
     * @param nodeIds List of node IDs to create
     * @param seed Random seed for deterministic simulation
     */
    public RaftModel(List<String> nodeIds, long seed) {
        this.nodeIds = new ArrayList<>(nodeIds);
        this.cluster = new Cluster();
        this.nodesById = new HashMap<>();
        
        // Set deterministic seed
        Determinism.setSeed(seed);
        
        // Create RaftNodes and add to cluster
        for (String nodeId : nodeIds) {
            List<String> peers = nodeIds.stream()
                .filter(id -> !id.equals(nodeId))
                .collect(Collectors.toList());
            
            RaftNode node = new RaftNode(nodeId, peers);
            node.setMessageBus(cluster.getMessageBus());
            
            nodesById.put(nodeId, node);
            cluster.add(node);
        }
    }
    
    /**
     * Get the underlying cluster for direct access if needed.
     * 
     * @return The simulation cluster
     */
    public Cluster cluster() {
        return cluster;
    }
    
    /**
     * Crash a specific node.
     * 
     * @param id Node ID to crash
     */
    public void crash(String id) {
        RaftNode node = nodesById.get(id);
        if (node != null) {
            node.setUp(false);
            System.out.println("Node " + id + " crashed");
        } else {
            System.out.println("Node " + id + " not found");
        }
    }
    
    /**
     * Recover a specific node.
     * 
     * @param id Node ID to recover
     */
    public void recover(String id) {
        RaftNode node = nodesById.get(id);
        if (node != null) {
            node.setUp(true);
            System.out.println("Node " + id + " recovered");
        } else {
            System.out.println("Node " + id + " not found");
        }
    }
    
    /**
     * Create a network partition between two groups of nodes.
     * Messages between the groups will be dropped.
     * 
     * @param groupA First group of node IDs
     * @param groupB Second group of node IDs
     */
    public void partition(List<String> groupA, List<String> groupB) {
        MessageBus bus = cluster.getMessageBus();
        
        // Add rules to drop messages between the two groups
        for (String nodeA : groupA) {
            for (String nodeB : groupB) {
                // Drop messages from groupA to groupB
                NetworkRule rule1 = NetworkRule.drop(nodeA, nodeB, "*");
                bus.addRule(rule1);
                
                // Drop messages from groupB to groupA
                NetworkRule rule2 = NetworkRule.drop(nodeB, nodeA, "*");
                bus.addRule(rule2);
            }
        }
        
        System.out.println("Network partition created between groups: " + groupA + " and " + groupB);
    }
    
    /**
     * Clear all network partitions by removing all network rules.
     */
    public void clearPartitions() {
        MessageBus bus = cluster.getMessageBus();
        bus.clearRules();
        System.out.println("Network partitions cleared (all rules removed)");
    }
    
    /**
     * Send a client write command to the current leader.
     * If no leader is known, sends to all nodes (naive approach).
     * 
     * @param command The command to write
     */
    public void clientWrite(String command) {
        Optional<String> leaderId = currentLeaderId();
        
        if (leaderId.isPresent()) {
            // Send to current leader
            RaftNode leader = nodesById.get(leaderId.get());
            if (leader != null && leader.isUp()) {
                System.out.println("Client write to leader " + leaderId.get() + ": " + command);
                
                // Add command to leader's log and start replication
                boolean success = leader.addClientCommand(command);
                if (success) {
                    System.out.println("Command added to leader's log and replication started");
                } else {
                    System.out.println("Failed to add command to leader's log");
                }
            }
        } else {
            // No leader known, send to all nodes (naive approach)
            System.out.println("No leader known, broadcasting client write to all nodes: " + command);
            boolean handled = false;
            for (String nodeId : nodeIds) {
                RaftNode node = nodesById.get(nodeId);
                if (node != null && node.isUp()) {
                    boolean success = node.handleClientCommand(command);
                    if (success) {
                        System.out.println("  -> " + nodeId + " (handled as leader)");
                        handled = true;
                        break; // Only one node should handle it
                    } else {
                        System.out.println("  -> " + nodeId + " (not leader)");
                    }
                }
            }
            if (!handled) {
                System.out.println("No active leader found to handle command");
            }
        }
    }
    
    /**
     * Find the current leader by scanning all nodes for LEADER role.
     * This is a naive implementation - in a real system, we'd have better leader discovery.
     * 
     * @return Optional containing the leader ID if found
     */
    public Optional<String> currentLeaderId() {
        for (Map.Entry<String, RaftNode> entry : nodesById.entrySet()) {
            RaftNode node = entry.getValue();
            if (node.isUp() && node.getRole() == RaftRole.LEADER) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }
    
    /**
     * Get a comprehensive dump of the current state of all nodes.
     * 
     * @return String representation of the current state
     */
    public String dump() {
        StringBuilder sb = new StringBuilder();
        sb.append("RaftModel State:\n");
        sb.append("  Cluster time: ").append(cluster.getCurrentTime()).append("\n");
        sb.append("  Nodes: ").append(nodesById.size()).append("\n");
        sb.append("  Pending messages: ").append(cluster.getPendingMessageCount()).append("\n");
        
        Optional<String> leader = currentLeaderId();
        if (leader.isPresent()) {
            sb.append("  Current leader: ").append(leader.get()).append("\n");
        } else {
            sb.append("  Current leader: None\n");
        }
        
        sb.append("\nNode Details:\n");
        for (Map.Entry<String, RaftNode> entry : nodesById.entrySet()) {
            String nodeId = entry.getKey();
            RaftNode node = entry.getValue();
            
            sb.append("  ").append(nodeId).append(": ");
            sb.append(node.isUp() ? "UP" : "DOWN").append(" ");
            sb.append(node.getRole()).append(" ");
            sb.append("term=").append(node.getCurrentTerm()).append(" ");
            sb.append("commit=").append(node.getCommitIndex()).append(" ");
            sb.append("logSize=").append(node.getLog().size());
            
            if (node.getRole() == RaftRole.LEADER) {
                sb.append(" (LEADER)");
            }
            
            sb.append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * Step the simulation forward by one time unit.
     */
    public void step() {
        cluster.step();
    }
    
    /**
     * Get a specific node by ID.
     * 
     * @param nodeId The node ID
     * @return The RaftNode, or null if not found
     */
    public RaftNode getNode(String nodeId) {
        return nodesById.get(nodeId);
    }
    
    /**
     * Get all node IDs.
     * 
     * @return List of all node IDs
     */
    public List<String> getNodeIds() {
        return new ArrayList<>(nodeIds);
    }
    
    /**
     * Get the current simulation time.
     * 
     * @return Current time in simulation steps
     */
    public int getCurrentTime() {
        return cluster.getCurrentTime();
    }
}