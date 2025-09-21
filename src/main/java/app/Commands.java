package app;

import java.util.List;
import java.util.Map;

/**
 * Command-line interface commands for the consensus playground.
 * Provides structured command definitions and parsing utilities.
 */
public class Commands {
    
    /**
     * Available command types
     */
    public enum CommandType {
        LOAD, STEP, STATUS, HELP, QUIT, UNKNOWN
    }
    
    /**
     * Represents a parsed command with its type and arguments
     */
    public static class Command {
        private final CommandType type;
        private final List<String> args;
        private final Map<String, String> options;
        
        public Command(CommandType type, List<String> args, Map<String, String> options) {
            this.type = type;
            this.args = args;
            this.options = options;
        }
        
        public CommandType getType() { return type; }
        public List<String> getArgs() { return args; }
        public Map<String, String> getOptions() { return options; }
        
        public String getArg(int index) {
            return index < args.size() ? args.get(index) : null;
        }
        
        public String getOption(String key) {
            return options.get(key);
        }
    }
    
    /**
     * Parse a command string into a Command object
     */
    public static Command parse(String input) {
        if (input == null || input.trim().isEmpty()) {
            return new Command(CommandType.UNKNOWN, List.of(), Map.of());
        }
        
        String[] parts = input.trim().split("\\s+");
        String command = parts[0].toLowerCase();
        
        CommandType type = switch (command) {
            case "load" -> CommandType.LOAD;
            case "step" -> CommandType.STEP;
            case "status" -> CommandType.STATUS;
            case "help" -> CommandType.HELP;
            case "quit", "exit" -> CommandType.QUIT;
            default -> CommandType.UNKNOWN;
        };
        
        List<String> args = List.of(parts).subList(1, parts.length);
        Map<String, String> options = Map.of(); // TODO: Implement option parsing if needed
        
        return new Command(type, args, options);
    }
    
    /**
     * Get help text for all available commands
     */
    public static String getHelpText() {
        return """
            Available commands:
              load <file>    - Load a scenario file
              step           - Step the simulation forward
              status         - Show current cluster status
              help           - Show this help message
              quit/exit      - Exit the program
            """;
    }
}

