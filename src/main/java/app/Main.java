package app;

import io.ScenarioLoader;
import sim.Cluster;
import sim.Determinism;

import java.nio.file.Path;
import java.util.Scanner;

/**
 * Main entry point for the consensus playground simulation.
 * Provides a command-line interface for loading scenarios and stepping through simulations.
 */
public class Main {
    private static final Scanner scanner = new Scanner(System.in);
    private static Cluster currentCluster;
    private static Determinism determinism;

    public static void main(String[] args) {
        System.out.println("Consensus Playground - Distributed Systems Simulation");
        System.out.println("=====================================================");
        
        determinism = new Determinism();
        
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
            ScenarioLoader loader = new ScenarioLoader();
            currentCluster = loader.loadScenario(scenarioPath);
            System.out.println("Loaded scenario: " + scenarioPath.getFileName());
            System.out.println("Cluster initialized with " + currentCluster.getNodeCount() + " nodes");
        } catch (Exception e) {
            System.err.println("Failed to load scenario: " + e.getMessage());
        }
    }
    
    private static void stepSimulation() {
        if (currentCluster == null) {
            System.out.println("No scenario loaded. Use 'load <file>' first.");
            return;
        }
        
        currentCluster.step();
        System.out.println("Simulation stepped. Current time: " + currentCluster.getCurrentTime());
    }
    
    private static void showStatus() {
        if (currentCluster == null) {
            System.out.println("No scenario loaded.");
            return;
        }
        
        System.out.println("Cluster Status:");
        System.out.println("- Current time: " + currentCluster.getCurrentTime());
        System.out.println("- Node count: " + currentCluster.getNodeCount());
        System.out.println("- Pending messages: " + currentCluster.getPendingMessageCount());
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

