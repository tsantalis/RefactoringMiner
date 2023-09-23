package org.kohsuke.github;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

public class GHRepositoryWrapperTest {
	private final GitHistoryRefactoringMinerImpl gitHistoryRefactoringMiner = new GitHistoryRefactoringMinerImpl();

    @ParameterizedTest
    @CsvSource({
            "https://github.com/hibernate/hibernate-orm.git, 9caca0ce37d5a2763d476c6fa2471addcca710ca, 1284",
            "https://github.com/hibernate/hibernate-orm.git, 37fc401da891544c1596c9b45822a3b1e459e7e2, 531",
            "https://github.com/hibernate/hibernate-orm.git, 9ae57a6f7addc80c6aa4bb9f55b43baeee47189e, 536",
            "https://github.com/hibernate/hibernate-orm.git, 825ab027231728f331ada37e1edd44027dc246ee, 103",
            "https://github.com/hibernate/hibernate-orm.git, d671fe1391945726e3a8ce1577904b42dda80a4b, 629",
            "https://github.com/hibernate/hibernate-orm.git, 782f023a5a16aba08dc1429f094f1048be434617, 32",
            "https://github.com/javaparser/javaparser.git, 0ac302de4ea549230305c544be78a1570a58d2fa, 491",
            "https://github.com/javaparser/javaparser.git, 6ff3b519de40fc443f79d6a6ecaeadf7dc52f9ed, 659",
            "https://github.com/checkstyle/checkstyle.git, 1e3fb3fae940bf7ea340592f7f0cda3af1b320e3, 948",
            "https://github.com/checkstyle/checkstyle.git, 6893affeac7285afd16f8a389c7d65023d0c327a, 366"
    })
    public void testNumberOfChangedFiles(String url, String commit, int changedFiles) {
        Assertions.assertEquals(changedFiles, getNumberOfChangedFile(url, commit));
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
