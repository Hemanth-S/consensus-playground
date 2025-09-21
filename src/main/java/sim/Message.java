package sim;

import java.util.Objects;

/**
 * Represents a message sent between nodes in the distributed system.
 * Contains sender, recipient, payload, and delivery timing information.
 */
public class Message {
    private final String from;
    private final String to;
    private final Object payload;
    private final int deliveryTime;
    private final int sendTime;
    
    public Message(String from, String to, Object payload, int deliveryTime) {
        this.from = from;
        this.to = to;
        this.payload = payload;
        this.deliveryTime = deliveryTime;
        this.sendTime = 0; // Will be set by the sender
    }
    
    public Message(String from, String to, Object payload, int sendTime, int deliveryTime) {
        this.from = from;
        this.to = to;
        this.payload = payload;
        this.sendTime = sendTime;
        this.deliveryTime = deliveryTime;
    }
    
    // Getters
    public String getFrom() { return from; }
    public String getTo() { return to; }
    public Object getPayload() { return payload; }
    public int getDeliveryTime() { return deliveryTime; }
    public int getSendTime() { return sendTime; }
    
    /**
     * Get the payload cast to a specific type
     */
    @SuppressWarnings("unchecked")
    public <T> T getPayload(Class<T> type) {
        if (type.isInstance(payload)) {
            return (T) payload;
        }
        throw new ClassCastException("Payload is not of type " + type.getName());
    }
    
    /**
     * Check if this message is ready for delivery at the given time
     */
    public boolean isReadyForDelivery(int currentTime) {
        return currentTime >= deliveryTime;
    }
    
    /**
     * Get the network delay for this message
     */
    public int getNetworkDelay() {
        return deliveryTime - sendTime;
    }
    
    @Override
    public String toString() {
        return String.format("Message{from='%s', to='%s', payload=%s, deliveryTime=%d}", 
                           from, to, payload, deliveryTime);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Message message = (Message) obj;
        return deliveryTime == message.deliveryTime &&
               sendTime == message.sendTime &&
               Objects.equals(from, message.from) &&
               Objects.equals(to, message.to) &&
               Objects.equals(payload, message.payload);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(from, to, payload, deliveryTime, sendTime);
    }
}

