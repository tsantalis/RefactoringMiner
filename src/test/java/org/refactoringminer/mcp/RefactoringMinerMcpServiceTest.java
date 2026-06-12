package org.refactoringminer.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.actions.model.Update;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.tree.TypeSet;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.Test;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.astDiff.actions.model.MultiMove;
import org.refactoringminer.astDiff.models.ASTDiff;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;
import org.refactoringminer.astDiff.models.ProjectASTDiff;
import org.refactoringminer.astDiff.utils.Constants;

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
	void analyzeFileContentsCanReturnBoundedAstDiffSummaries() {
		RefactoringMinerMcpService service = new RefactoringMinerMcpService((before, after) -> {
			ProjectASTDiff diff = new ProjectASTDiff(before, after);
			diff.addASTDiff(astDiffWithUpdate());
			diff.setRefactorings(List.of());
			return diff;
		});
		Map<String, String> before = Map.of("src/main/java/A.java", "class A {\n  void f() {}\n}");
		Map<String, String> after = Map.of("src/main/java/A.java", "class A {\n  void g() {}\n}");

		McpAnalysisResult result = service.analyzeFileContents(before, after, 20, 100, 200_000, 1, 1);

		assertEquals("ok", result.status());
		assertEquals(1, result.astDiffs().size());
		McpAstDiffResult astDiff = result.astDiffs().get(0);
		assertEquals("standard", astDiff.kind());
		assertEquals("src/main/java/A.java", astDiff.beforeFilePath());
		assertEquals("src/main/java/A.java", astDiff.afterFilePath());
		assertEquals(1, astDiff.mappingCount());
		assertEquals(1, astDiff.actionCount());
		assertEquals(1, astDiff.sampleActions().size());
		assertEquals("update-node", astDiff.sampleActions().get(0).name());
		assertEquals(2, astDiff.sampleActions().get(0).startLine());
	}

	@Test
	void analyzeFileContentsOmitsActionSamplesWithoutTruncationWarningWhenLimitIsZero() {
		RefactoringMinerMcpService service = new RefactoringMinerMcpService((before, after) -> {
			ProjectASTDiff diff = new ProjectASTDiff(before, after);
			diff.addASTDiff(astDiffWithUpdate());
			diff.setRefactorings(List.of());
			return diff;
		});
		Map<String, String> before = Map.of("src/main/java/A.java", "class A {\n  void f() {}\n}");
		Map<String, String> after = Map.of("src/main/java/A.java", "class A {\n  void g() {}\n}");

		McpAnalysisResult result = service.analyzeFileContents(before, after, 20, 100, 200_000, 1, 0);

		assertEquals("ok", result.status());
		assertEquals(1, result.astDiffs().size());
		assertEquals(1, result.astDiffs().get(0).actionCount());
		assertTrue(result.astDiffs().get(0).sampleActions().isEmpty());
		assertTrue(result.warnings().stream().noneMatch(warning -> warning.contains("truncated to 0")));
	}

	@Test
	void analyzeFileContentsReportsMissingPositionsWithConsistentSentinels() {
		RefactoringMinerMcpService service = new RefactoringMinerMcpService((before, after) -> {
			ProjectASTDiff diff = new ProjectASTDiff(before, after);
			diff.addASTDiff(astDiffWithNullNodeAction());
			diff.setRefactorings(List.of());
			return diff;
		});
		Map<String, String> before = Map.of("src/main/java/A.java", "class A {}");
		Map<String, String> after = Map.of("src/main/java/A.java", "class A {}");

		McpAnalysisResult result = service.analyzeFileContents(before, after, 20, 100, 200_000, 1, 1);

		McpAstDiffResult.ActionSummary action = result.astDiffs().get(0).sampleActions().get(0);
		assertEquals(-1, action.startOffset());
		assertEquals(-1, action.endOffset());
		assertEquals(-1, action.startLine());
		assertEquals(-1, action.endLine());
	}

	@Test
	void analyzeFileContentsTreatsEndOffsetAsExclusiveForEndLine() {
		String content = "class A {\n  void f() {}\n}\n";
		RefactoringMinerMcpService service = new RefactoringMinerMcpService((before, after) -> {
			ProjectASTDiff diff = new ProjectASTDiff(before, after);
			diff.addASTDiff(astDiffWithWholeFileUpdate(content));
			diff.setRefactorings(List.of());
			return diff;
		});
		Map<String, String> before = Map.of("src/main/java/A.java", content);
		Map<String, String> after = Map.of("src/main/java/A.java", content);

		McpAnalysisResult result = service.analyzeFileContents(before, after, 20, 100, 200_000, 1, 1);

		McpAstDiffResult.ActionSummary action = result.astDiffs().get(0).sampleActions().get(0);
		assertEquals(1, action.startLine());
		assertEquals(3, action.endLine());
	}

	@Test
	void analyzeFileContentsReportsMultiMoveTargetPathAndGroupSeparately() {
		RefactoringMinerMcpService service = new RefactoringMinerMcpService((before, after) -> {
			ProjectASTDiff diff = new ProjectASTDiff(before, after);
			diff.addASTDiff(astDiffWithMultiMove());
			diff.setRefactorings(List.of());
			return diff;
		});
		Map<String, String> before = Map.of("src/main/java/A.java", "class A {\n  void f() {}\n}");
		Map<String, String> after = Map.of("src/main/java/A.java", "class A {\n  void f() {}\n}");

		McpAnalysisResult result = service.analyzeFileContents(before, after, 20, 100, 200_000, 1, 1);

		McpAstDiffResult.ActionSummary action = result.astDiffs().get(0).sampleActions().get(0);
		assertEquals("multi-move-tree", action.name());
		assertEquals("src/main/java/A.java", action.targetFilePath());
		assertEquals(42, action.moveGroupId());
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
				(diff, port, inputSummary, warnings, maxAstDiffs, maxActionsPerAstDiff) ->
						McpDiffBrowserResult.ok(diff, port, inputSummary, warnings, maxAstDiffs,
								maxActionsPerAstDiff));
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
				(diff, port, inputSummary, warnings, maxAstDiffs, maxActionsPerAstDiff) -> {
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

	private static ASTDiff astDiffWithUpdate() {
		TreeContext beforeContext = new TreeContext();
		Tree beforeRoot = beforeContext.createTree(TypeSet.type("CompilationUnit"), "");
		Tree beforeMethod = beforeContext.createTree(TypeSet.type("MethodDeclaration"), "f");
		beforeMethod.setPos(12);
		beforeMethod.setLength(8);
		beforeRoot.addChild(beforeMethod);
		beforeContext.setRoot(beforeRoot);

		TreeContext afterContext = new TreeContext();
		Tree afterRoot = afterContext.createTree(TypeSet.type("CompilationUnit"), "");
		Tree afterMethod = afterContext.createTree(TypeSet.type("MethodDeclaration"), "g");
		afterMethod.setPos(12);
		afterMethod.setLength(8);
		afterRoot.addChild(afterMethod);
		afterContext.setRoot(afterRoot);

		ExtendedMultiMappingStore mappings = new ExtendedMultiMappingStore(beforeRoot, afterRoot,
				new Constants("src/main/java/A.java"), new Constants("src/main/java/A.java"));
		mappings.addMapping(beforeMethod, afterMethod);
		ASTDiff astDiff = new ASTDiff("src/main/java/A.java", "src/main/java/A.java", beforeContext, afterContext,
				mappings);
		astDiff.editScript.add(new Update(beforeMethod, "g"));
		return astDiff;
	}

	private static ASTDiff astDiffWithNullNodeAction() {
		TreeContext beforeContext = new TreeContext();
		Tree beforeRoot = beforeContext.createTree(TypeSet.type("CompilationUnit"), "");
		beforeContext.setRoot(beforeRoot);

		TreeContext afterContext = new TreeContext();
		Tree afterRoot = afterContext.createTree(TypeSet.type("CompilationUnit"), "");
		afterContext.setRoot(afterRoot);

		ExtendedMultiMappingStore mappings = new ExtendedMultiMappingStore(beforeRoot, afterRoot,
				new Constants("src/main/java/A.java"), new Constants("src/main/java/A.java"));
		ASTDiff astDiff = new ASTDiff("src/main/java/A.java", "src/main/java/A.java", beforeContext, afterContext,
				mappings);
		astDiff.editScript.add(new Action(null) {
			@Override
			public String getName() {
				return "unknown-action";
			}
		});
		return astDiff;
	}

	private static ASTDiff astDiffWithWholeFileUpdate(String content) {
		TreeContext beforeContext = new TreeContext();
		Tree beforeRoot = beforeContext.createTree(TypeSet.type("CompilationUnit"), "");
		beforeRoot.setPos(0);
		beforeRoot.setLength(content.length());
		beforeContext.setRoot(beforeRoot);

		TreeContext afterContext = new TreeContext();
		Tree afterRoot = afterContext.createTree(TypeSet.type("CompilationUnit"), "");
		afterRoot.setPos(0);
		afterRoot.setLength(content.length());
		afterContext.setRoot(afterRoot);

		ExtendedMultiMappingStore mappings = new ExtendedMultiMappingStore(beforeRoot, afterRoot,
				new Constants("src/main/java/A.java"), new Constants("src/main/java/A.java"));
		mappings.addMapping(beforeRoot, afterRoot);
		ASTDiff astDiff = new ASTDiff("src/main/java/A.java", "src/main/java/A.java", beforeContext, afterContext,
				mappings);
		astDiff.editScript.add(new Update(beforeRoot, ""));
		return astDiff;
	}

	private static ASTDiff astDiffWithMultiMove() {
		TreeContext beforeContext = new TreeContext();
		Tree beforeRoot = beforeContext.createTree(TypeSet.type("CompilationUnit"), "");
		Tree beforeMethod = beforeContext.createTree(TypeSet.type("MethodDeclaration"), "f");
		beforeMethod.setPos(12);
		beforeMethod.setLength(8);
		beforeRoot.addChild(beforeMethod);
		beforeContext.setRoot(beforeRoot);

		TreeContext afterContext = new TreeContext();
		Tree afterRoot = afterContext.createTree(TypeSet.type("CompilationUnit"), "");
		Tree afterMethod = afterContext.createTree(TypeSet.type("MethodDeclaration"), "f");
		afterMethod.setPos(12);
		afterMethod.setLength(8);
		afterRoot.addChild(afterMethod);
		afterContext.setRoot(afterRoot);

		ExtendedMultiMappingStore mappings = new ExtendedMultiMappingStore(beforeRoot, afterRoot,
				new Constants("src/main/java/A.java"), new Constants("src/main/java/A.java"));
		ASTDiff astDiff = new ASTDiff("src/main/java/A.java", "src/main/java/A.java", beforeContext, afterContext,
				mappings);
		astDiff.editScript.add(new MultiMove(beforeMethod, afterRoot, 0, 42, false));
		return astDiff;
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
