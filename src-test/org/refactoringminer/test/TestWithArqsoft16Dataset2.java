package org.refactoringminer.test;

import java.util.EnumSet;

import org.eclipse.jgit.lib.Repository;
import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.api.GitService;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.util.GitServiceImpl;
import org.refactoringminer.utils.Arqsoft16Dataset;
import org.refactoringminer.utils.CombinedCompareResult;
import org.refactoringminer.utils.RefactoringCollector;
import org.refactoringminer.utils.RefactoringSet;

public class TestWithArqsoft16Dataset2 {

  public static void main(String[] args) {
    Arqsoft16Dataset oracle = new Arqsoft16Dataset();
    EnumSet<RefactoringType> refTypesToIgnore = EnumSet.of(
        /*RefactoringType.EXTRACT_OPERATION, 
        RefactoringType.EXTRACT_SUPERCLASS, 
        RefactoringType.EXTRACT_INTERFACE, 
        RefactoringType.INLINE_OPERATION, 
        RefactoringType.MOVE_ATTRIBUTE, 
        RefactoringType.PULL_UP_ATTRIBUTE, 
        RefactoringType.PUSH_DOWN_ATTRIBUTE,*/
        RefactoringType.RENAME_PACKAGE);
    
    GitHistoryRefactoringMinerImpl rm2 = new GitHistoryRefactoringMinerImpl();
    
//    RefactoringSet atmosphere_cc2b3f1_rm2 = collectRmResult(rm2, "https://github.com/aserg-ufmg/atmosphere.git", "cc2b3f1");
//    RefactoringSet clojure_17217a1_rm2 = collectRmResult(rm2, "https://github.com/aserg-ufmg/clojure.git", "17217a1");
//    RefactoringSet guava_79767ec_rm2 = collectRmResult(rm2, "https://github.com/aserg-ufmg/guava.git", "79767ec");
//    RefactoringSet metrics_276d5e4_rm2 = collectRmResult(rm2, "https://github.com/aserg-ufmg/metrics.git", "276d5e4");
//    RefactoringSet orientdb_b213aaf_rm2 = collectRmResult(rm2, "https://github.com/aserg-ufmg/orientdb.git", "b213aaf");
    RefactoringSet retrofit_f13f317_rm2 = collectRmResult(rm2, "https://github.com/aserg-ufmg/retrofit.git", "f13f317");
//    RefactoringSet springBoot_48e893a_rm2 = collectRmResult(rm2, "https://github.com/aserg-ufmg/spring-boot.git", "48e893a");
    
    boolean grouped = false;
    CombinedCompareResult cr2 = new CombinedCompareResult(
//        oracle.atmosphere_cc2b3f1.ignoring(refTypesToIgnore).compareTo(atmosphere_cc2b3f1_rm2, grouped),
//        oracle.clojure_17217a1.ignoring(refTypesToIgnore).compareTo(clojure_17217a1_rm2, grouped),
//        oracle.guava_79767ec.ignoring(refTypesToIgnore).compareTo(guava_79767ec_rm2, grouped),
//        oracle.metrics_276d5e4.ignoring(refTypesToIgnore).compareTo(metrics_276d5e4_rm2, grouped),
//        oracle.orientdb_b213aaf.ignoring(refTypesToIgnore).compareTo(orientdb_b213aaf_rm2, grouped),
        oracle.retrofit_f13f317.ignoring(refTypesToIgnore).compareTo(retrofit_f13f317_rm2, grouped)
//        oracle.springBoot_48e893a.ignoring(refTypesToIgnore).compareTo(springBoot_48e893a_rm2, grouped)
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
