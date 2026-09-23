package org.refactoringminer.astDiff.graph.cluster.traverse;

import org.jgrapht.Graph;
import org.refactoringminer.astDiff.graph.Edge;
import org.refactoringminer.astDiff.graph.EdgeType;
import org.refactoringminer.astDiff.graph.Node;
import org.refactoringminer.astDiff.graph.ReviewNode;

import java.util.*;
import java.util.stream.Collectors;

public class NarrativeElement {
    private final String content;
    private final Set<ReviewNode> anchoredNodes;
    private final Set<Node> mains;
    private final Set<Node> sides;

    public NarrativeElement(String content, Set<ReviewNode> anchoredNodes, Set<Node> mains, Graph<Node, Edge> graph) {
        this.content = content;
        this.anchoredNodes = anchoredNodes;
        this.mains = mains;
        this.sides = getSides(mains, graph);
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

    public Set<ReviewNode> getAnchoredNodes() {
        return anchoredNodes;
    }

    private static Set<Node> getSides(Set<Node> mains, Graph<Node, Edge> graph) {
        Set<Node> sides = new HashSet<>();

        List<Node> usedNodes = mains.stream()
                .map(main -> graph.incomingEdgesOf(main).stream()
                        .filter(edge -> edge.getType().equals(EdgeType.DEF_USE)).toList())
                .flatMap(List::stream)
                .map(graph::getEdgeSource).toList();
        Set<Node> trackingNodes = new HashSet<>(usedNodes);
        while (true) {
            trackingNodes.addAll(trackingNodes.stream()
                    .map(trackingNode -> trackingNode.getMappingSources(graph))
                    .flatMap(List::stream).toList());
            trackingNodes.addAll(trackingNodes.stream()
                    .map(trackingNode -> trackingNode.getMappingTargets(graph))
                    .flatMap(List::stream).toList());

            sides.addAll(trackingNodes.stream().filter(trackingNode -> !trackingNode.isContext()).toList());

            trackingNodes = trackingNodes.stream().filter(Node::isContext).collect(Collectors.toSet());
            if (trackingNodes.isEmpty()) {
                break;
            }

            trackingNodes = trackingNodes.stream()
                    .map(trackingNode -> graph.incomingEdgesOf(trackingNode).stream()
                            .filter(edge -> edge.getType().equals(EdgeType.CONTEXT)).toList())
                    .flatMap(List::stream).map(graph::getEdgeSource).collect(Collectors.toSet());
        }

        return sides;
    }
}
