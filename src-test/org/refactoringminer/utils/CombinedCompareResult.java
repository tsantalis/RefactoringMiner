package org.refactoringminer.utils;

import java.io.PrintStream;

public class CombinedCompareResult {

  private final CompareResult[] results;
  
  public CombinedCompareResult(CompareResult ... results) {
    this.results = results;
  }

  public int getTPCount() {
    int count = 0;
    for (int i = 0; i < results.length; i++) {
      count += results[i].getTPCount();
    }
    return count;
  }

  public int getFPCount() {
    int count = 0;
    for (int i = 0; i < results.length; i++) {
      count += results[i].getFPCount();
    }
    return count;
  }

  public int getFNCount() {
    int count = 0;
    for (int i = 0; i < results.length; i++) {
      count += results[i].getFNCount();
    }
    return count;
  }

  public void printReport(PrintStream out, boolean verbose) {
    int tp = getTPCount();
    int fp = getFPCount();
    int fn = getFNCount();
    double precision = ((double) tp / (tp + fp));
    double recall = ((double) tp) / (tp + fn);
    out.println("Total");
    out.println(String.format("  TP: %d  FP: %d  FN: %d  Prec.: %.3f  Recall: %.3f", tp, fp, fn, precision, recall));
    
    for (CompareResult cr : results) {
      cr.printReport(out, verbose);
    }
  }

}
