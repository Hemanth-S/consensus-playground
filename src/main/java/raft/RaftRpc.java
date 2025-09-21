package raft;

import java.util.List;

/**
 * RPC message types used in the Raft consensus algorithm.
 * Defines the structure of messages sent between Raft nodes.
 */
public class RaftRpc {
    
    /**
     * RequestVote RPC - sent by candidates to request votes
     */
    public static class RequestVote {
        private final int term;
        private final String candidateId;
        private final int lastLogIndex;
        private final int lastLogTerm;
        
        public RequestVote(int term, String candidateId, int lastLogIndex, int lastLogTerm) {
            this.term = term;
            this.candidateId = candidateId;
            this.lastLogIndex = lastLogIndex;
            this.lastLogTerm = lastLogTerm;
        }
        
        // Getters
        public int getTerm() { return term; }
        public String getCandidateId() { return candidateId; }
        public int getLastLogIndex() { return lastLogIndex; }
        public int getLastLogTerm() { return lastLogTerm; }
        
        @Override
        public String toString() {
            return String.format("RequestVote{term=%d, candidateId='%s', lastLogIndex=%d, lastLogTerm=%d}", 
                               term, candidateId, lastLogIndex, lastLogTerm);
        }
    }
    
    /**
     * RequestVoteResponse - response to RequestVote RPC
     */
    public static class RequestVoteResponse {
        private final int term;
        private final boolean voteGranted;
        
        public RequestVoteResponse(int term, boolean voteGranted) {
            this.term = term;
            this.voteGranted = voteGranted;
        }
        
        // Getters
        public int getTerm() { return term; }
        public boolean isVoteGranted() { return voteGranted; }
        
        @Override
        public String toString() {
            return String.format("RequestVoteResponse{term=%d, voteGranted=%s}", term, voteGranted);
        }
    }
    
    /**
     * AppendEntries RPC - sent by leader to replicate log entries
     */
    public static class AppendEntries {
        private final int term;
        private final String leaderId;
        private final int prevLogIndex;
        private final int prevLogTerm;
        private final List<RaftLogEntry> entries;
        private final int leaderCommit;
        
        public AppendEntries(int term, String leaderId, int prevLogIndex, int prevLogTerm, 
                           List<RaftLogEntry> entries, int leaderCommit) {
            this.term = term;
            this.leaderId = leaderId;
            this.prevLogIndex = prevLogIndex;
            this.prevLogTerm = prevLogTerm;
            this.entries = entries;
            this.leaderCommit = leaderCommit;
        }
        
        // Getters
        public int getTerm() { return term; }
        public String getLeaderId() { return leaderId; }
        public int getPrevLogIndex() { return prevLogIndex; }
        public int getPrevLogTerm() { return prevLogTerm; }
        public List<RaftLogEntry> getEntries() { return entries; }
        public int getLeaderCommit() { return leaderCommit; }
        
        /**
         * Check if this is a heartbeat (no entries)
         */
        public boolean isHeartbeat() {
            return entries == null || entries.isEmpty();
        }
        
        @Override
        public String toString() {
            return String.format("AppendEntries{term=%d, leaderId='%s', prevLogIndex=%d, prevLogTerm=%d, entries=%d, leaderCommit=%d}", 
                               term, leaderId, prevLogIndex, prevLogTerm, 
                               entries != null ? entries.size() : 0, leaderCommit);
        }
    }
    
    /**
     * AppendEntriesResponse - response to AppendEntries RPC
     */
    public static class AppendEntriesResponse {
        private final int term;
        private final boolean success;
        private final int matchIndex;
        
        public AppendEntriesResponse(int term, boolean success, int matchIndex) {
            this.term = term;
            this.success = success;
            this.matchIndex = matchIndex;
        }
        
        // Getters
        public int getTerm() { return term; }
        public boolean isSuccess() { return success; }
        public int getMatchIndex() { return matchIndex; }
        
        @Override
        public String toString() {
            return String.format("AppendEntriesResponse{term=%d, success=%s, matchIndex=%d}", 
                               term, success, matchIndex);
        }
    }
    
    /**
     * InstallSnapshot RPC - sent by leader to install a snapshot
     */
    public static class InstallSnapshot {
        private final int term;
        private final String leaderId;
        private final int lastIncludedIndex;
        private final int lastIncludedTerm;
        private final Object snapshotData;
        
        public InstallSnapshot(int term, String leaderId, int lastIncludedIndex, 
                             int lastIncludedTerm, Object snapshotData) {
            this.term = term;
            this.leaderId = leaderId;
            this.lastIncludedIndex = lastIncludedIndex;
            this.lastIncludedTerm = lastIncludedTerm;
            this.snapshotData = snapshotData;
        }
        
        // Getters
        public int getTerm() { return term; }
        public String getLeaderId() { return leaderId; }
        public int getLastIncludedIndex() { return lastIncludedIndex; }
        public int getLastIncludedTerm() { return lastIncludedTerm; }
        public Object getSnapshotData() { return snapshotData; }
        
        @Override
        public String toString() {
            return String.format("InstallSnapshot{term=%d, leaderId='%s', lastIncludedIndex=%d, lastIncludedTerm=%d}", 
                               term, leaderId, lastIncludedIndex, lastIncludedTerm);
        }
    }
    
    /**
     * InstallSnapshotResponse - response to InstallSnapshot RPC
     */
    public static class InstallSnapshotResponse {
        private final int term;
        
        public InstallSnapshotResponse(int term) {
            this.term = term;
        }
        
        // Getters
        public int getTerm() { return term; }
        
        @Override
        public String toString() {
            return String.format("InstallSnapshotResponse{term=%d}", term);
        }
    }
}

