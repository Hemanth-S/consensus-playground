package io;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import raft.RaftModel;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Scenario Loader Tests")
class ScenarioLoaderTest {

    @Test
    @DisplayName("Should load YAML scenario successfully")
    void shouldLoadYamlScenarioSuccessfully() throws Exception {
        // Load the test scenario
        Path scenarioPath = Paths.get("scenarios/raft_leader_crash.yml");
        ScenarioLoader.Scenario scenario = ScenarioLoader.load(scenarioPath);
        
        // Verify scenario properties
        assertNotNull(scenario);
        assertEquals("raft", scenario.model);
        assertEquals(12345L, scenario.seed);
        assertNotNull(scenario.cluster);
        assertEquals(5, scenario.cluster.nodes.size());
        assertTrue(scenario.cluster.nodes.contains("n1"));
        assertTrue(scenario.cluster.nodes.contains("n2"));
        assertTrue(scenario.cluster.nodes.contains("n3"));
        assertTrue(scenario.cluster.nodes.contains("n4"));
        assertTrue(scenario.cluster.nodes.contains("n5"));
    }

    @Test
    @DisplayName("Should apply scenario to RaftModel successfully")
    void shouldApplyScenarioToRaftModelSuccessfully() throws Exception {
        // Load scenario
        Path scenarioPath = Paths.get("scenarios/raft_leader_crash.yml");
        ScenarioLoader.Scenario scenario = ScenarioLoader.load(scenarioPath);
        
        // Create and apply to model
        RaftModel model = new RaftModel(scenario.cluster.nodes, scenario.seed);
        ScenarioLoader.applyInitial(scenario, model);
        ScenarioLoader.applyNetworkRules(scenario, model.cluster());
        
        // Verify model was created successfully
        assertNotNull(model);
        assertEquals(5, model.getNodeIds().size());
        assertTrue(model.getNodeIds().contains("n1"));
        assertTrue(model.getNodeIds().contains("n2"));
        assertTrue(model.getNodeIds().contains("n3"));
        assertTrue(model.getNodeIds().contains("n4"));
        assertTrue(model.getNodeIds().contains("n5"));
    }

    @Test
    @DisplayName("Should load split vote scenario successfully")
    void shouldLoadSplitVoteScenarioSuccessfully() throws Exception {
        // Load the split vote scenario
        Path scenarioPath = Paths.get("scenarios/raft_split_vote.yml");
        ScenarioLoader.Scenario scenario = ScenarioLoader.load(scenarioPath);
        
        // Verify scenario properties
        assertNotNull(scenario);
        assertEquals("raft", scenario.model);
        assertEquals(54321L, scenario.seed);
        assertNotNull(scenario.cluster);
        assertEquals(5, scenario.cluster.nodes.size());
        
        // Verify network rules are present
        assertNotNull(scenario.network);
        assertNotNull(scenario.network.rules);
        assertTrue(scenario.network.rules.size() > 0);
        
        // Verify timeline actions
        assertNotNull(scenario.timeline);
        assertTrue(scenario.timeline.size() > 0);
    }
}
