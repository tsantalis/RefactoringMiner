package org.refactoringminer.utils;

import java.io.PrintStream;

import org.refactoringminer.api.RefactoringType;

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

  public int getTPCount(RefactoringType rt) {
      int count = 0;
      for (int i = 0; i < results.length; i++) {
          count += results[i].getTPCount(rt);
      }
      return count;
  }
  
  public int getFPCount(RefactoringType rt) {
      int count = 0;
      for (int i = 0; i < results.length; i++) {
          count += results[i].getFPCount(rt);
      }
      return count;
  }
  
  public int getFNCount(RefactoringType rt) {
      int count = 0;
      for (int i = 0; i < results.length; i++) {
          count += results[i].getFNCount(rt);
      }
      return count;
  }

  public void printReport(PrintStream out, boolean verbose) {
    out.println("Total  " + getResultLine(getTPCount(), getFPCount(), getFNCount()));
    
    for (RefactoringType refType : RefactoringType.values()) {
        int tpRt = getTPCount(refType);
        int fpRt = getFPCount(refType);
        int fnRt = getFNCount(refType);
        if (tpRt > 0 || fpRt > 0 || fnRt > 0) {
            out.println(String.format("%-7s" + getResultLine(tpRt, fpRt, fnRt), refType.getAbbreviation()));
        }
    }
    
    for (CompareResult cr : results) {
      out.println();
      cr.printReport(out, verbose);
    }
  }

  private String getResultLine(int tp, int fp, int fn) {
      double precision = ((double) tp / (tp + fp));
      double recall = ((double) tp) / (tp + fn);
      return String.format("TP: %3d  FP: %3d  FN: %3d  Prec.: %.3f  Recall: %.3f", tp, fp, fn, precision, recall);
  }

}
