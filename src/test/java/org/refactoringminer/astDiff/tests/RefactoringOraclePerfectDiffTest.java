package org.refactoringminer.astDiff.tests;

import com.fasterxml.jackson.core.JsonProcessingException;
import net.joshka.junit.json.params.JsonFileSource;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ConvertWith;
import org.refactoringminer.astDiff.actions.ASTDiff;
import org.refactoringminer.astDiff.utils.CaseInfo;
import org.refactoringminer.astDiff.utils.MappingExportModel;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.fail;
import static org.refactoringminer.astDiff.utils.UtilMethods.*;

/**
 * @author  Pourya Alikhani Fard pouryafard75@gmail.com
 */

//@Isolated
public class RefactoringOraclePerfectDiffTest {
    private static final String dir = getCommitsMappingsPath();
    @ParameterizedTest(name= "{index}: {0}")

    @JsonFileSource(resources = "/astDiff/commits/cases.json")
//    @JsonFileSource(resources = "/astDiff/commits/cases-problematic.json")
    public void testSubTreeMappings(@ConvertWith(CaseInfo.CaseInfoConverter.class) CaseInfo info) throws Exception {
        File mappingsDirFile = new File(getFinalFolderPath(dir, info.getRepo(), info.getCommit()));
        String[] files = mappingsDirFile.list();
        List<String> expectedFilesList = new ArrayList<>(List.of(Objects.requireNonNull(files)));
//        Set<ASTDiff> astDiffs = new GitHistoryRefactoringMinerImpl().diffAtCommit(info.getRepo(), info.getCommit(), 1000);
        Set<ASTDiff> astDiffs = getProjectDiffLocally(info.makeURL());

        for (ASTDiff astDiff : astDiffs) {
            String finalFilePath = getFinalFilePath(astDiff, dir, info.getRepo(), info.getCommit());
            String calculated = MappingExportModel.exportString(astDiff.getAllMappings());
            String msg = String.format("Failed for %s/commit/%s , srcFileName: %s",info.getRepo().replace(".git",""),info.getCommit(),astDiff.getSrcPath());
            String expected = null;
            try {
                expected = FileUtils.readFileToString(new File(finalFilePath), "utf-8");
                assertEquals(expected.length(), calculated.length(), msg);
            } catch (IOException e) {
                fail("File not found: " + finalFilePath + " for " + info.getRepo() + "/commit/" + info.getCommit() + " , srcFileName: " + astDiff.getSrcPath());
            }
            assertEquals(calculated, expected, msg);
            expectedFilesList.remove(getFileNameFromSrcDiff(astDiff.getSrcPath()));
        }
        for (String expectedButNotGeneratedFile : expectedFilesList) {
            String expectedDiffName = getSrcASTDiffFromFile(expectedButNotGeneratedFile);
            fail(expectedDiffName);
        }
    }
}

/*
List of already generated diffs but removed due to having more than 2 AST files:
    https://github.com/mongodb/mongo-java-driver/commit/8c5a20d786e66ee4c4b0d743f0f80bf681c419be
    https://github.com/ratpack/ratpack/commit/2581441eda268c45306423dd4c515514d98a14a0
    https://github.com/gradle/gradle/commit/ba1da95200d080aef6251f13ced0ca67dff282be
    https://github.com/brettwooldridge/HikariCP/commit/1571049ec04b1e7e6f082ed5ec071584e7200c12
    https://github.com/spring-attic/spring-roo/commit/0bb4cca1105fc6eb86e7c4b75bfff3dbbd55f0c8
    https://github.com/hazelcast/hazelcast/commit/f1e26fa73074a89680a2e1756d85eb80ad87c3bf
*/
