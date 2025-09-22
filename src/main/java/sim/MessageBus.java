package sim;

import java.util.*;

/**
 * Discrete-time message bus that manages message passing between nodes.
 * Applies network rules and handles message queuing with tick-based timing.
 */
public class MessageBus {
    private final List<NetworkRule> rules = new ArrayList<>();
    private final Queue<DelayedMessage> delayedMessages = new PriorityQueue<>(Comparator.comparingInt(m -> m.deliveryTime));
    private final Map<String, Queue<Message>> nodeInboxes = new HashMap<>();
    private int currentTime = 0;
    
    /**
     * Represents a message with its delivery time
     */
    private static class DelayedMessage {
        final Message message;
        final int deliveryTime;
        
        DelayedMessage(Message message, int deliveryTime) {
            this.message = message;
            this.deliveryTime = deliveryTime;
        }
    }
    
    /**
     * Add a network rule
     */
    public void addRule(NetworkRule rule) {
        rules.add(rule);
    }
    
    /**
     * Send a message through the bus
     */
    public void send(Message message) {
        // Apply rules in order
        for (NetworkRule rule : rules) {
            if (rule.match.matches(message)) {
                switch (rule.action) {
                    case PASS -> {
                        // Message passes through immediately
                        deliver(message);
                        return;
                    }
                    case DROP -> {
                        // Message is dropped
                        return;
                    }
                    case DELAY -> {
                        // Message is delayed
                        int deliveryTime = currentTime + rule.delaySteps;
                        delayedMessages.offer(new DelayedMessage(message, deliveryTime));
                        return;
                    }
                    case DROP_PCT -> {
                        // Probabilistic drop
                        if (Determinism.chance(rule.dropPct)) {
                            return; // Message dropped
                        }
                        // Message passes through, continue to next rule
                    }
                }
            }
        }
        
        // If no rules matched or all rules passed, deliver immediately
        deliver(message);
    }
    
    /**
     * Deliver a message to its recipient's inbox
     */
    private void deliver(Message message) {
        String recipient = message.to;
        nodeInboxes.computeIfAbsent(recipient, k -> new LinkedList<>()).offer(message);
    }
    
    /**
     * Drain all messages ready for delivery at the current time
     */
    public List<Message> drainReady() {
        List<Message> readyMessages = new ArrayList<>();
        
        // Process delayed messages that are ready
        while (!delayedMessages.isEmpty() && delayedMessages.peek().deliveryTime <= currentTime) {
            DelayedMessage delayedMessage = delayedMessages.poll();
            deliver(delayedMessage.message);
            readyMessages.add(delayedMessage.message);
        }
        
        return readyMessages;
    }
    
    /**
     * Step the message bus forward by one time unit
     */
    public void step() {
        currentTime++;
        drainReady();
    }
    
    /**
     * Get the current time
     */
    public int now() {
        return currentTime;
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
        return delayedMessages.size();
    }
    
    /**
     * Clear all messages and reset time (useful for testing)
     */
    public void clear() {
        delayedMessages.clear();
        nodeInboxes.clear();
        currentTime = 0;
    }
    
    /**
     * Get all network rules
     */
    public List<NetworkRule> getRules() {
        return new ArrayList<>(rules);
    }
}

