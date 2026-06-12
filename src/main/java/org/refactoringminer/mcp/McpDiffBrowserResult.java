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
			int filesAfter, List<McpRefactoringResult> refactorings, List<String> affectedFiles, List<String> warnings) {
	private static final int MAX_AFFECTED_FILES = 50;
	private static final int DEFAULT_MAX_REFACTORINGS = 20;

	public static final String OK = "ok";
	public static final String ERROR = "error";

	public McpDiffBrowserResult {
		refactorings = refactorings == null ? Collections.emptyList() : List.copyOf(refactorings);
		affectedFiles = affectedFiles == null ? Collections.emptyList() : List.copyOf(affectedFiles);
		warnings = warnings == null ? Collections.emptyList() : List.copyOf(warnings);
	}

	public static McpDiffBrowserResult ok(ProjectASTDiff diff, int port, String inputSummary,
			List<String> additionalWarnings) {
		return ok(diff, port, WebDiff.LOCAL_HOST, inputSummary, additionalWarnings, DEFAULT_MAX_REFACTORINGS);
	}

	public static McpDiffBrowserResult ok(ProjectASTDiff diff, int port, String inputSummary,
			List<String> additionalWarnings, int maxRefactorings) {
		return ok(diff, port, WebDiff.LOCAL_HOST, inputSummary, additionalWarnings, maxRefactorings);
	}

	public static McpDiffBrowserResult ok(ProjectASTDiff diff, int port, String publicHost, String inputSummary,
			List<String> additionalWarnings) {
		return ok(diff, port, publicHost, inputSummary, additionalWarnings, DEFAULT_MAX_REFACTORINGS);
	}

	public static McpDiffBrowserResult ok(ProjectASTDiff diff, int port, String publicHost, String inputSummary,
			List<String> additionalWarnings, int maxRefactorings) {
		List<Refactoring> refactorings = diff.getRefactorings() == null ? Collections.emptyList() : diff.getRefactorings();
		int boundedSize = Math.min(refactorings.size(), maxRefactorings);
		List<McpRefactoringResult> boundedRefactorings = new ArrayList<>();
		for (int i = 0; i < boundedSize; i++) {
			boundedRefactorings.add(McpRefactoringResult.from(refactorings.get(i)));
		}
		int astDiffCount = diff.getDiffSet() == null ? 0 : diff.getDiffSet().size();
		int moveAstDiffCount = diff.getMoveDiffSet() == null ? 0 : diff.getMoveDiffSet().size();
		int filesBefore = diff.getFileContentsBefore() == null ? 0 : diff.getFileContentsBefore().size();
		int filesAfter = diff.getFileContentsAfter() == null ? 0 : diff.getFileContentsAfter().size();

		List<String> warnings = new ArrayList<>();
		if (additionalWarnings != null) {
			warnings.addAll(additionalWarnings);
		}
		if (boundedSize < refactorings.size()) {
			warnings.add(String.format("Refactorings truncated to %d of %d.", boundedSize, refactorings.size()));
		}
		List<String> affectedFiles = affectedFiles(diff, warnings);
		String url = WebDiff.localUrl(publicHost, port);
		String summary = String.format("Started local AST diff browser with %d refactorings, %d AST diffs, %d moved AST diffs across %d before files and %d after files.",
				refactorings.size(), astDiffCount, moveAstDiffCount, filesBefore, filesAfter);

		return new McpDiffBrowserResult(OK, summary, WebDiff.startupMessage(publicHost, port), url, port, inputSummary,
				refactorings.size(), astDiffCount, moveAstDiffCount, filesBefore, filesAfter, boundedRefactorings,
				affectedFiles, warnings);
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
