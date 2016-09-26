package org.refactoringminer.utils;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

import org.refactoringminer.api.RefactoringType;

public class CompareResult {

  private final String project;
  private final String revision;
  private final Set<String> truePositives;
  private final Set<String> falsePositives;
  private final Set<String> falseNegatives;
  
  public CompareResult(String project, String revision, Set<?> expectedRefactorings, Set<?> actualRefactorings) {
    this.project = project;
    this.revision = revision;
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

  public void printReport(PrintStream out, boolean verbose) {
    String baseUrl = project.substring(0, project.length() - 4) + "/commit/";
    String commitUrl = baseUrl + revision;
    int tp = truePositives.size();
    int fp = falsePositives.size();
    int fn = falseNegatives.size();
    
    double precision = ((double) tp / (tp + fp));
    double recall = ((double) tp) / (tp + fn);
    out.println("at " + commitUrl);
    out.println(String.format("  TP: %d  FP: %d  FN: %d  Prec.: %.3f  Recall: %.3f", tp, fp, fn, precision, recall));
    
    if (verbose && (falsePositives.size() + falseNegatives.size()) > 0) {
      if (!truePositives.isEmpty()) {
        out.println(" true positives");
        truePositives.stream().sorted().forEach(r ->
          out.println("  " + r)
        );
      }
      if (!falsePositives.isEmpty()) {
        out.println(" false positives");
        falsePositives.stream().sorted().forEach(r ->
          out.println("  " + r)
        );
      }
      if (!falseNegatives.isEmpty()) {
        out.println(" false negatives");
        falseNegatives.stream().sorted().forEach(r ->
          out.println("  " + r)
        );
      }
    }
  }
  
}
