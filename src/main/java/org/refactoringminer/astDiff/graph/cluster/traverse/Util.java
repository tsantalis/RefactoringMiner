package org.refactoringminer.astDiff.graph.cluster.traverse;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.github.gumtreediff.tree.Tree;
import org.refactoringminer.astDiff.graph.Edge;
import org.refactoringminer.astDiff.graph.EdgeType;
import org.refactoringminer.astDiff.graph.Node;
import org.jgrapht.Graph;
import org.refactoringminer.astDiff.graph.SrcDst;

public class Util {

    Graph<Node, Edge> graph;

    public Util(Graph<Node, Edge> graph) {
        this.graph = graph;
    }

    public Set<Node> getUsedNodes(Node node) {
        return graph.incomingEdgesOf(node).stream()
                .filter(edge -> edge.getType().equals(EdgeType.DEF_USE))
                .map(edge -> graph.getEdgeSource(edge)).collect(Collectors.toSet());
    }

    // The changes the node relies on through unchanged code it uses which uses them, a bridge (see HunkNetwork#processBridges)
    public Set<Node> getBridgedNodes(Node node) {
        Set<Node> bridged = new HashSet<>();
        for (Node used : getUsedNodes(node)) {
            if (!used.isExtension()) {
                continue;
            }
            for (Node bridgedNode : getUsedNodes(used)) {
                if (!bridgedNode.isExtension() && !bridgedNode.equals(node)) {
                    bridged.add(bridgedNode);
                }
            }
        }
        return bridged;
    }

    public boolean doesUse(Node node) {
        return graph.incomingEdgesOf(node).stream()
                .anyMatch(edge -> edge.getType().equals(EdgeType.DEF_USE));
    }

    public List<Node> getMappingSources(Node node) {
        return graph.incomingEdgesOf(node).stream()
                .filter(edge -> edge.getType().equals(EdgeType.MAPPING)).map(graph::getEdgeSource)
                .toList();
    }

    public List<Node> getMappingTargets(Node node) {
        return graph.outgoingEdgesOf(node).stream()
                .filter(edge -> edge.getType().equals(EdgeType.MAPPING)).map(graph::getEdgeTarget)
                .toList();
    }

    public List<Node> findOverlappingNodes(String path, SrcDst srcDst, int pos, int endPos, Predicate<Node> filter) {
        List<Node> result = new ArrayList<>();

        for (Node node : graph.vertexSet()) {
            Tree tree = node.getTree();
            if (node.getPath().equals(path) && node.getSrcDst().equals(srcDst)
                    && tree.getPos() <= endPos && pos <= tree.getEndPos() && (filter == null
                    || filter.test(node))) {
                result.add(node);
            }
        }

        return result;
    }
}
