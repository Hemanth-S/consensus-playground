package raft;

import sim.Determinism;
import sim.Message;
import sim.MessageBus;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents a single node in the Raft consensus algorithm.
 * Implements the core Raft protocol including leader election and log replication.
 */
public class RaftNode {
    private final String nodeId;
    private final List<String> peerIds;
    private MessageBus messageBus;
    
    // Persistent state (updated on stable storage before responding to RPCs)
    private int currentTerm = 0;
    private String votedFor = null;
    private final List<RaftLogEntry> log = new ArrayList<>();
    
    // Volatile state on all servers
    private int commitIndex = 0;
    private int lastApplied = 0;
    
    // Volatile state on leaders (reinitialized after election)
    private final Map<String, Integer> nextIndex = new HashMap<>();
    private final Map<String, Integer> matchIndex = new HashMap<>();
    
    // Node state
    private RaftRole role = RaftRole.FOLLOWER;
    private int electionTimeout;
    private int heartbeatTimeout;
    private long lastHeartbeatTime = 0;
    private int votesReceived = 0;
    private boolean crashed = false;
    
    public RaftNode(String nodeId, List<String> peerIds, MessageBus messageBus) {
        this.nodeId = nodeId;
        this.peerIds = new ArrayList<>(peerIds);
        this.messageBus = messageBus;
        this.electionTimeout = 150 + new Random().nextInt(150); // 150-300ms
        this.heartbeatTimeout = 50; // 50ms
    }
    
    public RaftNode(String nodeId, List<String> peerIds) {
        this.nodeId = nodeId;
        this.peerIds = new ArrayList<>(peerIds);
        this.messageBus = null; // Will be set later
        this.electionTimeout = 150 + new Random().nextInt(150); // 150-300ms
        this.heartbeatTimeout = 50; // 50ms
    }
    
    /**
     * Set the message bus for this node
     */
    public void setMessageBus(MessageBus messageBus) {
        this.messageBus = messageBus;
    }
    
    /**
     * Step the node forward by one time unit
     */
    public void step(long currentTime, Determinism determinism) {
        if (crashed || messageBus == null) return;
        
        switch (role) {
            case FOLLOWER -> stepFollower(currentTime, determinism);
            case CANDIDATE -> stepCandidate(currentTime, determinism);
            case LEADER -> stepLeader(currentTime, determinism);
        }
        
        // Process incoming messages
        processMessages(currentTime, determinism);
    }
    
    /**
     * Step follower logic
     */
    private void stepFollower(long currentTime, Determinism determinism) {
        // Check if election timeout has passed
        if (currentTime - lastHeartbeatTime > electionTimeout) {
            startElection(currentTime, determinism);
        }
    }
    
    /**
     * Step candidate logic
     */
    private void stepCandidate(long currentTime, Determinism determinism) {
        // Check if election timeout has passed
        if (currentTime - lastHeartbeatTime > electionTimeout) {
            startElection(currentTime, determinism);
        }
    }
    
    /**
     * Step leader logic
     */
    private void stepLeader(long currentTime, Determinism determinism) {
        // Send heartbeats periodically
        if (currentTime - lastHeartbeatTime > heartbeatTimeout) {
            sendHeartbeats(currentTime, determinism);
            lastHeartbeatTime = currentTime;
        }
    }
    
    /**
     * Start a new election
     */
    private void startElection(long currentTime, Determinism determinism) {
        currentTerm++;
        role = RaftRole.CANDIDATE;
        votedFor = nodeId;
        votesReceived = 1; // Vote for self
        lastHeartbeatTime = currentTime;
        electionTimeout = 150 + determinism.nextInt(150);
        
        // Send RequestVote RPCs to all peers
        RaftLogEntry lastEntry = getLastLogEntry();
        RaftRpc.RequestVote request = new RaftRpc.RequestVote(
            currentTerm, nodeId, 
            lastEntry != null ? lastEntry.getIndex() : 0,
            lastEntry != null ? lastEntry.getTerm() : 0
        );
        
        for (String peerId : peerIds) {
            messageBus.sendMessage(nodeId, peerId, request);
        }
        
        System.out.println(nodeId + " started election for term " + currentTerm);
    }
    
    /**
     * Send heartbeats to all followers
     */
    private void sendHeartbeats(long currentTime, Determinism determinism) {
        for (String peerId : peerIds) {
            RaftRpc.AppendEntries heartbeat = new RaftRpc.AppendEntries(
                currentTerm, nodeId, 
                getLastLogIndex(), getLastLogTerm(),
                Collections.emptyList(), commitIndex
            );
            messageBus.sendMessage(nodeId, peerId, heartbeat);
        }
    }
    
    /**
     * Process incoming messages
     */
    private void processMessages(long currentTime, Determinism determinism) {
        List<Message> messages = messageBus.getAllMessages(nodeId);
        for (Message message : messages) {
            Object payload = message.getPayload();
            
            if (payload instanceof RaftRpc.RequestVote) {
                handleRequestVote((RaftRpc.RequestVote) payload, message.getFrom(), determinism);
            } else if (payload instanceof RaftRpc.RequestVoteResponse) {
                handleRequestVoteResponse((RaftRpc.RequestVoteResponse) payload, determinism);
            } else if (payload instanceof RaftRpc.AppendEntries) {
                handleAppendEntries((RaftRpc.AppendEntries) payload, message.getFrom(), determinism);
            } else if (payload instanceof RaftRpc.AppendEntriesResponse) {
                handleAppendEntriesResponse((RaftRpc.AppendEntriesResponse) payload, message.getFrom(), determinism);
            }
        }
    }
    
    /**
     * Handle RequestVote RPC
     */
    private void handleRequestVote(RaftRpc.RequestVote request, String candidateId, Determinism determinism) {
        boolean voteGranted = false;
        
        if (request.getTerm() > currentTerm) {
            currentTerm = request.getTerm();
            role = RaftRole.FOLLOWER;
            votedFor = null;
        }
        
        if (request.getTerm() == currentTerm && 
            (votedFor == null || votedFor.equals(candidateId)) &&
            isUpToDate(request.getLastLogIndex(), request.getLastLogTerm())) {
            votedFor = candidateId;
            voteGranted = true;
            lastHeartbeatTime = System.currentTimeMillis(); // Reset election timeout
        }
        
        RaftRpc.RequestVoteResponse response = new RaftRpc.RequestVoteResponse(currentTerm, voteGranted);
        messageBus.sendMessage(nodeId, candidateId, response);
    }
    
    /**
     * Handle RequestVoteResponse
     */
    private void handleRequestVoteResponse(RaftRpc.RequestVoteResponse response, Determinism determinism) {
        if (response.getTerm() > currentTerm) {
            currentTerm = response.getTerm();
            role = RaftRole.FOLLOWER;
            votedFor = null;
            return;
        }
        
        if (role == RaftRole.CANDIDATE && response.getTerm() == currentTerm) {
            if (response.isVoteGranted()) {
                votesReceived++;
                if (votesReceived > peerIds.size() / 2) {
                    becomeLeader(determinism);
                }
            }
        }
    }
    
    /**
     * Handle AppendEntries RPC
     */
    private void handleAppendEntries(RaftRpc.AppendEntries request, String leaderId, Determinism determinism) {
        boolean success = false;
        
        if (request.getTerm() >= currentTerm) {
            currentTerm = request.getTerm();
            role = RaftRole.FOLLOWER;
            votedFor = null;
            lastHeartbeatTime = System.currentTimeMillis();
            
            if (request.getPrevLogIndex() == 0 || 
                (request.getPrevLogIndex() <= log.size() && 
                 getLogEntry(request.getPrevLogIndex()).getTerm() == request.getPrevLogTerm())) {
                success = true;
                
                // Append new entries
                if (request.getEntries() != null && !request.getEntries().isEmpty()) {
                    // TODO: Implement log replication logic
                }
                
                // Update commit index
                if (request.getLeaderCommit() > commitIndex) {
                    commitIndex = Math.min(request.getLeaderCommit(), getLastLogIndex());
                }
            }
        }
        
        RaftRpc.AppendEntriesResponse response = new RaftRpc.AppendEntriesResponse(
            currentTerm, success, getLastLogIndex()
        );
        messageBus.sendMessage(nodeId, leaderId, response);
    }
    
    /**
     * Handle AppendEntriesResponse
     */
    private void handleAppendEntriesResponse(RaftRpc.AppendEntriesResponse response, String followerId, Determinism determinism) {
        if (response.getTerm() > currentTerm) {
            currentTerm = response.getTerm();
            role = RaftRole.FOLLOWER;
            votedFor = null;
            return;
        }
        
        if (role == RaftRole.LEADER && response.getTerm() == currentTerm) {
            if (response.isSuccess()) {
                nextIndex.put(followerId, response.getMatchIndex() + 1);
                matchIndex.put(followerId, response.getMatchIndex());
                
                // Update commit index
                updateCommitIndex();
            } else {
                nextIndex.put(followerId, Math.max(1, nextIndex.get(followerId) - 1));
            }
        }
    }
    
    /**
     * Become leader
     */
    private void becomeLeader(Determinism determinism) {
        role = RaftRole.LEADER;
        System.out.println(nodeId + " became leader for term " + currentTerm);
        
        // Initialize leader state
        for (String peerId : peerIds) {
            nextIndex.put(peerId, getLastLogIndex() + 1);
            matchIndex.put(peerId, 0);
        }
        
        // Send initial heartbeats
        sendHeartbeats(System.currentTimeMillis(), determinism);
    }
    
    /**
     * Update commit index based on match indices
     */
    private void updateCommitIndex() {
        List<Integer> matchIndices = new ArrayList<>(matchIndex.values());
        matchIndices.add(getLastLogIndex()); // Include leader's own log
        Collections.sort(matchIndices, Collections.reverseOrder());
        
        int majorityIndex = matchIndices.get(peerIds.size() / 2);
        if (majorityIndex > commitIndex && 
            getLogEntry(majorityIndex).getTerm() == currentTerm) {
            commitIndex = majorityIndex;
        }
    }
    
    /**
     * Check if candidate's log is up-to-date
     */
    private boolean isUpToDate(int lastLogIndex, int lastLogTerm) {
        RaftLogEntry lastEntry = getLastLogEntry();
        if (lastEntry == null) return true;
        
        return lastLogTerm > lastEntry.getTerm() ||
               (lastLogTerm == lastEntry.getTerm() && lastLogIndex >= lastEntry.getIndex());
    }
    
    /**
     * Get the last log entry
     */
    private RaftLogEntry getLastLogEntry() {
        return log.isEmpty() ? null : log.get(log.size() - 1);
    }
    
    /**
     * Get the last log index
     */
    private int getLastLogIndex() {
        return log.isEmpty() ? 0 : getLastLogEntry().getIndex();
    }
    
    /**
     * Get the last log term
     */
    private int getLastLogTerm() {
        return log.isEmpty() ? 0 : getLastLogEntry().getTerm();
    }
    
    /**
     * Get log entry at index (1-based)
     */
    private RaftLogEntry getLogEntry(int index) {
        if (index < 1 || index > log.size()) {
            return null;
        }
        return log.get(index - 1);
    }
    
    /**
     * Crash this node
     */
    public void crash() {
        crashed = true;
        role = RaftRole.FOLLOWER;
        System.out.println(nodeId + " crashed");
    }
    
    /**
     * Recover this node
     */
    public void recover() {
        crashed = false;
        role = RaftRole.FOLLOWER;
        votedFor = null;
        lastHeartbeatTime = System.currentTimeMillis();
        System.out.println(nodeId + " recovered");
    }
    
    // Getters
    public String getNodeId() { return nodeId; }
    public RaftRole getRole() { return role; }
    public int getCurrentTerm() { return currentTerm; }
    public boolean isCrashed() { return crashed; }
    public int getCommitIndex() { return commitIndex; }
    public List<RaftLogEntry> getLog() { return new ArrayList<>(log); }
}

