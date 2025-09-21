package raft;

import java.util.Objects;

/**
 * Represents a single entry in the Raft log.
 * Contains the command, term, and index information.
 */
public class RaftLogEntry {
    private final int term;
    private final int index;
    private final Object command;
    
    public RaftLogEntry(int term, int index, Object command) {
        this.term = term;
        this.index = index;
        this.command = command;
    }
    
    // Getters
    public int getTerm() { return term; }
    public int getIndex() { return index; }
    public Object getCommand() { return command; }
    
    /**
     * Check if this entry is from a specific term
     */
    public boolean isFromTerm(int term) {
        return this.term == term;
    }
    
    /**
     * Check if this entry is at a specific index
     */
    public boolean isAtIndex(int index) {
        return this.index == index;
    }
    
    /**
     * Get the command cast to a specific type
     */
    @SuppressWarnings("unchecked")
    public <T> T getCommand(Class<T> type) {
        if (type.isInstance(command)) {
            return (T) command;
        }
        throw new ClassCastException("Command is not of type " + type.getName());
    }
    
    /**
     * Create a no-op entry (used for leader heartbeat)
     */
    public static RaftLogEntry noOp(int term, int index) {
        return new RaftLogEntry(term, index, "NO_OP");
    }
    
    @Override
    public String toString() {
        return String.format("RaftLogEntry{term=%d, index=%d, command=%s}", 
                           term, index, command);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        RaftLogEntry that = (RaftLogEntry) obj;
        return term == that.term &&
               index == that.index &&
               Objects.equals(command, that.command);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(term, index, command);
    }
}

