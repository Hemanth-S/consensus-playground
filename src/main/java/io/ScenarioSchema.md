# Scenario Schema Documentation

This document describes the YAML schema for consensus algorithm testing scenarios.

## Top-Level Structure

```yaml
model: raft                    # Algorithm type: "raft" or "paxos"
description: "..."            # Human-readable description (optional)
seed: 12345                   # Random seed for deterministic testing
cluster: <ClusterSpec>        # Cluster configuration
initial: <InitialSpec>        # Initial state setup
network: <NetworkSpec>        # Network behavior rules
timeline: <List<TimedAction>> # Time-based actions
assertions: <List<Assertion>> # Post-execution checks
```

## Description Field

The `description` field is optional and provides a human-readable explanation of what the scenario tests. This description is automatically printed when the scenario is played, helping users understand the purpose and expected behavior of the test.

Example:
```yaml
description: "Tests leader election and recovery when the leader crashes. Verifies that a new leader is elected and client commands are properly handled during the transition."
```

## ClusterSpec

Defines the cluster topology and timing.

```yaml
cluster:
  nodes: [n1, n2, n3, n4, n5]  # List of node IDs
  tickMs: 1                    # Simulation tick duration in milliseconds
```

## InitialSpec

Sets up the initial state of nodes and logs.

```yaml
initial:
  nodeState:                   # Per-node initial state
    n1: {crashed: false}       # Node state properties
    n2: {crashed: true}        # crashed: boolean
  logs:                        # Per-node initial log entries
    n1:                        # List of log entries for node n1
      - term: 1                # Log entry term
        cmd: "SET key value"   # Log entry command
```

## NetworkSpec

Defines network behavior through rules.

```yaml
network:
  rules:                       # List of network rules
    - match: <MatchSpec>       # Message matching criteria
      action: "drop"           # Action: "pass", "drop", "delay", "drop_pct"
      delaySteps: 5            # Delay in simulation steps (for "delay")
      pct: 0.1                 # Drop percentage (for "drop_pct")
```

## MatchSpec

Specifies which messages a rule applies to.

```yaml
match:
  from: "n1"                   # Source node ID, "*" for any
  to: "n2"                     # Destination node ID, "*" for any
  type: "RequestVote"          # Message type, "*" for any
  between: [n1, n2]            # Alternative: specify node pair
  bidirectional: true          # Apply rule to both directions
```

## TimedAction

Executes actions at specific simulation times.

```yaml
timeline:
  - at: 10                     # Simulation time to execute
    actions:                   # List of actions to execute
      - kind: "crash"          # Action type
        args:                  # Action-specific arguments
          node: "n1"
```

## ActionSpec

Individual actions that can be executed.

### Supported Actions

- **crash**: Crash a node
  ```yaml
  kind: "crash"
  args: {node: "n1"}
  ```

- **recover**: Recover a crashed node
  ```yaml
  kind: "recover"
  args: {node: "n1"}
  ```

- **clientwrite**: Send a client command
  ```yaml
  kind: "clientwrite"
  args: {command: "SET key value"}
  ```

- **partition**: Create network partition
  ```yaml
  kind: "partition"
  args: {groupA: [n1, n2], groupB: [n3, n4, n5]}
  ```

- **clearpartitions**: Remove all network partitions
  ```yaml
  kind: "clearpartitions"
  args: {}
  ```

## Assertion

Post-execution checks to validate scenario outcomes.

```yaml
assertions:
  - type: "leader_exists"      # Assertion type
    args:                      # Assertion-specific arguments
      after: 20                # Check after this simulation time
```

### Supported Assertions

- **leader_exists**: Verify a leader exists
  ```yaml
  type: "leader_exists"
  args: {after: 20}
  ```

- **log_consistency**: Verify log consistency across nodes
  ```yaml
  type: "log_consistency"
  args: {after: 25}
  ```

- **no_split_brain**: Verify no split-brain scenario
  ```yaml
  type: "no_split_brain"
  args: {after: 30}
  ```

## Example Usage

```java
// Load and apply a scenario
Scenario scenario = ScenarioLoader.load(Paths.get("scenarios/raft_leader_crash.yml"));
RaftModel model = new RaftModel(scenario.cluster.nodes, scenario.seed);
ScenarioLoader.apply(scenario, model);
```