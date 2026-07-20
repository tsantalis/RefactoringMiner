package org.refactoringminer.astDiff.graph;

public enum SrcDst {
    SRC("SRC"), DST("DST");

    private final String name;

    SrcDst(String name) {
        this.name = name;
    }
}
