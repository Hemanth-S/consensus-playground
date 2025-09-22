package raft;

import sim.Cluster;
import sim.Determinism;
import sim.Message;
import sim.MessageBus;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents a single node in the Raft consensus algorithm.
 * Implements the core Raft protocol including leader election and log replication.
 */
public class RaftNode implements Cluster.Node {
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
        this.electionTimeout = 5 + new Random().nextInt(10); // 5-15 steps for testing
        this.heartbeatTimeout = 50; // 50ms
    }
    
    public RaftNode(String nodeId, List<String> peerIds) {
        this.nodeId = nodeId;
        this.peerIds = new ArrayList<>(peerIds);
        this.messageBus = null; // Will be set later
        this.electionTimeout = 5 + new Random().nextInt(10); // 5-15 steps for testing
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
    public void step(int currentTime, Determinism determinism) {
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
    private void stepFollower(int currentTime, Determinism determinism) {
        // Check if election timeout has passed
        if (currentTime - lastHeartbeatTime > electionTimeout) {
            startElection(currentTime, determinism);
        }
    }
    
    /**
     * Step candidate logic
     */
    private void stepCandidate(int currentTime, Determinism determinism) {
        // Check if election timeout has passed
        if (currentTime - lastHeartbeatTime > electionTimeout) {
            startElection(currentTime, determinism);
        }
    }
    
    /**
     * Step leader logic
     */
    private void stepLeader(int currentTime, Determinism determinism) {
        // Send heartbeats periodically
        if (currentTime - lastHeartbeatTime > heartbeatTimeout) {
            sendHeartbeats(currentTime, determinism);
            lastHeartbeatTime = currentTime;
        }
    }
    
    /**
     * Start a new election
     */
    private void startElection(int currentTime, Determinism determinism) {
        currentTerm++;
        role = RaftRole.CANDIDATE;
        votedFor = nodeId;
        votesReceived = 1; // Vote for self
        lastHeartbeatTime = currentTime;
        electionTimeout = 5 + (determinism != null ? determinism.nextInt(10) : new Random().nextInt(10));
        
        // Send RequestVote RPCs to all peers
        RaftLogEntry lastEntry = getLastLogEntry();
        RaftRpc.RequestVote request = new RaftRpc.RequestVote(
            currentTerm, nodeId, 
            lastEntry != null ? lastEntry.getIndex() : 0,
            lastEntry != null ? lastEntry.getTerm() : 0
        );
        
        for (String peerId : peerIds) {
            Map<String, Object> payload = Map.of("requestVote", request);
            Message message = new Message(nodeId, peerId, "RequestVote", payload);
            messageBus.send(message);
        }
        
        System.out.println(nodeId + " started election for term " + currentTerm);
    }
    
    /**
     * Send heartbeats to all followers
     */
    private void sendHeartbeats(int currentTime, Determinism determinism) {
        for (String peerId : peerIds) {
            RaftRpc.AppendEntries heartbeat = new RaftRpc.AppendEntries(
                currentTerm, nodeId, 
                getLastLogIndex(), getLastLogTerm(),
                Collections.emptyList(), commitIndex
            );
            Map<String, Object> payload = Map.of("heartbeat", heartbeat);
            Message message = new Message(nodeId, peerId, "AppendEntries", payload);
            messageBus.send(message);
        }
    }
    
    /**
     * Process incoming messages
     */
    private void processMessages(int currentTime, Determinism determinism) {
        List<Message> messages = messageBus.getAllMessages(nodeId);
        for (Message message : messages) {
            String messageType = message.type;
            
            if ("RequestVote".equals(messageType)) {
                RaftRpc.RequestVote request = (RaftRpc.RequestVote) message.get("requestVote");
                handleRequestVote(request, message.from, determinism);
            } else if ("RequestVoteResponse".equals(messageType)) {
                RaftRpc.RequestVoteResponse response = (RaftRpc.RequestVoteResponse) message.get("requestVoteResponse");
                handleRequestVoteResponse(response, determinism);
            } else if ("AppendEntries".equals(messageType)) {
                RaftRpc.AppendEntries request = (RaftRpc.AppendEntries) message.get("appendEntries");
                handleAppendEntries(request, message.from, determinism);
            } else if ("AppendEntriesResponse".equals(messageType)) {
                RaftRpc.AppendEntriesResponse response = (RaftRpc.AppendEntriesResponse) message.get("appendEntriesResponse");
                handleAppendEntriesResponse(response, message.from, determinism);
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
            lastHeartbeatTime = messageBus.now(); // Reset election timeout
        }
        
        RaftRpc.RequestVoteResponse response = new RaftRpc.RequestVoteResponse(currentTerm, voteGranted);
        Map<String, Object> payload = Map.of("requestVoteResponse", response);
        Message message = new Message(nodeId, candidateId, "RequestVoteResponse", payload);
        messageBus.send(message);
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
                // Need majority of total nodes (including self)
                int totalNodes = peerIds.size() + 1; // +1 for self
                if (votesReceived > totalNodes / 2) {
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
            lastHeartbeatTime = messageBus.now();
            
            if (request.getPrevLogIndex() == 0 || 
                (request.getPrevLogIndex() <= log.size() && 
                 getLogEntry(request.getPrevLogIndex()).getTerm() == request.getPrevLogTerm())) {
                success = true;
                
                // Append new entries
                if (request.getEntries() != null && !request.getEntries().isEmpty()) {
                    // Remove any conflicting entries
                    if (request.getPrevLogIndex() < log.size()) {
                        log.subList(request.getPrevLogIndex(), log.size()).clear();
                    }
                    // Append new entries
                    log.addAll(request.getEntries());
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
        Map<String, Object> payload = Map.of("appendEntriesResponse", response);
        Message message = new Message(nodeId, leaderId, "AppendEntriesResponse", payload);
        messageBus.send(message);
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
        sendHeartbeats(messageBus.now(), determinism);
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
        lastHeartbeatTime = messageBus != null ? messageBus.now() : 0;
        System.out.println(nodeId + " recovered");
    }
    
    // Cluster.Node interface implementation
    @Override
    public String id() {
        return nodeId;
    }
    
    @Override
    public boolean isUp() {
        return !crashed;
    }
    
    @Override
    public void setUp(boolean up) {
        if (up && crashed) {
            recover();
        } else if (!up && !crashed) {
            crash();
        }
    }
    
    @Override
    public void onTick(MessageBus bus) {
        // This will be called by the cluster for each simulation step
        // We'll use the current time from the message bus
        int currentTime = bus.now();
        
        if (crashed || messageBus == null) return;
        
        switch (role) {
            case FOLLOWER -> stepFollower(currentTime, null);
            case CANDIDATE -> stepCandidate(currentTime, null);
            case LEADER -> stepLeader(currentTime, null);
        }
    }
    
    @Override
    public void onMessage(Message m, MessageBus bus) {
        // Process incoming messages
        String messageType = m.type;
        
        if ("RequestVote".equals(messageType)) {
            RaftRpc.RequestVote request = (RaftRpc.RequestVote) m.get("requestVote");
            if (request != null) {
                handleRequestVote(request, m.from, null);
            }
        } else if ("RequestVoteResponse".equals(messageType)) {
            RaftRpc.RequestVoteResponse response = (RaftRpc.RequestVoteResponse) m.get("requestVoteResponse");
            if (response != null) {
                handleRequestVoteResponse(response, null);
            }
        } else if ("AppendEntries".equals(messageType)) {
            RaftRpc.AppendEntries request = (RaftRpc.AppendEntries) m.get("heartbeat");
            if (request != null) {
                handleAppendEntries(request, m.from, null);
            }
        } else if ("AppendEntriesResponse".equals(messageType)) {
            RaftRpc.AppendEntriesResponse response = (RaftRpc.AppendEntriesResponse) m.get("appendEntriesResponse");
            if (response != null) {
                handleAppendEntriesResponse(response, m.from, null);
            }
        }
    }
    
    @Override
    public String dump() {
        StringBuilder sb = new StringBuilder();
        sb.append("role=").append(role);
        sb.append(", term=").append(currentTerm);
        sb.append(", commitIndex=").append(commitIndex);
        sb.append(", logSize=").append(log.size());
        if (role == RaftRole.LEADER) {
            sb.append(", nextIndex=").append(nextIndex);
            sb.append(", matchIndex=").append(matchIndex);
        }
        return sb.toString();
    }
    
    /**
     * Add a client command to the log (only works for leaders)
     * 
     * @param command The client command to add
     * @return true if the command was added, false if not a leader
     */
    public boolean addClientCommand(String command) {
        if (role != RaftRole.LEADER) {
            return false;
        }
        
        // Create a new log entry with the current term and next index
        int nextIndex = log.size() + 1;
        RaftLogEntry entry = new RaftLogEntry(currentTerm, nextIndex, command);
        log.add(entry);
        
        // Send AppendEntries to all followers
        sendAppendEntriesToFollowers();
        
        return true;
    }
    
    /**
     * Send AppendEntries RPCs to all followers with new log entries
     */
    private void sendAppendEntriesToFollowers() {
        for (String peerId : peerIds) {
            int nextIdx = nextIndex.getOrDefault(peerId, 1);
            int prevLogIndex = nextIdx - 1;
            int prevLogTerm = 0;
            
            if (prevLogIndex > 0) {
                RaftLogEntry prevEntry = getLogEntry(prevLogIndex);
                if (prevEntry != null) {
                    prevLogTerm = prevEntry.getTerm();
                }
            }
            
            // Get entries to send (from nextIdx to end of log)
            List<RaftLogEntry> entriesToSend = new ArrayList<>();
            if (nextIdx <= log.size()) {
                entriesToSend = log.subList(nextIdx - 1, log.size());
            }
            
            RaftRpc.AppendEntries request = new RaftRpc.AppendEntries(
                currentTerm, nodeId, prevLogIndex, prevLogTerm, entriesToSend, commitIndex
            );
            
            Map<String, Object> payload = Map.of("heartbeat", request);
            Message message = new Message(nodeId, peerId, "AppendEntries", payload);
            messageBus.send(message);
        }
    }
    
    /**
     * Handle a client command (for followers, forward to leader if known)
     * 
     * @param command The client command
     * @return true if handled, false if not a leader and no leader known
     */
    public boolean handleClientCommand(String command) {
        if (role == RaftRole.LEADER) {
            return addClientCommand(command);
        } else {
            // For followers, we would typically forward to the leader
            // For now, just return false to indicate we can't handle it
            return false;
        }
    }
    
    // Getters
    public String getNodeId() { return nodeId; }
    public RaftRole getRole() { return role; }
    public int getCurrentTerm() { return currentTerm; }
    public boolean isCrashed() { return crashed; }
    public int getCommitIndex() { return commitIndex; }
    public List<RaftLogEntry> getLog() { return new ArrayList<>(log); }
}

