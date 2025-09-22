package sim;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a cluster of nodes in the distributed system simulation.
 * Manages nodes, message passing, and simulation timing.
 */
public class Cluster {
    
    /**
     * Interface for nodes in the cluster
     */
    public interface Node {
        String id();
        boolean isUp();
        void setUp(boolean up);
        void onTick(MessageBus bus);
        void onMessage(Message m, MessageBus bus);
        String dump();
    }
    
    private final Map<String, Node> nodes = new ConcurrentHashMap<>();
    private final MessageBus bus = new MessageBus();
    private final PriorityQueue<ScheduledEvent> eventQueue = new PriorityQueue<>();
    private int currentTime = 0;
    
    /**
     * Add a node to the cluster
     */
    public void add(Node node) {
        nodes.put(node.id(), node);
    }
    
    /**
     * Broadcast a message to all nodes
     */
    public void broadcast(String from, String type, Map<String, Object> payload) {
        for (String nodeId : nodes.keySet()) {
            if (!nodeId.equals(from)) {
                Message message = new Message(from, nodeId, type, payload);
                bus.send(message);
            }
        }
    }
    
    /**
     * Send a message through the bus
     */
    public void send(Message message) {
        bus.send(message);
    }
    
    /**
     * Step the simulation forward by one time unit
     */
    public void step() {
        currentTime++;
        
        // Process any events scheduled for this time
        while (!eventQueue.isEmpty() && eventQueue.peek().getTime() <= currentTime) {
            ScheduledEvent event = eventQueue.poll();
            processEvent(event);
        }
        
        // Step the message bus
        bus.step();
        
        // Notify all nodes of the tick
        for (Node node : nodes.values()) {
            if (node.isUp()) {
                node.onTick(bus);
            }
        }
        
        // Deliver messages to nodes
        for (Node node : nodes.values()) {
            if (node.isUp()) {
                List<Message> messages = bus.getAllMessages(node.id());
                for (Message message : messages) {
                    node.onMessage(message, bus);
                }
            }
        }
    }
    
    /**
     * Process a scheduled event
     */
    private void processEvent(ScheduledEvent event) {
        switch (event.getType()) {
            case "crash" -> {
                String nodeId = (String) event.getData().get("node");
                if (nodeId != null) {
                    Node node = nodes.get(nodeId);
                    if (node != null) {
                        node.setUp(false);
                        System.out.println("Node " + nodeId + " crashed at time " + currentTime);
                    }
                }
            }
            case "recover" -> {
                String nodeId = (String) event.getData().get("node");
                if (nodeId != null) {
                    Node node = nodes.get(nodeId);
                    if (node != null) {
                        node.setUp(true);
                        System.out.println("Node " + nodeId + " recovered at time " + currentTime);
                    }
                }
            }
            case "partition" -> {
                // TODO: Implement network partition logic
                System.out.println("Network partition event at time " + currentTime);
            }
            default -> {
                System.out.println("Unknown event type: " + event.getType() + " at time " + currentTime);
            }
        }
    }
    
    /**
     * Schedule an event to occur at a specific time
     */
    public void scheduleEvent(int time, String type, Map<String, Object> data) {
        eventQueue.offer(new ScheduledEvent(time, type, data));
    }
    
    /**
     * Get a node by ID
     */
    public Node get(String nodeId) {
        return nodes.get(nodeId);
    }
    
    /**
     * Get all nodes
     */
    public Collection<Node> all() {
        return new ArrayList<>(nodes.values());
    }
    
    /**
     * Get a dump of the cluster state
     */
    public String dump() {
        StringBuilder sb = new StringBuilder();
        sb.append("Cluster at time ").append(currentTime).append(":\n");
        sb.append("  Nodes: ").append(nodes.size()).append("\n");
        sb.append("  Pending messages: ").append(bus.getPendingMessageCount()).append("\n");
        sb.append("  Scheduled events: ").append(eventQueue.size()).append("\n");
        
        for (Node node : nodes.values()) {
            sb.append("  ").append(node.id()).append(": ").append(node.isUp() ? "UP" : "DOWN").append("\n");
            String nodeDump = node.dump();
            if (nodeDump != null && !nodeDump.trim().isEmpty()) {
                String[] lines = nodeDump.split("\n");
                for (String line : lines) {
                    sb.append("    ").append(line).append("\n");
                }
            }
        }
        
        return sb.toString();
    }
    
    // Getters
    public int getCurrentTime() { return currentTime; }
    public int getNodeCount() { return nodes.size(); }
    public int getPendingMessageCount() { return bus.getPendingMessageCount(); }
    public MessageBus getMessageBus() { return bus; }
    
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


