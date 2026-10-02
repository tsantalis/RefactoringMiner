package org.refactoringminer.astDiff.graph.cluster.traverse;

import org.jgrapht.Graph;
import org.refactoringminer.astDiff.graph.Edge;
import org.refactoringminer.astDiff.graph.Node;

import java.util.*;

public class NarrativeElement {
    Map<Node, Set<Node>> mainsToSides;
    private final List<TraversalPattern> leaves;
    private final Graph<Node, Edge> graph;

    public NarrativeElement(Map<Node, Set<Node>> mainsToSides, List<TraversalPattern> leaves, Graph<Node, Edge> graph) {
        this.mainsToSides = mainsToSides;
        this.leaves = leaves;
        this.graph = graph;
    }

    public Map<Node, Set<Node>> getMainsToSides() {
        return mainsToSides;
    }

    public List<TraversalPattern> getLeaves() {
        return leaves;
    }

    public Graph<Node, Edge> getGraph() {
        return graph;
    }
}
