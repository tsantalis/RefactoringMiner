package narrator.graph;

/**
 * Node declarations
 */
public enum NodeType {
    ADDITION("addition"), DELETION("deletion"), EXTENSION("extension"), LOCATION_CONTEXT(
            "location_context"), SEMANTIC_CONTEXT("semantic_context"), USAGE("usage"), SUCCESSIVE(
            "successive"), COMPONENT("component"), REQUIREMENT("requirement"), SIMILARITY(
            "similarity"), SINGULAR("singular"), CLUSTER("cluster"), ROOT("root");

    String label;

    NodeType(String label) {
        this.label = label;
    }
}
