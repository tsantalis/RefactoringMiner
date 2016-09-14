package org.refactoringminer.test;

import java.util.EnumSet;

import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.utils.Arqsoft16Dataset;
import org.refactoringminer.utils.CompareResult;
import org.refactoringminer.utils.RefactoringCrawlerResultReader;
import org.refactoringminer.utils.RefactoringSet;

public class TestWithArqsoft16Dataset {

  public static void main(String[] args) {
    Arqsoft16Dataset oracle = new Arqsoft16Dataset();
    EnumSet<RefactoringType> refTypesToIgnore = EnumSet.of(
        RefactoringType.EXTRACT_OPERATION, 
        RefactoringType.EXTRACT_SUPERCLASS, 
        RefactoringType.EXTRACT_INTERFACE, 
        RefactoringType.INLINE_OPERATION, 
        RefactoringType.MOVE_ATTRIBUTE, 
        RefactoringType.PULL_UP_ATTRIBUTE, 
        RefactoringType.PUSH_DOWN_ATTRIBUTE);
    
    RefactoringSet r1 = RefactoringCrawlerResultReader.read("https://github.com/aserg-ufmg/atmosphere.git", "cc2b3f1", "D:\\Danilo\\Workspaces\\phd-rmdataset\\results\\atmosphere-cc2b3f1");
    CompareResult cr = oracle.atmosphere_cc2b3f1.ignoring(refTypesToIgnore).compareTo(r1);
    cr.printReport(System.out, true);
  }

}
