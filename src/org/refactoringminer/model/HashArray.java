package org.refactoringminer.model;


public class HashArray {

    protected final long[] hashes;
    
    public HashArray(long[] hashes) {
        this.hashes = hashes;
    }

    public double similarity(HashArray other) {
        return similarity(other, 1);
    }
    
    public double similarity(HashArray other, int factor) {
        if (isEmpty() || other.isEmpty()) {
            return 0.0;
        }
        return computeSimilarity(this.hashes, factor, other.hashes, false);
    }

    public double partialSimilarity(HashArray other) {
        if (isEmpty() || other.isEmpty()) {
            return 0.0;
        }
        return computeSimilarity(this.hashes, 1, other.hashes, true);
    }
    
//    public HashArray minus(HashArray other) {
//        return new HashArray(computeMinus(hashes, other.hashes));
//    }
    
    public boolean isEmpty() {
        return hashes.length == 0;
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
    
    protected static long[] computeMinus(long[] sHashes, long[] tHashes) {
        int n = sHashes.length;
        int m = tHashes.length;

        int matches = 0, i = 0, j = 0;
        while (i < n && j < m) {
            if (sHashes[i] == tHashes[j]) {
                matches += 1;
                i++;
                j++;
            } else if (sHashes[i] < tHashes[j]) {
                i++;
            } else {
                j++;
            }
        }
        long[] result = new long[n - matches];
        i = 0; j = 0;
        int k = 0;
        while (i < n && j < m) {
            if (sHashes[i] == tHashes[j]) {
                i++;
                j++;
            } else if (sHashes[i] < tHashes[j]) {
                result[k++] = sHashes[i];
                i++;
            } else {
                j++;
            }
        }
        while (i < n) {
            result[k++] = sHashes[i];
            i++;
        }
        return result;
    }
    
    protected long get(int i) {
        return this.hashes[i];
    }

    protected int getHigh(int i) {
        return (int) ((this.hashes[i] >> 32) & 0x00000000FFFFFFFFL);
    }

    protected int getLow(int i) {
        return (int) (this.hashes[i] & 0x00000000FFFFFFFFL);
    }
    
}
