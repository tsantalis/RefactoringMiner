package org.refactoringminer.astDiff.tests;

import static org.junit.jupiter.api.Assertions.fail;
import static org.refactoringminer.astDiff.utils.ExportUtils.getFileNameFromSrcDiff;
import static org.refactoringminer.astDiff.utils.ExportUtils.getFinalFilePath;
import static org.refactoringminer.astDiff.utils.ExportUtils.getFinalFolderPath;
import static org.refactoringminer.astDiff.utils.ExportUtils.getSrcASTDiffFromFile;
import static org.refactoringminer.astDiff.utils.UtilMethods.getCommitsMappingsPath;
import static org.refactoringminer.astDiff.utils.UtilMethods.getProjectDiffLocally;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ConvertWith;
import org.refactoringminer.astDiff.models.ASTDiff;
import org.refactoringminer.astDiff.utils.CaseInfo;
import org.refactoringminer.astDiff.utils.MappingExportModel;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import net.joshka.junit.json.params.JsonFileSource;
import org.refactoringminer.astDiff.utils.MappingAssertions;

public class PythonDiffTest {
	public static final String REPOS = System.getProperty("user.dir") + "/src/test/resources/oracle/commits/python";
	public static final String MAPPING_PATH = System.getProperty("user.dir") + "/src/test/resources/astDiff/python";

	@Test
	public void testSubTreeMappings() throws Exception {
		MappingAssertions.Failures failures = new MappingAssertions.Failures();
		Set<ASTDiff> astDiffs = new GitHistoryRefactoringMinerImpl().diffAtDirectories(
				Path.of(REPOS + "/v1"),
				Path.of(REPOS + "/v2")).getDiffSet();
		for (ASTDiff astDiff : astDiffs) {
			String finalFilePath = MAPPING_PATH + "/" + astDiff.getSrcPath().replace(".py","").replace("/",".") + ".json";
			String calculated = MappingExportModel.exportString(astDiff.getAllMappings()).replaceAll("\\r\\n", "\n").replaceAll("\\r", "\n");
			String msg = String.format("Failed for srcFileName: %s",astDiff.getSrcPath());
			String expected = null;
			try {
				expected = FileUtils.readFileToString(new File(finalFilePath), "utf-8");
			} catch (IOException e) {
				fail("File not found: " + finalFilePath + " , srcFileName: " + astDiff.getSrcPath());
			}
			failures.check(expected, calculated, msg);
		}
		failures.assertNone();
	}
	private static final String dir = getCommitsMappingsPath();
    @ParameterizedTest(name= "{index}: {0}")

    @JsonFileSource(resources = "/astDiff/commits/python.json")
    public void testSubTreeMappings(@ConvertWith(CaseInfo.CaseInfoConverter.class) CaseInfo info) throws Exception {
        MappingAssertions.Failures failures = new MappingAssertions.Failures();
        File mappingsDirFile = new File(getFinalFolderPath(dir, info.getRepo(), info.getCommit()));
        String[] files = mappingsDirFile.list();
        List<String> expectedFilesList = new ArrayList<>(List.of(Objects.requireNonNull(files)));
        boolean partial = info.getSrc_files() != null && !info.getSrc_files().isEmpty();

        Set<ASTDiff> astDiffs = getProjectDiffLocally(info);
        boolean hit = false;
        for (ASTDiff astDiff : astDiffs) {
            String finalFilePath = getFinalFilePath(astDiff, dir, info.getRepo(), info.getCommit());
            if (partial)
                if (info.getSrc_files().size() > 1 && !info.getSrc_files().contains(astDiff.getSrcPath()))
                    continue;
            hit = true;
            String calculated = MappingExportModel.exportString(astDiff.getAllMappings()).replaceAll("\\r\\n", "\n").replaceAll("\\r", "\n");
            String msg = String.format("Failed for %s/commit/%s , srcFileName: %s",info.getRepo().replace(".git",""),info.getCommit(),astDiff.getSrcPath());
            String expected = null;
            try {
                expected = FileUtils.readFileToString(new File(finalFilePath), "utf-8");
            } catch (IOException e) {
                fail("File not found: " + finalFilePath + " for " + info.getRepo() + "/commit/" + info.getCommit() + " , srcFileName: " + astDiff.getSrcPath());
            }
            failures.check(expected, calculated, msg);
            expectedFilesList.remove(getFileNameFromSrcDiff(astDiff.getSrcPath()));
        }
        failures.assertNone();
        for (String expectedButNotGeneratedFile : expectedFilesList) {
            String expectedDiffName = getSrcASTDiffFromFile(expectedButNotGeneratedFile);
            fail(expectedDiffName + " not generated for " + expectedDiffName);
        }
        if (!hit)
            fail("No diff checked for " + info.getRepo() + "/commit/" + info.getCommit());
    }
}
