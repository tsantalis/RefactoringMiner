package org.refactoringminer.astDiff.graph.cluster.traverse;

import org.jgrapht.Graph;
import org.refactoringminer.astDiff.graph.Edge;
import org.refactoringminer.astDiff.graph.EdgeType;
import org.refactoringminer.astDiff.graph.Node;

import java.util.*;
import java.util.stream.Collectors;

public class NarrativeElement {
    private final String content;
    private final Set<Node> mains;
    private final Set<Node> sides;

    NarrativeElement(String content, Set<Node> mains, Graph<Node, Edge> clusterGraph) {
        this.content = content;
        this.mains = mains;
        this.sides = getSides(mains, clusterGraph);
    }

    public int lineCount() {
        return content.split("\n").length;
    }

    public String getContent() {
        return content;
    }

    public Set<Node> getMains() {
        return mains;
    }

    public Set<Node> getSides() {
        return sides;
    }

    private static Set<Node> getSides(Set<Node> mains, Graph<Node, Edge> clusterGraph) {
        Set<Node> sides = new HashSet<>();

        List<Node> usedNodes = mains.stream()
                .map(main -> clusterGraph.incomingEdgesOf(main).stream()
                        .filter(edge -> edge.getType().equals(EdgeType.DEF_USE)).toList())
                .flatMap(List::stream)
                .map(clusterGraph::getEdgeSource).toList();
        Set<Node> trackingNodes = new HashSet<>(usedNodes);
        while (true) {
            trackingNodes.addAll(trackingNodes.stream()
                    .map(trackingNode -> trackingNode.getMappingSources(clusterGraph))
                    .flatMap(List::stream).toList());
            trackingNodes.addAll(trackingNodes.stream()
                    .map(trackingNode -> trackingNode.getMappingTargets(clusterGraph))
                    .flatMap(List::stream).toList());

            sides.addAll(trackingNodes.stream().filter(trackingNode -> !trackingNode.isContext()).toList());

            trackingNodes = trackingNodes.stream().filter(Node::isContext).collect(Collectors.toSet());
            if (trackingNodes.isEmpty()) {
                break;
            }

            trackingNodes = trackingNodes.stream()
                    .map(trackingNode -> clusterGraph.incomingEdgesOf(trackingNode).stream()
                            .filter(edge -> edge.getType().equals(EdgeType.CONTEXT)).toList())
                    .flatMap(List::stream).map(clusterGraph::getEdgeSource).collect(Collectors.toSet());
        }

        return sides;
    }
}
