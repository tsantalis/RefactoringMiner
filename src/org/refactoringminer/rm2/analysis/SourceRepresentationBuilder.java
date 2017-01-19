package org.refactoringminer.rm2.analysis;

import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.refactoringminer.rm2.model.SDEntity;
import org.refactoringminer.rm2.model.SourceRepresentation;

public interface SourceRepresentationBuilder {

    SourceRepresentation buildSourceRepresentation(SDEntity entity, char[] charArray, ASTNode astNode);

    SourceRepresentation buildPartialSourceRepresentation(char[] charArray, ASTNode astNode);

    SourceRepresentation buildSourceRepresentation(SDEntity entity, List<SourceRepresentation> parts);

    SourceRepresentation buildEmptySourceRepresentation();

    default void onComplete() {}

}