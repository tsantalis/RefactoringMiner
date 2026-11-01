package org.refactoringminer.astDiff.utils;

import static org.refactoringminer.astDiff.utils.ExportUtils.getFinalFilePath;
import static org.refactoringminer.astDiff.utils.UtilMethods.getCommitsMappingsPath;
import static org.refactoringminer.astDiff.utils.UtilMethods.getDefect4jAfterDir;
import static org.refactoringminer.astDiff.utils.UtilMethods.getDefect4jBeforeDir;
import static org.refactoringminer.astDiff.utils.UtilMethods.getDefects4jMappingPath;
import static org.refactoringminer.astDiff.utils.UtilMethods.getProjectDiffLocally;
import static org.refactoringminer.astDiff.utils.UtilMethods.getSnapShotPath;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.refactoringminer.astDiff.models.ASTDiff;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Regenerates the expected mappings of AST diff test cases exactly as the tests compute and compare them,
 * i.e., ./gradlew regenerateExpectedMappings -Pdataset=commits/kotlin.json -Pcommits=6daac6d61c,297fa59817
 * or ./gradlew regenerateExpectedMappings -Pdataset=defects4j/cases.json -Pcommits=Compress:15
 * The dataset is a path under src/test/resources/astDiff, and the commits are full ids or prefixes (all cases when omitted).
 * The files are written in output order, and git shows what changed.
 */
public class RegenerateExpectedMappings {
	//the tests of these datasets skip a file outside the source files only when more than one source file is listed
	private static final Set<String> SKIP_ONLY_WITH_MULTIPLE_SOURCE_FILES = Set.of("commits/kotlin.json", "commits/typescript.json", "commits/python.json");

	public static void main(String[] args) throws Exception {
		if (args.length < 1 || args[0].isBlank()) {
			System.err.println("usage: RegenerateExpectedMappings <dataset, i.e., commits/kotlin.json or defects4j/cases.json> [comma-separated commit ids or prefixes]");
			System.exit(1);
		}
		String dataset = args[0];
		List<String> commits = args.length > 1 && !args[1].isBlank() ? List.of(args[1].split(",")) : List.of();
		File datasetFile = new File(System.getProperty("user.dir") + "/src/test/resources/astDiff/" + dataset);
		List<Map<String, Object>> cases = new ObjectMapper().readValue(datasetFile, List.class);
		boolean defects4j = dataset.startsWith("defects4j/");
		//the problematic cases are compared with their previous snapshot, while their expected files hold the curated mappings, which are never overwritten
		boolean problematic = dataset.endsWith("-problematic.json");
		//the Refactoring Oracle and Defects4J tests extend ConsideringCommentsVisitorTest, which parses the Java comments
		if (defects4j || dataset.startsWith("commits/cases"))
			System.setProperty("rm.jdt.comments", "true");
		int regenerated = 0;
		for (Map<String, Object> c : cases) {
			String repo = (String) c.get("repo");
			String commit = (String) c.get("commit");
			if (!commits.isEmpty() && commits.stream().noneMatch(selector -> selects(selector, repo, commit, defects4j)))
				continue;
			@SuppressWarnings("unchecked")
			List<String> srcFiles = (List<String>) c.get("src_files");
			List<String> written = defects4j ? regenerateDefects4j(repo, commit, problematic) : regenerateCommit(dataset, repo, commit, srcFiles, problematic);
			System.out.println(repo + " " + commit + ": " + written.size() + " file(s)");
			regenerated++;
		}
		System.out.println(regenerated + " case(s) regenerated");
		System.exit(0);
	}

	//a commit id prefix, or for Defects4J the exact bug id, optionally with its project, i.e., Compress:15 (15 alone also selects Closure 15, but not Closure 150)
	private static boolean selects(String selector, String repo, String commit, boolean defects4j) {
		if (defects4j)
			return selector.equals(commit) || selector.equals(repo + ":" + commit);
		return commit.startsWith(selector);
	}

	//as in the commit tests, i.e., KotlinDiffTest and JavaToKotlinDiffTest
	private static List<String> regenerateCommit(String dataset, String repo, String commit, List<String> srcFiles, boolean problematic) throws Exception {
		CaseInfo info = srcFiles == null ? new CaseInfo(repo, commit) : new CaseInfo(repo, commit, new LinkedHashSet<>(srcFiles));
		boolean partial = srcFiles != null && !srcFiles.isEmpty();
		boolean skipOnlyWithMultipleSourceFiles = SKIP_ONLY_WITH_MULTIPLE_SOURCE_FILES.contains(dataset);
		List<String> written = new ArrayList<>();
		//as in RefactoringOracleProblematicCasesTest, the problematic cases are computed from the commit URL
		Set<ASTDiff> astDiffs = problematic ? getProjectDiffLocally(info.makeURL()) : getProjectDiffLocally(info);
		for (ASTDiff astDiff : astDiffs) {
			if (partial && !srcFiles.contains(astDiff.getSrcPath()) && (!skipOnlyWithMultipleSourceFiles || srcFiles.size() > 1))
				continue;
			String path = getFinalFilePath(astDiff, getCommitsMappingsPath(), info.getRepo(), info.getCommit());
			written.add(write(astDiff, problematic ? getSnapShotPath(path) : path));
		}
		return written;
	}

	//as in Defects4JPerfectDiffTest, the bug id is the commit
	private static List<String> regenerateDefects4j(String project, String bugId, boolean problematic) throws Exception {
		Set<ASTDiff> astDiffs = new GitHistoryRefactoringMinerImpl().diffAtDirectories(
				Path.of(getDefect4jBeforeDir(project, bugId)), Path.of(getDefect4jAfterDir(project, bugId))).getDiffSet();
		List<String> written = new ArrayList<>();
		for (ASTDiff astDiff : astDiffs)
		{
			String path = getFinalFilePath(astDiff, getDefects4jMappingPath(), project, bugId);
			written.add(write(astDiff, problematic ? getSnapShotPath(path) : path));
		}
		return written;
	}

	private static String write(ASTDiff astDiff, String path) throws Exception {
		String calculated = MappingExportModel.exportString(astDiff.getAllMappings()).replaceAll("\\r\\n", "\n").replaceAll("\\r", "\n");
		FileUtils.writeStringToFile(new File(path), calculated, "utf-8");
		return path;
	}
}
