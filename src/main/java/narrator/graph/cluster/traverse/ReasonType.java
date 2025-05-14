package narrator.graph.cluster.traverse;

public enum ReasonType {
    COMMON("common"), SIMILAR("similar");

    String label;

    ReasonType(String label) {
        this.label = label;
    }
}
