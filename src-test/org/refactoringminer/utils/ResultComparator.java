package org.refactoringminer.utils;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jgit.lib.Repository;
import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.api.GitService;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.util.GitServiceImpl;

public class ResultComparator {

    Set<String> groupIds = new LinkedHashSet<>();
    Map<String, RefactoringSet> expectedMap = new LinkedHashMap<>();
    Map<String, RefactoringSet> notExpectedMap = new LinkedHashMap<>();
    Map<String, RefactoringSet> resultMap = new HashMap<>();

    public ResultComparator expect(RefactoringSet ... sets) {
        for (RefactoringSet set : sets) {
            expectedMap.put(getProjectRevisionId(set.getProject(), set.getRevision()), set);
        }
        return this;
    }

    public ResultComparator dontExpect(RefactoringSet ... sets) {
        for (RefactoringSet set : sets) {
            notExpectedMap.put(getProjectRevisionId(set.getProject(), set.getRevision()), set);
        }
        return this;
    }

    public ResultComparator compareWith(String groupId, RefactoringSet ... actualArray) {
        for (RefactoringSet actual : actualArray) {
            groupIds.add(groupId);
            resultMap.put(getResultId(actual.getProject(), actual.getRevision(), groupId), actual);
        }
        return this;
    }

    public void printSummary(PrintStream out, boolean groupRefactorings, EnumSet<RefactoringType> refTypesToConsider) {
        for (String groupId : groupIds) {
            CompareResult r = getCombinedResult(groupId, groupRefactorings, refTypesToConsider);
            out.println("# " + groupId + " #");
            out.println("Total  " + getResultLine(r.getTPCount(), r.getFPCount(), r.getFNCount()));

            for (RefactoringType refType : refTypesToConsider) {
                int tpRt = r.getTPCount(refType);
                int fpRt = r.getFPCount(refType);
                int fnRt = r.getFNCount(refType);
                if (tpRt > 0 || fpRt > 0 || fnRt > 0) {
                    out.println(String.format("%-7s" + getResultLine(tpRt, fpRt, fnRt), refType.getAbbreviation()));
                }
            }
            out.println();
        }
        out.println();
    }

    private CompareResult getCombinedResult(String groupId, boolean groupRefactorings, EnumSet<RefactoringType> refTypesToConsider) {
        Set<Object> truePositives = new HashSet<>();
        Set<Object> falsePositives = new HashSet<>();
        Set<Object> falseNegatives = new HashSet<>();

        EnumSet<RefactoringType> ignore = EnumSet.complementOf(refTypesToConsider);
        
        for (RefactoringSet expected : expectedMap.values()) {
            RefactoringSet actual = resultMap.get(getResultId(expected.getProject(), expected.getRevision(), groupId));
            if (actual != null) {
                Set<?> expectedRefactorings;
                Set<?> actualRefactorings;
                if (groupRefactorings) {
                    expectedRefactorings = expected.ignoring(ignore).getRefactoringsGroups();
                    actualRefactorings = actual.ignoring(ignore).getRefactoringsGroups();
                } else {
                    expectedRefactorings = expected.ignoring(ignore).getRefactorings();
                    actualRefactorings = actual.ignoring(ignore).getRefactorings();
                }

                for (Object r : actualRefactorings) {
                    if (expectedRefactorings.contains(r)) {
                        truePositives.add(r);
                    } else {
                        falsePositives.add(r);
                    }
                }
                for (Object r : expectedRefactorings) {
                    if (!actualRefactorings.contains(r)) {
                        falseNegatives.add(r);
                    }
                }
            }
        }
        return new CompareResult(truePositives, falsePositives, falseNegatives);
    }

    private String getResultLine(int tp, int fp, int fn) {
        double precision = ((double) tp / (tp + fp));
        double recall = ((double) tp) / (tp + fn);
        return String.format("TP: %3d  FP: %3d  FN: %3d  Prec.: %.3f  Recall: %.3f", tp, fp, fn, precision, recall);
    }

    public void printDetails(PrintStream out, boolean groupRefactorings, EnumSet<RefactoringType> refTypesToConsider) {
        String[] labels = {"TN", "FP", "FN", "TP"};
        EnumSet<RefactoringType> ignore = EnumSet.complementOf(refTypesToConsider);
        for (RefactoringSet expected : expectedMap.values()) {
            out.println(getProjectRevisionId(expected.getProject(), expected.getRevision()));
            out.print("Ref Type\tEntity before\tEntity after");
            Set<RefactoringRelationship> all = new HashSet<>();
            Set<RefactoringRelationship> expectedRefactorings = expected.ignoring(ignore).getRefactorings();
            all.addAll(expectedRefactorings); //
            
            for (String groupId : groupIds) {
                out.print('\t');
                out.print(groupId);
                RefactoringSet actual = resultMap.get(getResultId(expected.getProject(), expected.getRevision(), groupId));
                if (actual != null) {
                    all.addAll(actual.ignoring(ignore).getRefactorings()); //
                }
            }
            out.println();
            ArrayList<RefactoringRelationship> allList = new ArrayList<>();
            allList.addAll(all);
            Collections.sort(allList);
            for (RefactoringRelationship r : allList) {
                out.print(r.toString());
                for (String groupId : groupIds) {
                    RefactoringSet actual = resultMap.get(getResultId(expected.getProject(), expected.getRevision(), groupId));
                    out.print('\t');
                    if (actual != null) {
                        Set<RefactoringRelationship> actualRefactorings = actual.ignoring(ignore).getRefactorings();
                        int correct = expectedRefactorings.contains(r) ? 2 : 0;
                        int found = actualRefactorings.contains(r) ? 1 : 0;
                        out.print(labels[correct + found]);
                    }
                }
                out.println();
            }
        }
        out.println();
    }

    private String getProjectRevisionId(String project, String revision) {
        return project.substring(0, project.length() - 4) + "/commit/" + revision;
    }

    private String getResultId(String project, String revision, String groupId) {
        return project.substring(0, project.length() - 4) + "/commit/" + revision + ";" + groupId;
    }

    private static class CompareResult {
        private final Set<Object> truePositives;
        private final Set<Object> falsePositives;
        private final Set<Object> falseNegatives;

        public CompareResult(Set<Object> truePositives, Set<Object> falsePositives, Set<Object> falseNegatives) {
            this.truePositives = truePositives;
            this.falsePositives = falsePositives;
            this.falseNegatives = falseNegatives;
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
            return (int) this.truePositives.stream().filter(r -> r.toString().startsWith(rt.getDisplayName())).count();
        }

        public int getFPCount(RefactoringType rt) {
            return (int) this.falsePositives.stream().filter(r -> r.toString().startsWith(rt.getDisplayName())).count();
        }

        public int getFNCount(RefactoringType rt) {
            return (int) this.falseNegatives.stream().filter(r -> r.toString().startsWith(rt.getDisplayName())).count();
        }
    }

    public static RefactoringSet collectRmResult(GitHistoryRefactoringMiner rm, String cloneUrl, String commitId) {
        GitService git = new GitServiceImpl();
        String tempDir = "tmp";
        String resultCacheDir = "tmpResult";
        String projectName = cloneUrl.substring(cloneUrl.lastIndexOf('/') + 1, cloneUrl.lastIndexOf('.'));
        File cachedResult = new File(resultCacheDir + "/" + projectName + "-" + commitId + "-" + rm.getConfigId());
        if (cachedResult.exists()) {
            RefactoringSet rs = new RefactoringSet(cloneUrl, commitId);
            rs.readFromFile(cachedResult);
            return rs;
        } else {
            String folder = tempDir + "/" + projectName;
            final RefactoringCollector rc = new RefactoringCollector(cloneUrl, commitId);
            try (Repository repo = git.cloneIfNotExists(folder, cloneUrl)) {
                rm.detectAtCommit(repo, commitId, rc);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            RefactoringSet rs = rc.assertAndGetResult();
            rs.saveToFile(cachedResult);
            return rs;
        }
    }

    public static RefactoringSet[] collectRmResult(GitHistoryRefactoringMiner rm, RefactoringSet[] oracle) {
        RefactoringSet[] result = new RefactoringSet[oracle.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = collectRmResult(rm, oracle[i].getProject(), oracle[i].getRevision());
        }
        return result;
    }

}
