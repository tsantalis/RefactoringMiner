package org.refactoringminer.rm2.analysis.codesimilarity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.refactoringminer.rm2.analysis.SourceRepresentationBuilder;

class TokenBigramsSRBuilder implements SourceRepresentationBuilder {

    IScanner scanner = ToolFactory.createScanner(true, true, false, "1.8");

    public static final int TOKENS = 0;
    public static final int LINES = 1;

    private final int tokenType;

    public TokenBigramsSRBuilder(int tokenType) {
        this.tokenType = tokenType;
    }

    @Override
    public TokenBigramsSR buildSourceRepresentation(char[] charArray, int start, int length) {
        Map<Integer, String> debug = new HashMap<Integer, String>();
        List<Integer> lines = computeHashes(charArray, start, length, tokenType, debug);
        return new TokenBigramsSR(computeBigrams(lines), debug);
    }

    @Override
    public TokenBigramsSR buildEmptySourceRepresentation() {
        return new TokenBigramsSR(new long[0]);
    }

    private List<Integer> computeHashes(char[] charArray, int start, int length, int granularity, Map<Integer, String> debug) {
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
                            debug.put(h, new String(charArray, currentStart, currentEnd - currentStart + 1));
                        h = 0;
                    }
                } else {
                    if (granularity == LINES && indexOf(charArray, tokenStart, tokenEnd, '\n') != -1 && h != 0) {
                        hashes.add(h);
                        if (debug != null)
                            debug.put(h, new String(charArray, currentStart, currentEnd - currentStart + 1));
                        h = 0;
                    }
                }
            }
            if (granularity == LINES && h != 0) {
                hashes.add(h);
                if (debug != null)
                    debug.put(h, new String(charArray, currentStart, currentEnd - currentStart + 1));
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
