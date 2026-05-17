package org.refactoringminer.mcp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import gui.webdiff.WebDiff;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.astDiff.models.ProjectASTDiff;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record McpDiffBrowserResult(String status, String summary, String message, String url, Integer port,
		String inputSummary, int refactoringCount, int astDiffCount, int moveAstDiffCount, int filesBefore,
		int filesAfter, List<String> affectedFiles, List<McpAstDiffResult> astDiffs, List<String> warnings) {
	private static final int MAX_AFFECTED_FILES = 50;
	private static final int DEFAULT_MAX_AST_DIFFS = 20;
	private static final int DEFAULT_MAX_ACTIONS_PER_AST_DIFF = 20;

	public static final String OK = "ok";
	public static final String ERROR = "error";

	public McpDiffBrowserResult {
		affectedFiles = affectedFiles == null ? Collections.emptyList() : List.copyOf(affectedFiles);
		astDiffs = astDiffs == null ? Collections.emptyList() : List.copyOf(astDiffs);
		warnings = warnings == null ? Collections.emptyList() : List.copyOf(warnings);
	}

	public static McpDiffBrowserResult ok(ProjectASTDiff diff, int port, String inputSummary,
			List<String> additionalWarnings) {
		return ok(diff, port, WebDiff.LOCAL_HOST, inputSummary, additionalWarnings);
	}

	public static McpDiffBrowserResult ok(ProjectASTDiff diff, int port, String inputSummary,
			List<String> additionalWarnings, int maxAstDiffs, int maxActionsPerAstDiff) {
		return ok(diff, port, WebDiff.LOCAL_HOST, inputSummary, additionalWarnings, maxAstDiffs,
				maxActionsPerAstDiff);
	}

	public static McpDiffBrowserResult ok(ProjectASTDiff diff, int port, String publicHost, String inputSummary,
			List<String> additionalWarnings) {
		return ok(diff, port, publicHost, inputSummary, additionalWarnings, DEFAULT_MAX_AST_DIFFS,
				DEFAULT_MAX_ACTIONS_PER_AST_DIFF);
	}

	public static McpDiffBrowserResult ok(ProjectASTDiff diff, int port, String publicHost, String inputSummary,
			List<String> additionalWarnings, int maxAstDiffs, int maxActionsPerAstDiff) {
		List<Refactoring> refactorings = diff.getRefactorings() == null ? Collections.emptyList() : diff.getRefactorings();
		int astDiffCount = diff.getDiffSet() == null ? 0 : diff.getDiffSet().size();
		int moveAstDiffCount = diff.getMoveDiffSet() == null ? 0 : diff.getMoveDiffSet().size();
		int filesBefore = diff.getFileContentsBefore() == null ? 0 : diff.getFileContentsBefore().size();
		int filesAfter = diff.getFileContentsAfter() == null ? 0 : diff.getFileContentsAfter().size();

		List<String> warnings = new ArrayList<>();
		if (additionalWarnings != null) {
			warnings.addAll(additionalWarnings);
		}
		List<String> affectedFiles = affectedFiles(diff, warnings);
		List<McpAstDiffResult> astDiffs = McpAstDiffResult.from(diff, maxAstDiffs, maxActionsPerAstDiff, warnings);
		String url = WebDiff.localUrl(publicHost, port);
		String summary = String.format("Started local AST diff browser with %d refactorings, %d AST diffs, %d moved AST diffs across %d before files and %d after files.",
				refactorings.size(), astDiffCount, moveAstDiffCount, filesBefore, filesAfter);

		return new McpDiffBrowserResult(OK, summary, WebDiff.startupMessage(publicHost, port), url, port, inputSummary,
				refactorings.size(), astDiffCount, moveAstDiffCount, filesBefore, filesAfter, affectedFiles, astDiffs,
				warnings);
	}

	public static McpDiffBrowserResult error(String summary, Integer port, String inputSummary, List<String> warnings) {
		return new McpDiffBrowserResult(ERROR, summary, null, null, port, inputSummary, 0, 0, 0, 0, 0,
				Collections.emptyList(), Collections.emptyList(), warnings);
	}

	private static List<String> affectedFiles(ProjectASTDiff diff, List<String> warnings) {
		Set<String> files = new LinkedHashSet<>();
		if (diff.getFileContentsBefore() != null) {
			files.addAll(diff.getFileContentsBefore().keySet());
		}
		if (diff.getFileContentsAfter() != null) {
			files.addAll(diff.getFileContentsAfter().keySet());
		}
		List<String> affectedFiles = new ArrayList<>(files);
		if (affectedFiles.size() > MAX_AFFECTED_FILES) {
			warnings.add(String.format("Affected files truncated to %d of %d.", MAX_AFFECTED_FILES,
					affectedFiles.size()));
			return List.copyOf(affectedFiles.subList(0, MAX_AFFECTED_FILES));
		}
		return List.copyOf(affectedFiles);
	}
}
