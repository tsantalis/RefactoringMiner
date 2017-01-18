package org.refactoringminer.rm2.analysis.codesimilarity;

import java.util.HashSet;
import java.util.Set;

import org.refactoringminer.rm2.model.Multiset;
import org.refactoringminer.rm2.model.SourceRepresentation;

class TokenIdfSR implements SourceRepresentation {

    private final Multiset<String> tokens;
    private final TokenIdfSRBuilder builder;

    public TokenIdfSR(Multiset<String> tokens, TokenIdfSRBuilder builder) {
        this.tokens = tokens;
        this.builder = builder;
    }

    @Override
    public TokenIdfSR minus(SourceRepresentation other) {
        return new TokenIdfSR(tokens.minus(((TokenIdfSR) other).tokens), builder);
    }

    @Override
    public String toString() {
        return tokens.toString();
    }

    @Override
    public TokenIdfSR combine(SourceRepresentation sr) {
        Multiset<String> multisetUnion = tokens;
        TokenIdfSR tokenIdfSR = (TokenIdfSR) sr;
        multisetUnion = multisetUnion.plus(tokenIdfSR.tokens);
        return new TokenIdfSR(multisetUnion, builder);
    }

    @Override
    public double similarity(SourceRepresentation other) {
        return jaccardSimilarity(((TokenIdfSR) other).tokens, false);
    }

    @Override
    public double partialSimilarity(SourceRepresentation other) {
        return jaccardSimilarity(((TokenIdfSR) other).tokens, true);
    }

    public double jaccardSimilarity(Multiset<String> tokens2, boolean partial) {
        if (tokens.isEmpty() || tokens2.isEmpty()) {
            return 0.0;
        }
        Set<String> keys = new HashSet<String>();
        keys.addAll(tokens.asSet());
        keys.addAll(tokens2.asSet());
        double idfu = 0.0;
        double idfd = 0.0;
        for (String key : keys) {
            int c1 = tokens.getMultiplicity(key);
            int c2 = tokens2.getMultiplicity(key);
            idfu += Math.min(c1, c2) * builder.idf(key);
            idfd += Math.max(c1, c2) * builder.idf(key);
        }
        if (partial) {
            double idfp = 0.0;
            for (String key : tokens.asSet()) {
                int c1 = tokens.getMultiplicity(key);
                idfp += c1 * builder.idf(key);
            }
            return idfu / idfp;
        }
        return idfu / idfd;
    }

}
