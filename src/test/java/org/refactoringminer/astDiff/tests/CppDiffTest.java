package org.refactoringminer.astDiff.tests;

import static org.junit.jupiter.api.Assertions.fail;
import static org.refactoringminer.astDiff.utils.ExportUtils.getFileNameFromSrcDiff;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.refactoringminer.astDiff.models.ASTDiff;
import org.refactoringminer.astDiff.utils.MappingExportModel;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.skyscreamer.jsonassert.JSONAssert;

import com.fasterxml.jackson.core.JsonProcessingException;

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
}
