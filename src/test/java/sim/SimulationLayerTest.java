package sim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the simulation layer components.
 * Verifies that the simulation framework works correctly.
 */
@DisplayName("Simulation Layer Tests")
class SimulationLayerTest {
    
    private Cluster cluster;
    private MessageBus bus;
    
    @BeforeEach
    void setUp() {
        Determinism.setSeed(12345L);
        cluster = new Cluster();
        bus = cluster.getMessageBus();
    }
    
    @Test
    @DisplayName("Message should be created and accessed correctly")
    void messageCreationAndAccess() {
        Map<String, Object> payload = Map.of("key1", "value1", "key2", 42);
        Message message = new Message("node1", "node2", "TestMessage", payload);
        
        assertEquals("node1", message.from);
        assertEquals("node2", message.to);
        assertEquals("TestMessage", message.type);
        assertEquals("value1", message.get("key1"));
        assertEquals(42, message.get("key2"));
        assertEquals(42, message.get("key2", Integer.class));
    }
    
    @Test
    @DisplayName("NetworkRule should match messages correctly")
    void networkRuleMatching() {
        NetworkRule.Match match = new NetworkRule.Match("node1", "node2", "TestMessage", null, null, false);
        NetworkRule rule = new NetworkRule(match, NetworkRule.Action.PASS, 0, 0.0);
        
        Map<String, Object> payload = Map.of("data", "test");
        Message message = new Message("node1", "node2", "TestMessage", payload);
        
        assertTrue(rule.match.matches(message));
        
        // Test wildcard matching
        NetworkRule.Match wildcardMatch = new NetworkRule.Match("*", "*", "*", null, null, false);
        NetworkRule wildcardRule = new NetworkRule(wildcardMatch, NetworkRule.Action.PASS, 0, 0.0);
        assertTrue(wildcardRule.match.matches(message));
    }
    
    @Test
    @DisplayName("MessageBus should handle message delivery")
    void messageBusDelivery() {
        Map<String, Object> payload = Map.of("data", "test");
        Message message = new Message("node1", "node2", "TestMessage", payload);
        
        bus.send(message);
        
        List<Message> received = bus.getAllMessages("node2");
        assertEquals(1, received.size());
        assertEquals("TestMessage", received.get(0).type);
        assertEquals("test", received.get(0).get("data"));
    }
    
    @Test
    @DisplayName("MessageBus should apply delay rules")
    void messageBusDelay() {
        NetworkRule delayRule = NetworkRule.delay("node1", "node2", "TestMessage", 5);
        bus.addRule(delayRule);
        
        Map<String, Object> payload = Map.of("data", "test");
        Message message = new Message("node1", "node2", "TestMessage", payload);
        
        bus.send(message);
        
        // Message should not be delivered immediately
        List<Message> received = bus.getAllMessages("node2");
        assertTrue(received.isEmpty());
        
        // Step 5 times
        for (int i = 0; i < 5; i++) {
            bus.step();
        }
        
        // Message should now be delivered
        received = bus.getAllMessages("node2");
        assertEquals(1, received.size());
    }
    
    @Test
    @DisplayName("MessageBus should apply drop rules")
    void messageBusDrop() {
        NetworkRule dropRule = NetworkRule.drop("node1", "node2", "TestMessage");
        bus.addRule(dropRule);
        
        Map<String, Object> payload = Map.of("data", "test");
        Message message = new Message("node1", "node2", "TestMessage", payload);
        
        bus.send(message);
        
        // Message should be dropped
        List<Message> received = bus.getAllMessages("node2");
        assertTrue(received.isEmpty());
    }
    
    @Test
    @DisplayName("Cluster should manage nodes correctly")
    void clusterNodeManagement() {
        Cluster.Node testNode = new Cluster.Node() {
            private boolean up = true;
            private int tickCount = 0;
            private int messageCount = 0;
            
            @Override
            public String id() { return "test-node"; }
            
            @Override
            public boolean isUp() { return up; }
            
            @Override
            public void setUp(boolean up) { this.up = up; }
            
            @Override
            public void onTick(MessageBus bus) { tickCount++; }
            
            @Override
            public void onMessage(Message m, MessageBus bus) { messageCount++; }
            
            @Override
            public String dump() { return "tickCount=" + tickCount + ", messageCount=" + messageCount; }
        };
        
        cluster.add(testNode);
        
        assertEquals(1, cluster.getNodeCount());
        assertEquals("test-node", cluster.get("test-node").id());
        assertTrue(cluster.get("test-node").isUp());
        
        // Step the cluster
        cluster.step();
        
        // Node should have received a tick
        String dump = cluster.get("test-node").dump();
        assertTrue(dump.contains("tickCount=1"));
    }
    
    @Test
    @DisplayName("Determinism should provide reproducible randomness")
    void determinismReproducibility() {
        Determinism.setSeed(12345L);
        boolean first = Determinism.chance(0.5);
        int firstJitter = Determinism.jitter(1, 10);
        
        Determinism.setSeed(12345L);
        boolean second = Determinism.chance(0.5);
        int secondJitter = Determinism.jitter(1, 10);
        
        assertEquals(first, second);
        assertEquals(firstJitter, secondJitter);
    }
}
