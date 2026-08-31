package org.refactoringminer.astDiff.graph.cluster.traverse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Splitter {
    public static final int THRESHOLD = 200;

    public static List<List<Integer>> createBalancedSplits(List<String> elements) {
        int totalLines = elements.stream().mapToInt(Splitter::lineCount).sum();
        if (totalLines <= THRESHOLD) {
            List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < elements.size(); i++) {
                indices.add(i);
            }
            return List.of(indices);
        }

        int n = (int) Math.ceil((double) totalLines / THRESHOLD);
        n = Math.min(n, elements.size());

        return splitIntoN(elements, n);
    }

    private static List<List<Integer>> splitIntoN(List<String> elements, int n) {
        if (elements == null || elements.isEmpty()) return Collections.emptyList();
        if (n <= 1) {
            List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < elements.size(); i++) {
                indices.add(i);
            }
            return List.of(indices);
        };

        int low = 0;
        int high = 0;
        for (String e : elements) {
            low = Math.max(low, lineCount(e));
            high += lineCount(e);
        }

        int optimalMaxSum = high;
        while (low <= high) {
            int mid = low + (high - low) / 2;
            if (canSplitIntoN(elements, n, mid)) {
                optimalMaxSum = mid;
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }

        List<List<Integer>> result = new ArrayList<>();
        List<Integer> currentSplit = new ArrayList<>();
        int currentSum = 0;

        for (int i = 0; i < elements.size(); i++) {
            String e = elements.get(i);

            if (!currentSplit.isEmpty() && currentSum + lineCount(e) > optimalMaxSum) {
                result.add(currentSplit);
                currentSplit = new ArrayList<>();
                currentSum = 0;
            }
            currentSplit.add(i);
            currentSum += lineCount(e);
        }
        if (!currentSplit.isEmpty()) {
            result.add(currentSplit);
        }

        while (result.size() < n) {
            int bestSplitIdx = -1;
            int maxElements = 0;
            for (int i = 0; i < result.size(); i++) {
                if (result.get(i).size() > maxElements && result.get(i).size() > 1) {
                    maxElements = result.get(i).size();
                    bestSplitIdx = i;
                }
            }

            if (bestSplitIdx == -1) break;

            List<Integer> toSplit = result.remove(bestSplitIdx);
            int mid = toSplit.size() / 2;
            result.add(bestSplitIdx, new ArrayList<>(toSplit.subList(0, mid)));
            result.add(bestSplitIdx + 1, new ArrayList<>(toSplit.subList(mid, toSplit.size())));
        }

        return result;
    }

    private static boolean canSplitIntoN(List<String> elements, int n, int maxSum) {
        int count = 1;
        int currentSum = 0;
        for (String e : elements) {
            if (currentSum + lineCount(e) > maxSum) {
                count++;
                currentSum = lineCount(e);
                if (count > n) return false;
            } else {
                currentSum += lineCount(e);
            }
        }
        return true;
    }

    private static int lineCount(String element) {
        return element.split("\n").length;
    }
}
