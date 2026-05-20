package org.refactoringminer.mcp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.refactoringminer.api.RefactoringType;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record McpRefactoringIntent(String type, List<String> beforeFilePaths, List<String> afterFilePaths,
		List<String> classNames, List<String> methodNames, List<String> fieldNames, String descriptionContains) {
	public McpRefactoringIntent {
		type = normalizeType(type);
		beforeFilePaths = normalizeList(beforeFilePaths);
		afterFilePaths = normalizeList(afterFilePaths);
		classNames = normalizeList(classNames);
		methodNames = normalizeList(methodNames);
		fieldNames = normalizeList(fieldNames);
		descriptionContains = normalizeOptional(descriptionContains);
	}

	public static McpRefactoringIntent ofType(String type) {
		return new McpRefactoringIntent(type, List.of(), List.of(), List.of(), List.of(), List.of(), null);
	}

	boolean hasFilters() {
		return !beforeFilePaths.isEmpty() || !afterFilePaths.isEmpty() || !classNames.isEmpty()
				|| !methodNames.isEmpty() || !fieldNames.isEmpty() || descriptionContains != null;
	}

	private static String normalizeType(String type) {
		if (type == null || type.isBlank()) {
			throw new IllegalArgumentException("intent.type must be a non-empty RefactoringMiner refactoring type.");
		}
		String trimmed = type.trim();
		try {
			return RefactoringType.fromName(trimmed).getDisplayName();
		} catch (IllegalArgumentException ignored) {
			try {
				return RefactoringType.valueOf(trimmed.toUpperCase().replace(' ', '_')).getDisplayName();
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException("Unknown refactoring type: " + type);
			}
		}
	}

	private static List<String> normalizeList(List<String> values) {
		if (values == null || values.isEmpty()) {
			return Collections.emptyList();
		}
		List<String> normalized = new ArrayList<>();
		for (String value : values) {
			String optional = normalizeOptional(value);
			if (optional != null) {
				normalized.add(optional);
			}
		}
		return List.copyOf(normalized);
	}

	private static String normalizeOptional(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}
}
