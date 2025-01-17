package graph;

public class Edge {
    private Integer id;
    private EdgeType type;
    private Integer weight;

    public Edge(Integer id, EdgeType type) {
        this.id = id;
        this.type = type;
        this.weight = 1;
    }

    public Integer getId() {
        return id;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }

    /**
     * Increase the weight by one
     */
    public Integer increaseWeight() {
        this.weight += 1;
        return this.weight;
    }

    public EdgeType getType() {
        return type;
    }
}
