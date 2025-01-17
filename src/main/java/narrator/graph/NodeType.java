package narrator.graph;

/**
 * Node declarations
 */
public enum NodeType {
    HUNK("hunk");

    String label;

    NodeType(String label) {
        this.label = label;
    }
}
