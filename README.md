# Consensus Playground

A Java-based simulation framework for studying distributed consensus algorithms like Raft and Paxos.

## Purpose

This project provides a deterministic simulation environment for testing and understanding distributed consensus algorithms, with support for network failures, node crashes, and various failure scenarios.

## Getting Started

### Prerequisites

- Java 21 or higher
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

> help
Available commands:
  load <file>    - Load a scenario file
  step           - Step the simulation forward
  status         - Show current cluster status
  help           - Show this help message
  quit/exit      - Exit the program

> load scenarios/raft_leader_crash.yml
Loaded scenario: raft_leader_crash.yml
Cluster initialized with 3 nodes

> step
Simulation stepped. Current time: 1

> status
Cluster Status:
- Current time: 1
- Node count: 3
- Pending messages: 0
```

## Loading Scenario Files

Scenario files define the initial state and behavior of your distributed system simulation. They are written in YAML format and specify:

- Number of nodes in the cluster
- Network topology and rules (delays, drop rates)
- Initial events (crashes, recoveries, partitions)

### Example Scenario Usage

```bash
# Load a scenario and step through it
> load scenarios/raft_leader_crash.yml
> step
> step
> status
```

### Scenario Schema

For detailed information about the YAML schema and available options, see [ScenarioSchema.md](src/main/java/io/ScenarioSchema.md).

## Available Scenarios

The `scenarios/` directory contains example scenario files:

- **raft_leader_crash.yml** - Tests leader election and recovery when the leader crashes
- **raft_split_vote.yml** - Tests split vote scenarios with network delays

## Architecture

### Core Components

- **Cluster** - Manages the overall simulation state and timing
- **MessageBus** - Handles message passing between nodes with network rules
- **Determinism** - Provides reproducible random number generation
- **NetworkRule** - Defines network behavior (delays, drop rates)

### Consensus Algorithms

- **Raft** - Complete implementation with leader election and log replication
- **Paxos** - Placeholder for future implementation

### Key Features

- **Deterministic Simulation** - Reproducible results using seeded random number generation
- **Network Modeling** - Configurable message delays and drop rates
- **Failure Injection** - Support for node crashes, recoveries, and network partitions
- **Interactive CLI** - Step-by-step simulation control
- **YAML Configuration** - Easy-to-write scenario definitions

## Testing

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests RaftSafetyTests
```

## Development

### Project Structure

```
consensus-playground/
├── src/main/java/
│   ├── app/           # Main application and CLI
│   ├── io/            # Scenario loading and schema
│   ├── sim/           # Simulation framework
│   ├── raft/          # Raft consensus implementation
│   └── paxos/         # Paxos consensus (placeholder)
├── src/test/java/     # Test files
├── scenarios/         # Example scenario files
└── README.md
```

### Adding New Scenarios

1. Create a new YAML file in the `scenarios/` directory
2. Follow the schema defined in `ScenarioSchema.md`
3. Test your scenario by loading it in the interactive mode

### Extending the Framework

- **New Consensus Algorithms** - Implement in a new package following the existing patterns
- **New Event Types** - Add support in `Cluster.processEvent()`
- **New Network Rules** - Extend `NetworkRule` class
- **New Message Types** - Add to the appropriate RPC classes

## License

This project is open source and available under the MIT License.
