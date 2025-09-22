package io;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import sim.Cluster;
import sim.NetworkRule;
import sim.Message;
import sim.MessageBus;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Loads and parses scenario files in YAML format.
 * Creates Cluster instances from scenario definitions.
 */
public class ScenarioLoader {
    private final ObjectMapper yamlMapper;
    
    public ScenarioLoader() {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
    }
    
    /**
     * Load a scenario from a YAML file
     */
    public Cluster loadScenario(Path scenarioPath) throws IOException {
        ScenarioDefinition scenario = yamlMapper.readValue(
            scenarioPath.toFile(), 
            ScenarioDefinition.class
        );
        
        return createClusterFromScenario(scenario);
    }
    
    /**
     * Create a Cluster instance from a scenario definition
     */
    private Cluster createClusterFromScenario(ScenarioDefinition scenario) {
        Cluster cluster = new Cluster();
        
        // Initialize nodes (placeholder nodes for now)
        for (int i = 0; i < scenario.getNodeCount(); i++) {
            String nodeId = "node-" + i;
            Cluster.Node node = new Cluster.Node() {
                private boolean up = true;
                
                @Override
                public String id() { return nodeId; }
                
                @Override
                public boolean isUp() { return up; }
                
                @Override
                public void setUp(boolean up) { this.up = up; }
                
                @Override
                public void onTick(MessageBus bus) {
                    // Placeholder - no action
                }
                
                @Override
                public void onMessage(Message m, MessageBus bus) {
                    // Placeholder - no action
                }
                
                @Override
                public String dump() {
                    return "placeholder node";
                }
            };
            cluster.add(node);
        }
        
        // Apply network rules
        if (scenario.getNetworkRules() != null) {
            for (NetworkRuleDefinition ruleDef : scenario.getNetworkRules()) {
                NetworkRule.Match match = new NetworkRule.Match(
                    ruleDef.getFrom(),
                    ruleDef.getTo(),
                    "*", // Match any message type
                    null, null, false
                );
                NetworkRule.Action action = ruleDef.getDropRate() > 0 ? 
                    NetworkRule.Action.DROP_PCT : NetworkRule.Action.DELAY;
                NetworkRule rule = new NetworkRule(match, action, ruleDef.getDelay(), ruleDef.getDropRate());
                cluster.getMessageBus().addRule(rule);
            }
        }
        
        // Apply initial events
        if (scenario.getEvents() != null) {
            for (EventDefinition event : scenario.getEvents()) {
                cluster.scheduleEvent(event.getTime(), event.getType(), event.getData());
            }
        }
        
        return cluster;
    }
    
    /**
     * Scenario definition structure for YAML parsing
     */
    public static class ScenarioDefinition {
        private String name;
        private String description;
        private int nodeCount;
        private List<NetworkRuleDefinition> networkRules;
        private List<EventDefinition> events;
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public int getNodeCount() { return nodeCount; }
        public void setNodeCount(int nodeCount) { this.nodeCount = nodeCount; }
        
        public List<NetworkRuleDefinition> getNetworkRules() { return networkRules; }
        public void setNetworkRules(List<NetworkRuleDefinition> networkRules) { this.networkRules = networkRules; }
        
        public List<EventDefinition> getEvents() { return events; }
        public void setEvents(List<EventDefinition> events) { this.events = events; }
    }
    
    /**
     * Network rule definition for YAML parsing
     */
    public static class NetworkRuleDefinition {
        private String from;
        private String to;
        private int delay;
        private double dropRate;
        
        // Getters and setters
        public String getFrom() { return from; }
        public void setFrom(String from) { this.from = from; }
        
        public String getTo() { return to; }
        public void setTo(String to) { this.to = to; }
        
        public int getDelay() { return delay; }
        public void setDelay(int delay) { this.delay = delay; }
        
        public double getDropRate() { return dropRate; }
        public void setDropRate(double dropRate) { this.dropRate = dropRate; }
    }
    
    /**
     * Event definition for YAML parsing
     */
    public static class EventDefinition {
        private int time;
        private String type;
        private Map<String, Object> data;
        
        // Getters and setters
        public int getTime() { return time; }
        public void setTime(int time) { this.time = time; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public Map<String, Object> getData() { return data; }
        public void setData(Map<String, Object> data) { this.data = data; }
    }
}

