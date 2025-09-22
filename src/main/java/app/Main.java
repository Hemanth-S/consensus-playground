package app;

import io.ScenarioLoader;
import raft.RaftModel;
import sim.Determinism;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;

/**
 * Main entry point for the consensus playground simulation.
 * Provides a command-line interface for loading scenarios and stepping through simulations.
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("Consensus Playground - Distributed Systems Simulation");
        System.out.println("=====================================================");
        
        if (args.length > 0) {
            // Load scenario from command line argument
            loadAndPlayScenario(Path.of(args[0]));
        } else {
            // Start REPL loop
            runInteractiveMode();
        }
    }
    
    private static void runInteractiveMode() {
        Scanner scanner = new Scanner(System.in);
        Commands commands = new Commands(scanner);
        
        // Print help banner
        System.out.println("\nWelcome to the Consensus Playground!");
        System.out.println("Type 'help' for available commands or 'quit' to exit.");
        System.out.println("Example: init raft --nodes 5 --seed 42");
        
        while (true) {
            System.out.print("\n> ");
            String input;
            try {
                input = scanner.nextLine().trim();
            } catch (java.util.NoSuchElementException e) {
                // End of input stream
                System.out.println("\nGoodbye!");
                return;
            }
            
            if (input.isEmpty()) continue;
            
            // Handle interactive load command specially
            if (input.equals("load")) {
                handleInteractiveLoad(scanner, commands);
                continue;
            }
            
            // Execute command and check if we should continue
            if (!commands.execute(input)) {
                return; // Exit requested
            }
        }
    }
    
    private static void handleInteractiveLoad(Scanner scanner, Commands commands) {
        // Get available scenarios
        List<String> availableScenarios = getAvailableScenarios();
        
        if (availableScenarios.isEmpty()) {
            System.out.println("No scenarios found in scenarios/ directory.");
            return;
        }
        
        System.out.println("\nAvailable scenarios:");
        for (int i = 0; i < availableScenarios.size(); i++) {
            System.out.println("  " + (i + 1) + ". " + availableScenarios.get(i));
        }
        
        while (true) {
            System.out.print("\nEnter scenario number, name, or path (or 'cancel' to abort): ");
            String input = scanner.nextLine().trim();
            
            if (input.isEmpty()) continue;
            if (input.equalsIgnoreCase("cancel")) {
                System.out.println("Load cancelled.");
                return;
            }
            
            Path scenarioPath = null;
            
            // Try to parse as number
            try {
                int index = Integer.parseInt(input);
                if (index >= 1 && index <= availableScenarios.size()) {
                    scenarioPath = Paths.get("scenarios", availableScenarios.get(index - 1));
                } else {
                    System.out.println("Invalid scenario number. Please enter 1-" + availableScenarios.size());
                    continue;
                }
            } catch (NumberFormatException e) {
                // Not a number, try as name or path
                if (input.endsWith(".yml") || input.endsWith(".yaml")) {
                    // Full filename provided
                    scenarioPath = Paths.get("scenarios", input);
                } else {
                    // Try to find matching scenario name
                    String matchingScenario = findMatchingScenario(input, availableScenarios);
                    if (matchingScenario != null) {
                        scenarioPath = Paths.get("scenarios", matchingScenario);
                    } else {
                        // Try as direct path
                        scenarioPath = Paths.get(input);
                    }
                }
            }
            
            // Try to load the scenario
            if (scenarioPath != null) {
                try {
                    // Use the existing loadAndPlayScenario logic but delegate to Commands
                    String loadCommand = "load " + scenarioPath.toString();
                    commands.execute(loadCommand);
                    return;
                } catch (Exception e) {
                    System.out.println("Failed to load scenario: " + e.getMessage());
                    System.out.println("Please try again or enter 'cancel' to abort.");
                }
            } else {
                System.out.println("Scenario not found. Please try again or enter 'cancel' to abort.");
            }
        }
    }
    
    private static List<String> getAvailableScenarios() {
        List<String> scenarios = new ArrayList<>();
        try {
            Path scenariosDir = Paths.get("scenarios");
            if (scenariosDir.toFile().exists() && scenariosDir.toFile().isDirectory()) {
                java.nio.file.Files.list(scenariosDir)
                    .filter(path -> path.toString().endsWith(".yml") || path.toString().endsWith(".yaml"))
                    .map(path -> path.getFileName().toString())
                    .sorted()
                    .forEach(scenarios::add);
            }
        } catch (Exception e) {
            // Ignore errors, return empty list
        }
        return scenarios;
    }
    
    private static String findMatchingScenario(String input, List<String> availableScenarios) {
        String lowerInput = input.toLowerCase();
        for (String scenario : availableScenarios) {
            String lowerScenario = scenario.toLowerCase();
            if (lowerScenario.contains(lowerInput) || lowerScenario.startsWith(lowerInput)) {
                return scenario;
            }
        }
        return null;
    }
    
    private static void loadAndPlayScenario(Path scenarioPath) {
        try {
            ScenarioLoader.Scenario scenario = ScenarioLoader.load(scenarioPath);
            
            // Create RaftModel from scenario
            if (!"raft".equals(scenario.model)) {
                throw new IllegalArgumentException("Only 'raft' model is currently supported");
            }
            
            if (scenario.cluster == null || scenario.cluster.nodes == null) {
                throw new IllegalArgumentException("Scenario must specify cluster.nodes");
            }
            
            Long seed = scenario.seed != null ? scenario.seed : System.currentTimeMillis();
            RaftModel model = new RaftModel(scenario.cluster.nodes, seed);
            
            // Apply initial state and network rules (pure, no time stepping)
            ScenarioLoader.applyInitial(scenario, model);
            ScenarioLoader.applyNetworkRules(scenario, model.cluster());
            
            System.out.println("Loaded scenario: " + scenarioPath.getFileName());
            System.out.println("Cluster initialized with " + model.getNodeIds().size() + " nodes, seed=" + seed);
            if (scenario.network != null && scenario.network.rules != null) {
                System.out.println("Network rules: " + scenario.network.rules.size());
            }
            if (scenario.timeline != null) {
                System.out.println("Timeline actions: " + scenario.timeline.size());
            }
            if (scenario.assertions != null) {
                System.out.println("Assertions: " + scenario.assertions.size());
            }
            
            System.out.println("\nScenario loaded successfully!");
            System.out.println("Use 'play' command to execute timeline and evaluate assertions.");
            
        } catch (Exception e) {
            System.err.println("Failed to load scenario: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

