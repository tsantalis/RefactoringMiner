package org.refactoringminer.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class McpValidationContractTest {
	@Test
	void intentAcceptsRefactoringDisplayName() {
		McpRefactoringIntent intent = McpRefactoringIntent.ofType("Rename Method");

		assertEquals("Rename Method", intent.type());
		assertFalse(intent.hasFilters());
	}

	@Test
	void intentNormalizesEnumStyleRefactoringType() {
		McpRefactoringIntent intent = McpRefactoringIntent.ofType("RENAME_METHOD");

		assertEquals("Rename Method", intent.type());
	}

	@Test
	void intentTrimsOptionalFilters() {
		McpRefactoringIntent intent = new McpRefactoringIntent("Extract Method", List.of(" src/A.java ", ""),
				List.of("src/B.java"), List.of(" Worker "), List.of(" count "), List.of(" value "),
				" extracted from ");

		assertEquals(List.of("src/A.java"), intent.beforeFilePaths());
		assertEquals(List.of("Worker"), intent.classNames());
		assertEquals("extracted from", intent.descriptionContains());
		assertEquals("Extract Method", intent.type());
		assertTrue(intent.hasFilters());
	}

	@Test
	void intentRejectsUnknownRefactoringType() {
		assertThrows(IllegalArgumentException.class, () -> McpRefactoringIntent.ofType("Invented Refactoring"));
	}

	@Test
	void validationResultUsesLowercaseVerdicts() {
		McpRefactoringIntent intent = McpRefactoringIntent.ofType("Rename Method");

		assertEquals("matched", McpValidationResult.matched(intent, 1, List.of(), List.of()).status());
		assertEquals("missing", McpValidationResult.missing(intent, 1, List.of(), List.of()).status());
		assertEquals("ambiguous", McpValidationResult.ambiguous(intent, 2, List.of(), List.of()).status());
		assertEquals("error", McpValidationResult.error("bad input", intent, List.of()).status());
	}
}
