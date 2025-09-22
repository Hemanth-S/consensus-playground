package app;

import io.ScenarioLoader;
import raft.RaftModel;
import sim.Determinism;

import java.nio.file.Path;
import java.util.Scanner;

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
            
            // Execute command and check if we should continue
            if (!commands.execute(input)) {
                return; // Exit requested
            }
        }
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

