package narrator.graph;

/**
 * Node declarations
 */
public enum NodeType {
    BASE("base"), CONTEXT("context"), EXTENSION("extension"), USAGE("usage"), SUCCESSIVE("successive"), COMPONENT(
            "component"), REQUIREMENT("requirement"), SINGULAR("singular"), CLUSTER("cluster"), COMMIT("commit");

    String label;

    NodeType(String label) {
        this.label = label;
    }
}
