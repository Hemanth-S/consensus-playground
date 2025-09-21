# Scenario Schema Documentation

This document describes the YAML schema for scenario files used in the consensus playground.

## Overview

Scenario files define the initial state and behavior of a distributed system simulation, including:
- Number of nodes in the cluster
- Network topology and rules
- Initial events and timing

## Schema Structure

```yaml
name: string                    # Scenario name
description: string             # Human-readable description
nodeCount: integer              # Number of nodes in the cluster
networkRules:                   # Optional network configuration
  - from: string                # Source node ID
    to: string                  # Destination node ID
    delay: integer              # Message delay in time units
    dropRate: double            # Message drop probability (0.0-1.0)
events:                         # Optional initial events
  - time: integer               # When the event occurs
    type: string                # Event type identifier
    data:                       # Event-specific data
      key: value                # Arbitrary key-value pairs
```

## Example Scenarios

### Basic Raft Cluster
```yaml
name: "Basic Raft Cluster"
description: "Simple 3-node Raft cluster with no network issues"
nodeCount: 3
```

### Raft with Network Delays
```yaml
name: "Raft with Network Delays"
description: "3-node Raft cluster with asymmetric network delays"
nodeCount: 3
networkRules:
  - from: "node-0"
    to: "node-1"
    delay: 10
    dropRate: 0.0
  - from: "node-0"
    to: "node-2"
    delay: 20
    dropRate: 0.1
```

### Raft Leader Crash Scenario
```yaml
name: "Raft Leader Crash"
description: "Leader crashes after 100 time units"
nodeCount: 3
events:
  - time: 100
    type: "crash"
    data:
      node: "node-0"
      reason: "simulated_failure"
```

## Event Types

### Crash Events
- **Type**: `crash`
- **Data**:
  - `node`: Node ID to crash
  - `reason`: Optional reason string

### Recovery Events
- **Type**: `recover`
- **Data**:
  - `node`: Node ID to recover
  - `state`: Optional initial state

### Network Partition Events
- **Type**: `partition`
- **Data**:
  - `nodes`: List of node IDs in partition
  - `duration`: How long the partition lasts

## Network Rules

Network rules define the behavior of message passing between nodes:

- **delay**: Minimum time units before a message is delivered
- **dropRate**: Probability (0.0-1.0) that a message is dropped and never delivered
- **from/to**: Use `"*"` to match any node

## Best Practices

1. **Naming**: Use descriptive names and descriptions
2. **Node IDs**: Follow consistent naming patterns (e.g., "node-0", "node-1")
3. **Timing**: Use reasonable time units for delays and events
4. **Testing**: Start with simple scenarios and add complexity gradually
5. **Documentation**: Include clear descriptions of what each scenario tests

