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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.astDiff.models.ProjectASTDiff;

import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.diff.CodeRange;

class RefactoringMinerMcpToolsTest {
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@Test
	void fileContentsToolMetadataIsReadOnlyAndNamedForAgents() {
		SyncToolSpecification tool = RefactoringMinerMcpTools.analyzeFileContentsTool(
				new RefactoringMinerMcpService((before, after) -> new ProjectASTDiff(before, after)));

		assertEquals(RefactoringMinerMcpTools.ANALYZE_FILE_CONTENTS, tool.tool().name());
		assertTrue(tool.tool().description().contains("Read-only structural analysis"));
		assertTrue(tool.tool().annotations().readOnlyHint());
		assertFalse(tool.tool().annotations().destructiveHint());
		assertTrue(tool.tool().inputSchema().required().contains("beforeFiles"));
		assertTrue(tool.tool().inputSchema().required().contains("afterFiles"));
	}

	@Test
	void worktreeToolMetadataIsReadOnlyAndRequiresRepositoryPath() {
		SyncToolSpecification tool = RefactoringMinerMcpTools.analyzeWorktreeTool(
				new RefactoringMinerMcpService((before, after) -> new ProjectASTDiff(before, after)));

		assertEquals(RefactoringMinerMcpTools.ANALYZE_WORKTREE, tool.tool().name());
		assertTrue(tool.tool().description().contains("local modified, added, deleted"));
		assertTrue(tool.tool().annotations().readOnlyHint());
		assertFalse(tool.tool().annotations().destructiveHint());
		assertTrue(tool.tool().inputSchema().required().contains("repositoryPath"));
	}

	@Test
	void toolSpecificationsExposeFullReadOnlyV1Surface() {
		SyncToolSpecification[] tools = RefactoringMinerMcpTools.toolSpecifications();

		assertEquals(10, tools.length);
		assertTrue(java.util.Arrays.stream(tools)
				.anyMatch(tool -> RefactoringMinerMcpTools.ANALYZE_FILE_CONTENTS.equals(tool.tool().name())));
		assertTrue(java.util.Arrays.stream(tools)
				.anyMatch(tool -> RefactoringMinerMcpTools.ANALYZE_WORKTREE.equals(tool.tool().name())));
		assertTrue(java.util.Arrays.stream(tools)
				.anyMatch(tool -> RefactoringMinerMcpTools.ANALYZE_COMMIT.equals(tool.tool().name())));
		assertTrue(java.util.Arrays.stream(tools)
				.anyMatch(tool -> RefactoringMinerMcpTools.ANALYZE_PULL_REQUEST.equals(tool.tool().name())));
		assertTrue(java.util.Arrays.stream(tools)
				.anyMatch(tool -> RefactoringMinerMcpTools.ANALYZE_DIRECTORIES.equals(tool.tool().name())));
		assertTrue(java.util.Arrays.stream(tools)
				.anyMatch(tool -> RefactoringMinerMcpTools.VALIDATE_FILE_CONTENTS.equals(tool.tool().name())));
		assertTrue(java.util.Arrays.stream(tools)
				.anyMatch(tool -> RefactoringMinerMcpTools.VALIDATE_WORKTREE.equals(tool.tool().name())));
		assertTrue(java.util.Arrays.stream(tools)
				.anyMatch(tool -> RefactoringMinerMcpTools.VALIDATE_COMMIT.equals(tool.tool().name())));
		assertTrue(java.util.Arrays.stream(tools)
				.anyMatch(tool -> RefactoringMinerMcpTools.VALIDATE_PULL_REQUEST.equals(tool.tool().name())));
		assertTrue(java.util.Arrays.stream(tools)
				.anyMatch(tool -> RefactoringMinerMcpTools.VALIDATE_DIRECTORIES.equals(tool.tool().name())));
		assertTrue(java.util.Arrays.stream(tools).allMatch(tool -> tool.tool().annotations().readOnlyHint()));
		assertTrue(java.util.Arrays.stream(tools).noneMatch(tool -> tool.tool().annotations().destructiveHint()));
	}

	@Test
	void remainingToolMetadataIsReadOnlyAndHasExpectedRequiredInputs() {
		RefactoringMinerMcpService service = fakeService();
		SyncToolSpecification commitTool = RefactoringMinerMcpTools.analyzeCommitTool(service);
		SyncToolSpecification pullRequestTool = RefactoringMinerMcpTools.analyzePullRequestTool(service);
		SyncToolSpecification directoriesTool = RefactoringMinerMcpTools.analyzeDirectoriesTool(service);

		assertTrue(commitTool.tool().annotations().readOnlyHint());
		assertTrue(commitTool.tool().inputSchema().required().contains("repositoryPath"));
		assertTrue(commitTool.tool().inputSchema().required().contains("commitId"));
		assertTrue(pullRequestTool.tool().annotations().readOnlyHint());
		assertTrue(pullRequestTool.tool().annotations().openWorldHint());
		assertTrue(pullRequestTool.tool().inputSchema().required().contains("cloneUrl"));
		assertTrue(pullRequestTool.tool().inputSchema().required().contains("pullRequestId"));
		assertTrue(directoriesTool.tool().annotations().readOnlyHint());
		assertTrue(directoriesTool.tool().inputSchema().required().contains("beforePath"));
		assertTrue(directoriesTool.tool().inputSchema().required().contains("afterPath"));
	}

	@Test
	void validationToolMetadataIsReadOnlyAndRequiresIntent() {
		RefactoringMinerMcpService service = fakeService();
		SyncToolSpecification fileTool = RefactoringMinerMcpTools.validateFileContentsTool(service);
		SyncToolSpecification worktreeTool = RefactoringMinerMcpTools.validateWorktreeTool(service);
		SyncToolSpecification commitTool = RefactoringMinerMcpTools.validateCommitTool(service);
		SyncToolSpecification pullRequestTool = RefactoringMinerMcpTools.validatePullRequestTool(service);
		SyncToolSpecification directoriesTool = RefactoringMinerMcpTools.validateDirectoriesTool(service);

		for (SyncToolSpecification tool : List.of(fileTool, worktreeTool, commitTool, pullRequestTool, directoriesTool)) {
			assertTrue(tool.tool().annotations().readOnlyHint());
			assertFalse(tool.tool().annotations().destructiveHint());
			assertTrue(tool.tool().inputSchema().required().contains("intent"));
		}
		assertTrue(fileTool.tool().inputSchema().required().contains("beforeFiles"));
		assertTrue(fileTool.tool().inputSchema().required().contains("afterFiles"));
		assertTrue(worktreeTool.tool().inputSchema().required().contains("repositoryPath"));
		assertTrue(commitTool.tool().inputSchema().required().contains("repositoryPath"));
		assertTrue(commitTool.tool().inputSchema().required().contains("commitId"));
		assertTrue(pullRequestTool.tool().annotations().openWorldHint());
		assertTrue(pullRequestTool.tool().inputSchema().required().contains("cloneUrl"));
		assertTrue(pullRequestTool.tool().inputSchema().required().contains("pullRequestId"));
		assertTrue(directoriesTool.tool().inputSchema().required().contains("beforePath"));
		assertTrue(directoriesTool.tool().inputSchema().required().contains("afterPath"));
	}

	@Test
	void fileContentsToolReturnsJsonTextAndStructuredContent() throws Exception {
		RefactoringMinerMcpService service = new RefactoringMinerMcpService((before, after) -> {
			ProjectASTDiff diff = new ProjectASTDiff(before, after);
			diff.setRefactorings(java.util.List.of());
			return diff;
		});
		SyncToolSpecification tool = RefactoringMinerMcpTools.analyzeFileContentsTool(service);
		CallToolRequest request = new CallToolRequest(RefactoringMinerMcpTools.ANALYZE_FILE_CONTENTS, Map.of(
				"beforeFiles", Map.of("src/main/java/A.java", "class A {}"),
				"afterFiles", Map.of("src/main/java/A.java", "class A { int x; }"),
				"maxRefactorings", 5));

		CallToolResult result = tool.callHandler().apply(null, request);

		assertFalse(result.isError());
		assertTrue(result.structuredContent() instanceof McpAnalysisResult);
		TextContent content = (TextContent) result.content().get(0);
		JsonNode json = OBJECT_MAPPER.readTree(content.text());
		assertEquals("ok", json.get("status").asText());
		assertEquals(1, json.get("filesBefore").asInt());
		assertEquals(1, json.get("filesAfter").asInt());
	}

	@Test
	void fileContentsToolReturnsErrorShapeForInvalidArguments() throws Exception {
		SyncToolSpecification tool = RefactoringMinerMcpTools.analyzeFileContentsTool(
				new RefactoringMinerMcpService((before, after) -> new ProjectASTDiff(before, after)));
		CallToolRequest request = new CallToolRequest(RefactoringMinerMcpTools.ANALYZE_FILE_CONTENTS, Map.of(
				"beforeFiles", "not-an-object",
				"afterFiles", Map.of()));

		CallToolResult result = tool.callHandler().apply(null, request);

		assertTrue(result.isError());
		TextContent content = (TextContent) result.content().get(0);
		JsonNode json = OBJECT_MAPPER.readTree(content.text());
		assertEquals("error", json.get("status").asText());
		assertTrue(json.get("summary").asText().contains("beforeFiles and afterFiles are required"));
	}

	@Test
	void worktreeToolReturnsErrorShapeForInvalidRepositoryPath() throws Exception {
		SyncToolSpecification tool = RefactoringMinerMcpTools.analyzeWorktreeTool(
				new RefactoringMinerMcpService((before, after) -> new ProjectASTDiff(before, after)));
		CallToolRequest request = new CallToolRequest(RefactoringMinerMcpTools.ANALYZE_WORKTREE, Map.of(
				"repositoryPath", "relative/path"));

		CallToolResult result = tool.callHandler().apply(null, request);

		assertTrue(result.isError());
		TextContent content = (TextContent) result.content().get(0);
		JsonNode json = OBJECT_MAPPER.readTree(content.text());
		assertEquals("error", json.get("status").asText());
		assertTrue(json.get("summary").asText().contains("repositoryPath must be absolute"));
	}

	@Test
	void pullRequestToolReturnsJsonTextAndStructuredContentWithoutNetwork() throws Exception {
		SyncToolSpecification tool = RefactoringMinerMcpTools.analyzePullRequestTool(fakeService());
		CallToolRequest request = new CallToolRequest(RefactoringMinerMcpTools.ANALYZE_PULL_REQUEST, Map.of(
				"cloneUrl", "https://github.com/tsantalis/RefactoringMiner.git",
				"pullRequestId", 1,
				"timeoutSeconds", 30,
				"maxRefactorings", 5));

		CallToolResult result = tool.callHandler().apply(null, request);

		assertFalse(result.isError());
		assertTrue(result.structuredContent() instanceof McpAnalysisResult);
		TextContent content = (TextContent) result.content().get(0);
		JsonNode json = OBJECT_MAPPER.readTree(content.text());
		assertEquals("ok", json.get("status").asText());
		assertEquals(1, json.get("filesBefore").asInt());
		assertEquals(1, json.get("filesAfter").asInt());
	}

	@Test
	void commitToolReturnsErrorShapeForInvalidRepositoryPath() throws Exception {
		SyncToolSpecification tool = RefactoringMinerMcpTools.analyzeCommitTool(fakeService());
		CallToolRequest request = new CallToolRequest(RefactoringMinerMcpTools.ANALYZE_COMMIT, Map.of(
				"repositoryPath", "relative/path",
				"commitId", "abc123"));

		CallToolResult result = tool.callHandler().apply(null, request);

		assertTrue(result.isError());
		TextContent content = (TextContent) result.content().get(0);
		JsonNode json = OBJECT_MAPPER.readTree(content.text());
		assertEquals("error", json.get("status").asText());
		assertTrue(json.get("summary").asText().contains("repositoryPath must be absolute"));
	}

	@Test
	void pullRequestToolReturnsErrorShapeForInvalidPullRequestId() throws Exception {
		SyncToolSpecification tool = RefactoringMinerMcpTools.analyzePullRequestTool(fakeService());
		CallToolRequest request = new CallToolRequest(RefactoringMinerMcpTools.ANALYZE_PULL_REQUEST, Map.of(
				"cloneUrl", "https://github.com/tsantalis/RefactoringMiner.git",
				"pullRequestId", 0));

		CallToolResult result = tool.callHandler().apply(null, request);

		assertTrue(result.isError());
		TextContent content = (TextContent) result.content().get(0);
		JsonNode json = OBJECT_MAPPER.readTree(content.text());
		assertEquals("error", json.get("status").asText());
		assertTrue(json.get("summary").asText().contains("pullRequestId must be greater than 0"));
	}

	@Test
	void directoriesToolReturnsErrorShapeForInvalidBeforePath() throws Exception {
		SyncToolSpecification tool = RefactoringMinerMcpTools.analyzeDirectoriesTool(fakeService());
		CallToolRequest request = new CallToolRequest(RefactoringMinerMcpTools.ANALYZE_DIRECTORIES, Map.of(
				"beforePath", "relative/before",
				"afterPath", "/tmp/after"));

		CallToolResult result = tool.callHandler().apply(null, request);

		assertTrue(result.isError());
		TextContent content = (TextContent) result.content().get(0);
		JsonNode json = OBJECT_MAPPER.readTree(content.text());
		assertEquals("error", json.get("status").asText());
		assertTrue(json.get("summary").asText().contains("beforePath must be absolute"));
	}

	@Test
	void fileContentsValidationToolReturnsJsonTextAndStructuredContent() throws Exception {
		SyncToolSpecification tool = RefactoringMinerMcpTools.validateFileContentsTool(fakeServiceWithRefactoring());
		CallToolRequest request = new CallToolRequest(RefactoringMinerMcpTools.VALIDATE_FILE_CONTENTS, Map.of(
				"beforeFiles", Map.of("src/main/java/A.java", "class A { void f() {} }"),
				"afterFiles", Map.of("src/main/java/A.java", "class A { void g() {} }"),
				"intent", Map.of("type", "Rename Method", "methodNames", List.of("g")),
				"maxCandidates", 5));

		CallToolResult result = tool.callHandler().apply(null, request);

		assertFalse(result.isError());
		assertTrue(result.structuredContent() instanceof McpValidationResult);
		TextContent content = (TextContent) result.content().get(0);
		JsonNode json = OBJECT_MAPPER.readTree(content.text());
		assertEquals("matched", json.get("status").asText());
		assertFalse(json.get("intent").has("descriptionContains"));
		assertEquals(1, json.get("matches").size());
		assertEquals(0, json.get("candidates").size());
	}

	@Test
	void repositoryBackedValidationHandlersReturnStructuredContentWithoutNetwork(@TempDir Path tempDir)
			throws Exception {
		RefactoringMinerMcpService service = fakeServiceWithRefactoring();
		Path beforePath = Files.createDirectories(tempDir.resolve("before"));
		Path afterPath = Files.createDirectories(tempDir.resolve("after"));

		List<CallToolResult> results = List.of(
				RefactoringMinerMcpTools.validateCommitTool(service).callHandler().apply(null,
						new CallToolRequest(RefactoringMinerMcpTools.VALIDATE_COMMIT, Map.of(
								"repositoryPath", tempDir.toString(),
								"commitId", "abc123",
								"intent", renameMethodIntent()))),
				RefactoringMinerMcpTools.validatePullRequestTool(service).callHandler().apply(null,
						new CallToolRequest(RefactoringMinerMcpTools.VALIDATE_PULL_REQUEST, Map.of(
								"cloneUrl", "https://github.com/tsantalis/RefactoringMiner.git",
								"pullRequestId", 1,
								"timeoutSeconds", 30,
								"intent", renameMethodIntent()))),
				RefactoringMinerMcpTools.validateDirectoriesTool(service).callHandler().apply(null,
						new CallToolRequest(RefactoringMinerMcpTools.VALIDATE_DIRECTORIES, Map.of(
								"beforePath", beforePath.toString(),
								"afterPath", afterPath.toString(),
								"intent", renameMethodIntent()))));

		for (CallToolResult result : results) {
			assertFalse(result.isError());
			assertTrue(result.structuredContent() instanceof McpValidationResult);
			TextContent content = (TextContent) result.content().get(0);
			JsonNode json = OBJECT_MAPPER.readTree(content.text());
			assertEquals("matched", json.get("status").asText());
		}
	}

	@Test
	void worktreeValidationToolReturnsErrorShapeForInvalidRepositoryPath() throws Exception {
		SyncToolSpecification tool = RefactoringMinerMcpTools.validateWorktreeTool(fakeServiceWithRefactoring());
		CallToolRequest request = new CallToolRequest(RefactoringMinerMcpTools.VALIDATE_WORKTREE, Map.of(
				"repositoryPath", "relative/path",
				"intent", renameMethodIntent()));

		CallToolResult result = tool.callHandler().apply(null, request);

		assertTrue(result.isError());
		TextContent content = (TextContent) result.content().get(0);
		JsonNode json = OBJECT_MAPPER.readTree(content.text());
		assertEquals("error", json.get("status").asText());
		assertTrue(json.get("summary").asText().contains("repositoryPath must be absolute"));
	}

	@Test
	void fileContentsValidationToolReturnsErrorShapeForInvalidIntentArguments() throws Exception {
		SyncToolSpecification tool = RefactoringMinerMcpTools.validateFileContentsTool(fakeServiceWithRefactoring());
		CallToolRequest request = new CallToolRequest(RefactoringMinerMcpTools.VALIDATE_FILE_CONTENTS, Map.of(
				"beforeFiles", Map.of("src/main/java/A.java", "class A { void f() {} }"),
				"afterFiles", Map.of("src/main/java/A.java", "class A { void g() {} }"),
				"intent", Map.of("type", "Not A Refactoring")));

		CallToolResult result = tool.callHandler().apply(null, request);

		assertTrue(result.isError());
		TextContent content = (TextContent) result.content().get(0);
		JsonNode json = OBJECT_MAPPER.readTree(content.text());
		assertEquals("error", json.get("status").asText());
		assertFalse(json.has("intent"));
		assertTrue(json.get("summary").asText().contains("Unknown refactoring type"));
	}

	private static RefactoringMinerMcpService fakeService() {
		return new RefactoringMinerMcpService(
				(before, after) -> new ProjectASTDiff(before, after),
				(repositoryPath, commitId, parentIndex) -> new ProjectASTDiff(
						Map.of("src/main/java/A.java", "class A {}"),
						Map.of("src/main/java/A.java", "class A { int x; }")),
				(cloneUrl, pullRequestId, timeoutSeconds) -> new ProjectASTDiff(
						Map.of("src/main/java/A.java", "class A {}"),
						Map.of("src/main/java/A.java", "class A { int x; }")),
				(beforePath, afterPath) -> new ProjectASTDiff(
						Map.of("src/main/java/A.java", "class A {}"),
						Map.of("src/main/java/A.java", "class A { int x; }")));
	}

	private static RefactoringMinerMcpService fakeServiceWithRefactoring() {
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
						Map.of("src/main/java/A.java", "class A { void g() {} }")));
	}

	private static ProjectASTDiff diffWithRefactoring(Map<String, String> before, Map<String, String> after) {
		ProjectASTDiff diff = new ProjectASTDiff(before, after);
		diff.setRefactorings(List.of(new FakeRefactoring()));
		return diff;
	}

	private static Map<String, Object> renameMethodIntent() {
		return Map.of("type", "Rename Method", "methodNames", List.of("g"));
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
