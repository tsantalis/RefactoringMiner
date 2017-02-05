package org.refactoringminer.evaluation;

import java.util.EnumSet;
import java.util.Locale;

import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.rm2.analysis.GitHistoryRefactoringMiner2;
import org.refactoringminer.utils.RefFinderResultReader;
import org.refactoringminer.utils.RefactoringCrawlerResultReader;
import org.refactoringminer.utils.RefactoringSet;
import org.refactoringminer.utils.ResultComparator;
import org.refactoringminer.utils.ResultComparator.CompareResult;

public class TestWithArqsoft16Dataset {

    public static void main(String[] args) {
        new TestWithArqsoft16Dataset().run();
    }

    ResultComparator rcRDiff;
    ResultComparator rcRCraw;
    ResultComparator rcRCraw2;
    ResultComparator rcRFind;
    ResultComparator rcRFind2;

    EnumSet<RefactoringType> refTypesOracle = EnumSet.of(
        RefactoringType.RENAME_CLASS,
        RefactoringType.MOVE_CLASS,
        RefactoringType.EXTRACT_SUPERCLASS,
        RefactoringType.EXTRACT_INTERFACE,
        RefactoringType.RENAME_METHOD,
        RefactoringType.MOVE_OPERATION,
        RefactoringType.PULL_UP_OPERATION,
        RefactoringType.PUSH_DOWN_OPERATION,
        RefactoringType.EXTRACT_OPERATION,
        RefactoringType.INLINE_OPERATION,
        RefactoringType.MOVE_ATTRIBUTE,
        RefactoringType.PULL_UP_ATTRIBUTE,
        RefactoringType.PUSH_DOWN_ATTRIBUTE);

    EnumSet<RefactoringType> refTypesRCraw = EnumSet.of(
        RefactoringType.RENAME_CLASS,
        RefactoringType.RENAME_METHOD,
        RefactoringType.MOVE_OPERATION,
        RefactoringType.PULL_UP_OPERATION,
        RefactoringType.PUSH_DOWN_OPERATION);

    EnumSet<RefactoringType> refTypesRFind = EnumSet.of(
        RefactoringType.EXTRACT_SUPERCLASS,
        RefactoringType.EXTRACT_INTERFACE,
        RefactoringType.RENAME_METHOD,
        RefactoringType.MOVE_OPERATION,
        RefactoringType.PUSH_DOWN_OPERATION,
        RefactoringType.PULL_UP_OPERATION,
        RefactoringType.EXTRACT_OPERATION,
        RefactoringType.INLINE_OPERATION,
        RefactoringType.MOVE_ATTRIBUTE,
        RefactoringType.PUSH_DOWN_ATTRIBUTE,
        RefactoringType.PULL_UP_ATTRIBUTE);
    
    public void run() {
        Arqsoft16Dataset oracle = new Arqsoft16Dataset();
        GitHistoryRefactoringMinerImpl rm1 = new GitHistoryRefactoringMinerImpl();
        GitHistoryRefactoringMiner rm2 = new GitHistoryRefactoringMiner2();

        RefactoringSet[] rmResults = getRmResults(rm1);
        RefactoringSet[] refDiffResults = getRmResults(rm2);
        RefactoringSet[] refFinderResults = readRefFinderResults();
        RefactoringSet[] refactoringCrawlerResults = readRefactoringCrawlerResults();

        rcRDiff = new ResultComparator();
        rcRDiff.expect(oracle.all());
        rcRDiff.compareWith("RDiff", refDiffResults);
        rcRDiff.compareWith("RMinr", rmResults);
        
        rcRCraw = new ResultComparator();
        rcRCraw.expect(oracle.all());
        rcRCraw.compareWith("RCraw", refactoringCrawlerResults);

        rcRCraw2 = new ResultComparator();
        rcRCraw2.expect(oracle.all());
        rcRCraw2.setIgnoreMoveToMovedType(true);
        rcRCraw2.compareWith("RCraw", refactoringCrawlerResults);
        
        rcRFind = new ResultComparator(false, true);
        rcRFind.expect(oracle.all());
        rcRFind.compareWith("RFind", refFinderResults);

        rcRFind2 = new ResultComparator(false, true);
        rcRFind2.expect(oracle.all());
        rcRFind2.setIgnoreMoveToMovedType(true);
        rcRFind2.setIgnoreMoveToRenamedType(true);
        rcRFind2.compareWith("RFind", refFinderResults);
        
//        rcRDiff.printSummary(System.out, refTypesOracle);
//        rcRDiff.printDetails(System.out, refTypesOracle);
        
//        rcRCraw.printSummary(System.out, refTypesRCraw);
//        rcRCraw2.printSummary(System.out, refTypesRCraw);
//        rcRCraw.printDetails(System.out, refTypesRCraw);
        
//        rcRFind.printSummary(System.out, refTypesRFind);
//        rcRFind2.printSummary(System.out, refTypesRFind);
//        rcRFind.printDetails(System.out, refTypesRFind);
        
        printTable();
    }

    private void printTable() {
        String[] tools = new String[] {"RDiff", "RMinr", "RCraw", "RCraw*", "RFind", "RFind*"};
        CompareResult[] results = new CompareResult[] {
            rcRDiff.getCompareResult("RDiff", refTypesOracle),
            rcRDiff.getCompareResult("RMinr", refTypesOracle),
            rcRCraw.getCompareResult("RCraw", refTypesRCraw),
            rcRCraw2.getCompareResult("RCraw", refTypesRCraw),
            rcRFind.getCompareResult("RFind", refTypesRFind),
            rcRFind2.getCompareResult("RFind", refTypesRFind)
        };
        
        for (int i = 0; i < tools.length; i++) {
            String tool = tools[i];
            CompareResult result = results[i];
            double precision = result.getPrecision();
            double recall = result.getRecall();
            System.out.print(String.format(Locale.US, "%s\t%.3f\t%.3f", tool, precision, recall));
            for (RefactoringType refType : refTypesOracle) {
                
            }
            System.out.println();
        }
    }

    private static RefactoringSet[] readRefactoringCrawlerResults() {
        String basePath = "data/refactoring-crawler-results/";
        return new RefactoringSet[] {
            RefactoringCrawlerResultReader.read("https://github.com/aserg-ufmg/atmosphere.git", "cc2b3f1", basePath + "atmosphere-cc2b3f1"),
            RefactoringCrawlerResultReader.read("https://github.com/aserg-ufmg/clojure.git", "17217a1", basePath + "clojure-17217a1"),
            RefactoringCrawlerResultReader.read("https://github.com/aserg-ufmg/guava.git", "79767ec", basePath + "guava-79767ec"),
            RefactoringCrawlerResultReader.read("https://github.com/aserg-ufmg/metrics.git", "276d5e4", basePath + "metrics-276d5e4"),
            RefactoringCrawlerResultReader.read("https://github.com/aserg-ufmg/orientdb.git", "b213aaf", basePath + "orientdb-b213aaf"),
            RefactoringCrawlerResultReader.read("https://github.com/aserg-ufmg/retrofit.git", "f13f317", basePath + "retrofit-f13f317"),
            RefactoringCrawlerResultReader.read("https://github.com/aserg-ufmg/spring-boot.git", "48e893a", basePath + "spring-boot-48e893a")
        };
    }

    private static RefactoringSet[] readRefFinderResults() {
        String basePath = "data/ref-finder-results/";
        return new RefactoringSet[] {
            RefFinderResultReader.read("https://github.com/aserg-ufmg/atmosphere.git", "cc2b3f1", basePath + "atmosphere-cc2b3f1"),
            RefFinderResultReader.read("https://github.com/aserg-ufmg/clojure.git", "17217a1", basePath + "clojure-17217a1"),
            RefFinderResultReader.read("https://github.com/aserg-ufmg/guava.git", "79767ec", basePath + "guava-79767ec"),
            RefFinderResultReader.read("https://github.com/aserg-ufmg/metrics.git", "276d5e4", basePath + "metrics-276d5e4"),
            RefFinderResultReader.read("https://github.com/aserg-ufmg/orientdb.git", "b213aaf", basePath + "orientdb-b213aaf"),
            RefFinderResultReader.read("https://github.com/aserg-ufmg/retrofit.git", "f13f317", basePath + "retrofit-f13f317"),
            RefFinderResultReader.read("https://github.com/aserg-ufmg/spring-boot.git", "48e893a", basePath + "spring-boot-48e893a")
        };
    }
    
    private static RefactoringSet[] getRmResults(GitHistoryRefactoringMiner rm) {
        return new RefactoringSet[] {
            ResultComparator.collectRmResult(rm, "https://github.com/aserg-ufmg/atmosphere.git", "cc2b3f1"),
            ResultComparator.collectRmResult(rm, "https://github.com/aserg-ufmg/clojure.git", "17217a1"),
            ResultComparator.collectRmResult(rm, "https://github.com/aserg-ufmg/guava.git", "79767ec"),
            ResultComparator.collectRmResult(rm, "https://github.com/aserg-ufmg/metrics.git", "276d5e4"),
            ResultComparator.collectRmResult(rm, "https://github.com/aserg-ufmg/orientdb.git", "b213aaf"),
            ResultComparator.collectRmResult(rm, "https://github.com/aserg-ufmg/retrofit.git", "f13f317"),
            ResultComparator.collectRmResult(rm, "https://github.com/aserg-ufmg/spring-boot.git", "48e893a") };
    }

}
