package org.kohsuke.github;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.Assert;
import org.junit.Test;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

public class GHRepositoryWrapperTest {
	private final GitHistoryRefactoringMinerImpl gitHistoryRefactoringMiner = new GitHistoryRefactoringMinerImpl();
    private final static List<Triple<String, String, Integer>> TEST_CASES_LIST = new ArrayList<>();

    static {
        TEST_CASES_LIST.add(Triple.of("https://github.com/hibernate/hibernate-orm.git", "9caca0ce37d5a2763d476c6fa2471addcca710ca", 1284));
        TEST_CASES_LIST.add(Triple.of("https://github.com/hibernate/hibernate-orm.git", "37fc401da891544c1596c9b45822a3b1e459e7e2", 531));
        TEST_CASES_LIST.add(Triple.of("https://github.com/hibernate/hibernate-orm.git", "9ae57a6f7addc80c6aa4bb9f55b43baeee47189e", 536));
        TEST_CASES_LIST.add(Triple.of("https://github.com/hibernate/hibernate-orm.git", "825ab027231728f331ada37e1edd44027dc246ee", 103));
        TEST_CASES_LIST.add(Triple.of("https://github.com/hibernate/hibernate-orm.git", "d671fe1391945726e3a8ce1577904b42dda80a4b", 629));
        TEST_CASES_LIST.add(Triple.of("https://github.com/hibernate/hibernate-orm.git", "782f023a5a16aba08dc1429f094f1048be434617", 32));
        TEST_CASES_LIST.add(Triple.of("https://github.com/javaparser/javaparser.git", "0ac302de4ea549230305c544be78a1570a58d2fa", 491));
        TEST_CASES_LIST.add(Triple.of("https://github.com/javaparser/javaparser.git", "6ff3b519de40fc443f79d6a6ecaeadf7dc52f9ed", 659));
        TEST_CASES_LIST.add(Triple.of("https://github.com/checkstyle/checkstyle.git", "1e3fb3fae940bf7ea340592f7f0cda3af1b320e3", 948));
        TEST_CASES_LIST.add(Triple.of("https://github.com/checkstyle/checkstyle.git", "6893affeac7285afd16f8a389c7d65023d0c327a", 366));
    }

    @Test
    public void testNumberOfChangedFiles() {
        int numberOfTestCase = 0;
        for (Triple<String, String, Integer> testCase : TEST_CASES_LIST) {
            Assert.assertEquals(testCase.getRight().intValue(), getNumberOfChangedFile(testCase.getLeft(), testCase.getMiddle()));
            numberOfTestCase++;
        }
        Assert.assertEquals(10, numberOfTestCase);
    }


    private int getNumberOfChangedFile(String cloneUrl, String commitId) {
        try {
            List<GHCommit.File> commitFiles = new ArrayList<>();
            GHRepository ghRepository = gitHistoryRefactoringMiner.getGitHubRepository(cloneUrl);
            GHRepositoryWrapper ghRepositoryWrapper = new GHRepositoryWrapper(ghRepository);
			ghRepositoryWrapper.getCommit(commitId, commitFiles);
            return commitFiles.size();
        } catch (IOException e) {
            return -1;
        }
    }
}
