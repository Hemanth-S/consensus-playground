package app;

import io.ScenarioLoader;
import raft.RaftModel;
import sim.NetworkRule;
import sim.Determinism;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Command parser and executor for the consensus playground text UI.
 * Handles all user commands and delegates to RaftModel.
 */
public class Commands {
    private RaftModel model;
    private int speedMs = 1; // milliseconds per tick (stored but not used yet)
    private final Scanner scanner;
    
    public Commands(Scanner scanner) {
        this.scanner = scanner;
    }
    
    /**
     * Parse and execute a command string
     */
    public boolean execute(String input) {
        if (input == null || input.trim().isEmpty()) {
            return true; // Continue loop
        }
        
        String[] parts = input.trim().split("\\s+");
        String command = parts[0].toLowerCase();
        
        try {
            switch (command) {
                case "load" -> handleLoad(parts);
                case "init" -> handleInit(parts);
                case "step" -> handleStep(parts);
                case "run" -> handleRun(parts);
                case "write" -> handleWrite(parts);
                case "dump" -> handleDump(parts);
                case "crash" -> handleCrash(parts);
                case "recover" -> handleRecover(parts);
                case "partition" -> handlePartition(parts);
                case "delay" -> handleDelay(parts);
                case "drop" -> handleDrop(parts);
                case "speed" -> handleSpeed(parts);
                case "help" -> showHelp();
                case "quit", "exit" -> {
                    System.out.println("Goodbye!");
                    return false; // Exit loop
                }
                default -> {
                    System.out.println("Unknown command: " + command);
                    System.out.println("Type 'help' for available commands.");
                }
            }
        } catch (Exception e) {
            System.err.println("Error executing command: " + e.getMessage());
            e.printStackTrace();
        }
        
        return true; // Continue loop
    }
    
    private void handleLoad(String[] parts) {
        if (parts.length < 2) {
            System.out.println("Usage: load <path>");
            return;
        }
        
        try {
            Path scenarioPath = Paths.get(parts[1]);
            ScenarioLoader.Scenario scenario = ScenarioLoader.load(scenarioPath);
            
            if (!"raft".equals(scenario.model)) {
                throw new IllegalArgumentException("Only 'raft' model is currently supported");
            }
            
            if (scenario.cluster == null || scenario.cluster.nodes == null) {
                throw new IllegalArgumentException("Scenario must specify cluster.nodes");
            }
            
            Long seed = scenario.seed != null ? scenario.seed : System.currentTimeMillis();
            model = new RaftModel(scenario.cluster.nodes, seed);
            ScenarioLoader.apply(scenario, model);
            
            System.out.println("Loaded scenario: " + scenarioPath.getFileName());
            System.out.println("Cluster initialized with " + model.getNodeIds().size() + " nodes");
            
            // TODO: Execute timeline and print assertions
            System.out.println("TODO: Execute timeline and validate assertions");
            
        } catch (Exception e) {
            System.err.println("Failed to load scenario: " + e.getMessage());
        }
    }
    
    private void handleInit(String[] parts) {
        if (parts.length < 2 || !"raft".equals(parts[1])) {
            System.out.println("Usage: init raft --nodes <N> --seed <seed>");
            return;
        }
        
        int nodeCount = 3; // default
        long seed = System.currentTimeMillis();
        
        // Parse arguments
        for (int i = 2; i < parts.length; i++) {
            if ("--nodes".equals(parts[i]) && i + 1 < parts.length) {
                nodeCount = Integer.parseInt(parts[++i]);
            } else if ("--seed".equals(parts[i]) && i + 1 < parts.length) {
                seed = Long.parseLong(parts[++i]);
            }
        }
        
        // Create node IDs
        List<String> nodeIds = new ArrayList<>();
        for (int i = 0; i < nodeCount; i++) {
            nodeIds.add("n" + (i + 1));
        }
        
        model = new RaftModel(nodeIds, seed);
        System.out.println("Initialized Raft cluster with " + nodeCount + " nodes, seed=" + seed);
    }
    
    private void handleStep(String[] parts) {
        if (model == null) {
            System.out.println("No model loaded. Use 'init' or 'load' first.");
            return;
        }
        
        int steps = 1;
        if (parts.length > 1) {
            steps = Integer.parseInt(parts[1]);
        }
        
        for (int i = 0; i < steps; i++) {
            model.step();
        }
        
        System.out.println("Stepped simulation " + steps + " time(s). Current time: " + model.getCurrentTime());
    }
    
    private void handleRun(String[] parts) {
        if (model == null) {
            System.out.println("No model loaded. Use 'init' or 'load' first.");
            return;
        }
        
        int steps = 10; // default
        if (parts.length > 1) {
            steps = Integer.parseInt(parts[1]);
        }
        
        System.out.println("Running simulation for " + steps + " steps...");
        for (int i = 0; i < steps; i++) {
            model.step();
            if (i % 5 == 0) { // Print status every 5 steps
                System.out.println("  Step " + (i + 1) + ": time=" + model.getCurrentTime());
            }
        }
        System.out.println("Run complete. Final time: " + model.getCurrentTime());
    }
    
    private void handleWrite(String[] parts) {
        if (model == null) {
            System.out.println("No model loaded. Use 'init' or 'load' first.");
            return;
        }
        
        if (parts.length < 2) {
            System.out.println("Usage: write \"<command>\"");
            return;
        }
        
        // Join all parts after "write" and remove quotes
        String command = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
        command = command.replaceAll("^\"|\"$", ""); // Remove surrounding quotes
        
        model.clientWrite(command);
        System.out.println("Client write: " + command);
    }
    
    private void handleDump(String[] parts) {
        if (model == null) {
            System.out.println("No model loaded. Use 'init' or 'load' first.");
            return;
        }
        
        String type = parts.length > 1 ? parts[1].toLowerCase() : "state";
        
        switch (type) {
            case "nodes" -> {
                System.out.println("Nodes:");
                for (String nodeId : model.getNodeIds()) {
                    var node = model.getNode(nodeId);
                    System.out.println("  " + nodeId + ": " + (node.isUp() ? "UP" : "DOWN") + 
                                     " " + node.getRole() + " term=" + node.getCurrentTerm());
                }
            }
            case "logs" -> {
                System.out.println("Logs:");
                for (String nodeId : model.getNodeIds()) {
                    var node = model.getNode(nodeId);
                    var log = node.getLog();
                    System.out.println("  " + nodeId + ": " + log.size() + " entries");
                    for (int i = 0; i < Math.min(log.size(), 5); i++) { // Show first 5 entries
                        System.out.println("    [" + (i + 1) + "] " + log.get(i));
                    }
                    if (log.size() > 5) {
                        System.out.println("    ... and " + (log.size() - 5) + " more");
                    }
                }
            }
            case "net" -> {
                System.out.println("Network Rules:");
                var rules = model.cluster().getMessageBus().getRules();
                if (rules.isEmpty()) {
                    System.out.println("  No network rules");
                } else {
                    for (int i = 0; i < rules.size(); i++) {
                        System.out.println("  [" + i + "] " + rules.get(i));
                    }
                }
            }
            case "state" -> {
                System.out.println(model.dump());
            }
            default -> {
                System.out.println("Usage: dump [nodes|logs|net|state]");
            }
        }
    }
    
    private void handleCrash(String[] parts) {
        if (model == null) {
            System.out.println("No model loaded. Use 'init' or 'load' first.");
            return;
        }
        
        if (parts.length < 2) {
            System.out.println("Usage: crash <node>");
            return;
        }
        
        String nodeId = parts[1];
        model.crash(nodeId);
        System.out.println("Crashed node: " + nodeId);
    }
    
    private void handleRecover(String[] parts) {
        if (model == null) {
            System.out.println("No model loaded. Use 'init' or 'load' first.");
            return;
        }
        
        if (parts.length < 2) {
            System.out.println("Usage: recover <node>");
            return;
        }
        
        String nodeId = parts[1];
        model.recover(nodeId);
        System.out.println("Recovered node: " + nodeId);
    }
    
    private void handlePartition(String[] parts) {
        if (model == null) {
            System.out.println("No model loaded. Use 'init' or 'load' first.");
            return;
        }
        
        if (parts.length < 2) {
            System.out.println("Usage: partition add <A> <B> | partition clear");
            return;
        }
        
        String subcommand = parts[1].toLowerCase();
        
        if ("clear".equals(subcommand)) {
            model.clearPartitions();
            System.out.println("Cleared all partitions");
        } else if ("add".equals(subcommand) && parts.length >= 4) {
            // Parse node groups (simplified - just two nodes for now)
            String nodeA = parts[2];
            String nodeB = parts[3];
            model.partition(List.of(nodeA), List.of(nodeB));
            System.out.println("Added partition between " + nodeA + " and " + nodeB);
        } else {
            System.out.println("Usage: partition add <A> <B> | partition clear");
        }
    }
    
    private void handleDelay(String[] parts) {
        if (model == null) {
            System.out.println("No model loaded. Use 'init' or 'load' first.");
            return;
        }
        
        // Parse key=value arguments
        Map<String, String> args = parseKeyValueArgs(parts, 1);
        
        String from = args.get("from");
        String to = args.get("to");
        String type = args.getOrDefault("type", "*");
        int steps = Integer.parseInt(args.getOrDefault("steps", "1"));
        
        if (from == null || to == null) {
            System.out.println("Usage: delay from=<A> to=<B> type=<T> steps=<k>");
            return;
        }
        
        NetworkRule.Match match = new NetworkRule.Match(from, to, type, null, null, false);
        NetworkRule rule = new NetworkRule(match, NetworkRule.Action.DELAY, steps, 0.0);
        model.cluster().getMessageBus().addRule(rule);
        
        System.out.println("Added delay rule: " + from + " -> " + to + " (" + type + ") delay=" + steps);
    }
    
    private void handleDrop(String[] parts) {
        if (model == null) {
            System.out.println("No model loaded. Use 'init' or 'load' first.");
            return;
        }
        
        // Parse key=value arguments
        Map<String, String> args = parseKeyValueArgs(parts, 1);
        
        String from = args.get("from");
        String to = args.get("to");
        String type = args.getOrDefault("type", "*");
        double pct = Double.parseDouble(args.getOrDefault("pct", "1.0"));
        
        if (from == null || to == null) {
            System.out.println("Usage: drop from=<A> to=<B> [type=<T>] [pct=...]");
            return;
        }
        
        NetworkRule.Action action = pct < 1.0 ? NetworkRule.Action.DROP_PCT : NetworkRule.Action.DROP;
        NetworkRule.Match match = new NetworkRule.Match(from, to, type, null, null, false);
        NetworkRule rule = new NetworkRule(match, action, 0, pct);
        model.cluster().getMessageBus().addRule(rule);
        
        System.out.println("Added drop rule: " + from + " -> " + to + " (" + type + ") pct=" + pct);
    }
    
    private void handleSpeed(String[] parts) {
        if (parts.length < 2) {
            System.out.println("Usage: speed <ms_per_tick>");
            return;
        }
        
        speedMs = Integer.parseInt(parts[1]);
        System.out.println("Set speed to " + speedMs + "ms per tick (not implemented yet)");
    }
    
    private Map<String, String> parseKeyValueArgs(String[] parts, int startIndex) {
        Map<String, String> args = new HashMap<>();
        Pattern pattern = Pattern.compile("([^=]+)=(.+)");
        
        for (int i = startIndex; i < parts.length; i++) {
            Matcher matcher = pattern.matcher(parts[i]);
            if (matcher.matches()) {
                args.put(matcher.group(1), matcher.group(2));
            }
        }
        
        return args;
    }
    
    private void showHelp() {
        System.out.println("Available commands:");
        System.out.println("  load <path>                    - Load scenario from YAML file");
        System.out.println("  init raft --nodes N --seed S   - Initialize Raft cluster");
        System.out.println("  step [N]                       - Step simulation N times (default: 1)");
        System.out.println("  run [N]                        - Run simulation for N steps (default: 10)");
        System.out.println("  write \"<command>\"              - Send client command");
        System.out.println("  dump [nodes|logs|net|state]    - Dump cluster information");
        System.out.println("  crash <node>                   - Crash a node");
        System.out.println("  recover <node>                 - Recover a crashed node");
        System.out.println("  partition add <A> <B>          - Add partition between nodes");
        System.out.println("  partition clear                - Clear all partitions");
        System.out.println("  delay from=<A> to=<B> type=<T> steps=<k> - Add network delay");
        System.out.println("  drop from=<A> to=<B> [type=<T>] [pct=...] - Drop messages");
        System.out.println("  speed <ms_per_tick>            - Set simulation speed");
        System.out.println("  help                           - Show this help");
        System.out.println("  quit/exit                      - Exit program");
    }
}