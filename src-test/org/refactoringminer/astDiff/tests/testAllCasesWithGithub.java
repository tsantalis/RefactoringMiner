package org.refactoringminer.astDiff.tests;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.refactoringminer.astDiff.actions.ASTDiff;
import org.refactoringminer.astDiff.utils.CaseInfo;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import java.io.File;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.refactoringminer.astDiff.utils.UtilMethods.*;

/**
 * @author  Pourya Alikhani Fard pouryafard75@gmail.com
 */

@RunWith(Parameterized.class)
public class testAllCasesWithGithub {
    @Parameterized.Parameter(0)
    public String repo;
    @Parameterized.Parameter(1)
    public String commit;
    @Parameterized.Parameter(2)
    public String srcFileName;
    @Parameterized.Parameter(3)
    public String expected;
    @Parameterized.Parameter(4)
    public String actual;

    @Test
    public void testChecker() {
        String msg = String.format("Failed for %s/commit/%s , srcFileName: %s",repo.replace(".git",""),commit,srcFileName);
        assertEquals(msg, expected.length(),actual.length());
        assertEquals(msg, expected,actual);
    }
    @Parameterized.Parameters(name= "{index}: File: {2}, Repo: {0}, Commit: {1}")
    public static Iterable<Object[]> initData() throws Exception {
        List<Object[]> allCases = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        String jsonFile = getCommitsMappingsPath() + getTestInfoFile();
        List<CaseInfo> infos = mapper.readValue(new File(jsonFile), new TypeReference<List<CaseInfo>>(){});
        for (CaseInfo info : infos) {
            List<String> expectedFilesList = new ArrayList<>(List.of(Objects.requireNonNull(new File(getFinalFolderPath(getCommitsMappingsPath(), info.getRepo(), info.getCommit())).list())));

            Set<ASTDiff> astDiffs = new GitHistoryRefactoringMinerImpl().diffAtCommit(info.getRepo(), info.getCommit(), 1000);

            makeAllCases(allCases, info, expectedFilesList, astDiffs);
        }
        return allCases;
    }

}
