# Consensus Playground

A Java-based simulation framework for studying distributed consensus algorithms like Raft and Paxos.

## Purpose

This project provides a deterministic simulation environment for testing and understanding distributed consensus algorithms, with support for network failures, node crashes, and various failure scenarios. It features an interactive command-line interface for step-by-step simulation control and YAML-driven scenario definitions.

## Getting Started

### Prerequisites

- Java 24 or higher
- Gradle 8.5 or higher

### Building and Running

```bash
# Build the project
./gradlew build

# Run the application
./gradlew run

# Run with a specific scenario file
./gradlew run --args="scenarios/raft_leader_crash.yml"
```

### Interactive Mode

When you run the application, you'll enter an interactive command-line interface:

```
Consensus Playground - Distributed Systems Simulation
=====================================================

Welcome to the Consensus Playground!
Type 'help' for available commands or 'quit' to exit.
Example: init raft --nodes 5 --seed 42

> help
Available commands:
  load [path]                    - Load scenario from YAML file (interactive if no path)
  init raft --nodes N --seed S   - Initialize Raft cluster
  step [N]                       - Step simulation N times (default: 1)
  run [N]                        - Run simulation for N steps (default: 50)
  play                           - Play scenario to completion and evaluate assertions
  write "<command>"              - Send client command
  dump [nodes|logs|net|state]    - Dump cluster state
  crash <node>                   - Crash a specific node
  recover <node>                 - Recover a crashed node
  partition add <A> <B>          - Create network partition
  partition clear                - Clear all partitions
  delay from=<A> to=<B> type=<T> steps=<k> - Add network delay
  drop from=<A> to=<B> [type=<T>] [pct=...] - Add packet drop rule
  speed <ms_per_tick>            - Set simulation speed
  help                           - Show this help message
  quit/exit                      - Exit the program

> load
Available scenarios:
  1. raft_leader_crash.yml
  2. raft_split_vote.yml

Enter scenario number, name, or path (or 'cancel' to abort): 1
Loaded scenario: raft_leader_crash.yml
Cluster initialized with 5 nodes, seed=12345
Network rules: 0
Timeline actions: 2
Assertions: 2

> play
Playing scenario to completion...
Timeline complete at t=21. Stepped to t=26 for assertions.
[1] PASS leader_exists after=20 leader=n3
[2] PASS log_consistency after=25
```

## Loading Scenario Files

Scenario files define the initial state and behavior of your distributed system simulation. They are written in YAML format and specify:

- Number of nodes in the cluster
- Network topology and rules (delays, drop rates)
- Timeline of events (crashes, recoveries, partitions, client writes)
- Assertions to verify system behavior

### Interactive Scenario Loading

The `load` command supports both interactive and direct loading:

```bash
# Interactive mode - shows available scenarios
> load
Available scenarios:
  1. raft_leader_crash.yml
  2. raft_split_vote.yml

Enter scenario number, name, or path (or 'cancel' to abort): 1

# Direct mode - specify path directly
> load scenarios/raft_leader_crash.yml

# Partial name matching
> load leader  # matches raft_leader_crash.yml
```

### Example Scenario Usage

```bash
# Load a scenario and play it to completion
> load scenarios/raft_leader_crash.yml
> play

# Or step through manually
> load scenarios/raft_leader_crash.yml
> step 10
> dump state
> play
```

### Scenario Schema

For detailed information about the YAML schema and available options, see [ScenarioSchema.md](src/main/java/io/ScenarioSchema.md).

## Available Scenarios

The `scenarios/` directory contains example scenario files:

- **raft_leader_crash.yml** - Tests leader election and recovery when the leader crashes
  - 5 nodes with client write at t=1, leader crash at t=3
  - Verifies leader election and log consistency
- **raft_split_vote.yml** - Tests split vote scenarios with network delays
  - 3 nodes with network partition to simulate split votes

## Architecture

### Core Components

- **Cluster** - Manages the overall simulation state and timing
- **MessageBus** - Handles message passing between nodes with network rules
- **Determinism** - Provides reproducible random number generation
- **NetworkRule** - Defines network behavior (delays, drop rates)
- **SimulationController** - Manages timeline execution and assertion evaluation
- **RaftModel** - High-level abstraction for Raft consensus simulation

### Consensus Algorithms

- **Raft** - Complete implementation with leader election and log replication
  - Stabilized leadership with optimized heartbeat/election timeouts
  - Client write queuing when no leader exists
  - Log consistency verification
- **Paxos** - Placeholder for future implementation

### Key Features

- **Deterministic Simulation** - Reproducible results using seeded random number generation
- **Network Modeling** - Configurable message delays and drop rates
- **Failure Injection** - Support for node crashes, recoveries, and network partitions
- **Interactive CLI** - Step-by-step simulation control with comprehensive commands
- **YAML Configuration** - Easy-to-write scenario definitions with timeline and assertions
- **Timeline Execution** - Automated scenario playback with assertion evaluation
- **Client Write Queuing** - Automatic queuing and flushing of client commands
- **Tick-Prefixed Output** - All simulation output includes current simulation time

## Testing

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests RaftSafetyTests

# Run tests for specific package
./gradlew test --tests "raft.*"
./gradlew test --tests "sim.*"
./gradlew test --tests "app.*"
```

The test suite includes:
- **Raft Safety Tests** - Verifies Raft safety properties and leader election
- **Raft Model Tests** - Tests the high-level RaftModel abstraction
- **Simulation Layer Tests** - Tests core simulation components
- **Scenario Loader Tests** - Tests YAML scenario loading and parsing
- **Client Write Tests** - Tests client command queuing and processing

## Development

### Project Structure

```
consensus-playground/
├── src/main/java/
│   ├── app/           # Main application and CLI (Main.java, Commands.java, SimulationController.java)
│   ├── io/            # Scenario loading and schema (ScenarioLoader.java, ScenarioSchema.md)
│   ├── sim/           # Simulation framework (Message, MessageBus, Cluster, Determinism, NetworkRule)
│   ├── raft/          # Raft consensus implementation (RaftNode, RaftModel, RaftRpc, RaftLogEntry, RaftRole)
│   └── paxos/         # Paxos consensus (placeholder)
├── src/test/java/     # Comprehensive test suite
├── scenarios/         # Example scenario files (YAML format)
├── build.gradle       # Gradle build configuration
└── README.md
```

### Adding New Scenarios

1. Create a new YAML file in the `scenarios/` directory
2. Follow the schema defined in `ScenarioSchema.md`
3. Test your scenario by loading it in the interactive mode:
   ```bash
   > load  # Interactive mode will show your new scenario
   > play  # Execute timeline and evaluate assertions
   ```

### Extending the Framework

- **New Consensus Algorithms** - Implement in a new package following the existing patterns
  - Create a new `XxxModel` class similar to `RaftModel`
  - Implement the `Cluster.Node` interface for your nodes
  - Add support in `ScenarioLoader` for your model type
- **New Event Types** - Add support in `SimulationController.executeAction()`
- **New Network Rules** - Extend `NetworkRule` class and `MessageBus` handling
- **New Message Types** - Add to the appropriate RPC classes
- **New Assertions** - Add evaluation logic in `SimulationController.evaluateAssertions()`

### Recent Improvements

- **Stabilized Raft Leadership** - Optimized heartbeat (2-4 ticks) vs election timeout (9-15 ticks) ratios
- **Interactive Load Command** - Smart scenario discovery with numbered selection and partial name matching
- **Timeline Execution** - Automated scenario playback with assertion evaluation
- **Client Write Queuing** - Automatic queuing and flushing of client commands when no leader exists
- **Tick-Prefixed Output** - All simulation output includes current simulation time for better debugging

## License

This project is open source and available under the MIT License.
