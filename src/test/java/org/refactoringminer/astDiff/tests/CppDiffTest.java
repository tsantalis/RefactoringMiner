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
import org.skyscreamer.jsonassert.JSONAssert;

import com.fasterxml.jackson.core.JsonProcessingException;

import net.joshka.junit.json.params.JsonFileSource;

public class CppDiffTest {
	public static final String REPOS = System.getProperty("user.dir") + "/src/test/resources/oracle/commits/cpp";
	public static final String MAPPING_PATH = System.getProperty("user.dir") + "/src/test/resources/astDiff/cpp";

	@Test
	public void testSubTreeMappings() throws JsonProcessingException {
		File mappingsDirFile = new File(MAPPING_PATH);
		String[] files = mappingsDirFile.list();
		List<String> expectedFilesList = new ArrayList<>(List.of(Objects.requireNonNull(files)));
		Set<ASTDiff> astDiffs = new GitHistoryRefactoringMinerImpl().diffAtDirectories(
				Path.of(REPOS + "/v1"),
				Path.of(REPOS + "/v2")).getDiffSet();
		for (ASTDiff astDiff : astDiffs) {
			String finalFilePath = MAPPING_PATH + "/" + astDiff.getSrcPath().replace(".cpp","").replace("/",".") + ".json";
			String calculated = MappingExportModel.exportString(astDiff.getAllMappings());
			String msg = String.format("Failed for srcFileName: %s",astDiff.getSrcPath());
			String expected = null;
			try {
				expected = FileUtils.readFileToString(new File(finalFilePath), "utf-8");
			} catch (IOException e) {
				fail("File not found: " + finalFilePath + " , srcFileName: " + astDiff.getSrcPath());
			}
			JSONAssert.assertEquals(msg,expected, calculated, false);
			expectedFilesList.remove(getFileNameFromSrcDiff(astDiff.getSrcPath()));
		}
	}
	private static final String dir = getCommitsMappingsPath();
	@ParameterizedTest(name= "{index}: {0}")

	@JsonFileSource(resources = "/astDiff/commits/cpp.json")
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
