package org.refactoringminer.utils;

import static org.refactoringminer.util.RefactoringRelationship.parentOf;

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
import org.refactoringminer.util.RefactoringRelationship;

public class ResultComparator {

    Set<String> groupIds = new LinkedHashSet<>();
    Map<String, RefactoringSet> expectedMap = new LinkedHashMap<>();
    Map<String, RefactoringSet> notExpectedMap = new LinkedHashMap<>();
    Map<String, RefactoringSet> resultMap = new HashMap<>();

    private boolean groupRefactorings;
    private boolean ignoreMethodParams;

    private boolean ignorePullUpToExtractedSupertype = true;
    private boolean ignoreMoveToMovedType = false;
    private boolean ignoreMoveToRenamedType = false;

    public ResultComparator(boolean groupRefactorings, boolean ignoreMethodParams) {
        this.groupRefactorings = groupRefactorings;
        this.ignoreMethodParams = ignoreMethodParams;
    }

    public ResultComparator() {
        this(false, false);
    }

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

    
    
    public void printSummary(PrintStream out, EnumSet<RefactoringType> refTypesToConsider) {
        for (String groupId : groupIds) {
            CompareResult r = getCompareResult(groupId, refTypesToConsider);
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

    public CompareResult getCompareResult(String groupId, EnumSet<RefactoringType> refTypesToConsider) {
        Set<Object> truePositives = new HashSet<>();
        Set<Object> falsePositives = new HashSet<>();
        Set<Object> falseNegatives = new HashSet<>();

        EnumSet<RefactoringType> ignore = EnumSet.complementOf(refTypesToConsider);
        
        for (RefactoringSet expected : expectedMap.values()) {
            RefactoringSet actual = resultMap.get(getResultId(expected.getProject(), expected.getRevision(), groupId));
            if (actual != null) {
                Set<RefactoringRelationship> expectedRefactorings = expected.ignoring(ignore).ignoringMethodParameters(ignoreMethodParams).getRefactorings();
                Set<RefactoringRelationship> actualRefactorings = actual.ignoring(ignore).ignoringMethodParameters(ignoreMethodParams).getRefactorings();
                Set<RefactoringRelationship> expectedUnfiltered = expected.ignoringMethodParameters(ignoreMethodParams).getRefactorings();
                for (RefactoringRelationship r : actualRefactorings) {
                    if (expectedRefactorings.contains(r)) {
                        truePositives.add(r);
                        expectedRefactorings.remove(r);
                    } else {
                        boolean ignoreFp = 
                            ignoreMoveToMovedType && isMoveToMovedType(r, expectedUnfiltered) ||
                            ignoreMoveToRenamedType && isMoveToRenamedType(r, expectedUnfiltered) ||
                            ignorePullUpToExtractedSupertype && isPullUpToExtractedSupertype(r, expectedUnfiltered);
                        if (!ignoreFp) {
                            falsePositives.add(r);
                        }
                    }
                }
                for (Object r : expectedRefactorings) {
                    falseNegatives.add(r);
                }
            }
        }
        return new CompareResult(truePositives, falsePositives, falseNegatives);
    }

    private String getResultLine(int tp, int fp, int fn) {
        double precision = getPrecision(tp, fp, fn);
        double recall = getRecall(tp, fp, fn);
        double f1 = getF1(tp, fp, fn);
        //return String.format("& %3d & %3d & %3d & %3d & %.3f & %.3f \\", tp + fn, tp, fp, fn, precision, recall);
        return String.format("#: %3d  TP: %3d  FP: %3d  FN: %3d  Prec.: %.3f  Recall: %.3f  F1: %.3f", tp + fn, tp, fp, fn, precision, recall, f1);
    }

    private static double getPrecision(int tp, int fp, int fn) {
        return tp == 0 ? 0.0 : ((double) tp / (tp + fp));
    }

    private static double getRecall(int tp, int fp, int fn) {
        return tp == 0 ? 0.0 : ((double) tp) / (tp + fn);
    }

    private static double getF1(int tp, int fp, int fn) {
        double precision = ResultComparator.getPrecision(tp, fp, fn);
        double recall = ResultComparator.getRecall(tp, fp, fn);
        return tp == 0 ? 0.0 : 2.0 * precision * recall / (precision + recall);
    }

    public void printDetails(PrintStream out, EnumSet<RefactoringType> refTypesToConsider) {
        String[] labels = {"TN", "FP", "FN", "TP"};
        EnumSet<RefactoringType> ignore = EnumSet.complementOf(refTypesToConsider);
        boolean headerPrinted = false;
        for (RefactoringSet expected : expectedMap.values()) {
            Set<RefactoringRelationship> all = new HashSet<>();
            Set<RefactoringRelationship> expectedRefactorings = expected.ignoring(ignore).ignoringMethodParameters(ignoreMethodParams).getRefactorings();
            Set<RefactoringRelationship> expectedUnfiltered = expected.getRefactorings();
            all.addAll(expectedRefactorings); //

            StringBuilder header = new StringBuilder("Ref Type\tEntity before\tEntity after");
            for (String groupId : groupIds) {
                header.append('\t');
                header.append(groupId);
                RefactoringSet actual = resultMap.get(getResultId(expected.getProject(), expected.getRevision(), groupId));
                if (actual != null) {
                    all.addAll(actual.ignoring(ignore).ignoringMethodParameters(ignoreMethodParams).getRefactorings()); //
                }
            }
            if (!headerPrinted) {
                out.println(header.toString());
                headerPrinted = true;
            }
            if (!all.isEmpty()) {
                out.println(getProjectRevisionId(expected.getProject(), expected.getRevision()));
                ArrayList<RefactoringRelationship> allList = new ArrayList<>();
                allList.addAll(all);
                Collections.sort(allList);
                for (RefactoringRelationship r : allList) {
                    out.print(r.toString());
                    for (String groupId : groupIds) {
                        RefactoringSet actual = resultMap.get(getResultId(expected.getProject(), expected.getRevision(), groupId));
                        out.print('\t');
                        if (actual != null) {
                            Set<RefactoringRelationship> actualRefactorings = actual.ignoring(ignore).ignoringMethodParameters(ignoreMethodParams).getRefactorings();
                            int correct = expectedRefactorings.contains(r) ? 2 : 0;
                            int found = actualRefactorings.contains(r) ? 1 : 0;
                            String label = labels[correct + found];
                            out.print(label);
                            if (label == "FP" && isPullUpToExtractedSupertype(r, expectedUnfiltered)) {
                                out.print("<ES>");
                            }
                            if (label == "FP" && isMoveToRenamedType(r, expectedUnfiltered)) {
                                out.print("<RT>");
                            }
                            if (label == "FP" && isMoveToMovedType(r, expectedUnfiltered)) {
                                out.print("<MT>");
                            }
                            if (label == "FP" && (r.getRefactoringType() == RefactoringType.MOVE_ATTRIBUTE || r.getRefactoringType() == RefactoringType.MOVE_OPERATION)) {
                                if (expectedUnfiltered.contains(new RefactoringRelationship(RefactoringType.EXTRACT_SUPERCLASS, parentOf(r.getEntityBefore()), parentOf(r.getEntityAfter())))) {
                                    out.print("<ES>");
                                }
                                if (expectedUnfiltered.contains(new RefactoringRelationship(RefactoringType.EXTRACT_INTERFACE, parentOf(r.getEntityBefore()), parentOf(r.getEntityAfter())))) {
                                    out.print("<ES>");
                                }
                                
                                if (expectedUnfiltered.contains(new RefactoringRelationship(RefactoringType.PULL_UP_ATTRIBUTE, (r.getEntityBefore()), (r.getEntityAfter())))) {
                                    out.print("<PUF>");
                                }
                                if (expectedUnfiltered.contains(new RefactoringRelationship(RefactoringType.PUSH_DOWN_ATTRIBUTE, (r.getEntityBefore()), (r.getEntityAfter())))) {
                                    out.print("<PDF>");
                                }
                                if (expectedUnfiltered.contains(new RefactoringRelationship(RefactoringType.PULL_UP_OPERATION, (r.getEntityBefore()), (r.getEntityAfter())))) {
                                    out.print("<PUM>");
                                }
                                if (expectedUnfiltered.contains(new RefactoringRelationship(RefactoringType.PUSH_DOWN_OPERATION, (r.getEntityBefore()), (r.getEntityAfter())))) {
                                    out.print("<PDM>");
                                }
                            }
                        }
                    }
                    out.println();
                }
            }
        }
        out.println();
    }

    private boolean isPullUpToExtractedSupertype(RefactoringRelationship r, Set<RefactoringRelationship> expectedUnfiltered) {
        if (r.getRefactoringType() == RefactoringType.PULL_UP_ATTRIBUTE || r.getRefactoringType() == RefactoringType.PULL_UP_OPERATION) {
            if (expectedUnfiltered.contains(new RefactoringRelationship(RefactoringType.EXTRACT_SUPERCLASS, parentOf(r.getEntityBefore()), parentOf(r.getEntityAfter())))) {
                return true;
            }
            if (expectedUnfiltered.contains(new RefactoringRelationship(RefactoringType.EXTRACT_INTERFACE, parentOf(r.getEntityBefore()), parentOf(r.getEntityAfter())))) {
                return true;
            }
        }
        return false;
    }

    private boolean isMoveToRenamedType(RefactoringRelationship r, Set<RefactoringRelationship> expectedUnfiltered) {
        if (r.getRefactoringType() == RefactoringType.MOVE_OPERATION || r.getRefactoringType() == RefactoringType.MOVE_ATTRIBUTE) {
            if (expectedUnfiltered.contains(new RefactoringRelationship(RefactoringType.RENAME_CLASS, parentOf(r.getEntityBefore()), parentOf(r.getEntityAfter())))) {
                return true;
            }
            if (expectedUnfiltered.contains(new RefactoringRelationship(RefactoringType.RENAME_CLASS, parentOf(parentOf(r.getEntityBefore())), parentOf(parentOf(r.getEntityAfter()))))) {
                return true;
            }
        }
        return false;
    }
    
    private boolean isMoveToMovedType(RefactoringRelationship r, Set<?> expectedUnfiltered) {
        if (r.getRefactoringType() == RefactoringType.MOVE_OPERATION || r.getRefactoringType() == RefactoringType.MOVE_ATTRIBUTE) {
            if (expectedUnfiltered.contains(new RefactoringRelationship(RefactoringType.MOVE_CLASS, parentOf(r.getEntityBefore()), parentOf(r.getEntityAfter())))) {
                return true;
            }
            if (expectedUnfiltered.contains(new RefactoringRelationship(RefactoringType.MOVE_CLASS, parentOf(parentOf(r.getEntityBefore())), parentOf(parentOf(r.getEntityAfter()))))) {
                return true;
            }
            if (expectedUnfiltered.contains(new RefactoringRelationship(RefactoringType.MOVE_SOURCE_FOLDER, parentOf(r.getEntityBefore()), parentOf(r.getEntityAfter())))) {
                return true;
            }
        }
        return false;
    }
    
    private String getProjectRevisionId(String project, String revision) {
        return project.substring(0, project.length() - 4) + "/commit/" + revision;
    }

    private String getResultId(String project, String revision, String groupId) {
        return project.substring(0, project.length() - 4) + "/commit/" + revision + ";" + groupId;
    }

    public static class CompareResult {
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

        public double getPrecision() {
            int tp = this.truePositives.size();
            int fp = this.falsePositives.size();
            int fn = this.falseNegatives.size();
            return ResultComparator.getPrecision(tp, fp, fn);
        }
        
        public double getRecall() {
            int tp = this.truePositives.size();
            int fp = this.falsePositives.size();
            int fn = this.falseNegatives.size();
            return ResultComparator.getRecall(tp, fp, fn);
        }
        
        public double getF1() {
            int tp = this.truePositives.size();
            int fp = this.falsePositives.size();
            int fn = this.falseNegatives.size();
            return ResultComparator.getF1(tp, fp, fn);
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
        File cachedResult = new File(resultCacheDir + "/" + rm.getConfigId() + "-" + projectName + "-" + commitId);
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

    public boolean isGroupRefactorings() {
        return groupRefactorings;
    }

    public void setGroupRefactorings(boolean groupRefactorings) {
        this.groupRefactorings = groupRefactorings;
    }

    public boolean isIgnoreMethodParams() {
        return ignoreMethodParams;
    }

    public void setIgnoreMethodParams(boolean ignoreMethodParams) {
        this.ignoreMethodParams = ignoreMethodParams;
    }

    public boolean isIgnorePullUpToExtractedSupertype() {
        return ignorePullUpToExtractedSupertype;
    }

    public void setIgnorePullUpToExtractedSupertype(boolean ignorePullUpToExtractedSupertype) {
        this.ignorePullUpToExtractedSupertype = ignorePullUpToExtractedSupertype;
    }

    public boolean isIgnoreMoveToMovedType() {
        return ignoreMoveToMovedType;
    }

    public void setIgnoreMoveToMovedType(boolean ignoreMoveToMovedType) {
        this.ignoreMoveToMovedType = ignoreMoveToMovedType;
    }

    public boolean isIgnoreMoveToRenamedType() {
        return ignoreMoveToRenamedType;
    }

    public void setIgnoreMoveToRenamedType(boolean ignoreMoveToRenamedType) {
        this.ignoreMoveToRenamedType = ignoreMoveToRenamedType;
    }

}
