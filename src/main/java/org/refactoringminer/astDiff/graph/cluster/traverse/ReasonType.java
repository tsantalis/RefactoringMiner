package org.refactoringminer.astDiff.graph.cluster.traverse;

public enum ReasonType {
    CONTEXT("context"), USAGE("usage");

    String label;

    ReasonType(String label) {
        this.label = label;
    }
}
