package org.refactoringminer.rm2.analysis;

import org.refactoringminer.rm2.model.SourceRepresentation;

public interface SourceRepresentationBuilder {

    SourceRepresentation buildSourceRepresentation(char[] charArray, int start, int length);

    SourceRepresentation buildEmptySourceRepresentation();

    default void onComplete() {}

}