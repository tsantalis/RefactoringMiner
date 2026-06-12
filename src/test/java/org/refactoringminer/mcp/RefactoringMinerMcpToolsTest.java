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
import org.eclipse.jgit.api.Git;
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
	void analyzeToolMetadataIsReadOnlyAndNamedForAgents() {
		SyncToolSpecification tool = RefactoringMinerMcpTools.analyzeTool(
				new RefactoringMinerMcpService((before, after) -> new ProjectASTDiff(before, after)));

		assertEquals(RefactoringMinerMcpTools.ANALYZE, tool.tool().name());
		assertTrue(tool.tool().description().contains("Find refactorings"));
		assertTrue(tool.tool().annotations().readOnlyHint());
		assertFalse(tool.tool().annotations().destructiveHint());
		assertTrue(tool.tool().inputSchema().required().contains("source"));
	}

	@Test
	void toolSpecificationsExposeConsolidatedSurface() {
		SyncToolSpecification[] tools = RefactoringMinerMcpTools.toolSpecifications();

		assertEquals(3, tools.length);
		assertTrue(java.util.Arrays.stream(tools)
				.anyMatch(tool -> RefactoringMinerMcpTools.ANALYZE.equals(tool.tool().name())));
		assertTrue(java.util.Arrays.stream(tools)
				.anyMatch(tool -> RefactoringMinerMcpTools.VALIDATE.equals(tool.tool().name())));
		assertTrue(java.util.Arrays.stream(tools)
				.anyMatch(tool -> RefactoringMinerMcpTools.DIFF.equals(tool.tool().name())));
		assertTrue(java.util.Arrays.stream(tools).allMatch(tool -> tool.tool().annotations().readOnlyHint()));
		assertTrue(java.util.Arrays.stream(tools).noneMatch(tool -> tool.tool().annotations().destructiveHint()));
	}

	@Test
	void toolMetadataIsReadOnlyAndHasExpectedRequiredInputs() {
		RefactoringMinerMcpService service = fakeService();
		SyncToolSpecification analyzeTool = RefactoringMinerMcpTools.analyzeTool(service);
		SyncToolSpecification validateTool = RefactoringMinerMcpTools.validateTool(service);
		SyncToolSpecification diffTool = RefactoringMinerMcpTools.diffTool(service);

		assertTrue(analyzeTool.tool().annotations().readOnlyHint());
		assertTrue(analyzeTool.tool().inputSchema().required().contains("source"));

		assertTrue(validateTool.tool().annotations().readOnlyHint());
		assertTrue(validateTool.tool().inputSchema().required().contains("source"));
		assertTrue(validateTool.tool().inputSchema().required().contains("intent"));

		assertTrue(diffTool.tool().annotations().readOnlyHint());
		assertTrue(diffTool.tool().inputSchema().required().contains("source"));
	}

	@Test
	void diffBrowserToolMetadataIsReadOnlyAndUsesLocalhostPortDefault() {
		RefactoringMinerMcpService service = fakeDiffBrowserService();
		SyncToolSpecification tool = RefactoringMinerMcpTools.diffTool(service);

		assertTrue(tool.tool().annotations().readOnlyHint());
		assertFalse(tool.tool().annotations().destructiveHint());
		assertTrue(tool.tool().description().contains("localhost URL"));
		Object portSchema = tool.tool().inputSchema().properties().get("port");
		assertTrue(portSchema instanceof Map<?, ?>);
		assertEquals(6789, ((Map<?, ?>) portSchema).get("default"));
	}

	@Test
	void validationToolMetadataIsReadOnlyAndRequiresIntent() {
		RefactoringMinerMcpService service = fakeService();
		SyncToolSpecification tool = RefactoringMinerMcpTools.validateTool(service);

		assertTrue(tool.tool().annotations().readOnlyHint());
		assertFalse(tool.tool().annotations().destructiveHint());
		assertTrue(tool.tool().inputSchema().required().contains("intent"));
		assertTrue(tool.tool().inputSchema().required().contains("source"));
	}

	@Test
	void analyzeFileContentsToolReturnsJsonTextAndStructuredContent() throws Exception {
		RefactoringMinerMcpService service = new RefactoringMinerMcpService((before, after) -> {
			ProjectASTDiff diff = new ProjectASTDiff(before, after);
			diff.setRefactorings(java.util.List.of());
			return diff;
		});
		SyncToolSpecification tool = RefactoringMinerMcpTools.analyzeTool(service);
		CallToolRequest request = new CallToolRequest(RefactoringMinerMcpTools.ANALYZE, Map.of(
				"source", "FILE_CONTENTS",
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
	void analyzeFileContentsToolReturnsErrorShapeForInvalidArguments() throws Exception {
		SyncToolSpecification tool = RefactoringMinerMcpTools.analyzeTool(
				new RefactoringMinerMcpService((before, after) -> new ProjectASTDiff(before, after)));
		CallToolRequest request = new CallToolRequest(RefactoringMinerMcpTools.ANALYZE, Map.of(
				"source", "FILE_CONTENTS",
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
	void consolidatedToolsRejectOversizedInput() throws Exception {
		Map<String, String> beforeFiles = Map.of("src/main/java/A.java", "class A { void f() {} }");
		Map<String, String> afterFiles = Map.of("src/main/java/A.java", "class A { void g() {} }");
		List<CallToolResult> results = List.of(
				RefactoringMinerMcpTools.analyzeTool(fakeService()).callHandler().apply(null,
						new CallToolRequest(RefactoringMinerMcpTools.ANALYZE, Map.of(
								"source", "FILE_CONTENTS",
								"beforeFiles", beforeFiles,
								"afterFiles", afterFiles,
								"maxBytesPerFile", 4))),
				RefactoringMinerMcpTools.validateTool(fakeServiceWithRefactoring()).callHandler().apply(null,
						new CallToolRequest(RefactoringMinerMcpTools.VALIDATE, Map.of(
								"source", "FILE_CONTENTS",
								"beforeFiles", beforeFiles,
								"afterFiles", afterFiles,
								"intent", renameMethodIntent(),
								"maxBytesPerFile", 4))),
				RefactoringMinerMcpTools.diffTool(fakeDiffBrowserService()).callHandler().apply(null,
						new CallToolRequest(RefactoringMinerMcpTools.DIFF, Map.of(
								"source", "FILE_CONTENTS",
								"beforeFiles", beforeFiles,
								"afterFiles", afterFiles,
								"port", 6790,
								"maxBytesPerFile", 4))));

		for (CallToolResult result : results) {
			assertTrue(result.isError());
			TextContent content = (TextContent) result.content().get(0);
			JsonNode json = OBJECT_MAPPER.readTree(content.text());
			assertEquals("error", json.get("status").asText());
			assertTrue(json.get("summary").asText().contains("exceeds maxBytesPerFile=4"));
		}
	}

	@Test
	void analyzeWorktreeToolReturnsErrorShapeForInvalidRepositoryPath() throws Exception {
		SyncToolSpecification tool = RefactoringMinerMcpTools.analyzeTool(
				new RefactoringMinerMcpService((before, after) -> new ProjectASTDiff(before, after)));
		CallToolRequest request = new CallToolRequest(RefactoringMinerMcpTools.ANALYZE, Map.of(
				"source", "WORKTREE",
				"repositoryPath", "relative/path"));

		CallToolResult result = tool.callHandler().apply(null, request);

		assertTrue(result.isError());
		TextContent content = (TextContent) result.content().get(0);
		JsonNode json = OBJECT_MAPPER.readTree(content.text());
		assertEquals("error", json.get("status").asText());
		assertTrue(json.get("summary").asText().contains("repositoryPath must be absolute"));
	}

	@Test
	void analyzeWorktreeToolRejectsNegativeMaxRefactorings(@TempDir Path tempDir) throws Exception {
		SyncToolSpecification tool = RefactoringMinerMcpTools.analyzeTool(fakeService());
		CallToolRequest request = new CallToolRequest(RefactoringMinerMcpTools.ANALYZE, Map.of(
				"source", "WORKTREE",
				"repositoryPath", tempDir.toString(),
				"maxRefactorings", -1));

		CallToolResult result = tool.callHandler().apply(null, request);

		assertTrue(result.isError());
		TextContent content = (TextContent) result.content().get(0);
		JsonNode json = OBJECT_MAPPER.readTree(content.text());
		assertEquals("error", json.get("status").asText());
		assertTrue(json.get("summary").asText().contains("maxRefactorings must be greater than or equal to 0"));
	}

	@Test
	void analyzePullRequestToolReturnsJsonTextAndStructuredContentWithoutNetwork() throws Exception {
		SyncToolSpecification tool = RefactoringMinerMcpTools.analyzeTool(fakeService());
		CallToolRequest request = new CallToolRequest(RefactoringMinerMcpTools.ANALYZE, Map.of(
				"source", "PULL_REQUEST",
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
	void analyzeCommitToolReturnsErrorShapeForInvalidRepositoryPath() throws Exception {
		SyncToolSpecification tool = RefactoringMinerMcpTools.analyzeTool(fakeService());
		CallToolRequest request = new CallToolRequest(RefactoringMinerMcpTools.ANALYZE, Map.of(
				"source", "COMMIT",
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
	void analyzePullRequestToolReturnsErrorShapeForInvalidPullRequestId() throws Exception {
		SyncToolSpecification tool = RefactoringMinerMcpTools.analyzeTool(fakeService());
		CallToolRequest request = new CallToolRequest(RefactoringMinerMcpTools.ANALYZE, Map.of(
				"source", "PULL_REQUEST",
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
	void analyzeDirectoriesToolReturnsErrorShapeForInvalidBeforePath() throws Exception {
		SyncToolSpecification tool = RefactoringMinerMcpTools.analyzeTool(fakeService());
		CallToolRequest request = new CallToolRequest(RefactoringMinerMcpTools.ANALYZE, Map.of(
				"source", "DIRECTORIES",
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
	void validateFileContentsToolReturnsJsonTextAndStructuredContent() throws Exception {
		SyncToolSpecification tool = RefactoringMinerMcpTools.validateTool(fakeServiceWithRefactoring());
		CallToolRequest request = new CallToolRequest(RefactoringMinerMcpTools.VALIDATE, Map.of(
				"source", "FILE_CONTENTS",
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
				RefactoringMinerMcpTools.validateTool(service).callHandler().apply(null,
						new CallToolRequest(RefactoringMinerMcpTools.VALIDATE, Map.of(
								"source", "COMMIT",
								"repositoryPath", tempDir.toString(),
								"commitId", "abc123",
								"intent", renameMethodIntent()))),
				RefactoringMinerMcpTools.validateTool(service).callHandler().apply(null,
						new CallToolRequest(RefactoringMinerMcpTools.VALIDATE, Map.of(
								"source", "PULL_REQUEST",
								"cloneUrl", "https://github.com/tsantalis/RefactoringMiner.git",
								"pullRequestId", 1,
								"timeoutSeconds", 30,
								"intent", renameMethodIntent()))),
				RefactoringMinerMcpTools.validateTool(service).callHandler().apply(null,
						new CallToolRequest(RefactoringMinerMcpTools.VALIDATE, Map.of(
								"source", "DIRECTORIES",
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
	void validateWorktreeToolReturnsErrorShapeForInvalidRepositoryPath() throws Exception {
		SyncToolSpecification tool = RefactoringMinerMcpTools.validateTool(fakeServiceWithRefactoring());
		CallToolRequest request = new CallToolRequest(RefactoringMinerMcpTools.VALIDATE, Map.of(
				"source", "WORKTREE",
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
	void validateFileContentsToolReturnsErrorShapeForInvalidIntentArguments() throws Exception {
		SyncToolSpecification tool = RefactoringMinerMcpTools.validateTool(fakeServiceWithRefactoring());
		CallToolRequest request = new CallToolRequest(RefactoringMinerMcpTools.VALIDATE, Map.of(
				"source", "FILE_CONTENTS",
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

	@Test
	void diffFileContentsToolReturnsJsonTextAndStructuredContent() throws Exception {
		SyncToolSpecification tool = RefactoringMinerMcpTools.diffTool(fakeDiffBrowserService());
		CallToolRequest request = new CallToolRequest(RefactoringMinerMcpTools.DIFF, Map.of(
				"source", "FILE_CONTENTS",
				"beforeFiles", Map.of("src/main/java/A.java", "class A { void f() {} }"),
				"afterFiles", Map.of("src/main/java/A.java", "class A { void g() {} }"),
				"port", 6790));

		CallToolResult result = tool.callHandler().apply(null, request);

		assertFalse(result.isError());
		assertTrue(result.structuredContent() instanceof McpDiffBrowserResult);
		TextContent content = (TextContent) result.content().get(0);
		JsonNode json = OBJECT_MAPPER.readTree(content.text());
		assertEquals("ok", json.get("status").asText());
		assertEquals("http://127.0.0.1:6790", json.get("url").asText());
		assertEquals("Starting server: http://127.0.0.1:6790", json.get("message").asText());
		assertEquals(1, json.get("affectedFiles").size());
	}

	@Test
	void diffWorktreeToolReturnsJsonTextAndStructuredContentWithoutNetwork(@TempDir Path tempDir) throws Exception {
		Path repo = tempDir.resolve("repo");
		try (Git git = Git.init().setDirectory(repo.toFile()).call()) {
			write(repo, "src/main/java/A.java", "class A { void f() {} }");
			git.add().addFilepattern(".").call();
			git.commit().setMessage("initial").setAuthor("Test", "test@example.com")
					.setCommitter("Test", "test@example.com").call();
			write(repo, "src/main/java/A.java", "class A { void g() {} }");
		}
		SyncToolSpecification tool = RefactoringMinerMcpTools.diffTool(fakeDiffBrowserService());
		CallToolRequest request = new CallToolRequest(RefactoringMinerMcpTools.DIFF, Map.of(
				"source", "WORKTREE",
				"repositoryPath", repo.toString(),
				"baseRef", "HEAD",
				"port", 6793));

		CallToolResult result = tool.callHandler().apply(null, request);

		assertFalse(result.isError());
		assertTrue(result.structuredContent() instanceof McpDiffBrowserResult);
		TextContent content = (TextContent) result.content().get(0);
		JsonNode json = OBJECT_MAPPER.readTree(content.text());
		assertEquals("ok", json.get("status").asText());
		assertEquals("http://127.0.0.1:6793", json.get("url").asText());
		assertEquals("src/main/java/A.java", json.get("affectedFiles").get(0).asText());
	}

	@Test
	void diffWorktreeToolDefaultsRepositoryPathToUserDir(@TempDir Path tempDir) throws Exception {
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
			SyncToolSpecification tool = RefactoringMinerMcpTools.diffTool(fakeDiffBrowserService());
			CallToolRequest request = new CallToolRequest(RefactoringMinerMcpTools.DIFF, Map.of(
					"source", "WORKTREE",
					"baseRef", "HEAD",
					"port", 6793));

			CallToolResult result = tool.callHandler().apply(null, request);

			assertFalse(result.isError());
			TextContent content = (TextContent) result.content().get(0);
			JsonNode json = OBJECT_MAPPER.readTree(content.text());
			assertEquals("ok", json.get("status").asText());
			assertEquals("src/main/java/A.java", json.get("affectedFiles").get(0).asText());
		} finally {
			System.setProperty("user.dir", previousUserDir);
		}
	}

	@Test
	void repositoryBackedDiffBrowserHandlersReturnStructuredContentWithoutNetwork(@TempDir Path tempDir)
			throws Exception {
		RefactoringMinerMcpService service = fakeDiffBrowserService();

		List<CallToolResult> results = List.of(
				RefactoringMinerMcpTools.diffTool(service).callHandler().apply(null,
						new CallToolRequest(RefactoringMinerMcpTools.DIFF, Map.of(
								"source", "COMMIT",
								"repositoryPath", tempDir.toString(),
								"commitId", "abc123",
								"port", 6791))),
				RefactoringMinerMcpTools.diffTool(service).callHandler().apply(null,
						new CallToolRequest(RefactoringMinerMcpTools.DIFF, Map.of(
								"source", "PULL_REQUEST",
								"cloneUrl", "https://github.com/tsantalis/RefactoringMiner.git",
								"pullRequestId", 1,
								"timeoutSeconds", 30,
								"port", 6792))));

		for (CallToolResult result : results) {
			assertFalse(result.isError());
			assertTrue(result.structuredContent() instanceof McpDiffBrowserResult);
			TextContent content = (TextContent) result.content().get(0);
			JsonNode json = OBJECT_MAPPER.readTree(content.text());
			assertEquals("ok", json.get("status").asText());
			assertTrue(json.get("url").asText().startsWith("http://127.0.0.1:"));
		}
	}

	@Test
	void diffBrowserToolReturnsErrorShapeForInvalidPort() throws Exception {
		SyncToolSpecification tool = RefactoringMinerMcpTools.diffTool(fakeDiffBrowserService());
		CallToolRequest request = new CallToolRequest(RefactoringMinerMcpTools.DIFF, Map.of(
				"source", "FILE_CONTENTS",
				"beforeFiles", Map.of("src/main/java/A.java", "class A {}"),
				"afterFiles", Map.of("src/main/java/A.java", "class A { int x; }"),
				"port", 0));

		CallToolResult result = tool.callHandler().apply(null, request);

		assertTrue(result.isError());
		TextContent content = (TextContent) result.content().get(0);
		JsonNode json = OBJECT_MAPPER.readTree(content.text());
		assertEquals("error", json.get("status").asText());
		assertTrue(json.get("summary").asText().contains("port must be between 1 and 65535"));
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

	private static RefactoringMinerMcpService fakeDiffBrowserService() {
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
				(diff, port, inputSummary, warnings) -> {
					if (port < 1 || port > 65535) {
						throw new IllegalArgumentException("port must be between 1 and 65535.");
					}
					return McpDiffBrowserResult.ok(diff, port, inputSummary, warnings);
				});
	}

	private static ProjectASTDiff diffWithRefactoring(Map<String, String> before, Map<String, String> after) {
		ProjectASTDiff diff = new ProjectASTDiff(before, after);
		diff.setRefactorings(List.of(new FakeRefactoring()));
		return diff;
	}

	private static Map<String, Object> renameMethodIntent() {
		return Map.of("type", "Rename Method", "methodNames", List.of("g"));
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
