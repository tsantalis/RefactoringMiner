package org.refactoringminer.rm2.analysis.codesimilarity;

import org.refactoringminer.rm2.analysis.SourceRepresentationBuilder;

public interface CodeSimilarityStrategy {

    SourceRepresentationBuilder createSourceRepresentationBuilderForTypes();

    SourceRepresentationBuilder createSourceRepresentationBuilderForMethods();

    SourceRepresentationBuilder createSourceRepresentationBuilderForAttributes();

    public static CodeSimilarityStrategy BIGRAM = new CodeSimilarityStrategy() {
        @Override
        public SourceRepresentationBuilder createSourceRepresentationBuilderForTypes() {
            return new TokenBigramsSRBuilder(TokenBigramsSRBuilder.LINES);
        }
        @Override
        public SourceRepresentationBuilder createSourceRepresentationBuilderForMethods() {
            return new TokenBigramsSRBuilder(TokenBigramsSRBuilder.TOKENS);
        }
        @Override
        public SourceRepresentationBuilder createSourceRepresentationBuilderForAttributes() {
            return new TokenBigramsSRBuilder(TokenBigramsSRBuilder.TOKENS);
        }
    };

    public static CodeSimilarityStrategy TFIDF = new CodeSimilarityStrategy() {
        @Override
        public SourceRepresentationBuilder createSourceRepresentationBuilderForTypes() {
            return new TokenIdfSRBuilder();
        }
        @Override
        public SourceRepresentationBuilder createSourceRepresentationBuilderForMethods() {
            return new TokenIdfSRBuilder();
        }
        @Override
        public SourceRepresentationBuilder createSourceRepresentationBuilderForAttributes() {
            return new TokenIdfSRBuilder();
        }
    };

}
