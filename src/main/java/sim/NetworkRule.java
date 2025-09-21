package sim;

import java.util.Objects;

/**
 * Defines network behavior rules for message passing between nodes.
 * Controls message delays and drop rates for specific node pairs.
 */
public class NetworkRule {
    private final String from;
    private final String to;
    private final int delay;
    private final double dropRate;
    
    public NetworkRule(String from, String to, int delay, double dropRate) {
        this.from = from;
        this.to = to;
        this.delay = Math.max(0, delay);
        this.dropRate = Math.max(0.0, Math.min(1.0, dropRate));
    }
    
    /**
     * Check if this rule applies to a message from the given sender to the given recipient
     */
    public boolean matches(String from, String to) {
        return (this.from.equals("*") || this.from.equals(from)) &&
               (this.to.equals("*") || this.to.equals(to));
    }
    
    /**
     * Calculate the delivery time for a message sent at the given time
     */
    public int calculateDeliveryTime(int sendTime) {
        return sendTime + delay;
    }
    
    // Getters
    public String getFrom() { return from; }
    public String getTo() { return to; }
    public int getDelay() { return delay; }
    public double getDropRate() { return dropRate; }
    
    /**
     * Create a bidirectional network rule (applies in both directions)
     */
    public static NetworkRule bidirectional(String node1, String node2, int delay, double dropRate) {
        return new NetworkRule(node1, node2, delay, dropRate);
    }
    
    /**
     * Create a rule that applies to all messages from a specific node
     */
    public static NetworkRule fromNode(String from, int delay, double dropRate) {
        return new NetworkRule(from, "*", delay, dropRate);
    }
    
    /**
     * Create a rule that applies to all messages to a specific node
     */
    public static NetworkRule toNode(String to, int delay, double dropRate) {
        return new NetworkRule("*", to, delay, dropRate);
    }
    
    /**
     * Create a rule that applies to all messages in the network
     */
    public static NetworkRule global(int delay, double dropRate) {
        return new NetworkRule("*", "*", delay, dropRate);
    }
    
    @Override
    public String toString() {
        return String.format("NetworkRule{from='%s', to='%s', delay=%d, dropRate=%.2f}", 
                           from, to, delay, dropRate);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        NetworkRule that = (NetworkRule) obj;
        return delay == that.delay &&
               Double.compare(that.dropRate, dropRate) == 0 &&
               Objects.equals(from, that.from) &&
               Objects.equals(to, that.to);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(from, to, delay, dropRate);
    }
}

