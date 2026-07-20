package org.refactoringminer.astDiff.graph;

public class Edge {
    private EdgeType type;

    public Edge(EdgeType type) {
        this.type = type;
    }

    public EdgeType getType() {
        return type;
    }
}
