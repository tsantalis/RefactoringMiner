package org.kohsuke.github;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

public class GHRepositoryWrapperTest {
    private final static Map<String, Pair<String, Integer>> TEST_CASES_MAP = new HashMap<>();

    static {
        TEST_CASES_MAP.put("https://github.com/hibernate/hibernate-orm.git", Pair.of("9caca0ce37d5a2763d476c6fa2471addcca710ca", 1284));
        TEST_CASES_MAP.put("https://github.com/hibernate/hibernate-orm.git", Pair.of("37fc401da891544c1596c9b45822a3b1e459e7e2", 531));
        TEST_CASES_MAP.put("https://github.com/hibernate/hibernate-orm.git", Pair.of("9ae57a6f7addc80c6aa4bb9f55b43baeee47189e", 536));
        TEST_CASES_MAP.put("https://github.com/hibernate/hibernate-orm.git", Pair.of("825ab027231728f331ada37e1edd44027dc246ee", 103));
        TEST_CASES_MAP.put("https://github.com/hibernate/hibernate-orm.git", Pair.of("d671fe1391945726e3a8ce1577904b42dda80a4b", 629));
        TEST_CASES_MAP.put("https://github.com/hibernate/hibernate-orm.git", Pair.of("782f023a5a16aba08dc1429f094f1048be434617", 32));
        TEST_CASES_MAP.put("https://github.com/javaparser/javaparser.git", Pair.of("0ac302de4ea549230305c544be78a1570a58d2fa", 491));
        TEST_CASES_MAP.put("https://github.com/javaparser/javaparser.git", Pair.of("6ff3b519de40fc443f79d6a6ecaeadf7dc52f9ed", 659));
        TEST_CASES_MAP.put("https://github.com/checkstyle/checkstyle.git", Pair.of("1e3fb3fae940bf7ea340592f7f0cda3af1b320e3", 948));
        TEST_CASES_MAP.put("https://github.com/checkstyle/checkstyle.git", Pair.of("6893affeac7285afd16f8a389c7d65023d0c327a", 366));
    }

    @Test
    public void testNumberOfChangedFiles() {
        for (Map.Entry<String, Pair<String, Integer>> entry : TEST_CASES_MAP.entrySet()) {
            Assert.assertEquals(entry.getValue().getRight().intValue(), getNumberOfChangedFile(entry.getKey(), entry.getValue().getLeft()));
        }
    }


    private int getNumberOfChangedFile(String cloneUrl, String commitId) {
        try {
            GitHistoryRefactoringMinerImpl gitHistoryRefactoringMiner = new GitHistoryRefactoringMinerImpl();
            List<GHCommit.File> commitFiles = new ArrayList<>();
            GHRepository ghRepository = gitHistoryRefactoringMiner.getGhRepository(cloneUrl);
            GHCommit commit = new GHRepositoryWrapper(ghRepository).getCommit(commitId, commitFiles);
            return commitFiles.size();
        } catch (IOException e) {
            return -1;
        }
    }
}
