package org.refactoringminer.astDiff.graph.cluster;

import org.refactoringminer.astDiff.graph.Node;

public class Cluster extends GraphWrapper {
    public Cluster() {
    }

    Cluster(Node node) {
        addNode(node);
    }
}
