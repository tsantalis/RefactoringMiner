package org.refactoringminer.test;

import java.util.EnumSet;

import org.eclipse.jgit.lib.Repository;
import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.api.GitService;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.rm2.analysis.GitHistoryRefactoringMiner2;
import org.refactoringminer.util.GitServiceImpl;
import org.refactoringminer.utils.Arqsoft16Dataset;
import org.refactoringminer.utils.CombinedCompareResult;
import org.refactoringminer.utils.RefactoringCollector;
import org.refactoringminer.utils.RefactoringCrawlerResultReader;
import org.refactoringminer.utils.RefactoringSet;

public class TestWithArqsoft16Dataset {

  public static void main(String[] args) {
    Arqsoft16Dataset oracle = new Arqsoft16Dataset();
    EnumSet<RefactoringType> refTypesToIgnore1 = EnumSet.of(
        RefactoringType.EXTRACT_OPERATION, 
        RefactoringType.EXTRACT_SUPERCLASS, 
        RefactoringType.EXTRACT_INTERFACE, 
        RefactoringType.MOVE_CLASS, 
        RefactoringType.INLINE_OPERATION, 
        RefactoringType.MOVE_ATTRIBUTE,
        RefactoringType.PULL_UP_ATTRIBUTE, 
        RefactoringType.PUSH_DOWN_ATTRIBUTE);
    String basePath = "data/refactoring-crawler-results/";
    
    EnumSet<RefactoringType> refTypesToIgnore2 = EnumSet.of(
        RefactoringType.RENAME_PACKAGE,
        RefactoringType.CHANGE_METHOD_SIGNATURE);
    
    RefactoringSet atmosphere_cc2b3f1 = RefactoringCrawlerResultReader.read("https://github.com/aserg-ufmg/atmosphere.git", "cc2b3f1", basePath + "atmosphere-cc2b3f1");
    RefactoringSet clojure_17217a1 = RefactoringCrawlerResultReader.read("https://github.com/aserg-ufmg/clojure.git", "17217a1", basePath + "clojure-17217a1");
    RefactoringSet guava_79767ec = RefactoringCrawlerResultReader.read("https://github.com/aserg-ufmg/guava.git", "79767ec", basePath + "guava-79767ec");
    RefactoringSet metrics_276d5e4 = RefactoringCrawlerResultReader.read("https://github.com/aserg-ufmg/metrics.git", "276d5e4", basePath + "metrics-276d5e4");
    RefactoringSet orientdb_b213aaf = RefactoringCrawlerResultReader.read("https://github.com/aserg-ufmg/orientdb.git", "b213aaf", basePath + "orientdb-b213aaf");
    RefactoringSet retrofit_f13f317 = RefactoringCrawlerResultReader.read("https://github.com/aserg-ufmg/retrofit.git", "f13f317", basePath + "retrofit-f13f317");
    RefactoringSet springBoot_48e893a = RefactoringCrawlerResultReader.read("https://github.com/aserg-ufmg/spring-boot.git", "48e893a", basePath + "spring-boot-48e893a");
    
    boolean grouped = true;
    boolean verbose = false;
    
    System.out.println("# Refactoring Crawler #");
    CombinedCompareResult cr = new CombinedCompareResult(
        oracle.atmosphere_cc2b3f1.ignoring(refTypesToIgnore1).compareTo(atmosphere_cc2b3f1.ignoring(refTypesToIgnore2), grouped),
        oracle.clojure_17217a1.ignoring(refTypesToIgnore1).compareTo(clojure_17217a1.ignoring(refTypesToIgnore2), grouped),
        oracle.guava_79767ec.ignoring(refTypesToIgnore1).compareTo(guava_79767ec.ignoring(refTypesToIgnore2), grouped),
        oracle.metrics_276d5e4.ignoring(refTypesToIgnore1).compareTo(metrics_276d5e4.ignoring(refTypesToIgnore2), grouped),
        oracle.orientdb_b213aaf.ignoring(refTypesToIgnore1).compareTo(orientdb_b213aaf.ignoring(refTypesToIgnore2), grouped),
        oracle.retrofit_f13f317.ignoring(refTypesToIgnore1).compareTo(retrofit_f13f317.ignoring(refTypesToIgnore2), grouped),
        oracle.springBoot_48e893a.ignoring(refTypesToIgnore1).compareTo(springBoot_48e893a.ignoring(refTypesToIgnore2), grouped)
    );
    cr.printReport(System.out, verbose);
    
    
    GitHistoryRefactoringMinerImpl rm1 = new GitHistoryRefactoringMinerImpl();
    GitHistoryRefactoringMiner2 rm2 = new GitHistoryRefactoringMiner2();
    
    System.out.println("# RM1 #");
    runWithRM(oracle, grouped, rm1);
    
    System.out.println("# RM2 #");
    runWithRM(oracle, grouped, rm2);
  }

  private static void runWithRM(Arqsoft16Dataset oracle, boolean grouped, GitHistoryRefactoringMiner rm) {
    RefactoringSet atmosphere_cc2b3f1_rm2 = collectRmResult(rm, "https://github.com/aserg-ufmg/atmosphere.git", "cc2b3f1");
    RefactoringSet clojure_17217a1_rm2 = collectRmResult(rm, "https://github.com/aserg-ufmg/clojure.git", "17217a1");
    RefactoringSet guava_79767ec_rm2 = collectRmResult(rm, "https://github.com/aserg-ufmg/guava.git", "79767ec");
    RefactoringSet metrics_276d5e4_rm2 = collectRmResult(rm, "https://github.com/aserg-ufmg/metrics.git", "276d5e4");
    RefactoringSet orientdb_b213aaf_rm2 = collectRmResult(rm, "https://github.com/aserg-ufmg/orientdb.git", "b213aaf");
    RefactoringSet retrofit_f13f317_rm2 = collectRmResult(rm, "https://github.com/aserg-ufmg/retrofit.git", "f13f317");
    RefactoringSet springBoot_48e893a_rm2 = collectRmResult(rm, "https://github.com/aserg-ufmg/spring-boot.git", "48e893a");
    
    CombinedCompareResult cr2 = new CombinedCompareResult(
        oracle.atmosphere_cc2b3f1.compareTo(atmosphere_cc2b3f1_rm2, grouped),
        oracle.clojure_17217a1.compareTo(clojure_17217a1_rm2, grouped),
        oracle.guava_79767ec.compareTo(guava_79767ec_rm2, grouped),
        oracle.metrics_276d5e4.compareTo(metrics_276d5e4_rm2, grouped),
        oracle.orientdb_b213aaf.compareTo(orientdb_b213aaf_rm2, grouped),
        oracle.retrofit_f13f317.compareTo(retrofit_f13f317_rm2, grouped),
        oracle.springBoot_48e893a.compareTo(springBoot_48e893a_rm2, grouped)
    );
    cr2.printReport(System.out, true);
  }

  private static RefactoringSet collectRmResult(GitHistoryRefactoringMiner rm, String cloneUrl, String commitId) {
    GitService git = new GitServiceImpl();
    String tempDir = "temp";
    String folder = tempDir + "/" + cloneUrl.substring(cloneUrl.lastIndexOf('/') + 1, cloneUrl.lastIndexOf('.'));
    final RefactoringCollector rc = new RefactoringCollector(cloneUrl, commitId);
    try (Repository repo = git.cloneIfNotExists(folder, cloneUrl)) {
      rm.detectAtCommit(repo, commitId, rc);
    } catch (Exception e) {
      throw new RuntimeException(e); 
    }
    return rc.assertAndGetResult();
  } 
}
