package org.refactoringminer.utils;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

public class CompareResult {

  private final String project;
  private final String revision;
  private final Set<RefactoringRelationship> truePositives;
  private final Set<RefactoringRelationship> falsePositives;
  private final Set<RefactoringRelationship> falseNegatives;
  
  public CompareResult(String project, String revision) {
    this.project = project;
    this.revision = revision;
    this.truePositives = new HashSet<>();
    this.falsePositives = new HashSet<>();
    this.falseNegatives = new HashSet<>();
  }
  
  public Set<RefactoringRelationship> getFalsePositives() {
    return falsePositives;
  }

  public Set<RefactoringRelationship> getFalseNegatives() {
    return falseNegatives;
  }

  public void addTruePositive(RefactoringRelationship r) {
    this.truePositives.add(r);
  }
  
  public void addFalsePositive(RefactoringRelationship r) {
    this.falsePositives.add(r);
  }
  
  public void addFalseNegative(RefactoringRelationship r) {
    this.falseNegatives.add(r);
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
    out.println(String.format("TP: %d  FP: %d  FN: %d  Prec.: %.3f  Recall: %.3f", tp, fp, fn, precision, recall));
    
    if (verbose && (falsePositives.size() + falseNegatives.size()) > 0) {
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
