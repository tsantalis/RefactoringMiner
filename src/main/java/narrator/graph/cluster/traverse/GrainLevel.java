package narrator.graph.cluster.traverse;

public enum GrainLevel {
    LEAF("Detailed changes at the leaf level"),
    USAGE_CHAIN_ROOT("Root of usage chain changes"),
    METHOD("Changes at the method level"),
    CLASS("Changes at the class/type level"),
    FILE("Changes at the file level"),
    SEMANTIC_LEAF("Lowest semantic component changes"),
    SEMANTIC_ROOT("Highest semantic component changes");

    private final String description;

    GrainLevel(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
