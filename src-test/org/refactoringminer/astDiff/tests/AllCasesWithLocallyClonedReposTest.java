package org.refactoringminer.astDiff.tests;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jgit.lib.Repository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.refactoringminer.api.GitService;
import org.refactoringminer.astDiff.actions.ASTDiff;
import org.refactoringminer.astDiff.utils.CaseInfo;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.util.GitServiceImpl;

import java.io.File;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.refactoringminer.astDiff.utils.UtilMethods.*;

/**
 * @author  Pourya Alikhani Fard pouryafard75@gmail.com
 */

public class AllCasesWithLocallyClonedReposTest {
	private static final String REPOS = "tmp1";
	private static GitService gitService = new GitServiceImpl();
    @ParameterizedTest(name= "{index}: File: {2}, Repo: {0}, Commit: {1}")
    @MethodSource("initDataWithClonedRepos")
    public void testChecker(String repo, String commit, String srcFileName, String expected, String actual) {
        String msg = String.format("Failed for %s/commit/%s , srcFileName: %s",repo.replace(".git",""),commit,srcFileName);
        assertEquals(expected.length(), actual.length(), msg);
        assertEquals(expected, actual, msg);
    }

    public static Stream<Arguments> initDataWithClonedRepos() throws Exception {
        List<Arguments> allCases = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        String jsonFile = getCommitsMappingsPath() + getTestInfoFile();
        List<CaseInfo> infos = mapper.readValue(new File(jsonFile), new TypeReference<List<CaseInfo>>(){});
        for (CaseInfo info : infos) {
            List<String> expectedFilesList = new ArrayList<>(List.of(Objects.requireNonNull(new File(getFinalFolderPath(getCommitsMappingsPath(), info.getRepo(), info.getCommit())).list())));

            String repoFolder = info.getRepo().substring(info.getRepo().lastIndexOf("/"), info.getRepo().indexOf(".git"));
            Repository repo = gitService.cloneIfNotExists(REPOS + repoFolder, info.getRepo());
            Set<ASTDiff> astDiffs = new GitHistoryRefactoringMinerImpl().diffAtCommit(repo, info.getCommit());

            makeAllCases(allCases, info, expectedFilesList, astDiffs);
        }
        return allCases.stream();
    }

    public static Stream<Arguments> initDataWithGithub() throws Exception {
        List<Arguments> allCases = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        String jsonFile = getCommitsMappingsPath() + getTestInfoFile();
        List<CaseInfo> infos = mapper.readValue(new File(jsonFile), new TypeReference<List<CaseInfo>>(){});
        for (CaseInfo info : infos) {
            List<String> expectedFilesList = new ArrayList<>(List.of(Objects.requireNonNull(new File(getFinalFolderPath(getCommitsMappingsPath(), info.getRepo(), info.getCommit())).list())));

            Set<ASTDiff> astDiffs = new GitHistoryRefactoringMinerImpl().diffAtCommit(info.getRepo(), info.getCommit(), 1000);

            makeAllCases(allCases, info, expectedFilesList, astDiffs);
        }
        return allCases.stream();
    }
}
