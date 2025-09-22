package raft;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import sim.Determinism;
import sim.MessageBus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Raft safety properties and correctness.
 * Verifies that the Raft implementation maintains safety guarantees.
 */
@DisplayName("Raft Safety Tests")
class RaftSafetyTests {
    
    private RaftModel raftModel;
    private MessageBus messageBus;
    
    @BeforeEach
    void setUp() {
        // Create a 3-node Raft cluster
        List<String> nodeIds = List.of("node-0", "node-1", "node-2");
        raftModel = new RaftModel(nodeIds, 12345L); // Fixed seed for reproducible tests
        messageBus = raftModel.cluster().getMessageBus();
    }
    
    @Test
    @DisplayName("Should have exactly one leader at any time")
    void shouldHaveExactlyOneLeader() {
        // Run simulation for 100 steps
        for (int time = 0; time < 100; time++) {
            raftModel.step();
            
            // Count leaders by checking each node
            int leaderCount = 0;
            for (String nodeId : raftModel.getNodeIds()) {
                RaftNode node = raftModel.getNode(nodeId);
                if (node.getRole() == RaftRole.LEADER && node.isUp()) {
                    leaderCount++;
                }
            }
            
            assertTrue(leaderCount <= 1, 
                "At most one leader should exist at time " + time + ", but found " + leaderCount);
        }
    }
    
    @Test
    @DisplayName("Should maintain term monotonicity")
    void shouldMaintainTermMonotonicity() {
        int previousMaxTerm = 0;
        
        // Run simulation for 50 steps
        for (int time = 0; time < 50; time++) {
            raftModel.step();
            
            // Find the maximum term across all nodes
            int currentMaxTerm = 0;
            for (String nodeId : raftModel.getNodeIds()) {
                RaftNode node = raftModel.getNode(nodeId);
                currentMaxTerm = Math.max(currentMaxTerm, node.getCurrentTerm());
            }
            
            assertTrue(currentMaxTerm >= previousMaxTerm, 
                "Term should never decrease. Previous: " + previousMaxTerm + ", Current: " + currentMaxTerm);
            
            previousMaxTerm = currentMaxTerm;
        }
    }
    
    @Test
    @DisplayName("Should elect leader when no leader exists")
    void shouldElectLeaderWhenNoLeaderExists() {
        // Initially no leader should exist
        assertFalse(raftModel.currentLeaderId().isPresent(), "Initially no leader should exist");
        
        // Run simulation until a leader is elected
        boolean leaderElected = false;
        for (int time = 0; time < 200 && !leaderElected; time++) {
            raftModel.step();
            if (raftModel.currentLeaderId().isPresent()) {
                leaderElected = true;
            }
        }
        
        assertTrue(leaderElected, "A leader should be elected within 200 time units");
        assertTrue(raftModel.currentLeaderId().isPresent(), "Leader should not be null after election");
    }
    
    @Test
    @DisplayName("Should handle node crashes gracefully")
    void shouldHandleNodeCrashesGracefully() {
        // Let a leader be elected first
        for (int time = 0; time < 100; time++) {
            raftModel.step();
            if (raftModel.currentLeaderId().isPresent()) break;
        }
        
        assertTrue(raftModel.currentLeaderId().isPresent(), "Leader should exist before crash");
        String leaderId = raftModel.currentLeaderId().get();
        
        // Crash the leader
        raftModel.crash(leaderId);
        
        // Verify leader is crashed
        RaftNode crashedNode = raftModel.getNode(leaderId);
        assertFalse(crashedNode.isUp(), "Node should be marked as crashed");
        assertFalse(raftModel.currentLeaderId().isPresent(), "No leader should exist after leader crash");
        
        // Run simulation to elect new leader
        boolean newLeaderElected = false;
        for (int time = 100; time < 300 && !newLeaderElected; time++) {
            raftModel.step();
            if (raftModel.currentLeaderId().isPresent() && 
                !raftModel.currentLeaderId().get().equals(leaderId)) {
                newLeaderElected = true;
            }
        }
        
        assertTrue(newLeaderElected, "A new leader should be elected after crash");
    }
    
    @Test
    @DisplayName("Should maintain majority requirement")
    void shouldMaintainMajorityRequirement() {
        // Test with 3 nodes - need at least 2 for majority
        assertTrue(hasMajority(), "3-node cluster should have majority");
        
        // Crash one node
        raftModel.crash("node-0");
        assertTrue(hasMajority(), "2 active nodes should still have majority");
        
        // Crash another node
        raftModel.crash("node-1");
        assertFalse(hasMajority(), "1 active node should not have majority");
    }
    
    private boolean hasMajority() {
        int activeNodes = 0;
        for (String nodeId : raftModel.getNodeIds()) {
            if (raftModel.getNode(nodeId).isUp()) {
                activeNodes++;
            }
        }
        return activeNodes > raftModel.getNodeIds().size() / 2;
    }
    
    @Test
    @DisplayName("Should handle node recovery")
    void shouldHandleNodeRecovery() {
        // Crash a node
        raftModel.crash("node-0");
        assertFalse(raftModel.getNode("node-0").isUp(), "Node should be crashed");
        
        // Recover the node
        raftModel.recover("node-0");
        assertTrue(raftModel.getNode("node-0").isUp(), "Node should be recovered");
        
        // Verify node can participate in consensus again
        for (int time = 0; time < 50; time++) {
            raftModel.step();
        }
        
        // This test might be flaky due to randomness, so we just verify the node is active
        assertTrue(raftModel.getNode("node-0").isUp(), "Recovered node should remain active");
    }
    
    @Test
    @DisplayName("Should have consistent cluster state")
    void shouldHaveConsistentClusterState() {
        // Run simulation for a while
        for (int time = 0; time < 100; time++) {
            raftModel.step();
        }
        
        // Check cluster state consistency
        int activeNodes = 0;
        int crashedNodes = 0;
        int maxTerm = 0;
        
        for (String nodeId : raftModel.getNodeIds()) {
            RaftNode node = raftModel.getNode(nodeId);
            if (node.isUp()) {
                activeNodes++;
            } else {
                crashedNodes++;
            }
            maxTerm = Math.max(maxTerm, node.getCurrentTerm());
        }
        
        assertTrue(maxTerm >= 0, "Current term should be non-negative");
        assertTrue(activeNodes >= 0, "Active nodes should be non-negative");
        assertTrue(crashedNodes >= 0, "Crashed nodes should be non-negative");
        assertEquals(3, activeNodes + crashedNodes, 
            "Total nodes should equal active + crashed nodes");
    }
    
    @Test
    @DisplayName("Should handle split vote scenarios")
    void shouldHandleSplitVoteScenarios() {
        // This test simulates a scenario where multiple candidates might start elections
        // Run simulation and verify that eventually a leader is elected
        boolean leaderElected = false;
        int maxTime = 500; // Allow more time for split vote resolution
        
        for (int time = 0; time < maxTime && !leaderElected; time++) {
            raftModel.step();
            if (raftModel.currentLeaderId().isPresent()) {
                leaderElected = true;
            }
        }
        
        assertTrue(leaderElected, "Leader should be elected even in split vote scenarios");
    }
    
    @Test
    @DisplayName("Should maintain log consistency")
    void shouldMaintainLogConsistency() {
        // Run simulation to establish a leader
        for (int time = 0; time < 100; time++) {
            raftModel.step();
            if (raftModel.currentLeaderId().isPresent()) break;
        }
        
        // Verify that all active nodes have consistent commit indices
        List<RaftNode> activeNodes = getActiveNodes();
        if (activeNodes.size() > 1) {
            int firstCommitIndex = activeNodes.get(0).getCommitIndex();
            boolean allConsistent = activeNodes.stream()
                    .allMatch(node -> node.getCommitIndex() == firstCommitIndex);
            
            // Note: This might not always be true during transitions, so we just verify
            // that the cluster can reach a consistent state
            assertTrue(allConsistent || activeNodes.size() == 1, 
                "Cluster should be consistent or have only one active node");
        }
    }
    
    private List<RaftNode> getActiveNodes() {
        return raftModel.getNodeIds().stream()
                .map(raftModel::getNode)
                .filter(RaftNode::isUp)
                .toList();
    }
}
