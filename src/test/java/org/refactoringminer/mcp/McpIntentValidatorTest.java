package org.refactoringminer.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.Test;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.astDiff.models.ProjectASTDiff;

import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.diff.CodeRange;

class McpIntentValidatorTest {
	@Test
	void validationMatchesSingleRefactoringByTypeAndFilters() {
		ProjectASTDiff diff = diffWith(new FakeRefactoring("Rename Method",
				"Rename Method public count(text String) : int renamed to public measure(text String) : int in class e2e.Worker",
				"src/main/java/e2e/Worker.java", "public count(text String) : int",
				"src/main/java/e2e/Worker.java", "public measure(text String) : int"));
		McpRefactoringIntent intent = new McpRefactoringIntent("Rename Method",
				List.of("Worker.java"), List.of("Worker.java"), List.of("Worker"), List.of("measure"), List.of(),
				"count");

		McpValidationResult result = new McpIntentValidator().validate(diff, intent, 10);

		assertEquals("matched", result.status());
		assertEquals(1, result.refactoringCount());
		assertEquals(1, result.candidateCount());
		assertEquals(1, result.matches().size());
		assertTrue(result.candidates().isEmpty());
	}

	@Test
	void validationReturnsMissingWithSameTypeCandidatesWhenFiltersDoNotMatch() {
		ProjectASTDiff diff = diffWith(new FakeRefactoring("Rename Method",
				"Rename Method public count(text String) : int renamed to public measure(text String) : int in class e2e.Worker",
				"src/main/java/e2e/Worker.java", "public count(text String) : int",
				"src/main/java/e2e/Worker.java", "public measure(text String) : int"));
		McpRefactoringIntent intent = new McpRefactoringIntent("Rename Method",
				List.of(), List.of(), List.of("OtherClass"), List.of(), List.of(), null);

		McpValidationResult result = new McpIntentValidator().validate(diff, intent, 10);

		assertEquals("missing", result.status());
		assertEquals(1, result.candidateCount());
		assertEquals(1, result.candidates().size());
		assertTrue(result.matches().isEmpty());
	}

	@Test
	void validationReturnsAmbiguousForMultipleMatches() {
		ProjectASTDiff diff = diffWith(
				new FakeRefactoring("Rename Method", "Rename Method a() renamed to b() in class A",
						"src/main/java/A.java", "a()", "src/main/java/A.java", "b()"),
				new FakeRefactoring("Rename Method", "Rename Method c() renamed to d() in class C",
						"src/main/java/C.java", "c()", "src/main/java/C.java", "d()"));
		McpRefactoringIntent intent = McpRefactoringIntent.ofType("Rename Method");

		McpValidationResult result = new McpIntentValidator().validate(diff, intent, 10);

		assertEquals("ambiguous", result.status());
		assertEquals(2, result.candidateCount());
		assertEquals(2, result.matches().size());
	}

	@Test
	void validationBoundsAmbiguousEvidenceAndWarns() {
		ProjectASTDiff diff = diffWith(
				new FakeRefactoring("Rename Method", "Rename Method a() renamed to b() in class A",
						"src/main/java/A.java", "a()", "src/main/java/A.java", "b()"),
				new FakeRefactoring("Rename Method", "Rename Method c() renamed to d() in class C",
						"src/main/java/C.java", "c()", "src/main/java/C.java", "d()"));

		McpValidationResult result = new McpIntentValidator()
				.validate(diff, McpRefactoringIntent.ofType("Rename Method"), 1);

		assertEquals("ambiguous", result.status());
		assertEquals(2, result.candidateCount());
		assertEquals(1, result.matches().size());
		assertFalse(result.warnings().isEmpty());
		assertTrue(result.warnings().get(0).contains("truncated to 1 of 2"));
	}

	@Test
	void validationReturnsErrorForInvalidInputs() {
		McpValidationResult nullIntent = new McpIntentValidator().validate(diffWith(), null, 10);
		McpValidationResult nullDiff = new McpIntentValidator()
				.validate(null, McpRefactoringIntent.ofType("Rename Method"), 10);
		McpValidationResult badLimit = new McpIntentValidator()
				.validate(diffWith(), McpRefactoringIntent.ofType("Rename Method"), -1);

		assertEquals("error", nullIntent.status());
		assertEquals("error", nullDiff.status());
		assertEquals("error", badLimit.status());
	}

	@Test
	void serviceValidatesFileContentsWithExistingDiffer() {
		RefactoringMinerMcpService service = new RefactoringMinerMcpService((before, after) -> diffWith(
				new FakeRefactoring("Rename Method", "Rename Method f() renamed to g() in class A",
						"src/main/java/A.java", "f()", "src/main/java/A.java", "g()")));

		McpValidationResult result = service.validateFileContents(
				Map.of("src/main/java/A.java", "class A { void f() {} }"),
				Map.of("src/main/java/A.java", "class A { void g() {} }"),
				McpRefactoringIntent.ofType("Rename Method"), 10);

		assertEquals("matched", result.status());
		assertEquals(1, result.matches().size());
	}

	private static ProjectASTDiff diffWith(Refactoring... refactorings) {
		ProjectASTDiff diff = new ProjectASTDiff(
				Map.of("src/main/java/A.java", "class A { void f() {} }"),
				Map.of("src/main/java/A.java", "class A { void g() {} }"));
		diff.setRefactorings(List.of(refactorings));
		return diff;
	}

	private static class FakeRefactoring implements Refactoring {
		private final String name;
		private final String description;
		private final String leftPath;
		private final String leftCodeElement;
		private final String rightPath;
		private final String rightCodeElement;

		FakeRefactoring(String name, String description, String leftPath, String leftCodeElement, String rightPath,
				String rightCodeElement) {
			this.name = name;
			this.description = description;
			this.leftPath = leftPath;
			this.leftCodeElement = leftCodeElement;
			this.rightPath = rightPath;
			this.rightCodeElement = rightCodeElement;
		}

		@Override
		public RefactoringType getRefactoringType() {
			return RefactoringType.fromName(name);
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
			return Set.of();
		}

		@Override
		public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
			return Set.of();
		}

		@Override
		public List<CodeRange> leftSide() {
			return List.of(new CodeRange(leftPath, 1, 1, 10, 18,
					CodeElementType.METHOD_DECLARATION, 10, 18)
					.setDescription("before element").setCodeElement(leftCodeElement));
		}

		@Override
		public List<CodeRange> rightSide() {
			return List.of(new CodeRange(rightPath, 1, 1, 10, 18,
					CodeElementType.METHOD_DECLARATION, 10, 18)
					.setDescription("after element").setCodeElement(rightCodeElement));
		}

		@Override
		public String toString() {
			return description;
		}
	}
}
