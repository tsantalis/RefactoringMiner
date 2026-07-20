package org.refactoringminer.astDiff.graph;

public enum EdgeType {
    MAPPING("mapping"),
    DEF_USE("def_use"),
    SIMILARITY("similarity"),
    SUCCESSION("succession"),
    CONTEXT("context"),
    EXPANSION("expansion");

    String label;

    EdgeType(String label) {
        this.label = label;
    }
}
