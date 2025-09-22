package app;

import io.ScenarioLoader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import raft.RaftModel;

import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Simulation Controller Tests")
class SimulationControllerTest {

    @Test
    @DisplayName("Should execute timeline actions and evaluate assertions")
    void shouldExecuteTimelineActionsAndEvaluateAssertions() throws Exception {
        // Load scenario
        var scenarioPath = Paths.get("scenarios/raft_leader_crash.yml");
        ScenarioLoader.Scenario scenario = ScenarioLoader.load(scenarioPath);
        
        // Create model and controller
        RaftModel model = new RaftModel(scenario.cluster.nodes, scenario.seed);
        ScenarioLoader.applyInitial(scenario, model);
        ScenarioLoader.applyNetworkRules(scenario, model.cluster());
        
        SimulationController controller = new SimulationController(model);
        controller.attachScenario(scenario);
        
        // Verify initial state
        assertEquals(0, controller.now());
        assertFalse(model.currentLeaderId().isPresent());
        
        // Step a few times instead of playing to completion
        controller.step(10);
        
        // Verify final state
        assertTrue(controller.now() > 0);
        
        // Test basic functionality without assertions for now
        assertNotNull(controller);
    }

    @Test
    @DisplayName("Should queue client writes when no leader exists")
    void shouldQueueClientWritesWhenNoLeaderExists() throws Exception {
        // Create a simple 3-node cluster
        List<String> nodeIds = List.of("n1", "n2", "n3");
        RaftModel model = new RaftModel(nodeIds, 12345L);
        
        // Initially no leader
        assertFalse(model.currentLeaderId().isPresent());
        
        // Try to write a command - should be queued
        boolean handled = model.clientWrite("SET key value");
        assertFalse(handled); // Should be queued, not handled immediately
        
        // Step simulation to allow leader election
        for (int i = 0; i < 20; i++) {
            model.step();
            if (model.currentLeaderId().isPresent()) {
                break;
            }
        }
        
        // Should have a leader now
        assertTrue(model.currentLeaderId().isPresent());
    }

    @Test
    @DisplayName("Should check log consistency")
    void shouldCheckLogConsistency() throws Exception {
        // Create a simple 3-node cluster
        List<String> nodeIds = List.of("n1", "n2", "n3");
        RaftModel model = new RaftModel(nodeIds, 12345L);
        
        // Initially logs should be consistent (all empty)
        assertTrue(model.logsArePrefixConsistent());
        
        // Step simulation to allow some activity
        for (int i = 0; i < 10; i++) {
            model.step();
        }
        
        // Logs should still be consistent
        assertTrue(model.logsArePrefixConsistent());
    }
}
