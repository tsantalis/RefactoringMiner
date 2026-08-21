package org.refactoringminer.astDiff.graph.cluster;

import org.refactoringminer.astDiff.graph.Node;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class Cluster extends GraphWrapper {
    public Cluster() {
    }

    @Nullable
    public Node findNode(String promptId) {
        Optional<Node> promptIdNode = getGraph().vertexSet().stream()
                .filter(node -> node.getPromptId().equals(promptId)).findFirst();
        return promptIdNode.orElse(null);
    }

    public Set<Node> findNodes(String side, String path, int line) {
        return getGraph().vertexSet().stream()
                .filter(node -> node.overlapLine(path, side, line)).collect(Collectors.toSet());
    }
}
