package org.refactoringminer.astDiff.test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.lib.Repository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.refactoringminer.api.GitService;
import org.refactoringminer.astDiff.actions.ASTDiff;
import org.refactoringminer.astDiff.utils.CaseInfo;
import org.refactoringminer.astDiff.utils.MappingExportModel;
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
	private static final String REPOS = "tmp1";
	private static GitService gitService = new GitServiceImpl();
	
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
        assertEquals(String.format("Failed for %s/commit/%s , srcFileName: %s",repo.replace(".git",""),commit,srcFileName),
                expected,actual);
    }

    @Parameterized.Parameters(name= "{index}: File: {2}, Repo: {0}, Commit: {1}")
    public static Iterable<Object[]> initData() throws Exception {
        List<Object[]> allCases = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        String jsonFile = getTestDir() + getTestInfoFile();
        List<CaseInfo> infos = mapper.readValue(new File(jsonFile), new TypeReference<List<CaseInfo>>(){});
        for (CaseInfo info : infos) {
            //Set<ASTDiff> astDiffs = new GitHistoryRefactoringMinerImpl().diffAtCommit(info.getRepo(), info.getCommit(), 1000);
        	String repoFolder = info.getRepo().substring(info.getRepo().lastIndexOf("/"), info.getRepo().indexOf(".git"));
        	Repository repo = gitService.cloneIfNotExists(REPOS + repoFolder, info.getRepo());
        	Set<ASTDiff> astDiffs = new GitHistoryRefactoringMinerImpl().diffAtCommit(repo, info.getCommit());
            List<String> expectedFilesList = new ArrayList<>(List.of(Objects.requireNonNull(new File(getFinalFolderPath(getTestDir(), info.getRepo(), info.getCommit())).list())));
            for (ASTDiff astDiff : astDiffs) {
                String finalFilePath = getFinalFilePath(astDiff, getTestDir(), info.getRepo(), info.getCommit());
                String calculated = MappingExportModel.exportString(astDiff.getMultiMappings());
                String expected = FileUtils.readFileToString(new File(finalFilePath), "utf-8");
                allCases.add(new Object[]{info.getRepo(),info.getCommit(),astDiff.getSrcPath(),expected,calculated});
                expectedFilesList.remove(getFileNameFromSrcDiff(astDiff.getSrcPath()));
            }
            for (String expectedButNotGeneratedFile : expectedFilesList) {
                String expectedDiffName = getSrcASTDiffFromFile(expectedButNotGeneratedFile);
                allCases.add(new Object[]
                        {
                        info.getRepo(),info.getCommit(),expectedDiffName,"{JSON}","NOT GENERATED"
                        }
                );
            }
        }
        return allCases;
    }
}
