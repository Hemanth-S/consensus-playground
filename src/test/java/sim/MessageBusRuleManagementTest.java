package sim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MessageBus rule management functionality.
 * Verifies that rules can be added, removed, and cleared properly.
 */
@DisplayName("MessageBus Rule Management Tests")
class MessageBusRuleManagementTest {
    
    private MessageBus bus;
    
    @BeforeEach
    void setUp() {
        bus = new MessageBus();
        Determinism.setSeed(12345L); // Ensure reproducible tests
    }
    
    @Test
    @DisplayName("MessageBus should start with no rules")
    void shouldStartWithNoRules() {
        List<NetworkRule> rules = bus.getRules();
        assertTrue(rules.isEmpty());
    }
    
    @Test
    @DisplayName("MessageBus should add rules correctly")
    void shouldAddRulesCorrectly() {
        NetworkRule rule1 = NetworkRule.drop("node1", "node2", "*");
        NetworkRule rule2 = NetworkRule.delay("node3", "node4", "*", 5);
        
        bus.addRule(rule1);
        bus.addRule(rule2);
        
        List<NetworkRule> rules = bus.getRules();
        assertEquals(2, rules.size());
        assertTrue(rules.contains(rule1));
        assertTrue(rules.contains(rule2));
    }
    
    @Test
    @DisplayName("MessageBus should remove specific rules")
    void shouldRemoveSpecificRules() {
        NetworkRule rule1 = NetworkRule.drop("node1", "node2", "*");
        NetworkRule rule2 = NetworkRule.delay("node3", "node4", "*", 5);
        
        bus.addRule(rule1);
        bus.addRule(rule2);
        
        // Remove rule1
        boolean removed = bus.removeRule(rule1);
        assertTrue(removed);
        
        List<NetworkRule> rules = bus.getRules();
        assertEquals(1, rules.size());
        assertFalse(rules.contains(rule1));
        assertTrue(rules.contains(rule2));
    }
    
    @Test
    @DisplayName("MessageBus should return false when removing non-existent rule")
    void shouldReturnFalseWhenRemovingNonExistentRule() {
        NetworkRule rule1 = NetworkRule.drop("node1", "node2", "*");
        NetworkRule rule2 = NetworkRule.delay("node3", "node4", "*", 5);
        
        bus.addRule(rule1);
        
        // Try to remove rule2 (not added)
        boolean removed = bus.removeRule(rule2);
        assertFalse(removed);
        
        List<NetworkRule> rules = bus.getRules();
        assertEquals(1, rules.size());
        assertTrue(rules.contains(rule1));
    }
    
    @Test
    @DisplayName("MessageBus should clear all rules")
    void shouldClearAllRules() {
        NetworkRule rule1 = NetworkRule.drop("node1", "node2", "*");
        NetworkRule rule2 = NetworkRule.delay("node3", "node4", "*", 5);
        NetworkRule rule3 = NetworkRule.dropPct("node5", "node6", "*", 0.5);
        
        bus.addRule(rule1);
        bus.addRule(rule2);
        bus.addRule(rule3);
        
        assertEquals(3, bus.getRules().size());
        
        bus.clearRules();
        
        List<NetworkRule> rules = bus.getRules();
        assertTrue(rules.isEmpty());
    }
    
    @Test
    @DisplayName("MessageBus should apply rules in order and allow removal")
    void shouldApplyRulesInOrderAndAllowRemoval() {
        // Add a drop rule
        NetworkRule dropRule = NetworkRule.drop("node1", "node2", "*");
        bus.addRule(dropRule);
        
        // Send a message that should be dropped
        Message message = new Message("node1", "node2", "test", Map.of("data", "hello"));
        bus.send(message);
        bus.step();
        
        // Message should be dropped
        assertTrue(bus.getAllMessages("node2").isEmpty());
        
        // Remove the drop rule
        bus.removeRule(dropRule);
        
        // Send another message - should now pass through
        Message message2 = new Message("node1", "node2", "test", Map.of("data", "world"));
        bus.send(message2);
        bus.step();
        
        // Message should now be delivered
        List<Message> received = bus.getAllMessages("node2");
        assertFalse(received.isEmpty());
        assertEquals(1, received.size());
        assertEquals(message2, received.get(0));
    }
    
    @Test
    @DisplayName("MessageBus should handle multiple rules of same type")
    void shouldHandleMultipleRulesOfSameType() {
        // Add multiple drop rules
        NetworkRule dropRule1 = NetworkRule.drop("node1", "node2", "*");
        NetworkRule dropRule2 = NetworkRule.drop("node3", "node4", "*");
        NetworkRule dropRule3 = NetworkRule.drop("node1", "node2", "specific");
        
        bus.addRule(dropRule1);
        bus.addRule(dropRule2);
        bus.addRule(dropRule3);
        
        assertEquals(3, bus.getRules().size());
        
        // Remove one specific rule
        bus.removeRule(dropRule2);
        
        List<NetworkRule> rules = bus.getRules();
        assertEquals(2, rules.size());
        assertTrue(rules.contains(dropRule1));
        assertFalse(rules.contains(dropRule2));
        assertTrue(rules.contains(dropRule3));
    }
    
    @Test
    @DisplayName("MessageBus should maintain rule order after removal")
    void shouldMaintainRuleOrderAfterRemoval() {
        NetworkRule rule1 = NetworkRule.drop("node1", "node2", "*");
        NetworkRule rule2 = NetworkRule.delay("node3", "node4", "*", 5);
        NetworkRule rule3 = NetworkRule.dropPct("node5", "node6", "*", 0.5);
        
        bus.addRule(rule1);
        bus.addRule(rule2);
        bus.addRule(rule3);
        
        // Remove middle rule
        bus.removeRule(rule2);
        
        List<NetworkRule> rules = bus.getRules();
        assertEquals(2, rules.size());
        assertEquals(rule1, rules.get(0));
        assertEquals(rule3, rules.get(1));
    }
}
