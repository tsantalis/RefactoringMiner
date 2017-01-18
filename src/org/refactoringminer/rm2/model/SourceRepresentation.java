package org.refactoringminer.rm2.model;

public interface SourceRepresentation {

    SourceRepresentation combine(SourceRepresentation other);

    SourceRepresentation minus(SourceRepresentation other);

    double similarity(SourceRepresentation other);

    double partialSimilarity(SourceRepresentation other);

}
