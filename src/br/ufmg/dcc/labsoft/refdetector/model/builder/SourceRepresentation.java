package br.ufmg.dcc.labsoft.refdetector.model.builder;

import java.util.Arrays;
import java.util.Map;


public class SourceRepresentation {

    private final int linesCount;
    private final long[] bigramHashes;
    private final Map<Integer, String> debug;
    
    public SourceRepresentation(int linesCount, long[] bigramHashes, Map<Integer, String> debug) {
        this.linesCount = linesCount;
        this.bigramHashes = bigramHashes;
        this.debug = debug;
    }

    public SourceRepresentation(int linesCount, long[] bigramHashes) {
        this(linesCount, bigramHashes, null);
    }

    public double similarity(SourceRepresentation other) {
        return similarity(other, 1);
    }
    
    public double similarity(SourceRepresentation other, int factor) {
        if (isEmpty() || other.isEmpty()) {
            return 0.0;
        }
        return computeSimilarity(this.bigramHashes, factor, other.bigramHashes, false);
    }

    public double partialSimilarity(SourceRepresentation other) {
        if (isEmpty() || other.isEmpty()) {
            return 0.0;
        }
        return computeSimilarity(this.bigramHashes, 1, other.bigramHashes, true);
    }
    
    public SourceRepresentation minus(SourceRepresentation other) {
        return new SourceRepresentation(linesCount, computeMinus(bigramHashes, other.bigramHashes), debug);
    }
    
//    public int linesCount() {
//        return linesCount;
//    }
    
    public boolean isEmpty() {
        return bigramHashes.length == 0;
    }
    
    private static double computeSimilarity(long[] sPairs, int sFactor, long[] tPairs, boolean partialSimilarity) {
        int n = sPairs.length * sFactor;
        int m = tPairs.length;

        int matches = 0, i = 0, j = 0;
        while (i < n && j < m) {
            if (sPairs[i / sFactor] == tPairs[j]) {
                matches += 1;
                i++;
                j++;
            } else if (sPairs[i / sFactor] < tPairs[j])
                i++;
            else
                j++;
        }
        if (partialSimilarity) {
            return (double) (matches) / n;
        }
        return (double) (2 * matches) / (n + m);
    }
    
    private static long[] computeMinus(long[] sPairs, long[] tPairs) {
        int n = sPairs.length;
        int m = tPairs.length;

        int matches = 0, i = 0, j = 0;
        while (i < n && j < m) {
            if (sPairs[i] == tPairs[j]) {
                matches += 1;
                i++;
                j++;
            } else if (sPairs[i] < tPairs[j]) {
                i++;
            } else {
                j++;
            }
        }
        long[] result = new long[n - matches];
        i = 0; j = 0;
        int k = 0;
        while (i < n && j < m) {
            if (sPairs[i] == tPairs[j]) {
                i++;
                j++;
            } else if (sPairs[i] < tPairs[j]) {
                result[k++] = sPairs[i];
                i++;
            } else {
                j++;
            }
        }
        while (i < n) {
            result[k++] = sPairs[i];
            i++;
        }
        return result;
    }
    
    @Override
    public String toString() {
        if (debug == null) {
            return Arrays.toString(bigramHashes);
        } else {
            StringBuilder sb = new StringBuilder();
            for (long bigram : bigramHashes) {
                int h1 = (int) ((bigram >> 32) & 0x00000000FFFFFFFFL);
                int h2 = (int) (bigram & 0x00000000FFFFFFFFL);
                sb.append(debug.get(h1));
                sb.append(' ');
                sb.append(debug.get(h2));
                sb.append('\n');
            }
            return sb.toString();
        }
    }
}
