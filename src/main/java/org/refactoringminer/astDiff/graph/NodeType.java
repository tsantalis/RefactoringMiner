package org.refactoringminer.astDiff.graph;

/**
 * Node declarations
 */
public enum NodeType {
    ADDITION("addition"), DST_MOVE("dst_move"), DST_UPDATE("dst_update"), DELETION(
            "deletion"), SRC_MOVE("src_move"), SRC_UPDATE("src_update"), EXTENSION(
            "extension"), LOCATION_CONTEXT("location_context"), SEMANTIC_CONTEXT(
            "semantic_context"), USAGE("usage"), SUCCESSIVE("successive"), COMPONENT(
            "component"), REQUIREMENT("requirement"), SIMILARITY("similarity"), SINGULAR(
            "singular"), CLUSTER("cluster"), ROOT("root");

    String label;

    NodeType(String label) {
        this.label = label;
    }

    public static NodeType alternativeNodeType(NodeType nodeType) {
        if (nodeType == ADDITION) {
            return DELETION;
        }
        if (nodeType == DELETION) {
            return ADDITION;
        }
        if (nodeType == SRC_MOVE) {
            return DST_MOVE;
        }
        if (nodeType == DST_MOVE) {
            return SRC_MOVE;
        }
        if (nodeType == SRC_UPDATE) {
            return DST_UPDATE;
        }
        if (nodeType == DST_UPDATE) {
            return SRC_UPDATE;
        }
        if (nodeType == EXTENSION) {
            return EXTENSION;
        }
        if (nodeType == LOCATION_CONTEXT) {
            return LOCATION_CONTEXT;
        }
        if (nodeType == SEMANTIC_CONTEXT) {
            return SEMANTIC_CONTEXT;
        }
        return null;
    }
}
