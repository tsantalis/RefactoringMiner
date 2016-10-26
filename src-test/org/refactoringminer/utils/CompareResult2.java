package org.refactoringminer.utils;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.refactoringminer.api.RefactoringType;

public class CompareResult2<T extends Comparable<T>> {

    private final String project;
    private final String revision;
    private final Set<T> expectedRefactorings;
    private final Set<T> allRefactorings = new HashSet<>();
    private final Map<String, Group> groups = new HashMap<>();

    public CompareResult2(String project, String revision, Set<T> expectedRefactorings) {
        this.project = project;
        this.revision = revision;
        this.expectedRefactorings = expectedRefactorings;
    }

    public void addGroup(String groupId, Set<T> actualRefactorings) {
        Group group = new Group(actualRefactorings);
        groups.put(groupId, group);
        allRefactorings.addAll(actualRefactorings);
    }

    class Group {
        private final Set<String> truePositives;
        private final Set<String> falsePositives;
        private final Set<String> falseNegatives;

        public Group(Set<T> actualRefactorings) {
            this.truePositives = new HashSet<>();
            this.falsePositives = new HashSet<>();
            this.falseNegatives = new HashSet<>();

            for (Object r : actualRefactorings) {
                if (expectedRefactorings.contains(r)) {
                    addTruePositive(r);
                } else {
                    addFalsePositive(r);
                }
            }
            for (Object r : expectedRefactorings) {
                if (!actualRefactorings.contains(r)) {
                    addFalseNegative(r);
                }
            }
        }

        private void addTruePositive(Object r) {
            this.truePositives.add(r.toString());
        }

        private void addFalsePositive(Object r) {
            this.falsePositives.add(r.toString());
        }

        private void addFalseNegative(Object r) {
            this.falseNegatives.add(r.toString());
        }

        public int getTPCount() {
            return this.truePositives.size();
        }

        public int getFPCount() {
            return this.falsePositives.size();
        }

        public int getFNCount() {
            return this.falseNegatives.size();
        }

        public int getTPCount(RefactoringType rt) {
            return (int) this.truePositives.stream().filter(r -> r.startsWith(rt.getDisplayName())).count();
        }

        public int getFPCount(RefactoringType rt) {
            return (int) this.falsePositives.stream().filter(r -> r.startsWith(rt.getDisplayName())).count();
        }

        public int getFNCount(RefactoringType rt) {
            return (int) this.falseNegatives.stream().filter(r -> r.startsWith(rt.getDisplayName())).count();
        }
    }

    public void printReport(PrintStream out, String groupId, boolean verbose) {
        String baseUrl = project.substring(0, project.length() - 4) + "/commit/";
        String commitUrl = baseUrl + revision;
        Group group = groups.get(groupId);
        int tp = group.truePositives.size();
        int fp = group.falsePositives.size();
        int fn = group.falseNegatives.size();

        double precision = ((double) tp / (tp + fp));
        double recall = ((double) tp) / (tp + fn);
        out.println("at " + commitUrl);
        out.println(String.format("  TP: %d  FP: %d  FN: %d  Prec.: %.3f  Recall: %.3f", tp, fp, fn, precision, recall));

        if (verbose && (group.falsePositives.size() + group.falseNegatives.size()) > 0) {
            if (!group.truePositives.isEmpty()) {
                out.println(" true positives");
                group.truePositives.stream().sorted().forEach(r -> out.println("  " + r));
            }
            if (!group.falsePositives.isEmpty()) {
                out.println(" false positives");
                group.falsePositives.stream().sorted().forEach(r -> out.println("  " + r));
            }
            if (!group.falseNegatives.isEmpty()) {
                out.println(" false negatives");
                group.falseNegatives.stream().sorted().forEach(r -> out.println("  " + r));
            }
        }
    }

    public void printDetails(PrintStream out) {
        String baseUrl = project.substring(0, project.length() - 4) + "/commit/";
        String commitUrl = baseUrl + revision;
        
        Set<String> groupKeys = groups.keySet();
        // TODO
    }
}
