package org.refactoringminer.mcp;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record McpValidationResult(String status, String summary, McpRefactoringIntent intent, int refactoringCount,
		int candidateCount, List<McpRefactoringResult> matches, List<McpRefactoringResult> candidates,
		List<String> warnings) {
	public static final String MATCHED = "matched";
	public static final String MISSING = "missing";
	public static final String AMBIGUOUS = "ambiguous";
	public static final String ERROR = "error";

	public McpValidationResult {
		matches = matches == null ? Collections.emptyList() : List.copyOf(matches);
		candidates = candidates == null ? Collections.emptyList() : List.copyOf(candidates);
		warnings = warnings == null ? Collections.emptyList() : List.copyOf(warnings);
	}

	public static McpValidationResult matched(McpRefactoringIntent intent, int refactoringCount,
			List<McpRefactoringResult> matches, List<String> warnings) {
		return new McpValidationResult(MATCHED, "Intent matched one refactoring.", intent, refactoringCount,
				matches.size(), matches, Collections.emptyList(), warnings);
	}

	public static McpValidationResult missing(McpRefactoringIntent intent, int refactoringCount,
			List<McpRefactoringResult> candidates, List<String> warnings) {
		return new McpValidationResult(MISSING, "Intent was not found in the analyzed refactorings.", intent,
				refactoringCount, candidates.size(), Collections.emptyList(), candidates, warnings);
	}

	public static McpValidationResult ambiguous(McpRefactoringIntent intent, int refactoringCount,
			List<McpRefactoringResult> matches, List<String> warnings) {
		return new McpValidationResult(AMBIGUOUS, "Intent matched multiple refactorings.", intent, refactoringCount,
				matches.size(), matches, Collections.emptyList(), warnings);
	}

	public static McpValidationResult error(String summary, McpRefactoringIntent intent, List<String> warnings) {
		return new McpValidationResult(ERROR, summary, intent, 0, 0, Collections.emptyList(), Collections.emptyList(),
				warnings);
	}
}
