package narrator.graph;

public enum EdgeType {
    DEF_USE("def_use"),
    SIMILARITY("similarity"),
    SUCCESSION("succession"),
    CONTEXT("context"),
    EXPANSION("expansion");

    String label;

    EdgeType(String label) {
        this.label = label;
    }
}
