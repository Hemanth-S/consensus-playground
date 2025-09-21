package raft;

/**
 * Represents the different roles a Raft node can have in the consensus algorithm.
 */
public enum RaftRole {
    /**
     * Leader role - can accept client requests and replicate log entries
     */
    LEADER,
    
    /**
     * Follower role - receives log entries from leader and votes in elections
     */
    FOLLOWER,
    
    /**
     * Candidate role - attempting to become leader by starting an election
     */
    CANDIDATE;
    
    /**
     * Check if this role can accept client requests
     */
    public boolean canAcceptRequests() {
        return this == LEADER;
    }
    
    /**
     * Check if this role can vote in elections
     */
    public boolean canVote() {
        return this == FOLLOWER || this == CANDIDATE;
    }
    
    /**
     * Check if this role can start elections
     */
    public boolean canStartElection() {
        return this == FOLLOWER;
    }
    
    /**
     * Get a human-readable description of this role
     */
    public String getDescription() {
        return switch (this) {
            case LEADER -> "Leader - accepts client requests and replicates log entries";
            case FOLLOWER -> "Follower - receives log entries and votes in elections";
            case CANDIDATE -> "Candidate - attempting to become leader";
        };
    }
}

