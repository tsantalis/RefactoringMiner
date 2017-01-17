package org.refactoringminer.rm2.analysis;

import org.refactoringminer.rm2.analysis.codesimilarity.CodeSimilarityStrategy;

public interface RefactoringDetectorConfig {

    String getId();

    double getMoveTypeThreshold();

    double getRenameTypeThreshold();

    double getMoveAndRenameTypeThreshold();

    double getExtractSupertypeThreshold();

    double getRenameMethodThreshold();

    double getMoveMethodThreshold();

    double getPullUpMethodThreshold();

    double getPushDownMethodThreshold();

    double getExtractMethodThreshold();

    double getInlineMethodThreshold();

    double getMoveAttributeThreshold();

    double getPullUpAttributeThreshold();

    double getPushDownAttributeThreshold();

    CodeSimilarityStrategy getCodeSimilarityStrategy();

}