package org.refactoringminer.astDiff.tests;

import net.joshka.junit.json.params.JsonFileSource;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ConvertWith;
import org.refactoringminer.astDiff.models.ASTDiff;
import org.refactoringminer.astDiff.utils.CaseInfo;
import org.refactoringminer.astDiff.utils.MappingExportModel;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.fail;
import static org.refactoringminer.astDiff.utils.ExportUtils.*;
import static org.refactoringminer.astDiff.utils.UtilMethods.*;

/**
 * @author  Pourya Alikhani Fard pouryafard75@gmail.com
 */

//@Isolated
public class RefactoringOraclePerfectDiffTest {
    private static final String dir = getCommitsMappingsPath();
    @ParameterizedTest(name= "{index}: {0}")

    @JsonFileSource(resources = {"/astDiff/commits/cases.json", "/astDiff/commits/cases-miscellaneous.json"})
//    @JsonFileSource(resources = "/astDiff/commits/cases-problematic.json")
    public void testSubTreeMappings(@ConvertWith(CaseInfo.CaseInfoConverter.class) CaseInfo info) throws Exception {
        File mappingsDirFile = new File(getFinalFolderPath(dir, info.getRepo(), info.getCommit()));
        String[] files = mappingsDirFile.list();
        List<String> expectedFilesList = new ArrayList<>(List.of(Objects.requireNonNull(files)));
        boolean partial = info.getSrc_files() != null && !info.getSrc_files().isEmpty();


        Set<ASTDiff> astDiffs = getProjectDiffLocally(info.makeURL());
        boolean hit = false;
        for (ASTDiff astDiff : astDiffs) {
            String finalFilePath = getFinalFilePath(astDiff, dir, info.getRepo(), info.getCommit());
            if (partial)
                if (!info.getSrc_files().contains(astDiff.getSrcPath()))
                    continue;
            hit = true;
            String calculated = MappingExportModel.exportString(astDiff.getAllMappings()).replaceAll("\\r\\n", "\n").replaceAll("\\r", "\n");
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
            fail(expectedDiffName + " not generated for " + expectedDiffName);
        }
        if (!hit)
            fail("No diff checked for " + info.getRepo() + "/commit/" + info.getCommit());
    }
}