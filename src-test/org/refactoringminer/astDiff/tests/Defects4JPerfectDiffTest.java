package org.refactoringminer.astDiff.tests;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.refactoringminer.astDiff.actions.ASTDiff;
import org.refactoringminer.astDiff.utils.CaseInfo;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.refactoringminer.astDiff.utils.UtilMethods.*;

/**
 * @author  Pourya Alikhani Fard pouryafard75@gmail.com
 */
public class Defects4JPerfectDiffTest {
    private static final String dir = getDefects4jMappingPath();
    @ParameterizedTest(name= "{index}: File: {2}, Repo: {0}, Commit: {1}")
    @MethodSource("initDataWithClonedRepos")
    public void testSubTreeMappings(String repo, String commit, String srcFileName, boolean sameLen,  boolean status) {
        String msg = String.format("Failed for %s/commit/%s , srcFileName: %s",repo.replace(".git",""),commit,srcFileName);
        assertTrue(sameLen, msg);
        assertTrue(status, msg);
    }

    public static Stream<Arguments> initDataWithClonedRepos() throws Exception {
        List<Arguments> allCases = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        String jsonFile = dir + getTestInfoFile();
        List<CaseInfo> infos = mapper.readValue(new File(jsonFile), new TypeReference<List<CaseInfo>>(){});
        for (CaseInfo info : infos) {
            List<String> expectedFilesList = new ArrayList<>(List.of(Objects.requireNonNull(new File(getFinalFolderPath(dir, info.getRepo(), info.getCommit())).list())));
            String projectDir = info.getRepo();
            String bugID = info.getCommit();
            Set<ASTDiff> astDiffs = new GitHistoryRefactoringMinerImpl().diffAtDirectories(
                    Path.of(getDefect4jBeforeDir(projectDir, bugID)),
                    Path.of(getDefect4jAfterDir(projectDir, bugID)));
            makeAndCheckAllCases(allCases, info, expectedFilesList, astDiffs, dir);
        }
        return allCases.stream();
    }
}
