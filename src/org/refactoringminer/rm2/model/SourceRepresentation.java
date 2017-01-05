package org.refactoringminer.rm2.model;

public interface SourceRepresentation {

    SourceRepresentation combine(Iterable<SourceRepresentation> others);

    SourceRepresentation minus(SourceRepresentation other);

    double similarity(SourceRepresentation other);

    double partialSimilarity(SourceRepresentation other);

}
