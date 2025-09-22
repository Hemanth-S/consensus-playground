package raft;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import sim.Cluster;
import sim.Message;
import sim.MessageBus;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RaftNode implementing Cluster.Node interface.
 * Verifies that RaftNode properly implements the simulation layer interface.
 */
@DisplayName("RaftNode Interface Tests")
class RaftNodeInterfaceTest {
    
    private RaftNode raftNode;
    private MessageBus messageBus;
    
    @BeforeEach
    void setUp() {
        List<String> peers = List.of("node-1", "node-2");
        raftNode = new RaftNode("node-0", peers);
        messageBus = new MessageBus();
        raftNode.setMessageBus(messageBus);
    }
    
    @Test
    @DisplayName("RaftNode should implement Cluster.Node interface")
    void shouldImplementClusterNodeInterface() {
        assertTrue(raftNode instanceof Cluster.Node);
    }
    
    @Test
    @DisplayName("RaftNode should return correct ID")
    void shouldReturnCorrectId() {
        assertEquals("node-0", raftNode.id());
    }
    
    @Test
    @DisplayName("RaftNode should start as up")
    void shouldStartAsUp() {
        assertTrue(raftNode.isUp());
        assertFalse(raftNode.isCrashed());
    }
    
    @Test
    @DisplayName("RaftNode should handle crash and recovery")
    void shouldHandleCrashAndRecovery() {
        // Initially up
        assertTrue(raftNode.isUp());
        
        // Crash the node
        raftNode.setUp(false);
        assertFalse(raftNode.isUp());
        assertTrue(raftNode.isCrashed());
        
        // Recover the node
        raftNode.setUp(true);
        assertTrue(raftNode.isUp());
        assertFalse(raftNode.isCrashed());
    }
    
    @Test
    @DisplayName("RaftNode should handle onTick calls")
    void shouldHandleOnTickCalls() {
        // Should not throw exception
        assertDoesNotThrow(() -> raftNode.onTick(messageBus));
    }
    
    @Test
    @DisplayName("RaftNode should handle onMessage calls")
    void shouldHandleOnMessageCalls() {
        Map<String, Object> payload = Map.of("test", "data");
        Message message = new Message("node-1", "node-0", "TestMessage", payload);
        
        // Should not throw exception
        assertDoesNotThrow(() -> raftNode.onMessage(message, messageBus));
    }
    
    @Test
    @DisplayName("RaftNode should provide meaningful dump")
    void shouldProvideMeaningfulDump() {
        String dump = raftNode.dump();
        
        assertNotNull(dump);
        assertTrue(dump.contains("role="));
        assertTrue(dump.contains("term="));
        assertTrue(dump.contains("commitIndex="));
        assertTrue(dump.contains("logSize="));
        
        // Should contain FOLLOWER role initially
        assertTrue(dump.contains("FOLLOWER"));
    }
    
    @Test
    @DisplayName("RaftNode should maintain Raft-specific state")
    void shouldMaintainRaftSpecificState() {
        assertEquals("node-0", raftNode.getNodeId());
        assertEquals(RaftRole.FOLLOWER, raftNode.getRole());
        assertEquals(0, raftNode.getCurrentTerm());
        assertEquals(0, raftNode.getCommitIndex());
        assertTrue(raftNode.getLog().isEmpty());
    }
}
