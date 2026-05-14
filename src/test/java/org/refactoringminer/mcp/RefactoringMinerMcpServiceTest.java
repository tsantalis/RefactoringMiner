package org.refactoringminer.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.LinkedHashMap;
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

class RefactoringMinerMcpServiceTest {
	@Test
	void summarizeProjectDiffReturnsCompactBoundedResult() {
		Map<String, String> before = new LinkedHashMap<>();
		before.put("src/main/java/A.java", "class A { void f() {} }");
		Map<String, String> after = new LinkedHashMap<>();
		after.put("src/main/java/A.java", "class A { void g() {} }");
		ProjectASTDiff diff = new ProjectASTDiff(before, after);
		diff.setRefactorings(List.of(new FakeRefactoring("Rename Method f() renamed to g() in class A"),
				new FakeRefactoring("Rename Method h() renamed to i() in class A")));

		McpAnalysisResult result = McpAnalysisResult.ok(diff, 1);

		assertEquals("ok", result.status());
		assertEquals(2, result.refactoringCount());
		assertEquals(1, result.filesBefore());
		assertEquals(1, result.filesAfter());
		assertEquals(1, result.refactorings().size());
		assertEquals("Rename Method", result.refactorings().get(0).type());
		assertFalse(result.refactorings().get(0).leftSideLocations().isEmpty());
		assertEquals(1, result.warnings().size());
		assertTrue(result.warnings().get(0).contains("Refactorings truncated to 1 of 2"));
	}

	@Test
	void analyzeFileContentsReturnsErrorShapeForValidationFailures() {
		RefactoringMinerMcpService service = new RefactoringMinerMcpService((before, after) -> {
			throw new AssertionError("differ should not run for invalid input");
		});

		McpAnalysisResult result = service.analyzeFileContents(Collections.emptyMap(), Collections.emptyMap(), 20);

		assertEquals("error", result.status());
		assertEquals(0, result.refactoringCount());
		assertTrue(result.summary().contains("At least one before or after file"));
		assertFalse(result.warnings().isEmpty());
	}

	@Test
	void analyzeFileContentsUsesExistingDifferAndPreservesCounts() {
		RefactoringMinerMcpService service = new RefactoringMinerMcpService((before, after) -> {
			ProjectASTDiff diff = new ProjectASTDiff(before, after);
			diff.setRefactorings(List.of());
			return diff;
		});
		Map<String, String> before = Map.of("src/main/java/A.java", "class A { void f() {} }");
		Map<String, String> after = Map.of("src/main/java/A.java", "class A { void f() { int x = 1; } }");

		McpAnalysisResult result = service.analyzeFileContents(before, after, 20);

		assertEquals("ok", result.status());
		assertEquals(1, result.filesBefore());
		assertEquals(1, result.filesAfter());
		assertEquals(0, result.refactoringCount());
		assertTrue(result.summary().contains("0 refactorings"));
	}

	@Test
	void diffFileContentsUsesLauncherAndReturnsLocalBrowserResult() {
		RefactoringMinerMcpService service = new RefactoringMinerMcpService((before, after) -> {
			ProjectASTDiff diff = new ProjectASTDiff(before, after);
			diff.setRefactorings(List.of(new FakeRefactoring("Rename Method f() renamed to g() in class A")));
			return diff;
		}, (repositoryPath, commitId, parentIndex) -> new ProjectASTDiff(Map.of(), Map.of()),
				(cloneUrl, pullRequestId, timeoutSeconds) -> new ProjectASTDiff(Map.of(), Map.of()),
				(beforePath, afterPath) -> new ProjectASTDiff(Map.of(), Map.of()),
				(diff, port, inputSummary, warnings) -> McpDiffBrowserResult.ok(diff, port, inputSummary, warnings));
		Map<String, String> before = Map.of("src/main/java/A.java", "class A { void f() {} }");
		Map<String, String> after = Map.of("src/main/java/A.java", "class A { void g() {} }");

		McpDiffBrowserResult result = service.diffFileContents(before, after, 6790);

		assertEquals("ok", result.status());
		assertEquals("http://127.0.0.1:6790", result.url());
		assertEquals("Starting server: http://127.0.0.1:6790", result.message());
		assertEquals(1, result.refactoringCount());
		assertEquals(1, result.filesBefore());
		assertEquals(1, result.filesAfter());
		assertEquals(List.of("src/main/java/A.java"), result.affectedFiles());
		assertTrue(result.inputSummary().contains("Explicit file contents"));
	}

	@Test
	void diffFileContentsReturnsErrorShapeForLauncherFailures() {
		RefactoringMinerMcpService service = new RefactoringMinerMcpService((before, after) -> new ProjectASTDiff(before, after),
				(repositoryPath, commitId, parentIndex) -> new ProjectASTDiff(Map.of(), Map.of()),
				(cloneUrl, pullRequestId, timeoutSeconds) -> new ProjectASTDiff(Map.of(), Map.of()),
				(beforePath, afterPath) -> new ProjectASTDiff(Map.of(), Map.of()),
				(diff, port, inputSummary, warnings) -> {
					throw new IllegalArgumentException("port must be between 1 and 65535.");
				});

		McpDiffBrowserResult result = service.diffFileContents(
				Map.of("src/main/java/A.java", "class A {}"),
				Map.of("src/main/java/A.java", "class A { int x; }"), 0);

		assertEquals("error", result.status());
		assertTrue(result.summary().contains("port must be between 1 and 65535"));
		assertEquals(0, result.refactoringCount());
		assertFalse(result.warnings().isEmpty());
	}

	private static class FakeRefactoring implements Refactoring {
		private final String description;

		FakeRefactoring(String description) {
			this.description = description;
		}

		@Override
		public RefactoringType getRefactoringType() {
			return RefactoringType.RENAME_METHOD;
		}

		@Override
		public String getName() {
			return "Rename Method";
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
			return List.of(new CodeRange("src/main/java/A.java", 1, 1, 10, 18,
					CodeElementType.METHOD_DECLARATION, 10, 18).setDescription("original method").setCodeElement("f()"));
		}

		@Override
		public List<CodeRange> rightSide() {
			return List.of(new CodeRange("src/main/java/A.java", 1, 1, 10, 18,
					CodeElementType.METHOD_DECLARATION, 10, 18).setDescription("renamed method").setCodeElement("g()"));
		}

		@Override
		public String toString() {
			return description;
		}
	}
}
