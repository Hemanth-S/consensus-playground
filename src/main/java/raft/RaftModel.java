package raft;

import sim.Cluster;
import sim.Determinism;
import sim.Message;
import sim.MessageBus;
import sim.NetworkRule;

import java.util.*;
import java.util.stream.Collectors;
import java.util.ArrayDeque;
import java.util.Deque;

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
    private final Deque<String> pendingClientCommands = new ArrayDeque<>();
    
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
        } else {
            printWithTick("Node " + id + " not found");
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
        } else {
            printWithTick("Node " + id + " not found");
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
        
        printWithTick("Network partition created between groups: " + groupA + " and " + groupB);
    }
    
    /**
     * Clear all network partitions by removing all network rules.
     */
    public void clearPartitions() {
        MessageBus bus = cluster.getMessageBus();
        bus.clearRules();
        printWithTick("Network partitions cleared (all rules removed)");
    }
    
    /**
     * Send a client write command to the current leader.
     * If no leader exists, queue the command for later execution.
     *
     * @param command The command to write
     * @return true if command was handled immediately, false if queued
     */
    public boolean clientWrite(String command) {
        Optional<String> leaderId = currentLeaderId();

        if (leaderId.isPresent()) {
            // Send to current leader
            RaftNode leader = nodesById.get(leaderId.get());
            if (leader != null && leader.isUp()) {
                printWithTick("Client write to leader " + leaderId.get() + ": " + command);

                // Add command to leader's log and start replication
                boolean success = leader.addClientCommand(command);
                if (success) {
                    printWithTick("Command added to leader's log and replication started");
                } else {
                    printWithTick("Failed to add command to leader's log");
                }
                return true;
            }
        }
        
        // No leader available, queue the command
        pendingClientCommands.offer(command);
        printWithTick("No leader yet; queued command: " + command);
        return false;
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
     * Flush pending client commands to the current leader
     */
    public void flushPendingClientCommands() {
        if (pendingClientCommands.isEmpty()) {
            return;
        }
        
        Optional<String> leaderId = currentLeaderId();
        if (leaderId.isPresent()) {
            RaftNode leader = nodesById.get(leaderId.get());
            if (leader != null && leader.isUp()) {
                printWithTick("Flushing " + pendingClientCommands.size() + " queued commands to leader " + leaderId.get());
                while (!pendingClientCommands.isEmpty()) {
                    String command = pendingClientCommands.poll();
                    leader.addClientCommand(command);
                    printWithTick("  Flushed: " + command);
                }
            }
        }
    }

    /**
     * Step the simulation forward by one time unit.
     */
    public void step() {
        cluster.step();
        flushPendingClientCommands();
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
    
    /**
     * Print a message with the current tick prefix
     */
    private void printWithTick(String message) {
        System.out.println("t=" + getCurrentTime() + " " + message);
    }

    /**
     * Check if logs are prefix consistent across all nodes.
     * This verifies the Raft log matching property.
     *
     * @return true if logs are prefix consistent, false otherwise
     */
    public boolean logsArePrefixConsistent() {
        List<List<RaftLogEntry>> allLogs = new ArrayList<>();
        
        // Collect all logs
        for (String nodeId : nodeIds) {
            RaftNode node = nodesById.get(nodeId);
            if (node != null && node.isUp()) {
                allLogs.add(node.getLog());
            }
        }
        
        if (allLogs.size() <= 1) {
            return true; // Trivially consistent
        }
        
        // Check prefix consistency for all pairs
        for (int i = 0; i < allLogs.size(); i++) {
            for (int j = i + 1; j < allLogs.size(); j++) {
                List<RaftLogEntry> log1 = allLogs.get(i);
                List<RaftLogEntry> log2 = allLogs.get(j);
                
                int minLength = Math.min(log1.size(), log2.size());
                
                // Check that logs are identical up to min length
                for (int k = 0; k < minLength; k++) {
                    RaftLogEntry entry1 = log1.get(k);
                    RaftLogEntry entry2 = log2.get(k);
                    
                    if (!entry1.equals(entry2)) {
                        return false;
                    }
                }
            }
        }
        
        return true;
    }
}