package org.refactoringminer.mcp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.refactoringminer.api.Refactoring;
import org.refactoringminer.astDiff.models.ProjectASTDiff;

final class McpIntentValidator {
	McpValidationResult validate(ProjectASTDiff diff, McpRefactoringIntent intent, int maxCandidates) {
		return validate(diff, intent, maxCandidates, Collections.emptyList());
	}

	McpValidationResult validate(ProjectASTDiff diff, McpRefactoringIntent intent, int maxCandidates,
			List<String> additionalWarnings) {
		if (intent == null) {
			return McpValidationResult.error("intent is required.", null, List.of("intent=null"));
		}
		if (maxCandidates < 0) {
			return McpValidationResult.error("maxCandidates must be greater than or equal to 0.", intent,
					List.of("maxCandidates=" + maxCandidates));
		}
		if (diff == null) {
			return McpValidationResult.error("Validation did not produce a diff.", intent,
					List.of("RefactoringMiner returned null."));
		}

		List<Refactoring> refactorings = diff.getRefactorings() == null ? Collections.emptyList() : diff.getRefactorings();
		List<McpRefactoringResult> typeCandidates = new ArrayList<>();
		List<McpRefactoringResult> matches = new ArrayList<>();
		for (Refactoring refactoring : refactorings) {
			McpRefactoringResult candidate = McpRefactoringResult.from(refactoring);
			if (!candidate.type().equals(intent.type())) {
				continue;
			}
			typeCandidates.add(candidate);
			if (matchesFilters(candidate, intent)) {
				matches.add(candidate);
			}
		}

		if (matches.size() == 1) {
			return new McpValidationResult(McpValidationResult.MATCHED, "Intent matched one refactoring.", intent,
					refactorings.size(), 1, bounded(matches, maxCandidates), Collections.emptyList(),
					warnings(additionalWarnings, matches.size(), maxCandidates));
		}
		if (matches.size() > 1) {
			return new McpValidationResult(McpValidationResult.AMBIGUOUS, "Intent matched multiple refactorings.", intent,
					refactorings.size(), matches.size(), bounded(matches, maxCandidates), Collections.emptyList(),
					warnings(additionalWarnings, matches.size(), maxCandidates));
		}
		return new McpValidationResult(McpValidationResult.MISSING,
				"Intent was not found in the analyzed refactorings.", intent, refactorings.size(),
				typeCandidates.size(), Collections.emptyList(), bounded(typeCandidates, maxCandidates),
				warnings(additionalWarnings, typeCandidates.size(), maxCandidates));
	}

	private static boolean matchesFilters(McpRefactoringResult candidate, McpRefactoringIntent intent) {
		return matchesLocations(intent.beforeFilePaths(), candidate.leftSideLocations())
				&& matchesLocations(intent.afterFilePaths(), candidate.rightSideLocations())
				&& matchesText(intent.classNames(), searchText(candidate))
				&& matchesText(intent.methodNames(), searchText(candidate))
				&& matchesText(intent.fieldNames(), searchText(candidate))
				&& matchesDescription(intent.descriptionContains(), candidate.description());
	}

	private static boolean matchesLocations(List<String> expectedPaths, List<McpRefactoringResult.Location> locations) {
		if (expectedPaths.isEmpty()) {
			return true;
		}
		for (String expectedPath : expectedPaths) {
			boolean found = locations.stream()
					.anyMatch(location -> containsIgnoreCase(normalizePath(location.filePath()), normalizePath(expectedPath)));
			if (!found) {
				return false;
			}
		}
		return true;
	}

	private static boolean matchesText(List<String> filters, String searchText) {
		for (String filter : filters) {
			if (!containsIgnoreCase(searchText, filter)) {
				return false;
			}
		}
		return true;
	}

	private static boolean matchesDescription(String descriptionContains, String description) {
		return descriptionContains == null || containsIgnoreCase(description, descriptionContains);
	}

	private static String searchText(McpRefactoringResult candidate) {
		StringBuilder text = new StringBuilder(candidate.description());
		appendLocations(text, candidate.leftSideLocations());
		appendLocations(text, candidate.rightSideLocations());
		return text.toString();
	}

	private static void appendLocations(StringBuilder text, List<McpRefactoringResult.Location> locations) {
		for (McpRefactoringResult.Location location : locations) {
			text.append(' ').append(location.filePath())
					.append(' ').append(location.description())
					.append(' ').append(location.codeElement());
		}
	}

	private static List<McpRefactoringResult> bounded(List<McpRefactoringResult> candidates, int maxCandidates) {
		return List.copyOf(candidates.subList(0, Math.min(candidates.size(), maxCandidates)));
	}

	private static List<String> warnings(List<String> additionalWarnings, int totalCandidates, int maxCandidates) {
		List<String> warnings = new ArrayList<>();
		if (additionalWarnings != null) {
			warnings.addAll(additionalWarnings);
		}
		if (totalCandidates > maxCandidates) {
			warnings.add(String.format("Validation candidates truncated to %d of %d.", maxCandidates, totalCandidates));
		}
		return warnings;
	}

	private static boolean containsIgnoreCase(String value, String expected) {
		return value != null && expected != null
				&& value.toLowerCase(Locale.ROOT).contains(expected.toLowerCase(Locale.ROOT));
	}

	private static String normalizePath(String path) {
		return path == null ? "" : path.replace('\\', '/');
	}
}
