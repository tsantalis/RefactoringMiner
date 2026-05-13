package org.refactoringminer.mcp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.refactoringminer.api.Refactoring;
import org.refactoringminer.astDiff.models.ProjectASTDiff;

public record McpAnalysisResult(String status, String summary, int refactoringCount, int astDiffCount,
		int moveAstDiffCount, int filesBefore, int filesAfter, List<McpRefactoringResult> refactorings,
		List<String> warnings) {

	public static McpAnalysisResult ok(ProjectASTDiff diff, int maxRefactorings) {
		return ok(diff, maxRefactorings, Collections.emptyList());
	}

	public static McpAnalysisResult ok(ProjectASTDiff diff, int maxRefactorings, List<String> additionalWarnings) {
		List<Refactoring> sourceRefactorings = diff.getRefactorings() == null
				? Collections.emptyList() : diff.getRefactorings();
		int boundedSize = Math.min(sourceRefactorings.size(), maxRefactorings);
		List<McpRefactoringResult> boundedRefactorings = new ArrayList<>();
		for (int i = 0; i < boundedSize; i++) {
			boundedRefactorings.add(McpRefactoringResult.from(sourceRefactorings.get(i)));
		}

		List<String> warnings = new ArrayList<>();
		warnings.addAll(additionalWarnings);
		if (boundedSize < sourceRefactorings.size()) {
			warnings.add(String.format("Refactorings truncated to %d of %d.", boundedSize, sourceRefactorings.size()));
		}

		int astDiffCount = diff.getDiffSet() == null ? 0 : diff.getDiffSet().size();
		int moveAstDiffCount = diff.getMoveDiffSet() == null ? 0 : diff.getMoveDiffSet().size();
		int filesBefore = diff.getFileContentsBefore() == null ? 0 : diff.getFileContentsBefore().size();
		int filesAfter = diff.getFileContentsAfter() == null ? 0 : diff.getFileContentsAfter().size();
		String summary = String.format("%d refactorings, %d AST diffs, %d moved AST diffs across %d before files and %d after files.",
				sourceRefactorings.size(), astDiffCount, moveAstDiffCount, filesBefore, filesAfter);

		return new McpAnalysisResult("ok", summary, sourceRefactorings.size(), astDiffCount, moveAstDiffCount,
				filesBefore, filesAfter, boundedRefactorings, warnings);
	}

	public static McpAnalysisResult error(String summary, List<String> warnings) {
		return new McpAnalysisResult("error", summary, 0, 0, 0, 0, 0, Collections.emptyList(), warnings);
	}
}
