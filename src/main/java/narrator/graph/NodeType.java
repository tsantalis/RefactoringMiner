package narrator.graph;

/**
 * Node declarations
 */
public enum NodeType {
    BASE("base"), CONTEXT("context"), EXTENSION("extension"), AGGREGATOR("aggregator");

    String label;

    NodeType(String label) {
        this.label = label;
    }
}
