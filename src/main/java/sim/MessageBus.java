package sim;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Manages message passing between nodes in the cluster.
 * Handles message queuing, delivery timing, and network rules.
 */
public class MessageBus {
    private final Queue<Message> pendingMessages = new ConcurrentLinkedQueue<>();
    private final Map<String, Queue<Message>> nodeInboxes = new HashMap<>();
    
    /**
     * Send a message from one node to another
     */
    public void sendMessage(String from, String to, Object payload) {
        Message message = new Message(from, to, payload, 0); // Will be updated with delivery time
        pendingMessages.offer(message);
    }
    
    /**
     * Send a message with a specific delivery time
     */
    public void sendMessage(String from, String to, Object payload, int deliveryTime) {
        Message message = new Message(from, to, payload, deliveryTime);
        pendingMessages.offer(message);
    }
    
    /**
     * Deliver messages that are ready at the current time
     */
    public void deliverMessages(int currentTime, List<NetworkRule> networkRules, Determinism determinism) {
        Queue<Message> readyMessages = new LinkedList<>();
        
        // Find messages ready for delivery
        Iterator<Message> iterator = pendingMessages.iterator();
        while (iterator.hasNext()) {
            Message message = iterator.next();
            if (message.getDeliveryTime() <= currentTime) {
                readyMessages.offer(message);
                iterator.remove();
            }
        }
        
        // Deliver ready messages (applying network rules)
        for (Message message : readyMessages) {
            deliverMessage(message, networkRules, determinism);
        }
    }
    
    /**
     * Deliver a single message, applying network rules
     */
    private void deliverMessage(Message message, List<NetworkRule> networkRules, Determinism determinism) {
        // Check if message should be dropped
        for (NetworkRule rule : networkRules) {
            if (rule.matches(message.getFrom(), message.getTo())) {
                if (determinism.shouldDropMessage(rule.getDropRate())) {
                    // Message dropped
                    return;
                }
            }
        }
        
        // Deliver message to recipient's inbox
        String recipient = message.getTo();
        nodeInboxes.computeIfAbsent(recipient, k -> new LinkedList<>()).offer(message);
    }
    
    /**
     * Get the next message for a node (if any)
     */
    public Message getNextMessage(String nodeId) {
        Queue<Message> inbox = nodeInboxes.get(nodeId);
        return (inbox != null && !inbox.isEmpty()) ? inbox.poll() : null;
    }
    
    /**
     * Get all pending messages for a node
     */
    public List<Message> getAllMessages(String nodeId) {
        Queue<Message> inbox = nodeInboxes.get(nodeId);
        if (inbox == null || inbox.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<Message> messages = new ArrayList<>();
        Message message;
        while ((message = inbox.poll()) != null) {
            messages.add(message);
        }
        return messages;
    }
    
    /**
     * Check if a node has pending messages
     */
    public boolean hasMessages(String nodeId) {
        Queue<Message> inbox = nodeInboxes.get(nodeId);
        return inbox != null && !inbox.isEmpty();
    }
    
    /**
     * Get the total number of pending messages in the system
     */
    public int getPendingMessageCount() {
        return pendingMessages.size();
    }
    
    /**
     * Clear all messages (useful for testing)
     */
    public void clear() {
        pendingMessages.clear();
        nodeInboxes.clear();
    }
}

