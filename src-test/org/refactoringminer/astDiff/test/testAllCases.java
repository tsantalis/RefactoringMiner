package org.refactoringminer.astDiff.test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import git4idea.repo.GitRepository;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.refactoringminer.api.GitService;
import org.refactoringminer.astDiff.actions.ASTDiff;
import org.refactoringminer.astDiff.utils.CaseInfo;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.util.GitServiceImpl;

import java.io.File;
import java.util.*;

import static org.refactoringminer.astDiff.utils.UtilMethods.*;

/**
 * @author  Pourya Alikhani Fard pouryafard75@gmail.com
 */

public class testAllCases extends LightJavaCodeInsightFixtureTestCase {
    private static final String REPOS = "tmp1";
    private GitService gitService = new GitServiceImpl();
    @Test
    public void testASTDiffs() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String jsonFile = getTestDir() + getTestInfoFile();
        List<CaseInfo> infos = mapper.readValue(new File(jsonFile), new TypeReference<List<CaseInfo>>(){});
        for (CaseInfo info : infos) {
            String repoFolder = info.getRepo().substring(info.getRepo().lastIndexOf("/"), info.getRepo().indexOf(".git"));
            GitRepository repo = gitService.cloneIfNotExists(getProject(), REPOS + repoFolder, info.getRepo());
            Set<ASTDiff> astDiffs = new GitHistoryRefactoringMinerImpl().diffAtCommit(repo, info.getCommit());
            for (ASTDiff astDiff : astDiffs) {
                String finalFilePath = getFinalFilePath(astDiff, getTestDir(), info.getRepo(), info.getCommit());
                String calculated = astDiff.getMultiMappings().exportString();
                String expected = FileUtils.readFileToString(new File(finalFilePath), "utf-8");
                assertEquals(String.format("Failed for the repo : %s, commit : %s , srcFileName: %s", info.getRepo(),info.getCommit(),astDiff.getSrcPath()),
                        expected, calculated);
            }
        }
    }
}
