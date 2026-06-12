package org.refactoringminer.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.astDiff.models.ProjectASTDiff;

import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.diff.CodeRange;

class RefactoringMinerMcpToolsTest {
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@Test
	void toolSpecificationsExposeThreeSourceParameterizedTools() {
		SyncToolSpecification[] tools = RefactoringMinerMcpTools.toolSpecifications();

		assertEquals(3, tools.length);
		assertEquals(RefactoringMinerMcpTools.ANALYZE, tools[0].tool().name());
		assertEquals(RefactoringMinerMcpTools.VALIDATE, tools[1].tool().name());
		assertEquals(RefactoringMinerMcpTools.DIFF, tools[2].tool().name());
		for (SyncToolSpecification tool : tools) {
			assertTrue(tool.tool().annotations().readOnlyHint());
			assertFalse(tool.tool().annotations().destructiveHint());
			assertTrue(tool.tool().annotations().openWorldHint());
			assertTrue(tool.tool().inputSchema().properties().containsKey("source"));
			assertFalse(tool.tool().inputSchema().properties().containsKey("repositoryPath"));
			assertFalse(tool.tool().inputSchema().toString().contains("repositoryPath"));
		}
	}

	@Test
	void analyzeToolUsesFileContentsSource() throws Exception {
		SyncToolSpecification tool = RefactoringMinerMcpTools.analyzeTool(fakeService());
		CallToolRequest request = new CallToolRequest(RefactoringMinerMcpTools.ANALYZE, Map.of(
				"source", Map.of(
						"type", "fileContents",
						"beforeFiles", Map.of("src/main/java/A.java", "class A { void f() {} }"),
						"afterFiles", Map.of("src/main/java/A.java", "class A { void g() {} }")),
				"maxRefactorings", 5));

		CallToolResult result = tool.callHandler().apply(null, request);

		assertFalse(result.isError());
		assertTrue(result.structuredContent() instanceof McpAnalysisResult);
		JsonNode json = json(result);
		assertEquals("ok", json.get("status").asText());
		assertEquals(1, json.get("refactorings").size());
	}

	@Test
	void validateToolUsesFileContentsSource() throws Exception {
		SyncToolSpecification tool = RefactoringMinerMcpTools.validateTool(fakeService());
		CallToolRequest request = new CallToolRequest(RefactoringMinerMcpTools.VALIDATE, Map.of(
				"source", Map.of(
						"type", "fileContents",
						"beforeFiles", Map.of("src/main/java/A.java", "class A { void f() {} }"),
						"afterFiles", Map.of("src/main/java/A.java", "class A { void g() {} }")),
				"intent", Map.of("type", "Rename Method", "methodNames", List.of("g")),
				"maxCandidates", 5));

		CallToolResult result = tool.callHandler().apply(null, request);

		assertFalse(result.isError());
		assertTrue(result.structuredContent() instanceof McpValidationResult);
		JsonNode json = json(result);
		assertEquals("matched", json.get("status").asText());
		assertEquals(1, json.get("matches").size());
	}

	@Test
	@ResourceLock("user.dir")
	void diffToolDefaultsSourceToUserDirWorktree(@TempDir Path tempDir) throws Exception {
		Path repo = tempDir.resolve("repo");
		try (Git git = Git.init().setDirectory(repo.toFile()).call()) {
			write(repo, "src/main/java/A.java", "class A { void f() {} }");
			git.add().addFilepattern(".").call();
			git.commit().setMessage("initial").setAuthor("Test", "test@example.com")
					.setCommitter("Test", "test@example.com").call();
			write(repo, "src/main/java/A.java", "class A { void g() {} }");
		}
		String previousUserDir = System.getProperty("user.dir");
		try {
			System.setProperty("user.dir", repo.toString());
			SyncToolSpecification tool = RefactoringMinerMcpTools.diffTool(fakeService());
			CallToolRequest request = new CallToolRequest(RefactoringMinerMcpTools.DIFF, Map.of("port", 6794));

			CallToolResult result = tool.callHandler().apply(null, request);

			assertFalse(result.isError());
			assertTrue(result.structuredContent() instanceof McpDiffBrowserResult);
			JsonNode json = json(result);
			assertEquals("ok", json.get("status").asText());
			assertEquals("http://127.0.0.1:6794", json.get("url").asText());
			assertEquals("src/main/java/A.java", json.get("affectedFiles").get(0).asText());
			assertEquals(1, json.get("refactorings").size());
		} finally {
			System.setProperty("user.dir", previousUserDir);
		}
	}

	@Test
	@ResourceLock("user.dir")
	void diffToolSupportsRelativeWorkingDirectoryUnderUserDir(@TempDir Path tempDir) throws Exception {
		Path repo = tempDir.resolve("repo-a");
		try (Git git = Git.init().setDirectory(repo.toFile()).call()) {
			write(repo, "src/main/java/A.java", "class A { void f() {} }");
			git.add().addFilepattern(".").call();
			git.commit().setMessage("initial").setAuthor("Test", "test@example.com")
					.setCommitter("Test", "test@example.com").call();
			write(repo, "src/main/java/A.java", "class A { void g() {} }");
		}
		String previousUserDir = System.getProperty("user.dir");
		try {
			System.setProperty("user.dir", tempDir.toString());
			SyncToolSpecification tool = RefactoringMinerMcpTools.diffTool(fakeService());
			CallToolRequest request = new CallToolRequest(RefactoringMinerMcpTools.DIFF, Map.of(
					"source", Map.of(
							"type", "worktree",
							"workingDirectory", "repo-a",
							"baseRef", "HEAD"),
					"port", 6798));

			CallToolResult result = tool.callHandler().apply(null, request);

			assertFalse(result.isError());
			JsonNode json = json(result);
			assertEquals("ok", json.get("status").asText());
			assertTrue(json.get("inputSummary").asText().contains(repo.toString()));
			assertEquals("src/main/java/A.java", json.get("affectedFiles").get(0).asText());
		} finally {
			System.setProperty("user.dir", previousUserDir);
		}
	}

	@Test
	void toolsRejectAbsoluteWorkingDirectory(@TempDir Path tempDir) throws Exception {
		SyncToolSpecification tool = RefactoringMinerMcpTools.diffTool(fakeService());
		CallToolRequest request = new CallToolRequest(RefactoringMinerMcpTools.DIFF, Map.of(
				"source", Map.of(
						"type", "worktree",
						"workingDirectory", tempDir.toString()),
				"port", 6798));

		CallToolResult result = tool.callHandler().apply(null, request);

		assertTrue(result.isError());
		JsonNode json = json(result);
		assertEquals("error", json.get("status").asText());
		assertTrue(json.get("summary").asText().contains("source.workingDirectory must be relative"));
	}

	@Test
	void toolsRejectEscapingWorkingDirectory() throws Exception {
		SyncToolSpecification tool = RefactoringMinerMcpTools.diffTool(fakeService());
		CallToolRequest request = new CallToolRequest(RefactoringMinerMcpTools.DIFF, Map.of(
				"source", Map.of(
						"type", "worktree",
						"workingDirectory", "../repo"),
				"port", 6798));

		CallToolResult result = tool.callHandler().apply(null, request);

		assertTrue(result.isError());
		JsonNode json = json(result);
		assertEquals("error", json.get("status").asText());
		assertTrue(json.get("summary").asText().contains("source.workingDirectory must stay inside"));
	}

	@Test
	void diffToolInfersPullRequestSourceFromPullRequestFields() throws Exception {
		RefactoringMinerMcpService service = new RefactoringMinerMcpService(
				(before, after) -> {
					throw new AssertionError("worktree differ should not run for PR input");
				},
				(repositoryPath, commitId, parentIndex) -> {
					throw new AssertionError("local commit differ should not run for PR input");
				},
				(cloneUrl, commitId, parentIndex, timeoutSeconds) -> {
					throw new AssertionError("commit URL differ should not run for PR input");
				},
				(cloneUrl, pullRequestId, timeoutSeconds) -> {
					assertEquals("https://github.com/tsantalis/RefactoringMiner.git", cloneUrl);
					assertEquals(1055, pullRequestId);
					assertEquals(30, timeoutSeconds);
					return diffWithRefactoring(
							Map.of("src/main/java/A.java", "class A { void f() {} }"),
							Map.of("src/main/java/A.java", "class A { void g() {} }"));
				},
				(beforePath, afterPath) -> new ProjectASTDiff(Map.of(), Map.of()),
				(diff, port, inputSummary, warnings, maxRefactorings) ->
						McpDiffBrowserResult.ok(diff, port, inputSummary, warnings, maxRefactorings));
		SyncToolSpecification tool = RefactoringMinerMcpTools.diffTool(service);
		CallToolRequest request = new CallToolRequest(RefactoringMinerMcpTools.DIFF, Map.of(
				"source", Map.of(
						"cloneUrl", "https://github.com/tsantalis/RefactoringMiner.git",
						"pullRequestId", 1055,
						"timeoutSeconds", 30),
				"port", 6795));

		CallToolResult result = tool.callHandler().apply(null, request);

		assertFalse(result.isError());
		JsonNode json = json(result);
		assertEquals("ok", json.get("status").asText());
		assertEquals("http://127.0.0.1:6795", json.get("url").asText());
		assertEquals(1, json.get("refactorings").size());
	}

	@Test
	void diffToolAcceptsPullRequestSourceAlias() throws Exception {
		SyncToolSpecification tool = RefactoringMinerMcpTools.diffTool(fakeService());
		CallToolRequest request = new CallToolRequest(RefactoringMinerMcpTools.DIFF, Map.of(
				"source", Map.of(
						"type", "PULL_REQUEST",
						"cloneUrl", "https://github.com/tsantalis/RefactoringMiner.git",
						"pullRequestId", 1055),
				"port", 6795));

		CallToolResult result = tool.callHandler().apply(null, request);

		assertFalse(result.isError());
		JsonNode json = json(result);
		assertEquals("ok", json.get("status").asText());
		assertEquals("http://127.0.0.1:6795", json.get("url").asText());
	}

	@Test
	void diffToolDispatchesPullRequestUrlSource() throws Exception {
		RefactoringMinerMcpService service = new RefactoringMinerMcpService(
				(before, after) -> {
					throw new AssertionError("worktree differ should not run for URL input");
				},
				(repositoryPath, commitId, parentIndex) -> {
					throw new AssertionError("local commit differ should not run for PR URL input");
				},
				(cloneUrl, commitId, parentIndex, timeoutSeconds) -> {
					throw new AssertionError("commit URL differ should not run for PR URL input");
				},
				(cloneUrl, pullRequestId, timeoutSeconds) -> {
					assertEquals("https://github.com/tsantalis/RefactoringMiner.git", cloneUrl);
					assertEquals(1086, pullRequestId);
					assertEquals(30, timeoutSeconds);
					return diffWithRefactoring(
							Map.of("src/main/java/A.java", "class A { void f() {} }"),
							Map.of("src/main/java/A.java", "class A { void g() {} }"));
				},
				(beforePath, afterPath) -> new ProjectASTDiff(Map.of(), Map.of()),
				(diff, port, inputSummary, warnings, maxRefactorings) ->
						McpDiffBrowserResult.ok(diff, port, inputSummary, warnings, maxRefactorings));
		SyncToolSpecification tool = RefactoringMinerMcpTools.diffTool(service);
		CallToolRequest request = new CallToolRequest(RefactoringMinerMcpTools.DIFF, Map.of(
				"source", Map.of(
						"type", "url",
						"url", "https://github.com/tsantalis/RefactoringMiner/pull/1086/files",
						"timeoutSeconds", 30),
				"port", 6795));

		CallToolResult result = tool.callHandler().apply(null, request);

		assertFalse(result.isError());
		JsonNode json = json(result);
		assertEquals("ok", json.get("status").asText());
		assertEquals("http://127.0.0.1:6795", json.get("url").asText());
		assertTrue(json.get("inputSummary").asText().contains("/pull/1086/files"));
		assertEquals(1, json.get("refactorings").size());
	}

	@Test
	void diffToolInfersUrlSourceFromUrlField() throws Exception {
		RefactoringMinerMcpService service = new RefactoringMinerMcpService(
				(before, after) -> {
					throw new AssertionError("worktree differ should not run for URL input");
				},
				(repositoryPath, commitId, parentIndex) -> {
					throw new AssertionError("local commit differ should not run for URL input");
				},
				(cloneUrl, commitId, parentIndex, timeoutSeconds) -> {
					throw new AssertionError("commit URL differ should not run for PR URL input");
				},
				(cloneUrl, pullRequestId, timeoutSeconds) -> {
					assertEquals("https://github.com/tsantalis/RefactoringMiner.git", cloneUrl);
					assertEquals(1055, pullRequestId);
					return diffWithRefactoring(
							Map.of("src/main/java/A.java", "class A { void f() {} }"),
							Map.of("src/main/java/A.java", "class A { void g() {} }"));
				},
				(beforePath, afterPath) -> new ProjectASTDiff(Map.of(), Map.of()),
				(diff, port, inputSummary, warnings, maxRefactorings) ->
						McpDiffBrowserResult.ok(diff, port, inputSummary, warnings, maxRefactorings));
		SyncToolSpecification tool = RefactoringMinerMcpTools.diffTool(service);
		CallToolRequest request = new CallToolRequest(RefactoringMinerMcpTools.DIFF, Map.of(
				"source", Map.of(
						"url", "https://github.com/tsantalis/RefactoringMiner/pull/1055/files",
						"timeoutSeconds", 30),
				"port", 6795));

		CallToolResult result = tool.callHandler().apply(null, request);

		assertFalse(result.isError());
		JsonNode json = json(result);
		assertEquals("ok", json.get("status").asText());
		assertEquals("http://127.0.0.1:6795", json.get("url").asText());
		assertTrue(json.get("inputSummary").asText().contains("/pull/1055/files"));
	}

	@Test
	void diffToolDispatchesCommitUrlSourceWithParentIndex() throws Exception {
		RefactoringMinerMcpService service = new RefactoringMinerMcpService(
				(before, after) -> {
					throw new AssertionError("worktree differ should not run for URL input");
				},
				(repositoryPath, commitId, parentIndex) -> {
					throw new AssertionError("local commit differ should not run for URL input");
				},
				(cloneUrl, commitId, parentIndex, timeoutSeconds) -> {
					assertEquals("https://github.com/tsantalis/RefactoringMiner.git", cloneUrl);
					assertEquals("abcdef1234567890", commitId);
					assertEquals(1, parentIndex);
					assertEquals(45, timeoutSeconds);
					return diffWithRefactoring(
							Map.of("src/main/java/A.java", "class A { void f() {} }"),
							Map.of("src/main/java/A.java", "class A { void g() {} }"));
				},
				(cloneUrl, pullRequestId, timeoutSeconds) -> {
					throw new AssertionError("PR differ should not run for commit URL input");
				},
				(beforePath, afterPath) -> new ProjectASTDiff(Map.of(), Map.of()),
				(diff, port, inputSummary, warnings, maxRefactorings) ->
						McpDiffBrowserResult.ok(diff, port, inputSummary, warnings, maxRefactorings));
		SyncToolSpecification tool = RefactoringMinerMcpTools.diffTool(service);
		CallToolRequest request = new CallToolRequest(RefactoringMinerMcpTools.DIFF, Map.of(
				"source", Map.of(
						"type", "url",
						"url", "https://github.com/tsantalis/RefactoringMiner/commit/abcdef1234567890?diff=split",
						"parentIndex", 1,
						"timeoutSeconds", 45),
				"maxRefactorings", 0,
				"port", 6796));

		CallToolResult result = tool.callHandler().apply(null, request);

		assertFalse(result.isError());
		JsonNode json = json(result);
		assertEquals("ok", json.get("status").asText());
		assertEquals("http://127.0.0.1:6796", json.get("url").asText());
		assertEquals(1, json.get("refactoringCount").asInt());
		assertEquals(0, json.get("refactorings").size());
		assertTrue(json.get("warnings").get(0).asText().contains("Refactorings truncated to 0 of 1"));
	}

	@Test
	void toolsRejectRepositoryPathInSource(@TempDir Path tempDir) throws Exception {
		SyncToolSpecification tool = RefactoringMinerMcpTools.diffTool(fakeService());
		CallToolRequest request = new CallToolRequest(RefactoringMinerMcpTools.DIFF, Map.of(
				"source", Map.of(
						"type", "worktree",
						"repositoryPath", tempDir.toString()),
				"port", 6797));

		CallToolResult result = tool.callHandler().apply(null, request);

		assertTrue(result.isError());
		JsonNode json = json(result);
		assertEquals("error", json.get("status").asText());
		assertTrue(json.get("summary").asText().contains("Unsupported source argument"));
		assertTrue(json.get("warnings").get(0).asText().contains("target project directory"));
	}

	@Test
	void diffToolReturnsErrorShapeForInvalidPort() throws Exception {
		SyncToolSpecification tool = RefactoringMinerMcpTools.diffTool(fakeService());
		CallToolRequest request = new CallToolRequest(RefactoringMinerMcpTools.DIFF, Map.of("port", 0));

		CallToolResult result = tool.callHandler().apply(null, request);

		assertTrue(result.isError());
		JsonNode json = json(result);
		assertEquals("error", json.get("status").asText());
		assertTrue(json.get("summary").asText().contains("port must be between 1 and 65535"));
	}

	private static JsonNode json(CallToolResult result) throws Exception {
		TextContent content = (TextContent) result.content().get(0);
		return OBJECT_MAPPER.readTree(content.text());
	}

	private static RefactoringMinerMcpService fakeService() {
		return new RefactoringMinerMcpService(
				(before, after) -> diffWithRefactoring(before, after),
				(repositoryPath, commitId, parentIndex) -> diffWithRefactoring(
						Map.of("src/main/java/A.java", "class A { void f() {} }"),
						Map.of("src/main/java/A.java", "class A { void g() {} }")),
				(cloneUrl, pullRequestId, timeoutSeconds) -> diffWithRefactoring(
						Map.of("src/main/java/A.java", "class A { void f() {} }"),
						Map.of("src/main/java/A.java", "class A { void g() {} }")),
				(beforePath, afterPath) -> diffWithRefactoring(
						Map.of("src/main/java/A.java", "class A { void f() {} }"),
						Map.of("src/main/java/A.java", "class A { void g() {} }")),
				(diff, port, inputSummary, warnings, maxRefactorings) -> {
					if (port < 1 || port > 65535) {
						throw new IllegalArgumentException("port must be between 1 and 65535.");
					}
					return McpDiffBrowserResult.ok(diff, port, inputSummary, warnings, maxRefactorings);
				});
	}

	private static ProjectASTDiff diffWithRefactoring(Map<String, String> before, Map<String, String> after) {
		ProjectASTDiff diff = new ProjectASTDiff(before, after);
		diff.setRefactorings(List.of(new FakeRefactoring()));
		return diff;
	}

	private static void write(Path repo, String relativePath, String content) throws Exception {
		Path file = repo.resolve(relativePath);
		Files.createDirectories(file.getParent());
		Files.writeString(file, content);
	}

	private static class FakeRefactoring implements Refactoring {
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
					CodeElementType.METHOD_DECLARATION, 10, 18).setDescription("original method")
					.setCodeElement("f()"));
		}

		@Override
		public List<CodeRange> rightSide() {
			return List.of(new CodeRange("src/main/java/A.java", 1, 1, 10, 18,
					CodeElementType.METHOD_DECLARATION, 10, 18).setDescription("renamed method")
					.setCodeElement("g()"));
		}

		@Override
		public String toString() {
			return "Rename Method f() renamed to g() in class A";
		}
	}
}
