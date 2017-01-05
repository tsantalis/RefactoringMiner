package org.refactoringminer.rm2.analysis.codesimilarity;

import org.refactoringminer.rm2.analysis.SourceRepresentationBuilder;

public interface CodeSimilarityStrategy {

    SourceRepresentationBuilder createSourceRepresentationBuilder();

    public static CodeSimilarityStrategy BIGRAM = new CodeSimilarityStrategy() {
        @Override
        public SourceRepresentationBuilder createSourceRepresentationBuilder() {
            return new TokenBigramsSRBuilder();
        }
    };

}
