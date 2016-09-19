package org.refactoringminer.test;

import java.util.EnumSet;

import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.utils.Arqsoft16Dataset;
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
        RefactoringType.PUSH_DOWN_ATTRIBUTE,
        RefactoringType.RENAME_PACKAGE);
    String basePath = "data/refactoring-crawler-results/";
    
    RefactoringSet atmosphere_cc2b3f1 = RefactoringCrawlerResultReader.read("https://github.com/aserg-ufmg/atmosphere.git", "cc2b3f1", basePath + "atmosphere-cc2b3f1");
    RefactoringSet clojure_17217a1 = RefactoringCrawlerResultReader.read("https://github.com/aserg-ufmg/clojure.git", "17217a1", basePath + "clojure-17217a1");
    RefactoringSet guava_79767ec = RefactoringCrawlerResultReader.read("https://github.com/aserg-ufmg/guava.git", "79767ec", basePath + "guava-79767ec");
    RefactoringSet metrics_276d5e4 = RefactoringCrawlerResultReader.read("https://github.com/aserg-ufmg/metrics.git", "276d5e4", basePath + "metrics-276d5e4");
    RefactoringSet orientdb_b213aaf = RefactoringCrawlerResultReader.read("https://github.com/aserg-ufmg/orientdb.git", "b213aaf", basePath + "orientdb-b213aaf");
    RefactoringSet retrofit_f13f317 = RefactoringCrawlerResultReader.read("https://github.com/aserg-ufmg/retrofit.git", "f13f317", basePath + "retrofit-f13f317");
    RefactoringSet springBoot_48e893a = RefactoringCrawlerResultReader.read("https://github.com/aserg-ufmg/spring-boot.git", "48e893a", basePath + "spring-boot-48e893a");
    
    boolean grouped = false;
    boolean verbose = true;
    oracle.atmosphere_cc2b3f1.ignoring(refTypesToIgnore).compareTo(atmosphere_cc2b3f1, grouped).printReport(System.out, verbose);
    oracle.clojure_17217a1.ignoring(refTypesToIgnore).compareTo(clojure_17217a1, grouped).printReport(System.out, verbose);
    oracle.guava_79767ec.ignoring(refTypesToIgnore).compareTo(guava_79767ec, grouped).printReport(System.out, verbose);
    oracle.metrics_276d5e4.ignoring(refTypesToIgnore).compareTo(metrics_276d5e4, grouped).printReport(System.out, verbose);
    oracle.orientdb_b213aaf.ignoring(refTypesToIgnore).compareTo(orientdb_b213aaf, grouped).printReport(System.out, verbose);
    oracle.retrofit_f13f317.ignoring(refTypesToIgnore).compareTo(retrofit_f13f317, grouped).printReport(System.out, verbose);
    oracle.springBoot_48e893a.ignoring(refTypesToIgnore).compareTo(springBoot_48e893a, grouped).printReport(System.out, verbose);

  }

}
