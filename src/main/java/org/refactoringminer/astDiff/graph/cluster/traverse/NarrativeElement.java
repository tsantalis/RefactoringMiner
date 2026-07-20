package org.refactoringminer.astDiff.graph.cluster.traverse;

public record NarrativeElement(
    String content,
    int lineCount,
    ElementType type
) {
    public enum ElementType {
        DEPENDENCY,   // Global context/dependencies
        SUB_CHAPTER   // Actual content block
    }
}
