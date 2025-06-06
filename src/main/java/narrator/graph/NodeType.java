package narrator.graph;

/**
 * Node declarations
 */
public enum NodeType {
    BASE("base"), LOCATION_CONTEXT("location_context"), SEMANTIC_CONTEXT("semantic_context"), EXTENSION("extension"),
    USAGE("usage"), SUCCESSIVE("successive"), COMPONENT("component"), REQUIREMENT("requirement"),
    SINGULAR("singular"), CLUSTER("cluster"), ROOT("root");

    String label;

    NodeType(String label) {
        this.label = label;
    }
}
