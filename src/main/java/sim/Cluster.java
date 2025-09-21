package sim;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a cluster of nodes in the distributed system simulation.
 * Manages nodes, message passing, and simulation timing.
 */
public class Cluster {
    private final Map<String, Object> nodes = new ConcurrentHashMap<>();
    private final MessageBus messageBus = new MessageBus();
    private final List<NetworkRule> networkRules = new ArrayList<>();
    private final PriorityQueue<ScheduledEvent> eventQueue = new PriorityQueue<>();
    private int currentTime = 0;
    
    /**
     * Add a node to the cluster
     */
    public void addNode(String nodeId) {
        nodes.put(nodeId, new Object()); // Placeholder for actual node implementation
    }
    
    /**
     * Add a network rule to the cluster
     */
    public void addNetworkRule(NetworkRule rule) {
        networkRules.add(rule);
    }
    
    /**
     * Schedule an event to occur at a specific time
     */
    public void scheduleEvent(int time, String type, Map<String, Object> data) {
        eventQueue.offer(new ScheduledEvent(time, type, data));
    }
    
    /**
     * Step the simulation forward by one time unit
     */
    public void step(Determinism determinism) {
        currentTime++;
        
        // Process any events scheduled for this time
        while (!eventQueue.isEmpty() && eventQueue.peek().getTime() <= currentTime) {
            ScheduledEvent event = eventQueue.poll();
            processEvent(event, determinism);
        }
        
        // Process message delivery
        messageBus.deliverMessages(currentTime, networkRules, determinism);
        
        // Update all nodes
        for (String nodeId : nodes.keySet()) {
            updateNode(nodeId, determinism);
        }
    }
    
    /**
     * Process a scheduled event
     */
    private void processEvent(ScheduledEvent event, Determinism determinism) {
        switch (event.getType()) {
            case "crash" -> {
                String nodeId = (String) event.getData().get("node");
                if (nodeId != null) {
                    crashNode(nodeId);
                }
            }
            case "recover" -> {
                String nodeId = (String) event.getData().get("node");
                if (nodeId != null) {
                    recoverNode(nodeId);
                }
            }
            case "partition" -> {
                // TODO: Implement network partition logic
            }
            default -> {
                System.out.println("Unknown event type: " + event.getType());
            }
        }
    }
    
    /**
     * Update a node's state
     */
    private void updateNode(String nodeId, Determinism determinism) {
        // TODO: Implement node update logic
        // This would call the appropriate consensus algorithm's step method
    }
    
    /**
     * Crash a node
     */
    private void crashNode(String nodeId) {
        System.out.println("Node " + nodeId + " crashed at time " + currentTime);
        // TODO: Implement node crash logic
    }
    
    /**
     * Recover a node
     */
    private void recoverNode(String nodeId) {
        System.out.println("Node " + nodeId + " recovered at time " + currentTime);
        // TODO: Implement node recovery logic
    }
    
    // Getters
    public int getCurrentTime() { return currentTime; }
    public int getNodeCount() { return nodes.size(); }
    public int getPendingMessageCount() { return messageBus.getPendingMessageCount(); }
    public MessageBus getMessageBus() { return messageBus; }
    
    /**
     * Represents a scheduled event
     */
    private static class ScheduledEvent implements Comparable<ScheduledEvent> {
        private final int time;
        private final String type;
        private final Map<String, Object> data;
        
        public ScheduledEvent(int time, String type, Map<String, Object> data) {
            this.time = time;
            this.type = type;
            this.data = data;
        }
        
        public int getTime() { return time; }
        public String getType() { return type; }
        public Map<String, Object> getData() { return data; }
        
        @Override
        public int compareTo(ScheduledEvent other) {
            return Integer.compare(this.time, other.time);
        }
    }
}

