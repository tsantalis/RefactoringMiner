package narrator.graph.cluster.traverse;

public enum ReasonType {
    COMMON("common"), SIMILAR("similar"), REQUIREMENT("requirement");

    String label;

    ReasonType(String label) {
        this.label = label;
    }
}
