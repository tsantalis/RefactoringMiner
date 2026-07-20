package org.refactoringminer.astDiff.graph;

import java.util.*;
import org.jgrapht.Graph;

public class NodeScorer {

    private final Graph<Node, Edge> graph;

    public NodeScorer(Graph<Node, Edge> graph) {
        this.graph = graph;
    }

    /**
     * Scores nodes based on the size of their support base.
     * A support base consists of all reachable nodes via incoming DEF_USE edges,
     * excluding context and extension nodes.
     */
    public List<Node> scoreBySupportGroup() {
        Set<Node> allNodes = graph.vertexSet();
        Map<Node, Set<Node>> supportBases = new HashMap<>();

        for (Node n : allNodes) {
            if (n.isContext() || n.isExtension()) {
                continue;
            }

            Set<Node> ancestors = new HashSet<>();
            Queue<Node> queue = new LinkedList<>();
            queue.add(n);

            Set<Node> visited = new HashSet<>();
            visited.add(n);

            while (!queue.isEmpty()) {
                Node curr = queue.poll();
                for (Edge edge : graph.incomingEdgesOf(curr)) {
                    Node parent = graph.getEdgeSource(edge);
                    if (edge.getType() == EdgeType.DEF_USE && !visited.contains(parent)) {
                        visited.add(parent);
                        if (!parent.isContext() && !parent.isExtension()) {
                            ancestors.add(parent);
                        }
                        queue.add(parent);
                    }
                }
            }
            supportBases.put(n, ancestors);
        }

        return supportBases.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue().size(), e1.getValue().size()))
                .map(Map.Entry::getKey)
                .toList();
    }

    /**
     * Scores nodes based on the number of incoming and outgoing MAPPING edges.
     */
    public List<Node> scoreByMappingEdges() {
        return graph.vertexSet().stream()
                .filter(n -> !n.isContext() && !n.isExtension())
                .sorted((n1, n2) -> {
                    int score1 = countMappingEdges(n1);
                    int score2 = countMappingEdges(n2);
                    return Integer.compare(score2, score1);
                })
                .toList();
    }

    private int countMappingEdges(Node n) {
        long incoming = graph.incomingEdgesOf(n).stream()
                .filter(e -> e.getType() == EdgeType.MAPPING)
                .count();
        long outgoing = graph.outgoingEdgesOf(n).stream()
                .filter(e -> e.getType() == EdgeType.MAPPING)
                .count();
        return (int) (incoming + outgoing);
    }

    /**
     * Scores nodes based on the number of incoming DEF_USE edges.
     */
    public List<Node> scoreByDefUseEdges() {
        return graph.vertexSet().stream()
                .filter(n -> !n.isContext() && !n.isExtension())
                .sorted((n1, n2) -> {
                    int score1 = countIncomingDefUse(n1);
                    int score2 = countIncomingDefUse(n2);
                    return Integer.compare(score2, score1);
                })
                .toList();
    }

    private int countIncomingDefUse(Node n) {
        return (int) graph.incomingEdgesOf(n).stream()
                .filter(e -> e.getType() == EdgeType.DEF_USE)
                .count();
    }
}
