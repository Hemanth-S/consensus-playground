package app;

import io.ScenarioLoader;
import raft.RaftModel;
import sim.NetworkRule;

import java.util.*;

/**
 * Controls simulation execution, timeline actions, and assertion evaluation.
 * Keeps track of current tick and executes scenario timeline actions at scheduled times.
 */
public class SimulationController {
    private int currentTick = 0;
    private int nextActionIdx = 0;
    private ScenarioLoader.Scenario scenario;
    private final RaftModel raftModel;
    
    public SimulationController(RaftModel raftModel) {
        this.raftModel = raftModel;
    }
    
    /**
     * Attach a scenario to this controller and reset state
     */
    public void attachScenario(ScenarioLoader.Scenario s) {
        this.scenario = s;
        this.currentTick = 0;
        this.nextActionIdx = 0;
    }
    
    /**
     * Get the current simulation tick
     */
    public int now() {
        return currentTick;
    }
    
    /**
     * Execute one simulation step
     */
    public void step() {
        // Execute all timeline actions scheduled for current tick
        executeTimelineActions();
        
        // Advance the simulation
        raftModel.step();
        
        // Advance tick
        currentTick++;
    }
    
    /**
     * Execute multiple simulation steps
     */
    public void step(int n) {
        for (int i = 0; i < n; i++) {
            step();
        }
    }
    
    /**
     * Play simulation to the end of timeline, then settle
     */
    public void playToEnd() {
        if (scenario == null || scenario.timeline == null) {
            System.out.println("No timeline to play");
            return;
        }
        
        // Find the last action time (make a copy to avoid concurrent modification)
        List<ScenarioLoader.TimedAction> timeline = new ArrayList<>(scenario.timeline);
        int lastActionTime = timeline.stream()
            .mapToInt(action -> action.at)
            .max()
            .orElse(0);
        
        // Step to the last action time
        while (currentTick <= lastActionTime) {
            step();
        }
        
        int endTick = currentTick;
        
        // Compute target time for assertions
        int target = Math.max(currentTick, maxAssertionAfter());
        // Add settle buffer for heartbeats/commits to propagate
        target += 5;
        
        // Step until target time
        while (currentTick < target) {
            step();
        }
        
        System.out.println("Timeline complete at t=" + endTick + ". Stepped to t=" + target + " for assertions.");
    }
    
    /**
     * Get the maximum 'after' time across all assertions
     */
    private int maxAssertionAfter() {
        if (scenario == null || scenario.assertions == null) {
            return 0;
        }
        
        return scenario.assertions.stream()
            .filter(assertion -> assertion.args != null && assertion.args.containsKey("after"))
            .mapToInt(assertion -> (Integer) assertion.args.get("after"))
            .max()
            .orElse(0);
    }

    /**
     * Execute timeline actions scheduled for the current tick
     */
    private void executeTimelineActions() {
        if (scenario == null || scenario.timeline == null) {
            return;
        }
        
        // Make a copy to avoid concurrent modification
        List<ScenarioLoader.TimedAction> timeline = new ArrayList<>(scenario.timeline);
        for (ScenarioLoader.TimedAction timedAction : timeline) {
            if (timedAction.at == currentTick && timedAction.actions != null) {
                for (ScenarioLoader.ActionSpec action : timedAction.actions) {
                    executeAction(action);
                }
            }
        }
    }
    
    /**
     * Execute a single action
     */
    private void executeAction(ScenarioLoader.ActionSpec action) {
        switch (action.kind.toLowerCase()) {
            case "clientwrite" -> {
                String command = (String) action.args.get("command");
                if (command != null) {
                    raftModel.clientWrite(command);
                }
            }
            case "crash" -> {
                String nodeId = (String) action.args.get("node");
                if (nodeId != null) {
                    raftModel.crash(nodeId);
                }
            }
            case "recover" -> {
                String nodeId = (String) action.args.get("node");
                if (nodeId != null) {
                    raftModel.recover(nodeId);
                }
            }
            case "partition" -> {
                @SuppressWarnings("unchecked")
                List<List<String>> groups = (List<List<String>>) action.args.get("groups");
                if (groups != null && groups.size() >= 2) {
                    raftModel.partition(groups.get(0), groups.get(1));
                }
            }
            case "partition_clear" -> {
                raftModel.clearPartitions();
            }
            case "delay" -> {
                String from = (String) action.args.get("from");
                String to = (String) action.args.get("to");
                String type = (String) action.args.getOrDefault("type", "*");
                Integer steps = (Integer) action.args.get("steps");
                if (from != null && to != null && steps != null) {
                    NetworkRule.Match match = new NetworkRule.Match(from, to, type, null, null, false);
                    NetworkRule rule = new NetworkRule(match, NetworkRule.Action.DELAY, steps, 0.0);
                    raftModel.cluster().getMessageBus().addRule(rule);
                }
            }
            case "drop" -> {
                String from = (String) action.args.get("from");
                String to = (String) action.args.get("to");
                String type = (String) action.args.getOrDefault("type", "*");
                Double pct = (Double) action.args.getOrDefault("pct", 1.0);
                if (from != null && to != null) {
                    NetworkRule.Action actionType = pct < 1.0 ? NetworkRule.Action.DROP_PCT : NetworkRule.Action.DROP;
                    NetworkRule.Match match = new NetworkRule.Match(from, to, type, null, null, false);
                    NetworkRule rule = new NetworkRule(match, actionType, 0, pct);
                    raftModel.cluster().getMessageBus().addRule(rule);
                }
            }
            case "run" -> {
                Integer ticks = (Integer) action.args.get("ticks");
                if (ticks != null) {
                    step(ticks);
                }
            }
            default -> {
                System.out.println("Unknown action: " + action.kind);
            }
        }
    }
    
    /**
     * Evaluate all assertions in the scenario
     */
    public void evaluateAssertions() {
        if (scenario == null || scenario.assertions == null) {
            System.out.println("No assertions to evaluate");
            return;
        }
        
        System.out.println("Evaluating assertions...");
        
        // Make a copy to avoid concurrent modification
        List<ScenarioLoader.Assertion> assertions = new ArrayList<>(scenario.assertions);
        for (int i = 0; i < assertions.size(); i++) {
            ScenarioLoader.Assertion assertion = assertions.get(i);
            evaluateAssertion(i + 1, assertion);
        }
    }
    
    /**
     * Evaluate a single assertion
     */
    private void evaluateAssertion(int index, ScenarioLoader.Assertion assertion) {
        switch (assertion.type.toLowerCase()) {
            case "leader_exists" -> evaluateLeaderExists(index, assertion);
            case "log_consistency" -> evaluateLogConsistency(index, assertion);
            default -> {
                System.out.println("[" + index + "] UNKNOWN assertion type: " + assertion.type);
            }
        }
    }
    
    /**
     * Evaluate leader_exists assertion
     */
    private void evaluateLeaderExists(int index, ScenarioLoader.Assertion assertion) {
        Integer after = (Integer) assertion.args.get("after");
        if (after == null) {
            System.out.println("[" + index + "] FAIL leader_exists (missing 'after' parameter)");
            return;
        }
        
        // Step to the required time if needed
        if (currentTick < after) {
            step(after - currentTick);
        }
        
        var leader = raftModel.currentLeaderId();
        if (leader.isPresent()) {
            System.out.println("[" + index + "] PASS leader_exists after=" + after + " leader=" + leader.get() + " at t=" + currentTick);
        } else {
            System.out.println("[" + index + "] FAIL leader_exists after=" + after + " (no leader at t=" + currentTick + ")");
        }
    }
    
    /**
     * Evaluate log_consistency assertion
     */
    private void evaluateLogConsistency(int index, ScenarioLoader.Assertion assertion) {
        Integer after = (Integer) assertion.args.get("after");
        if (after == null) {
            System.out.println("[" + index + "] FAIL log_consistency (missing 'after' parameter)");
            return;
        }
        
        // Step to the required time if needed
        if (currentTick < after) {
            step(after - currentTick);
        }
        
        if (raftModel.logsArePrefixConsistent()) {
            System.out.println("[" + index + "] PASS log_consistency after=" + after + " at t=" + currentTick);
        } else {
            System.out.println("[" + index + "] FAIL log_consistency after=" + after + " at t=" + currentTick);
            // TODO: Add brief diff showing node ids and lengths
        }
    }
}
