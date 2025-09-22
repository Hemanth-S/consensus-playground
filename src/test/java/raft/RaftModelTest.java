package raft;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import sim.Cluster;
import sim.MessageBus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RaftModel high-level operations.
 * Verifies that RaftModel properly abstracts the Raft consensus simulation.
 */
@DisplayName("RaftModel Tests")
class RaftModelTest {
    
    private RaftModel raftModel;
    private List<String> nodeIds;
    
    @BeforeEach
    void setUp() {
        nodeIds = List.of("node-0", "node-1", "node-2");
        raftModel = new RaftModel(nodeIds, 12345L);
    }
    
    @Test
    @DisplayName("RaftModel should create cluster with correct number of nodes")
    void shouldCreateClusterWithCorrectNumberOfNodes() {
        Cluster cluster = raftModel.cluster();
        assertNotNull(cluster);
        assertEquals(3, cluster.getNodeCount());
        assertEquals(3, raftModel.getNodeIds().size());
    }
    
    @Test
    @DisplayName("RaftModel should create RaftNodes with correct IDs")
    void shouldCreateRaftNodesWithCorrectIds() {
        for (String nodeId : nodeIds) {
            RaftNode node = raftModel.getNode(nodeId);
            assertNotNull(node);
            assertEquals(nodeId, node.id());
            assertTrue(node.isUp());
        }
    }
    
    @Test
    @DisplayName("RaftModel should handle node crash and recovery")
    void shouldHandleNodeCrashAndRecovery() {
        String nodeId = "node-0";
        RaftNode node = raftModel.getNode(nodeId);
        
        // Initially up
        assertTrue(node.isUp());
        
        // Crash the node
        raftModel.crash(nodeId);
        assertFalse(node.isUp());
        
        // Recover the node
        raftModel.recover(nodeId);
        assertTrue(node.isUp());
    }
    
    @Test
    @DisplayName("RaftModel should handle crash of non-existent node")
    void shouldHandleCrashOfNonExistentNode() {
        // Should not throw exception
        assertDoesNotThrow(() -> raftModel.crash("non-existent"));
    }
    
    @Test
    @DisplayName("RaftModel should handle recovery of non-existent node")
    void shouldHandleRecoveryOfNonExistentNode() {
        // Should not throw exception
        assertDoesNotThrow(() -> raftModel.recover("non-existent"));
    }
    
    @Test
    @DisplayName("RaftModel should create network partitions")
    void shouldCreateNetworkPartitions() {
        List<String> groupA = List.of("node-0", "node-1");
        List<String> groupB = List.of("node-2");
        
        // Should not throw exception
        assertDoesNotThrow(() -> raftModel.partition(groupA, groupB));
    }
    
    @Test
    @DisplayName("RaftModel should clear network partitions")
    void shouldClearNetworkPartitions() {
        // Should not throw exception
        assertDoesNotThrow(() -> raftModel.clearPartitions());
    }
    
    @Test
    @DisplayName("RaftModel should handle client writes")
    void shouldHandleClientWrites() {
        String command = "SET key value";
        
        // Should not throw exception
        assertDoesNotThrow(() -> raftModel.clientWrite(command));
    }
    
    @Test
    @DisplayName("RaftModel should find current leader")
    void shouldFindCurrentLeader() {
        Optional<String> leader = raftModel.currentLeaderId();
        
        // Initially no leader (all nodes are followers)
        assertFalse(leader.isPresent());
    }
    
    @Test
    @DisplayName("RaftModel should provide meaningful dump")
    void shouldProvideMeaningfulDump() {
        String dump = raftModel.dump();
        
        assertNotNull(dump);
        assertTrue(dump.contains("RaftModel State"));
        assertTrue(dump.contains("Cluster time"));
        assertTrue(dump.contains("Nodes: 3"));
        assertTrue(dump.contains("Current leader: None"));
        assertTrue(dump.contains("Node Details"));
        
        // Should contain all node IDs
        for (String nodeId : nodeIds) {
            assertTrue(dump.contains(nodeId));
        }
    }
    
    @Test
    @DisplayName("RaftModel should step simulation")
    void shouldStepSimulation() {
        int initialTime = raftModel.getCurrentTime();
        
        raftModel.step();
        
        assertEquals(initialTime + 1, raftModel.getCurrentTime());
    }
    
    @Test
    @DisplayName("RaftModel should provide access to underlying cluster")
    void shouldProvideAccessToUnderlyingCluster() {
        Cluster cluster = raftModel.cluster();
        assertNotNull(cluster);
        assertTrue(cluster instanceof Cluster);
    }
    
    @Test
    @DisplayName("RaftModel should maintain node references")
    void shouldMaintainNodeReferences() {
        for (String nodeId : nodeIds) {
            RaftNode node = raftModel.getNode(nodeId);
            assertNotNull(node);
            assertEquals(nodeId, node.getNodeId());
            assertEquals(RaftRole.FOLLOWER, node.getRole());
        }
    }
}
