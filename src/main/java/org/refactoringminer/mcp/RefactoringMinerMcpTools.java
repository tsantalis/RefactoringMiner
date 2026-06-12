package org.refactoringminer.mcp;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gui.webdiff.WebDiff;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import io.modelcontextprotocol.spec.McpSchema.ToolAnnotations;

public final class RefactoringMinerMcpTools {
	public enum AnalysisSource {
		FILE_CONTENTS, WORKTREE, COMMIT, PULL_REQUEST, DIRECTORIES
	}

	static final String ANALYZE = "refactoringminer_analyze";
	static final String VALIDATE = "refactoringminer_validate";
	static final String DIFF = "refactoringminer_diff";

	private static final int DEFAULT_MAX_REFACTORINGS = 20;
	private static final int DEFAULT_MAX_CANDIDATES = 20;
	private static final int DEFAULT_MAX_FILES = 100;
	private static final int DEFAULT_MAX_BYTES_PER_FILE = 200_000;
	private static final int DEFAULT_TIMEOUT_SECONDS = 300;
	private static final int DEFAULT_WEB_DIFF_PORT = WebDiff.DEFAULT_PORT;

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private RefactoringMinerMcpTools() {
	}

	public static void registerTools(McpSyncServer server) {
		for (SyncToolSpecification specification : toolSpecifications()) {
			server.addTool(specification);
		}
	}

	static SyncToolSpecification[] toolSpecifications() {
		RefactoringMinerMcpService service = new RefactoringMinerMcpService();
		return new SyncToolSpecification[] {
			analyzeTool(service),
			validateTool(service),
			diffTool(service)
		};
	}

	static SyncToolSpecification analyzeTool(RefactoringMinerMcpService service) {
		return SyncToolSpecification.builder()
				.tool(Tool.builder()
						.name(ANALYZE)
						.title("Analyze refactorings")
						.description("Find refactorings across various sources. Required 'source' parameter: " +
								"FILE_CONTENTS (needs beforeFiles, afterFiles), WORKTREE (needs repositoryPath, baseRef, includeUntracked), " +
								"COMMIT (needs commitId, repositoryPath), PULL_REQUEST (needs pullRequestId, cloneUrl), " +
								"DIRECTORIES (needs beforePath, afterPath).")
						.inputSchema(analyzeInputSchema())
						.outputSchema(outputSchema())
						.annotations(new ToolAnnotations("Analyze refactorings", true, false, true, false, false))
						.build())
				.callHandler((exchange, request) -> handleAnalyze(service, request))
				.build();
	}

	static SyncToolSpecification validateTool(RefactoringMinerMcpService service) {
		return SyncToolSpecification.builder()
				.tool(Tool.builder()
						.name(VALIDATE)
						.title("Validate intended refactoring")
						.description("Check a stated refactoring intent against various sources. Required 'source' parameter: " +
								"FILE_CONTENTS (needs beforeFiles, afterFiles), WORKTREE (needs repositoryPath, baseRef, includeUntracked), " +
								"COMMIT (needs commitId, repositoryPath), PULL_REQUEST (needs pullRequestId, cloneUrl), " +
								"DIRECTORIES (needs beforePath, afterPath).")
						.inputSchema(validateInputSchema())
						.outputSchema(validationOutputSchema())
						.annotations(new ToolAnnotations("Validate refactoring", true, false, true, false, false))
						.build())
				.callHandler((exchange, request) -> handleValidate(service, request))
				.build();
	}

	static SyncToolSpecification diffTool(RefactoringMinerMcpService service) {
		return SyncToolSpecification.builder()
				.tool(Tool.builder()
						.name(DIFF)
						.title("Open AST diff browser")
						.description("Start a local RefactoringMiner AST diff page for various sources. Returns a localhost URL. " +
								"Required 'source' parameter: FILE_CONTENTS (needs beforeFiles, afterFiles), " +
								"WORKTREE (needs repositoryPath, baseRef, includeUntracked), COMMIT (needs commitId, repositoryPath), " +
								"PULL_REQUEST (needs pullRequestId, cloneUrl).")
						.inputSchema(diffInputSchema())
						.outputSchema(diffBrowserOutputSchema())
						.annotations(new ToolAnnotations("Open AST diff browser", true, false, true, false, false))
						.build())
				.callHandler((exchange, request) -> handleDiff(service, request))
				.build();
	}

	private static CallToolResult handleAnalyze(RefactoringMinerMcpService service, CallToolRequest request) {
		Map<String, Object> args = request.arguments();
		if (args == null) return toCallToolResult(McpAnalysisResult.error("Arguments are required.", List.of("arguments=null")));

		String sourceStr = stringValue(args.get("source"), "source");
		AnalysisSource source;
		try {
			source = AnalysisSource.valueOf(sourceStr.toUpperCase());
		} catch (IllegalArgumentException e) {
			return toCallToolResult(McpAnalysisResult.error("Invalid source: " + sourceStr, List.of("Valid sources are: FILE_CONTENTS, WORKTREE, COMMIT, PULL_REQUEST, DIRECTORIES")));
		}

		int maxRefactorings = integerValue(args.get("maxRefactorings"), DEFAULT_MAX_REFACTORINGS, "maxRefactorings");

		try {
			switch (source) {
				case FILE_CONTENTS:
					Map<String, String> bfA = stringMap(args.get("beforeFiles"), "beforeFiles");
					Map<String, String> afA = stringMap(args.get("afterFiles"), "afterFiles");
					int maxF_A = integerValue(args.get("maxFiles"), DEFAULT_MAX_FILES, "maxFiles");
					int maxB_A = integerValue(args.get("maxBytesPerFile"), DEFAULT_MAX_BYTES_PER_FILE, "maxBytesPerFile");
					return toCallToolResult(service.analyzeFileContents(bfA, afA, maxRefactorings, maxF_A, maxB_A));
				case WORKTREE:
					Path repW = repositoryPath(args.get("repositoryPath"));
					String baseW = optionalStringValue(args.get("baseRef"), "HEAD");
					boolean untW = booleanValue(args.get("includeUntracked"), false);
					int maxF_W = integerValue(args.get("maxFiles"), DEFAULT_MAX_FILES, "maxFiles");
					int maxB_W = integerValue(args.get("maxBytesPerFile"), DEFAULT_MAX_BYTES_PER_FILE, "maxBytesPerFile");
					return toCallToolResult(service.analyzeWorktree(repW, baseW, untW, maxF_W, maxB_W, maxRefactorings));
				case COMMIT:
					Path repC = repositoryPath(args.get("repositoryPath"));
					String cidC = stringValue(args.get("commitId"), "commitId");
					Integer pIdxC = optionalIntegerValue(args.get("parentIndex"), "parentIndex");
					return toCallToolResult(service.analyzeCommit(repC, cidC, pIdxC, maxRefactorings));
				case PULL_REQUEST:
					String urlPR = optionalCloneUrlValue(args.get("cloneUrl"));
					int prIdPR = integerValue(args.get("pullRequestId"), 0, "pullRequestId");
					int timeoutPR = integerValue(args.get("timeoutSeconds"), DEFAULT_TIMEOUT_SECONDS, "timeoutSeconds");
					return toCallToolResult(service.analyzePullRequest(urlPR, prIdPR, timeoutPR, maxRefactorings));
				case DIRECTORIES:
					String bfD = stringValue(args.get("beforePath"), "beforePath");
					String afD = stringValue(args.get("afterPath"), "afterPath");
					return toCallToolResult(service.analyzeDirectories(Path.of(bfD), Path.of(afD), maxRefactorings));
				default:
					throw new IllegalStateException("Unexpected source: " + source);
			}
		} catch (IllegalArgumentException e) {
			return toCallToolResult(McpAnalysisResult.error(e.getMessage(), List.of("Invalid tool arguments.")));
		}
	}

	private static CallToolResult handleValidate(RefactoringMinerMcpService service, CallToolRequest request) {
		Map<String, Object> args = request.arguments();
		if (args == null) return toCallToolResult(McpValidationResult.error("Arguments are required.", null, List.of("arguments=null")));

		String sourceStr = stringValue(args.get("source"), "source");
		AnalysisSource source;
		try {
			source = AnalysisSource.valueOf(sourceStr.toUpperCase());
		} catch (IllegalArgumentException e) {
			return toCallToolResult(McpValidationResult.error("Invalid source: " + sourceStr, null, List.of("Valid sources are: FILE_CONTENTS, WORKTREE, COMMIT, PULL_REQUEST, DIRECTORIES")));
		}

		McpRefactoringIntent intent = intentValue(args.get("intent"));
		int maxCandidates = integerValue(args.get("maxCandidates"), DEFAULT_MAX_CANDIDATES, "maxCandidates");

		try {
			switch (source) {
				case FILE_CONTENTS:
					Map<String, String> bfV = stringMap(args.get("beforeFiles"), "beforeFiles");
					Map<String, String> afV = stringMap(args.get("afterFiles"), "afterFiles");
					int maxF_V = integerValue(args.get("maxFiles"), DEFAULT_MAX_FILES, "maxFiles");
					int maxB_V = integerValue(args.get("maxBytesPerFile"), DEFAULT_MAX_BYTES_PER_FILE, "maxBytesPerFile");
					return toCallToolResult(service.validateFileContents(bfV, afV, intent, maxCandidates, maxF_V, maxB_V));
				case WORKTREE:
					Path repVW = repositoryPath(args.get("repositoryPath"));
					String baseVW = optionalStringValue(args.get("baseRef"), "HEAD");
					boolean untVW = booleanValue(args.get("includeUntracked"), false);
					int maxF_VW = integerValue(args.get("maxFiles"), DEFAULT_MAX_FILES, "maxFiles");
					int maxB_VW = integerValue(args.get("maxBytesPerFile"), DEFAULT_MAX_BYTES_PER_FILE, "maxBytesPerFile");
					return toCallToolResult(service.validateWorktree(repVW, baseVW, untVW, maxF_VW, maxB_VW, intent, maxCandidates));
				case COMMIT:
					Path repVC = repositoryPath(args.get("repositoryPath"));
					String cidVC = stringValue(args.get("commitId"), "commitId");
					Integer pIdxVC = optionalIntegerValue(args.get("parentIndex"), "parentIndex");
					return toCallToolResult(service.validateCommit(repVC, cidVC, pIdxVC, intent, maxCandidates));
				case PULL_REQUEST:
					String urlVP = optionalCloneUrlValue(args.get("cloneUrl"));
					int prIdVP = integerValue(args.get("pullRequestId"), 0, "pullRequestId");
					int timeoutVP = integerValue(args.get("timeoutSeconds"), DEFAULT_TIMEOUT_SECONDS, "timeoutSeconds");
					return toCallToolResult(service.validatePullRequest(urlVP, prIdVP, timeoutVP, intent, maxCandidates));
				case DIRECTORIES:
					String bfVD = stringValue(args.get("beforePath"), "beforePath");
					String afVD = stringValue(args.get("afterPath"), "afterPath");
					return toCallToolResult(service.validateDirectories(Path.of(bfVD), Path.of(afVD), intent, maxCandidates));
				default:
					throw new IllegalStateException("Unexpected source: " + source);
			}
		} catch (IllegalArgumentException e) {
			return toCallToolResult(McpValidationResult.error(e.getMessage(), intent, List.of("Invalid tool arguments.")));
		}
	}

	private static CallToolResult handleDiff(RefactoringMinerMcpService service, CallToolRequest request) {
		Map<String, Object> args = request.arguments();
		if (args == null) return toCallToolResult(McpDiffBrowserResult.error("Arguments are required.", 0, "Unknown", List.of("arguments=null")));

		String sourceStr = stringValue(args.get("source"), "source");
		AnalysisSource source;
		try {
			source = AnalysisSource.valueOf(sourceStr.toUpperCase());
		} catch (IllegalArgumentException e) {
			return toCallToolResult(McpDiffBrowserResult.error("Invalid source: " + sourceStr, 0, "Unknown", List.of("Valid sources are: FILE_CONTENTS, WORKTREE, COMMIT, PULL_REQUEST, DIRECTORIES")));
		}

		int port = integerValue(args.get("port"), DEFAULT_WEB_DIFF_PORT, "port");

		try {
			switch (source) {
				case FILE_CONTENTS:
					Map<String, String> bfDif = stringMap(args.get("beforeFiles"), "beforeFiles");
					Map<String, String> afDif = stringMap(args.get("afterFiles"), "afterFiles");
					int maxF_Dif = integerValue(args.get("maxFiles"), DEFAULT_MAX_FILES, "maxFiles");
					int maxB_Dif = integerValue(args.get("maxBytesPerFile"), DEFAULT_MAX_BYTES_PER_FILE, "maxBytesPerFile");
					return toCallToolResult(service.diffFileContents(bfDif, afDif, port, maxF_Dif, maxB_Dif));
				case WORKTREE:
					Path repDW = repositoryPath(args.get("repositoryPath"));
					String baseDW = optionalStringValue(args.get("baseRef"), "HEAD");
					boolean untDW = booleanValue(args.get("includeUntracked"), false);
					int maxF_DW = integerValue(args.get("maxFiles"), DEFAULT_MAX_FILES, "maxFiles");
					int maxB_DW = integerValue(args.get("maxBytesPerFile"), DEFAULT_MAX_BYTES_PER_FILE, "maxBytesPerFile");
					return toCallToolResult(service.diffWorktree(repDW, baseDW, untDW, maxF_DW, maxB_DW, port));
				case COMMIT:
					Path repDC = repositoryPath(args.get("repositoryPath"));
					String cidDC = stringValue(args.get("commitId"), "commitId");
					Integer pIdxDC = optionalIntegerValue(args.get("parentIndex"), "parentIndex");
					return toCallToolResult(service.diffCommit(repDC, cidDC, pIdxDC, port));
				case PULL_REQUEST:
					String urlDP = optionalCloneUrlValue(args.get("cloneUrl"));
					int prIdDP = integerValue(args.get("pullRequestId"), 0, "pullRequestId");
					int timeoutDP = integerValue(args.get("timeoutSeconds"), DEFAULT_TIMEOUT_SECONDS, "timeoutSeconds");
					return toCallToolResult(service.diffPullRequest(urlDP, prIdDP, timeoutDP, port));
				case DIRECTORIES:
					return toCallToolResult(McpDiffBrowserResult.error("AST diff browser is not supported for DIRECTORIES source.", port, "Directories", List.of("Unsupported source.")));
				default:
					throw new IllegalStateException("Unexpected source: " + source);
			}
		} catch (IllegalArgumentException e) {
			return toCallToolResult(McpDiffBrowserResult.error(e.getMessage(), port, "Unknown", List.of("Invalid tool arguments.")));
		}
	}

	private static CallToolResult toCallToolResult(McpAnalysisResult result) {
		return CallToolResult.builder()
				.addContent(new TextContent(writeResult(result)))
				.structuredContent(result)
				.isError("error".equals(result.status()))
				.build();
	}

	private static CallToolResult toCallToolResult(McpDiffBrowserResult result) {
		return CallToolResult.builder()
				.addContent(new TextContent(writeResult(result)))
				.structuredContent(result)
				.isError(McpDiffBrowserResult.ERROR.equals(result.status()))
				.build();
	}

	private static CallToolResult toCallToolResult(McpValidationResult result) {
		return CallToolResult.builder()
				.addContent(new TextContent(writeResult(result)))
				.structuredContent(result)
				.isError(McpValidationResult.ERROR.equals(result.status()))
				.build();
	}

	private static String writeResult(Object result) {
		try {
			return OBJECT_MAPPER.writeValueAsString(result);
		} catch (JsonProcessingException e) {
			return "{\"status\":\"error\",\"summary\":\"Failed to serialize MCP result.\"}";
		}
	}

	private static McpRefactoringIntent intentValue(Object value) {
		if (!(value instanceof Map<?, ?> map)) {
			throw new IllegalArgumentException("intent must be an object with at least a type field.");
		}
		Object type = map.get("type");
		if (!(type instanceof String typeString)) {
			throw new IllegalArgumentException("intent.type must be a non-empty string.");
		}
		return new McpRefactoringIntent(typeString,
				stringList(map.get("beforeFilePaths"), "intent.beforeFilePaths"),
				stringList(map.get("afterFilePaths"), "intent.afterFilePaths"),
				stringList(map.get("classNames"), "intent.classNames"),
				stringList(map.get("methodNames"), "intent.methodNames"),
				stringList(map.get("fieldNames"), "intent.fieldNames"),
				optionalIntentString(map.get("descriptionContains"), "intent.descriptionContains"));
	}

	private static List<String> stringList(Object value, String name) {
		if (value == null) {
			return List.of();
		}
		if (value instanceof String stringValue) {
			return List.of(stringValue);
		}
		if (!(value instanceof List<?> list)) {
			throw new IllegalArgumentException(name + " must be a string or array of strings.");
		}
		List<String> strings = new java.util.ArrayList<>();
		for (Object item : list) {
			if (!(item instanceof String stringItem)) {
				throw new IllegalArgumentException(name + " must contain only strings.");
			}
			strings.add(stringItem);
		}
		return strings;
	}

	private static String optionalIntentString(Object value, String name) {
		if (value == null) {
			return null;
		}
		if (value instanceof String stringValue) {
			return stringValue;
		}
		throw new IllegalArgumentException(name + " must be a string.");
	}

	private static Map<String, String> stringMap(Object value, String name) {
		if (!(value instanceof Map<?, ?> map)) {
			return null;
		}
		Map<String, String> strings = new LinkedHashMap<>();
		for (Map.Entry<?, ?> entry : map.entrySet()) {
			if (!(entry.getKey() instanceof String key) || !(entry.getValue() instanceof String stringValue)) {
				throw new IllegalArgumentException(name + " must map string paths to string file contents.");
			}
			strings.put(key, stringValue);
		}
		return strings;
	}

	private static int integerValue(Object value, int defaultValue, String name) {
		if (value == null) {
			return defaultValue;
		}
		if (value instanceof Number number) {
			return number.intValue();
		}
		if (value instanceof String stringValue) {
			return Integer.parseInt(stringValue);
		}
		throw new IllegalArgumentException(name + " must be an integer.");
	}

	private static Integer optionalIntegerValue(Object value, String name) {
		if (value == null) {
			return null;
		}
		return integerValue(value, 0, name);
	}

	private static boolean booleanValue(Object value, boolean defaultValue) {
		if (value == null) {
			return defaultValue;
		}
		if (value instanceof Boolean booleanValue) {
			return booleanValue;
		}
		if (value instanceof String stringValue) {
			return Boolean.parseBoolean(stringValue);
		}
		throw new IllegalArgumentException("includeUntracked must be a boolean.");
	}

	private static String stringValue(Object value, String name) {
		if (value instanceof String stringValue && !stringValue.isBlank()) {
			return stringValue;
		}
		throw new IllegalArgumentException(name + " must be a non-empty string.");
	}

	private static String optionalCloneUrlValue(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof String stringValue && !stringValue.isBlank()) {
			return stringValue;
		}
		throw new IllegalArgumentException("cloneUrl must be a non-empty string when provided.");
	}

	private static Path repositoryPath(Object value) {
		if (value == null) {
			return Path.of(System.getProperty("user.dir"));
		}
		return Path.of(stringValue(value, "repositoryPath"));
	}

	private static String optionalStringValue(Object value, String defaultValue) {
		if (value == null) {
			return defaultValue;
		}
		if (value instanceof String stringValue && !stringValue.isBlank()) {
			return stringValue;
		}
		throw new IllegalArgumentException("baseRef must be a non-empty string.");
	}

	private static JsonSchema analyzeInputSchema() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("source", Map.of(
				"type", "string",
				"enum", List.of("FILE_CONTENTS", "WORKTREE", "COMMIT", "PULL_REQUEST", "DIRECTORIES"),
				"description", "The source of the code changes to analyze."));

		properties.put("beforeFiles", fileMapSchema());
		properties.put("afterFiles", fileMapSchema());
		properties.put("repositoryPath", Map.of("type", "string"));
		properties.put("baseRef", Map.of("type", "string", "default", "HEAD"));
		properties.put("includeUntracked", Map.of("type", "boolean", "default", false));
		properties.put("commitId", Map.of("type", "string"));
		properties.put("parentIndex", Map.of("type", "integer", "minimum", 0));
		properties.put("cloneUrl", Map.of("type", "string"));
		properties.put("pullRequestId", Map.of("type", "integer", "minimum", 1));
		properties.put("timeoutSeconds", Map.of("type", "integer", "minimum", 1, "default", DEFAULT_TIMEOUT_SECONDS));
		properties.put("beforePath", Map.of("type", "string"));
		properties.put("afterPath", Map.of("type", "string"));
		properties.put("maxRefactorings", Map.of("type", "integer", "minimum", 0, "default", DEFAULT_MAX_REFACTORINGS));

		putFileContentBoundsProperties(properties);

		return new JsonSchema("object", properties, List.of("source"), false, null, null);
	}

	private static JsonSchema validateInputSchema() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("source", Map.of(
				"type", "string",
				"enum", List.of("FILE_CONTENTS", "WORKTREE", "COMMIT", "PULL_REQUEST", "DIRECTORIES"),
				"description", "The source of the code changes to validate."));

		properties.put("intent", intentSchema());
		properties.put("beforeFiles", fileMapSchema());
		properties.put("afterFiles", fileMapSchema());
		properties.put("repositoryPath", Map.of("type", "string"));
		properties.put("baseRef", Map.of("type", "string", "default", "HEAD"));
		properties.put("includeUntracked", Map.of("type", "boolean", "default", false));
		properties.put("commitId", Map.of("type", "string"));
		properties.put("parentIndex", Map.of("type", "integer", "minimum", 0));
		properties.put("cloneUrl", Map.of("type", "string"));
		properties.put("pullRequestId", Map.of("type", "integer", "minimum", 1));
		properties.put("timeoutSeconds", Map.of("type", "integer", "minimum", 1, "default", DEFAULT_TIMEOUT_SECONDS));
		properties.put("beforePath", Map.of("type", "string"));
		properties.put("afterPath", Map.of("type", "string"));
		properties.put("maxCandidates", Map.of("type", "integer", "minimum", 0, "default", DEFAULT_MAX_CANDIDATES));

		putFileContentBoundsProperties(properties);

		return new JsonSchema("object", properties, List.of("source", "intent"), false, null, null);
	}

	private static JsonSchema diffInputSchema() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("source", Map.of(
				"type", "string",
				"enum", List.of("FILE_CONTENTS", "WORKTREE", "COMMIT", "PULL_REQUEST"),
				"description", "The source of the code changes to diff. (Note: DIRECTORIES not supported)"));

		properties.put("beforeFiles", fileMapSchema());
		properties.put("afterFiles", fileMapSchema());
		properties.put("repositoryPath", Map.of("type", "string"));
		properties.put("baseRef", Map.of("type", "string", "default", "HEAD"));
		properties.put("includeUntracked", Map.of("type", "boolean", "default", false));
		properties.put("commitId", Map.of("type", "string"));
		properties.put("parentIndex", Map.of("type", "integer", "minimum", 0));
		properties.put("cloneUrl", Map.of("type", "string"));
		properties.put("pullRequestId", Map.of("type", "integer", "minimum", 1));
		properties.put("timeoutSeconds", Map.of("type", "integer", "minimum", 1, "default", DEFAULT_TIMEOUT_SECONDS));
		properties.put("port", Map.of("type", "integer", "minimum", 1, "maximum", 65535, "default", DEFAULT_WEB_DIFF_PORT));

		putFileContentBoundsProperties(properties);

		return new JsonSchema("object", properties, List.of("source"), false, null, null);
	}

	private static void putFileContentBoundsProperties(Map<String, Object> properties) {
		properties.put("maxFiles", Map.of("type", "integer", "minimum", 1, "default", DEFAULT_MAX_FILES));
		properties.put("maxBytesPerFile", Map.of("type", "integer", "minimum", 1, "default", DEFAULT_MAX_BYTES_PER_FILE));
	}

	private static Map<String, Object> fileMapSchema() {
		return Map.of(
				"type", "object",
				"additionalProperties", Map.of("type", "string"));
	}

	private static Map<String, Object> intentSchema() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("type", Map.of("type", "string",
				"description", "RefactoringMiner refactoring type, for example Rename Method."));
		properties.put("beforeFilePaths", stringOrStringArraySchema());
		properties.put("afterFilePaths", stringOrStringArraySchema());
		properties.put("classNames", stringOrStringArraySchema());
		properties.put("methodNames", stringOrStringArraySchema());
		properties.put("fieldNames", stringOrStringArraySchema());
		properties.put("descriptionContains", Map.of("type", "string"));
		return Map.of(
				"type", "object",
				"properties", properties,
				"required", List.of("type"),
				"additionalProperties", false);
	}

	private static Map<String, Object> stringOrStringArraySchema() {
		return Map.of("oneOf", List.of(
				Map.of("type", "string"),
				Map.of("type", "array", "items", Map.of("type", "string"))));
	}

	private static Map<String, Object> validationOutputSchema() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("status", Map.of("type", "string",
				"enum", List.of("matched", "missing", "ambiguous", "error")));
		properties.put("summary", Map.of("type", "string"));
		properties.put("intent", intentSchema());
		properties.put("refactoringCount", Map.of("type", "integer"));
		properties.put("candidateCount", Map.of("type", "integer"));
		properties.put("matches", Map.of("type", "array"));
		properties.put("candidates", Map.of("type", "array"));
		properties.put("warnings", Map.of("type", "array", "items", Map.of("type", "string")));
		return Map.of(
				"type", "object",
				"properties", properties,
				"required", List.of("status", "summary", "refactoringCount", "candidateCount", "matches",
						"candidates", "warnings"));
	}

	private static Map<String, Object> outputSchema() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("status", Map.of("type", "string", "enum", List.of("ok", "error")));
		properties.put("summary", Map.of("type", "string"));
		properties.put("refactoringCount", Map.of("type", "integer"));
		properties.put("astDiffCount", Map.of("type", "integer"));
		properties.put("moveAstDiffCount", Map.of("type", "integer"));
		properties.put("filesBefore", Map.of("type", "integer"));
		properties.put("filesAfter", Map.of("type", "integer"));
		properties.put("refactorings", Map.of("type", "array"));
		properties.put("warnings", Map.of("type", "array", "items", Map.of("type", "string")));
		return Map.of(
				"type", "object",
				"properties", properties,
				"required", List.of("status", "summary", "refactoringCount", "astDiffCount", "moveAstDiffCount",
						"filesBefore", "filesAfter", "refactorings", "warnings"));
	}

	private static Map<String, Object> diffBrowserOutputSchema() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("status", Map.of("type", "string", "enum", List.of("ok", "error")));
		properties.put("summary", Map.of("type", "string"));
		properties.put("message", Map.of("type", "string"));
		properties.put("url", Map.of("type", "string"));
		properties.put("port", Map.of("type", "integer"));
		properties.put("inputSummary", Map.of("type", "string"));
		properties.put("refactoringCount", Map.of("type", "integer"));
		properties.put("astDiffCount", Map.of("type", "integer"));
		properties.put("moveAstDiffCount", Map.of("type", "integer"));
		properties.put("filesBefore", Map.of("type", "integer"));
		properties.put("filesAfter", Map.of("type", "integer"));
		properties.put("affectedFiles", Map.of("type", "array", "items", Map.of("type", "string")));
		properties.put("warnings", Map.of("type", "array", "items", Map.of("type", "string")));
		return Map.of(
				"type", "object",
				"properties", properties,
				"required", List.of("status", "summary", "inputSummary", "refactoringCount", "astDiffCount",
						"moveAstDiffCount", "filesBefore", "filesAfter", "affectedFiles", "warnings"));
	}
}
