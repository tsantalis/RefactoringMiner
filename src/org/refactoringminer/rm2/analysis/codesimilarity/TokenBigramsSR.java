package org.refactoringminer.rm2.analysis.codesimilarity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.refactoringminer.rm2.model.HashArray;
import org.refactoringminer.rm2.model.SourceRepresentation;

class TokenBigramsSR extends HashArray implements SourceRepresentation {

    private final Map<Integer, String> debug;

    public TokenBigramsSR(long[] bigramHashes, Map<Integer, String> debug) {
        super(bigramHashes);
        this.debug = debug;
    }

    public TokenBigramsSR(long[] bigramHashes) {
        this(bigramHashes, null);
    }

    @Override
    public TokenBigramsSR minus(SourceRepresentation other) {
        return new TokenBigramsSR(computeMinus(hashes, ((TokenBigramsSR) other).hashes), debug);
    }

    @Override
    public String toString() {
        if (debug == null) {
            return Arrays.toString(hashes);
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < hashes.length; i++) {
                int h1 = getHigh(i);
                int h2 = getLow(i);
                sb.append(debug.get(h1));
                sb.append(' ');
                sb.append(debug.get(h2));
                sb.append('\n');
            }
            return sb.toString();
        }
    }

    @Override
    public TokenBigramsSR combine(SourceRepresentation other) {
        int totalLength = this.hashes.length;
        List<SourceRepresentation> others = Collections.singletonList(other);
        for (SourceRepresentation sr : others) {
            long[] hashes = ((TokenBigramsSR) sr).hashes;
            totalLength += hashes.length;
        }
        long[] hs = new long[totalLength];
        int j = 0;
        for (int i = 0; i < this.hashes.length; i++) {
            hs[j++] = this.hashes[i];
        }
        for (SourceRepresentation sr : others) {
            long[] hashes = ((TokenBigramsSR) sr).hashes;
            for (int i = 0; i < hashes.length; i++) {
                hs[j++] = hashes[i];
            }
        }
        Arrays.sort(hs);
        return new TokenBigramsSR(hs);
    }

    @Override
    public double similarity(SourceRepresentation other) {
        return similarity((HashArray) other);
    }

    @Override
    public double partialSimilarity(SourceRepresentation other) {
        return partialSimilarity((HashArray) other);
    }
}
