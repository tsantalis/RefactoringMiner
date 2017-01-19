package org.refactoringminer.rm2.analysis;

import org.refactoringminer.rm2.analysis.codesimilarity.CodeSimilarityStrategy;
import org.refactoringminer.rm2.model.RelationshipType;

public interface RefactoringDetectorConfig {

    String getId();

    double getThreshold(RelationshipType relationshipType);

    CodeSimilarityStrategy getCodeSimilarityStrategy();

}