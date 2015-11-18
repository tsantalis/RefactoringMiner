package br.ufmg.dcc.labsoft.refdetector.model.builder;


public class SourceRepresentation {

    private final int linesCount;
    private final long[] bigramHashes;
    
    public SourceRepresentation(int linesCount, long[] bigramHashes) {
        this.linesCount = linesCount;
        this.bigramHashes = bigramHashes;
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
    
    public int linesCount() {
        return linesCount;
    }
    
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
}
