package org.refactoringminer.rm2.analysis.codesimilarity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.refactoringminer.rm2.analysis.SourceRepresentationBuilder;
import org.refactoringminer.rm2.model.Multiset;

class TokenIdfSRBuilder implements SourceRepresentationBuilder {

    IScanner scanner = ToolFactory.createScanner(true, true, false, "1.8");
    Map<String, Integer> df = new HashMap<String, Integer>();
    int dc = 0;
    
    private static final int TOKENS = 0;
    private static final int LINES = 1;

//    private void countDf(Map<Integer, String> debug) {
//        dc++;
//        for (String token : debug.values()) {
//            if (df.containsKey(token)) {
//                df.put(token, df.get(token) + 1);
//            } else {
//                df.put(token, 1);
//            }
//        }
//    }
    
    private void countDf(List<String> debug) {
        dc++;
        HashSet<String> tokens = new HashSet<String>(debug);
        for (String token : tokens) {
            if (df.containsKey(token)) {
                df.put(token, df.get(token) + 1);
            } else {
                df.put(token, 1);
            }
        }
    }

    public double idf(String key) {
        if (key == null) {
            throw new NullPointerException("key cannot be null");
        }
        return Math.log(((double) dc)/df.get(key));
    }

    @Override
    public void onComplete() {
        System.out.println(String.format("Vocabulary size: %d", df.size()));
        df.entrySet().stream().sorted(Map.Entry.comparingByValue()).forEachOrdered(entry -> {
            double idf = Math.log(((double) dc)/entry.getValue());
            System.out.println(String.format("%d %.2f %s", entry.getValue(), idf, entry.getKey()));
        });
    }

    @Override
    public TokenIdfSR buildSourceRepresentationForType(char[] charArray, int start, int length) {
        return getTokenBasedSourceRepresentation(charArray, start, length, true);
    }

    @Override
    public TokenIdfSR buildSourceRepresentationForMethodBody(char[] charArray, int start, int length) {
        return getTokenBasedSourceRepresentation(charArray, start, length, true);
    }

    @Override
    public TokenIdfSR buildSourceRepresentationForStatement(char[] charArray, int start, int length) {
        return getTokenBasedSourceRepresentation(charArray, start, length, false);
    }

    @Override
    public TokenIdfSR buildSourceRepresentationForExpression(char[] charArray, int start, int length) {
        return getTokenBasedSourceRepresentation(charArray, start, length, false);
    }

    @Override
    public TokenIdfSR buildEmptySourceRepresentation() {
        return new TokenIdfSR(new Multiset<String>(), this);
    }

    private TokenIdfSR getTokenBasedSourceRepresentation(char[] charArray, int start, int length, boolean count) {
        List<String> debug = new ArrayList<String>();
        List<Integer> tokens = computeHashes(charArray, start, length, TOKENS, debug);
        if (count) countDf(debug);
        Multiset<String> multiset = new Multiset<String>();
        multiset.addAll(debug);
        return new TokenIdfSR(multiset, this);
    }

    private List<Integer> computeHashes(char[] charArray, int start, int length, int granularity, List<String> debug) {
        try {
            scanner.setSource(charArray);
            scanner.resetTo(start, start + length - 1);

            List<Integer> hashes = new ArrayList<Integer>();
            int token;
            int h = 0;
            int currentStart = start;
            int currentEnd = start - 1;
            while ((token = scanner.getNextToken()) != ITerminalSymbols.TokenNameEOF) {
                int tokenStart = scanner.getCurrentTokenStartPosition();
                int tokenEnd = scanner.getCurrentTokenEndPosition();
                if (h == 0) {
                    currentStart = tokenStart;
                }
                if (token != ITerminalSymbols.TokenNameWHITESPACE) {
                    h = hash(h, charArray, tokenStart, tokenEnd);
                    currentEnd = tokenEnd;
                    if (granularity == TOKENS) {
                        hashes.add(h);
                        if (debug != null)
                            debug.add(new String(charArray, currentStart, currentEnd - currentStart + 1));
                        h = 0;
                    }
                } else {
                    if (granularity == LINES && indexOf(charArray, tokenStart, tokenEnd, '\n') != -1 && h != 0) {
                        hashes.add(h);
                        if (debug != null)
                            debug.add(new String(charArray, currentStart, currentEnd - currentStart + 1));
                        h = 0;
                    }
                }
            }
            if (granularity == LINES && h != 0) {
                hashes.add(h);
                if (debug != null)
                    debug.add(new String(charArray, currentStart, currentEnd - currentStart + 1));
                h = 0;
            }

            return hashes;
        } catch (InvalidInputException e) {
            throw new RuntimeException(e);
        }
    }

    private static long[] computeBigrams(List<Integer> hashes) {
        int n = hashes.size();
        long[] bigrams = new long[n];
        if (n > 0) {
            bigrams[0] = 0L | hashes.get(0) & 0xFFFFFFFFL;
            for (int i = 1; i < n; i++) {
                int h1 = hashes.get(i - 1);
                int h2 = hashes.get(i);
                long bh = (long) h1 << 32 | h2 & 0xFFFFFFFFL;
                bigrams[i] = bh;
            }
            Arrays.sort(bigrams);
        }
        return bigrams;
    }

    private static int hash(int h, char[] charArray, int tokenStart, int tokenEnd) {
        if (tokenEnd >= tokenStart) {
            for (int i = tokenStart; i <= tokenEnd; i++) {
                h = 31 * h + charArray[i];
            }
        }
        return h;
    }

    private static int indexOf(char[] charArray, int start, int end, char c) {
        for (int i = start; i <= end; i++) {
            if (charArray[i] == c) {
                return i;
            }
        }
        return -1;
    }
}
