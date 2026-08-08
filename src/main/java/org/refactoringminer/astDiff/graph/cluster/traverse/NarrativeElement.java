package org.refactoringminer.astDiff.graph.cluster.traverse;

import org.refactoringminer.astDiff.graph.Node;

import java.util.Set;

public record NarrativeElement(
    String content,
    ElementType type,
    Set<Node> mains,
    Set<Node> sides
) {
    public int lineCount() {
        return content.split("\n").length;
    };

    public enum ElementType {
        DEPENDENCY,   // Global context/dependencies
        SUB_CHAPTER   // Actual content block
    }
}
