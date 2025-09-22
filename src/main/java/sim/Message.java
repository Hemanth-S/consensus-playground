package sim;

import java.util.Map;
import java.util.Objects;

/**
 * Represents a message sent between nodes in the distributed system.
 * Contains sender, recipient, type, and payload information.
 */
public class Message {
    public final String from;
    public final String to;
    public final String type;
    public final Map<String, Object> payload;
    
    public Message(String from, String to, String type, Map<String, Object> payload) {
        this.from = from;
        this.to = to;
        this.type = type;
        this.payload = payload;
    }
    
    /**
     * Get a payload value by key
     */
    public Object get(String key) {
        return payload != null ? payload.get(key) : null;
    }
    
    /**
     * Get a payload value by key with type casting
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Object value = get(key);
        if (type.isInstance(value)) {
            return (T) value;
        }
        throw new ClassCastException("Payload value for key '" + key + "' is not of type " + type.getName());
    }
    
    @Override
    public String toString() {
        return String.format("%s: %s -> %s (%s)", type, from, to, payload);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Message message = (Message) obj;
        return Objects.equals(from, message.from) &&
               Objects.equals(to, message.to) &&
               Objects.equals(type, message.type) &&
               Objects.equals(payload, message.payload);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(from, to, type, payload);
    }
}

