package io;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import raft.RaftModel;
import sim.NetworkRule;
import sim.Determinism;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Loads and applies YAML-driven scenarios for consensus algorithm testing.
 * Supports both Raft and Paxos models (Paxos implementation pending).
 */
public class ScenarioLoader {
    private static final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    /**
     * Load a scenario from a YAML file
     */
    public static Scenario load(Path file) throws IOException {
        return yamlMapper.readValue(file.toFile(), Scenario.class);
    }

    /**
     * Apply a scenario to a Raft model
     */
    public static void apply(Scenario s, RaftModel model) {
        // Set seed if specified
        if (s.seed != null) {
            Determinism.setSeed(s.seed);
        }

        // Apply network rules
        if (s.network != null && s.network.rules != null) {
            for (RuleSpec ruleSpec : s.network.rules) {
                NetworkRule rule = createNetworkRule(ruleSpec);
                model.cluster().getMessageBus().addRule(rule);
            }
        }

        // Apply initial state
        if (s.initial != null) {
            applyInitialState(s.initial, model);
        }

        // Execute timeline actions
        if (s.timeline != null) {
            executeTimeline(s.timeline, model);
        }
    }

    private static NetworkRule createNetworkRule(RuleSpec ruleSpec) {
        NetworkRule.Match match = new NetworkRule.Match(
            ruleSpec.match.from,
            ruleSpec.match.to,
            ruleSpec.match.type,
            ruleSpec.match.between != null ? String.join(",", ruleSpec.match.between) : null,
            ruleSpec.match.between != null ? String.join(",", ruleSpec.match.between) : null,
            ruleSpec.match.bidirectional != null ? ruleSpec.match.bidirectional : false
        );

        NetworkRule.Action action = switch (ruleSpec.action.toLowerCase()) {
            case "pass" -> NetworkRule.Action.PASS;
            case "drop" -> NetworkRule.Action.DROP;
            case "delay" -> NetworkRule.Action.DELAY;
            case "drop_pct" -> NetworkRule.Action.DROP_PCT;
            default -> throw new IllegalArgumentException("Unknown action: " + ruleSpec.action);
        };

        return new NetworkRule(match, action, 
            ruleSpec.delaySteps != null ? ruleSpec.delaySteps : 0,
            ruleSpec.pct != null ? ruleSpec.pct : 0.0);
    }

    private static void applyInitialState(InitialSpec initial, RaftModel model) {
        // Apply node state if specified
        if (initial.nodeState != null) {
            for (Map.Entry<String, Map<String, Object>> entry : initial.nodeState.entrySet()) {
                String nodeId = entry.getKey();
                Map<String, Object> state = entry.getValue();
                
                // Handle node crashes
                if (state.containsKey("crashed") && (Boolean) state.get("crashed")) {
                    model.crash(nodeId);
                }
            }
        }

        // Apply log entries if specified
        if (initial.logs != null) {
            for (Map.Entry<String, List<LogEntrySpec>> entry : initial.logs.entrySet()) {
                String nodeId = entry.getKey();
                List<LogEntrySpec> logEntries = entry.getValue();
                
                // TODO: Apply log entries to node (requires RaftNode API extension)
                System.out.println("Log entries for " + nodeId + ": " + logEntries.size() + " entries");
            }
        }
    }

    private static void executeTimeline(List<TimedAction> timeline, RaftModel model) {
        int currentTime = 0;
        
        for (TimedAction timedAction : timeline) {
            // Step simulation to the target time
            while (currentTime < timedAction.at) {
                model.step();
                currentTime++;
            }
            
            // Execute actions at this time
            if (timedAction.actions != null) {
                for (ActionSpec action : timedAction.actions) {
                    executeAction(action, model);
                }
            }
        }
    }

    private static void executeAction(ActionSpec action, RaftModel model) {
        switch (action.kind.toLowerCase()) {
            case "crash" -> {
                String nodeId = (String) action.args.get("node");
                if (nodeId != null) {
                    model.crash(nodeId);
                }
            }
            case "recover" -> {
                String nodeId = (String) action.args.get("node");
                if (nodeId != null) {
                    model.recover(nodeId);
                }
            }
            case "clientwrite" -> {
                String command = (String) action.args.get("command");
                if (command != null) {
                    model.clientWrite(command);
                }
            }
            case "partition" -> {
                @SuppressWarnings("unchecked")
                List<String> groupA = (List<String>) action.args.get("groupA");
                @SuppressWarnings("unchecked")
                List<String> groupB = (List<String>) action.args.get("groupB");
                if (groupA != null && groupB != null) {
                    model.partition(groupA, groupB);
                }
            }
            case "clearpartitions" -> {
                model.clearPartitions();
            }
            default -> {
                System.out.println("Unknown action: " + action.kind);
            }
        }
    }

    // Data classes for YAML binding
    public static class Scenario {
        public String model; // "raft" or "paxos"
        public Long seed;
        public ClusterSpec cluster;
        public InitialSpec initial;
        public NetworkSpec network;
        public List<TimedAction> timeline;
        public List<Assertion> assertions;
    }

    public static class ClusterSpec {
        public List<String> nodes;
        public Integer tickMs;
    }

    public static class InitialSpec {
        public Map<String, Map<String, Object>> nodeState;
        public Map<String, List<LogEntrySpec>> logs;
    }

    public static class LogEntrySpec {
        public int term;
        public String cmd;
    }

    public static class NetworkSpec {
        public List<RuleSpec> rules;
    }

    public static class RuleSpec {
        public MatchSpec match;
        public String action;
        public Integer delaySteps;
        public Double pct;
    }

    public static class MatchSpec {
        public String from;
        public String to;
        public String type;
        public List<String> between;
        public Boolean bidirectional;
    }

    public static class TimedAction {
        public int at;
        public List<ActionSpec> actions;
    }

    public static class ActionSpec {
        public String kind;
        public Map<String, Object> args;
    }

    public static class Assertion {
        public String type;
        public Map<String, Object> args;
    }
}