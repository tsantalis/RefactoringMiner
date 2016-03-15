package org.refactoringminer.model;

import java.util.Arrays;
import java.util.Map;


public class SourceRepresentation extends HashArray {

    private final Map<Integer, String> debug;
    
    public SourceRepresentation(long[] bigramHashes, Map<Integer, String> debug) {
        super(bigramHashes);
        this.debug = debug;
    }

    public SourceRepresentation(long[] bigramHashes) {
        this(bigramHashes, null);
    }

    public SourceRepresentation minus(SourceRepresentation other) {
        return new SourceRepresentation(computeMinus(hashes, other.hashes), debug);
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
    
    public SourceRepresentation combine(Iterable<SourceRepresentation> others) {
        int totalLength = this.hashes.length;
        for (SourceRepresentation sr : others) {
            totalLength += sr.hashes.length;
        }
        long[] hs = new long[totalLength];
        int j = 0;
        for (int i = 0; i < this.hashes.length; i++) {
            hs[j++] = this.hashes[i];
        }
        for (SourceRepresentation sr : others) {
            for (int i = 0; i < sr.hashes.length; i++) {
                hs[j++] = sr.hashes[i];
            }
        }
        Arrays.sort(hs);
        return new SourceRepresentation(hs);
    }
}
