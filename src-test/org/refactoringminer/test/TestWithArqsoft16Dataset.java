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
import org.refactoringminer.utils.RefactoringCollector;
import org.refactoringminer.utils.RefactoringCrawlerResultReader;
import org.refactoringminer.utils.RefactoringSet;
import org.refactoringminer.utils.ResultComparator;

public class TestWithArqsoft16Dataset {

    public static void main(String[] args) {
        EnumSet<RefactoringType> allRefTypes = EnumSet.allOf(RefactoringType.class);
        EnumSet<RefactoringType> refTypesToConsider = EnumSet.of(
            RefactoringType.RENAME_CLASS,
            RefactoringType.RENAME_METHOD, 
            RefactoringType.MOVE_OPERATION, 
            RefactoringType.PULL_UP_OPERATION, 
            RefactoringType.PUSH_DOWN_OPERATION);
        
        Arqsoft16Dataset oracle = new Arqsoft16Dataset();
        GitHistoryRefactoringMinerImpl rm1 = new GitHistoryRefactoringMinerImpl();
        GitHistoryRefactoringMiner rm2 = new GitHistoryRefactoringMiner2();

        RefactoringSet[] rm1Results = getRmResults(rm1);
        RefactoringSet[] rm2Results = getRmResults(rm2);
        
        ResultComparator rc1 = new ResultComparator();
        rc1.expect(oracle.all());
        rc1.compareWith("RM1", rm1Results);
        rc1.compareWith("RM2", rm2Results);
        rc1.compareWith("RC", readRefactoringCrawlerResults());
        rc1.printSummary(System.out, true, refTypesToConsider);
        
        
        ResultComparator rc2 = new ResultComparator();
        rc2.expect(oracle.all());
        rc2.compareWith("RM1", rm1Results);
        rc2.compareWith("RM2", rm2Results);
        rc2.printSummary(System.out, false, allRefTypes);
        rc1.printDetails(System.out, false, allRefTypes);
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
    
    private static RefactoringSet[] getRmResults(GitHistoryRefactoringMiner rm) {
        return new RefactoringSet[] {
                        collectRmResult(rm, "https://github.com/aserg-ufmg/atmosphere.git", "cc2b3f1"),
                        collectRmResult(rm, "https://github.com/aserg-ufmg/clojure.git", "17217a1"),
                        collectRmResult(rm, "https://github.com/aserg-ufmg/guava.git", "79767ec"),
                        collectRmResult(rm, "https://github.com/aserg-ufmg/metrics.git", "276d5e4"),
                        collectRmResult(rm, "https://github.com/aserg-ufmg/orientdb.git", "b213aaf"),
                        collectRmResult(rm, "https://github.com/aserg-ufmg/retrofit.git", "f13f317"),
                        collectRmResult(rm, "https://github.com/aserg-ufmg/spring-boot.git", "48e893a") };
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
