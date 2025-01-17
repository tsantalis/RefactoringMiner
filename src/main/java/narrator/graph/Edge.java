package narrator.graph;

public class Edge {
    private EdgeType type;
    private float weight;

    public Edge(EdgeType type, float weight) {
        this.type = type;
        this.weight = weight;
    }

    public EdgeType getType() {
        return type;
    }

    public float getWeight() {
        return weight;
    }
}
