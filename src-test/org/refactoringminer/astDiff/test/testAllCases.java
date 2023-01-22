package org.refactoringminer.astDiff.test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import git4idea.repo.GitRepository;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.refactoringminer.api.GitService;
import org.refactoringminer.astDiff.actions.ASTDiff;
import org.refactoringminer.astDiff.utils.CaseInfo;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.util.GitServiceImpl;

import java.io.File;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.refactoringminer.astDiff.utils.UtilMethods.*;

/**
 * @author  Pourya Alikhani Fard pouryafard75@gmail.com
 */

@RunWith(Parameterized.class)
public class testAllCases {

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
        assertEquals(String.format("Failed for the repo : %s, commit : %s , srcFileName: %s",repo,commit,srcFileName),
                expected,actual);
    }

    @Parameterized.Parameters(name= "{index}: {2} in {0} : {1} }")
    public static Iterable<Object[]> initData() throws Exception {
        List<Object[]> allCases = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        String jsonFile = getTestDir() + getTestInfoFile();
        List<CaseInfo> infos = mapper.readValue(new File(jsonFile), new TypeReference<List<CaseInfo>>(){});
        for (CaseInfo info : infos) {
            Set<ASTDiff> astDiffs = new GitHistoryRefactoringMinerImpl().diffAtCommit(info.getRepo(), info.getCommit(), 1000);
        	//String repoFolder = info.getRepo().substring(info.getRepo().lastIndexOf("/"), info.getRepo().indexOf(".git"));
        	//GitRepository repo = gitService.cloneIfNotExists(getProject(), REPOS + repoFolder, info.getRepo());
        	//Set<ASTDiff> astDiffs = new GitHistoryRefactoringMinerImpl().diffAtCommit(repo, info.getCommit());
            for (ASTDiff astDiff : astDiffs) {
                String finalFilePath = getFinalFilePath(astDiff, getTestDir(), info.getRepo(), info.getCommit());
                String calculated = astDiff.getMultiMappings().exportString();
                String expected = FileUtils.readFileToString(new File(finalFilePath), "utf-8");
                allCases.add(new Object[]{info.getRepo(),info.getCommit(),astDiff.getSrcPath(),expected,calculated});
            }
        }
        return allCases;
    }
}
