package org.refactoringminer.rm2.analysis;

import org.refactoringminer.rm2.model.SourceRepresentation;

public interface SourceRepresentationBuilder {

    SourceRepresentation buildSourceRepresentationForType(char[] charArray, int start, int length);

    SourceRepresentation buildSourceRepresentationForMethodBody(char[] charArray, int start, int length);

    SourceRepresentation buildSourceRepresentationForStatement(char[] charArray, int start, int length);

    SourceRepresentation buildSourceRepresentationForExpression(char[] charArray, int start, int length);

    SourceRepresentation buildEmptySourceRepresentation();

}