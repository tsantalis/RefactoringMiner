package org.refactoringminer.astDiff.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.refactoringminer.astDiff.utils.ExportUtils.getFileNameFromSrcDiff;
import static org.refactoringminer.astDiff.utils.ExportUtils.getFinalFilePath;
import static org.refactoringminer.astDiff.utils.ExportUtils.getFinalFolderPath;
import static org.refactoringminer.astDiff.utils.ExportUtils.getSrcASTDiffFromFile;
import static org.refactoringminer.astDiff.utils.UtilMethods.getCommitsMappingsPath;
import static org.refactoringminer.astDiff.utils.UtilMethods.getProjectDiffLocally;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ConvertWith;
import org.refactoringminer.astDiff.models.ASTDiff;
import org.refactoringminer.astDiff.utils.CaseInfo;
import org.refactoringminer.astDiff.utils.MappingExportModel;

import net.joshka.junit.json.params.JsonFileSource;

public class PythonDiffTest {
	private static final String dir = getCommitsMappingsPath();
    @ParameterizedTest(name= "{index}: {0}")

    @JsonFileSource(resources = "/astDiff/commits/python.json")
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
