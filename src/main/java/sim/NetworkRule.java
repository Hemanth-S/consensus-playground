package sim;

import java.util.Objects;

/**
 * Defines network behavior rules for message passing between nodes.
 * Controls message delays, drops, and other network behaviors.
 */
public class NetworkRule {
    
    /**
     * Actions that can be applied to messages
     */
    public enum Action {
        PASS,    // Allow message to pass through
        DROP,    // Drop the message completely
        DELAY,   // Delay the message by delaySteps
        DROP_PCT // Drop message with probability dropPct
    }
    
    /**
     * Match criteria for network rules
     */
    public record Match(
        String from,        // Source node ID (null or "*" matches any)
        String to,          // Destination node ID (null or "*" matches any)
        String type,        // Message type (null or "*" matches any)
        String betweenA,    // First node in bidirectional match (null if not bidirectional)
        String betweenB,    // Second node in bidirectional match (null if not bidirectional)
        boolean bidirectional // Whether this is a bidirectional rule
    ) {
        public Match {
            // Canonical constructor - normalize nulls to empty strings for easier matching
            from = from == null ? "*" : from;
            to = to == null ? "*" : to;
            type = type == null ? "*" : type;
            betweenA = betweenA == null ? "*" : betweenA;
            betweenB = betweenB == null ? "*" : betweenB;
        }
        
        /**
         * Check if this match applies to a message
         */
        public boolean matches(Message message) {
            if (bidirectional) {
                return (betweenA.equals("*") || betweenA.equals(message.from) || betweenA.equals(message.to)) &&
                       (betweenB.equals("*") || betweenB.equals(message.from) || betweenB.equals(message.to)) &&
                       (type.equals("*") || type.equals(message.type));
            } else {
                return (from.equals("*") || from.equals(message.from)) &&
                       (to.equals("*") || to.equals(message.to)) &&
                       (type.equals("*") || type.equals(message.type));
            }
        }
    }
    
    public final Match match;
    public final Action action;
    public final int delaySteps;
    public final double dropPct;
    
    public NetworkRule(Match match, Action action, int delaySteps, double dropPct) {
        this.match = match;
        this.action = action;
        this.delaySteps = Math.max(0, delaySteps);
        this.dropPct = Math.max(0.0, Math.min(1.0, dropPct));
    }
    
    /**
     * Create a simple pass-through rule
     */
    public static NetworkRule pass(String from, String to, String type) {
        return new NetworkRule(new Match(from, to, type, null, null, false), Action.PASS, 0, 0.0);
    }
    
    /**
     * Create a drop rule
     */
    public static NetworkRule drop(String from, String to, String type) {
        return new NetworkRule(new Match(from, to, type, null, null, false), Action.DROP, 0, 0.0);
    }
    
    /**
     * Create a delay rule
     */
    public static NetworkRule delay(String from, String to, String type, int delaySteps) {
        return new NetworkRule(new Match(from, to, type, null, null, false), Action.DELAY, delaySteps, 0.0);
    }
    
    /**
     * Create a probabilistic drop rule
     */
    public static NetworkRule dropPct(String from, String to, String type, double dropPct) {
        return new NetworkRule(new Match(from, to, type, null, null, false), Action.DROP_PCT, 0, dropPct);
    }
    
    /**
     * Create a bidirectional rule
     */
    public static NetworkRule bidirectional(String nodeA, String nodeB, String type, Action action, int delaySteps, double dropPct) {
        return new NetworkRule(new Match(null, null, type, nodeA, nodeB, true), action, delaySteps, dropPct);
    }
    
    @Override
    public String toString() {
        return String.format("NetworkRule{match=%s, action=%s, delaySteps=%d, dropPct=%.2f}", 
                           match, action, delaySteps, dropPct);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        NetworkRule that = (NetworkRule) obj;
        return delaySteps == that.delaySteps &&
               Double.compare(that.dropPct, dropPct) == 0 &&
               Objects.equals(match, that.match) &&
               action == that.action;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(match, action, delaySteps, dropPct);
    }
}

