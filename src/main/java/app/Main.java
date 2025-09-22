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
    private static final Scanner scanner = new Scanner(System.in);
    private static RaftModel currentModel;
    private static Determinism determinism;

    public static void main(String[] args) {
        System.out.println("Consensus Playground - Distributed Systems Simulation");
        System.out.println("=====================================================");
        
        // Determinism is now handled by RaftModel
        
        if (args.length > 0) {
            loadScenario(Path.of(args[0]));
        }
        
        runInteractiveMode();
    }
    
    private static void runInteractiveMode() {
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
            
            String[] parts = input.split("\\s+");
            String command = parts[0].toLowerCase();
            
            try {
                switch (command) {
                    case "load" -> {
                        if (parts.length < 2) {
                            System.out.println("Usage: load <scenario-file>");
                        } else {
                            loadScenario(Path.of(parts[1]));
                        }
                    }
                    case "step" -> stepSimulation();
                    case "status" -> showStatus();
                    case "help" -> showHelp();
                    case "quit", "exit" -> {
                        System.out.println("Goodbye!");
                        return;
                    }
                    default -> System.out.println("Unknown command. Type 'help' for available commands.");
                }
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
    }
    
    private static void loadScenario(Path scenarioPath) {
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
            currentModel = new RaftModel(scenario.cluster.nodes, seed);
            
            // Apply scenario to model
            ScenarioLoader.apply(scenario, currentModel);
            
            System.out.println("Loaded scenario: " + scenarioPath.getFileName());
            System.out.println("Cluster initialized with " + currentModel.getNodeIds().size() + " nodes");
        } catch (Exception e) {
            System.err.println("Failed to load scenario: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void stepSimulation() {
        if (currentModel == null) {
            System.out.println("No scenario loaded. Use 'load <file>' first.");
            return;
        }
        
        currentModel.step();
        System.out.println("Simulation stepped. Current time: " + currentModel.getCurrentTime());
    }
    
    private static void showStatus() {
        if (currentModel == null) {
            System.out.println("No scenario loaded.");
            return;
        }
        
        System.out.println("Cluster Status:");
        System.out.println("- Current time: " + currentModel.getCurrentTime());
        System.out.println("- Node count: " + currentModel.getNodeIds().size());
        System.out.println("- Pending messages: " + currentModel.cluster().getPendingMessageCount());
        
        // Show current leader if any
        var leader = currentModel.currentLeaderId();
        if (leader.isPresent()) {
            System.out.println("- Current leader: " + leader.get());
        } else {
            System.out.println("- Current leader: None");
        }
    }
    
    private static void showHelp() {
        System.out.println("Available commands:");
        System.out.println("  load <file>    - Load a scenario file");
        System.out.println("  step           - Step the simulation forward");
        System.out.println("  status         - Show current cluster status");
        System.out.println("  help           - Show this help message");
        System.out.println("  quit/exit      - Exit the program");
    }
}

