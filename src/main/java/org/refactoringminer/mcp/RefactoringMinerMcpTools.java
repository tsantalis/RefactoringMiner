package org.refactoringminer.mcp;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
	static final String ANALYZE = "refactoringminer_analyze";
	static final String VALIDATE = "refactoringminer_validate";
	static final String DIFF = "refactoringminer_diff";

	private static final Set<String> ANALYSIS_SOURCE_TYPES =
			Set.of("fileContents", "worktree", "commit", "pullRequest", "directories");
	private static final Set<String> DIFF_SOURCE_TYPES =
			Set.of("fileContents", "worktree", "commit", "pullRequest", "url");
	private static final Set<String> SOURCE_ARGUMENTS =
			Set.of("type", "beforeFiles", "afterFiles", "baseRef", "includeUntracked", "maxFiles",
					"maxBytesPerFile", "commitId", "parentIndex", "cloneUrl", "pullRequestId", "timeoutSeconds",
					"beforePath", "afterPath", "url", "workingDirectory");
	private static final Map<String, Set<String>> SOURCE_ARGUMENTS_BY_TYPE = Map.of(
			"fileContents", Set.of("type", "beforeFiles", "afterFiles", "maxFiles", "maxBytesPerFile"),
			"worktree", Set.of("type", "workingDirectory", "baseRef", "includeUntracked", "maxFiles",
					"maxBytesPerFile"),
			"commit", Set.of("type", "workingDirectory", "commitId", "parentIndex"),
			"pullRequest", Set.of("type", "cloneUrl", "pullRequestId", "timeoutSeconds"),
			"directories", Set.of("type", "beforePath", "afterPath"),
			"url", Set.of("type", "url", "parentIndex", "timeoutSeconds"));
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
		return new SyncToolSpecification[] { analyzeTool(service), validateTool(service), diffTool(service) };
	}

	static SyncToolSpecification analyzeTool(RefactoringMinerMcpService service) {
		return SyncToolSpecification.builder()
				.tool(Tool.builder()
						.name(ANALYZE)
						.title("Analyze refactorings")
						.description("Find refactorings from a source. The source can be fileContents, worktree, commit, pullRequest, or directories. Does not edit files.")
						.inputSchema(analyzeInputSchema())
						.outputSchema(outputSchema())
						.annotations(new ToolAnnotations("Analyze refactorings", true, false, true, true, false))
						.build())
				.callHandler((exchange, request) -> handleAnalyze(service, request))
				.build();
	}

	static SyncToolSpecification validateTool(RefactoringMinerMcpService service) {
		return SyncToolSpecification.builder()
				.tool(Tool.builder()
						.name(VALIDATE)
						.title("Validate intended refactoring")
						.description("Check whether detected refactorings from a source match a stated intent. Does not edit files.")
						.inputSchema(validateInputSchema())
						.outputSchema(validationOutputSchema())
						.annotations(new ToolAnnotations("Validate intended refactoring", true, false, true, true,
								false))
						.build())
				.callHandler((exchange, request) -> handleValidate(service, request))
				.build();
	}

	static SyncToolSpecification diffTool(RefactoringMinerMcpService service) {
		return SyncToolSpecification.builder()
				.tool(Tool.builder()
						.name(DIFF)
						.title("Open RefactoringMiner AST diff")
						.description("Start a local RefactoringMiner AST diff page from a source. The source can be fileContents, worktree, commit, pullRequest, or url.")
						.inputSchema(diffInputSchema())
						.outputSchema(diffBrowserOutputSchema())
						.annotations(new ToolAnnotations("Open RefactoringMiner AST diff", true, false, true, true,
								false))
						.build())
				.callHandler((exchange, request) -> handleDiff(service, request))
				.build();
	}

	private static CallToolResult handleAnalyze(RefactoringMinerMcpService service, CallToolRequest request) {
		McpAnalysisResult result = analyze(service, request.arguments());
		return toCallToolResult(result);
	}

	static McpAnalysisResult analyze(RefactoringMinerMcpService service, Map<String, Object> arguments) {
		Map<String, Object> resolvedArguments = arguments == null ? Map.of() : arguments;
		try {
			String unsupportedArgument = unsupportedArgument(resolvedArguments, Set.of("source", "maxRefactorings"));
			if (unsupportedArgument != null) {
				return McpAnalysisResult.error("Unsupported argument for " + ANALYZE + ": " + unsupportedArgument
						+ ".", unsupportedArgumentWarnings(unsupportedArgument));
			}
			Map<String, Object> source = sourceValue(resolvedArguments.get("source"));
			McpAnalysisResult unsupported = unsupportedSourceArgument(source, ANALYZE);
			if (unsupported != null) {
				return unsupported;
			}
			String sourceType = sourceType(source);
			if (!ANALYSIS_SOURCE_TYPES.contains(sourceType)) {
				return McpAnalysisResult.error("Unsupported source.type for " + ANALYZE + ": " + sourceType + ".",
						List.of("Supported source types are fileContents, worktree, commit, pullRequest, and directories."));
			}
			String unsupportedSourceArgument = unsupportedSourceArgumentForType(source, sourceType);
			if (unsupportedSourceArgument != null) {
				return McpAnalysisResult.error("Unsupported source argument for " + sourceType + ": "
						+ unsupportedSourceArgument + ".", unsupportedArgumentWarnings(unsupportedSourceArgument));
			}
			int maxRefactorings = integerValue(resolvedArguments.get("maxRefactorings"), DEFAULT_MAX_REFACTORINGS,
					"maxRefactorings");
			return switch (sourceType) {
				case "fileContents" -> service.analyzeFileContents(
						stringMap(source.get("beforeFiles"), "source.beforeFiles"),
						stringMap(source.get("afterFiles"), "source.afterFiles"),
						maxRefactorings,
						integerValue(source.get("maxFiles"), DEFAULT_MAX_FILES, "source.maxFiles"),
						integerValue(source.get("maxBytesPerFile"), DEFAULT_MAX_BYTES_PER_FILE,
								"source.maxBytesPerFile"));
				case "worktree" -> service.analyzeWorktree(workingDirectory(source),
						optionalStringValue(source.get("baseRef"), "HEAD"),
						booleanValue(source.get("includeUntracked"), false),
						integerValue(source.get("maxFiles"), DEFAULT_MAX_FILES, "source.maxFiles"),
						integerValue(source.get("maxBytesPerFile"), DEFAULT_MAX_BYTES_PER_FILE,
								"source.maxBytesPerFile"),
						maxRefactorings);
				case "commit" -> service.analyzeCommit(workingDirectory(source),
						stringValue(source.get("commitId"), "source.commitId"),
						optionalIntegerValue(source.get("parentIndex"), "source.parentIndex"),
						maxRefactorings);
				case "pullRequest" -> service.analyzePullRequest(
						optionalCloneUrlValue(source.get("cloneUrl")),
						integerValue(source.get("pullRequestId"), 0, "source.pullRequestId"),
						integerValue(source.get("timeoutSeconds"), DEFAULT_TIMEOUT_SECONDS, "source.timeoutSeconds"),
						maxRefactorings);
				case "directories" -> service.analyzeDirectories(
						pathValue(source.get("beforePath"), "source.beforePath"),
						pathValue(source.get("afterPath"), "source.afterPath"),
						maxRefactorings);
				default -> throw new IllegalStateException("Unexpected source type: " + sourceType);
			};
		} catch (IllegalArgumentException e) {
			return McpAnalysisResult.error(e.getMessage(), List.of("Invalid tool arguments."));
		}
	}

	private static CallToolResult handleValidate(RefactoringMinerMcpService service, CallToolRequest request) {
		McpValidationResult result = validate(service, request.arguments());
		return toCallToolResult(result);
	}

	static McpValidationResult validate(RefactoringMinerMcpService service, Map<String, Object> arguments) {
		Map<String, Object> resolvedArguments = arguments == null ? Map.of() : arguments;
		McpRefactoringIntent intent = null;
		try {
			String unsupportedArgument = unsupportedArgument(resolvedArguments,
					Set.of("source", "intent", "maxCandidates"));
			if (unsupportedArgument != null) {
				return McpValidationResult.error("Unsupported argument for " + VALIDATE + ": " + unsupportedArgument
						+ ".", null, unsupportedArgumentWarnings(unsupportedArgument));
			}
			intent = intentValue(resolvedArguments.get("intent"));
			Map<String, Object> source = sourceValue(resolvedArguments.get("source"));
			McpValidationResult unsupported = unsupportedValidationSourceArgument(source, intent);
			if (unsupported != null) {
				return unsupported;
			}
			String sourceType = sourceType(source);
			if (!ANALYSIS_SOURCE_TYPES.contains(sourceType)) {
				return McpValidationResult.error("Unsupported source.type for " + VALIDATE + ": " + sourceType + ".",
						intent, List.of("Supported source types are fileContents, worktree, commit, pullRequest, and directories."));
			}
			String unsupportedSourceArgument = unsupportedSourceArgumentForType(source, sourceType);
			if (unsupportedSourceArgument != null) {
				return McpValidationResult.error("Unsupported source argument for " + sourceType + ": "
						+ unsupportedSourceArgument + ".", intent, unsupportedArgumentWarnings(unsupportedSourceArgument));
			}
			int maxCandidates = integerValue(resolvedArguments.get("maxCandidates"), DEFAULT_MAX_CANDIDATES,
					"maxCandidates");
			return switch (sourceType) {
				case "fileContents" -> service.validateFileContents(
						stringMap(source.get("beforeFiles"), "source.beforeFiles"),
						stringMap(source.get("afterFiles"), "source.afterFiles"),
						intent,
						maxCandidates,
						integerValue(source.get("maxFiles"), DEFAULT_MAX_FILES, "source.maxFiles"),
						integerValue(source.get("maxBytesPerFile"), DEFAULT_MAX_BYTES_PER_FILE,
								"source.maxBytesPerFile"));
				case "worktree" -> service.validateWorktree(workingDirectory(source),
						optionalStringValue(source.get("baseRef"), "HEAD"),
						booleanValue(source.get("includeUntracked"), false),
						integerValue(source.get("maxFiles"), DEFAULT_MAX_FILES, "source.maxFiles"),
						integerValue(source.get("maxBytesPerFile"), DEFAULT_MAX_BYTES_PER_FILE,
								"source.maxBytesPerFile"),
						intent,
						maxCandidates);
				case "commit" -> service.validateCommit(workingDirectory(source),
						stringValue(source.get("commitId"), "source.commitId"),
						optionalIntegerValue(source.get("parentIndex"), "source.parentIndex"),
						intent,
						maxCandidates);
				case "pullRequest" -> service.validatePullRequest(
						optionalCloneUrlValue(source.get("cloneUrl")),
						integerValue(source.get("pullRequestId"), 0, "source.pullRequestId"),
						integerValue(source.get("timeoutSeconds"), DEFAULT_TIMEOUT_SECONDS, "source.timeoutSeconds"),
						intent,
						maxCandidates);
				case "directories" -> service.validateDirectories(
						pathValue(source.get("beforePath"), "source.beforePath"),
						pathValue(source.get("afterPath"), "source.afterPath"),
						intent,
						maxCandidates);
				default -> throw new IllegalStateException("Unexpected source type: " + sourceType);
			};
		} catch (IllegalArgumentException e) {
			return McpValidationResult.error(e.getMessage(), intent, List.of("Invalid tool arguments."));
		}
	}

	private static CallToolResult handleDiff(RefactoringMinerMcpService service, CallToolRequest request) {
		McpDiffBrowserResult result = diff(service, request.arguments());
		return toCallToolResult(result);
	}

	static McpDiffBrowserResult diff(RefactoringMinerMcpService service, Map<String, Object> arguments) {
		Map<String, Object> resolvedArguments = arguments == null ? Map.of() : arguments;
		try {
			String unsupportedArgument = unsupportedArgument(resolvedArguments,
					Set.of("source", "port", "maxRefactorings"));
			int port = portValue(resolvedArguments.get("port"));
			if (unsupportedArgument != null) {
				return McpDiffBrowserResult.error("Unsupported argument for " + DIFF + ": " + unsupportedArgument
						+ ".", port, "RefactoringMiner diff", unsupportedArgumentWarnings(unsupportedArgument));
			}
			Map<String, Object> source = sourceValue(resolvedArguments.get("source"));
			McpDiffBrowserResult unsupported = unsupportedDiffSourceArgument(source, port);
			if (unsupported != null) {
				return unsupported;
			}
			String sourceType = sourceType(source);
			if (!DIFF_SOURCE_TYPES.contains(sourceType)) {
				return McpDiffBrowserResult.error("Unsupported source.type for " + DIFF + ": " + sourceType + ".",
						port, "RefactoringMiner diff",
						List.of("Supported source types are fileContents, worktree, commit, pullRequest, and url."));
			}
			String unsupportedSourceArgument = unsupportedSourceArgumentForType(source, sourceType);
			if (unsupportedSourceArgument != null) {
				return McpDiffBrowserResult.error("Unsupported source argument for " + sourceType + ": "
						+ unsupportedSourceArgument + ".", port, "RefactoringMiner diff",
						unsupportedArgumentWarnings(unsupportedSourceArgument));
			}
			int maxRefactorings = integerValue(resolvedArguments.get("maxRefactorings"), DEFAULT_MAX_REFACTORINGS,
					"maxRefactorings");
			return switch (sourceType) {
				case "fileContents" -> service.diffFileContents(
						stringMap(source.get("beforeFiles"), "source.beforeFiles"),
						stringMap(source.get("afterFiles"), "source.afterFiles"),
						port,
						integerValue(source.get("maxFiles"), DEFAULT_MAX_FILES, "source.maxFiles"),
						integerValue(source.get("maxBytesPerFile"), DEFAULT_MAX_BYTES_PER_FILE,
								"source.maxBytesPerFile"),
						maxRefactorings);
				case "worktree" -> service.diffWorktree(workingDirectory(source),
						optionalStringValue(source.get("baseRef"), "HEAD"),
						booleanValue(source.get("includeUntracked"), false),
						integerValue(source.get("maxFiles"), DEFAULT_MAX_FILES, "source.maxFiles"),
						integerValue(source.get("maxBytesPerFile"), DEFAULT_MAX_BYTES_PER_FILE,
								"source.maxBytesPerFile"),
						port,
						maxRefactorings);
				case "commit" -> service.diffCommit(workingDirectory(source),
						stringValue(source.get("commitId"), "source.commitId"),
						optionalIntegerValue(source.get("parentIndex"), "source.parentIndex"),
						port,
						maxRefactorings);
				case "pullRequest" -> service.diffPullRequest(
						optionalCloneUrlValue(source.get("cloneUrl")),
						integerValue(source.get("pullRequestId"), 0, "source.pullRequestId"),
						integerValue(source.get("timeoutSeconds"), DEFAULT_TIMEOUT_SECONDS, "source.timeoutSeconds"),
						port,
						maxRefactorings);
				case "url" -> service.diffUrl(
						stringValue(source.get("url"), "source.url"),
						optionalIntegerValue(source.get("parentIndex"), "source.parentIndex"),
						integerValue(source.get("timeoutSeconds"), DEFAULT_TIMEOUT_SECONDS, "source.timeoutSeconds"),
						port,
						maxRefactorings);
				default -> throw new IllegalStateException("Unexpected source type: " + sourceType);
			};
		} catch (IllegalArgumentException e) {
			return McpDiffBrowserResult.error(e.getMessage(), null, "RefactoringMiner diff",
					List.of("Invalid tool arguments."));
		}
	}

	private static CallToolResult toCallToolResult(McpAnalysisResult result) {
		return CallToolResult.builder()
				.addContent(new TextContent(writeResult(result)))
				.structuredContent(result)
				.isError("error".equals(result.status()))
				.build();
	}

	private static CallToolResult toCallToolResult(McpValidationResult result) {
		return CallToolResult.builder()
				.addContent(new TextContent(writeResult(result)))
				.structuredContent(result)
				.isError(McpValidationResult.ERROR.equals(result.status()))
				.build();
	}

	private static CallToolResult toCallToolResult(McpDiffBrowserResult result) {
		return CallToolResult.builder()
				.addContent(new TextContent(writeResult(result)))
				.structuredContent(result)
				.isError(McpDiffBrowserResult.ERROR.equals(result.status()))
				.build();
	}

	private static String writeResult(Object result) {
		try {
			return OBJECT_MAPPER.writeValueAsString(result);
		} catch (JsonProcessingException e) {
			return "{\"status\":\"error\",\"summary\":\"Failed to serialize MCP result.\"}";
		}
	}

	private static Map<String, Object> sourceValue(Object value) {
		if (value == null) {
			return Map.of("type", "worktree");
		}
		if (!(value instanceof Map<?, ?> map)) {
			throw new IllegalArgumentException("source must be an object.");
		}
		Map<String, Object> source = new LinkedHashMap<>();
		for (Map.Entry<?, ?> entry : map.entrySet()) {
			if (!(entry.getKey() instanceof String key)) {
				throw new IllegalArgumentException("source keys must be strings.");
			}
			source.put(key, entry.getValue());
		}
		return source;
	}

	private static String sourceType(Map<String, Object> source) {
		Object value = source.get("type");
		if (value == null) {
			return inferSourceType(source);
		}
		if (value instanceof String stringValue && !stringValue.isBlank()) {
			return normalizeSourceType(stringValue);
		}
		throw new IllegalArgumentException("source.type must be a non-empty string.");
	}

	private static String inferSourceType(Map<String, Object> source) {
		if (source.containsKey("url")) {
			return "url";
		}
		if (source.containsKey("pullRequestId") || source.containsKey("cloneUrl")) {
			return "pullRequest";
		}
		if (source.containsKey("commitId")) {
			return "commit";
		}
		if (source.containsKey("beforeFiles") || source.containsKey("afterFiles")) {
			return "fileContents";
		}
		if (source.containsKey("beforePath") || source.containsKey("afterPath")) {
			return "directories";
		}
		return "worktree";
	}

	private static String normalizeSourceType(String sourceType) {
		String normalized = sourceType.trim().toLowerCase(Locale.ROOT)
				.replace("_", "")
				.replace("-", "")
				.replace(" ", "");
		return switch (normalized) {
			case "filecontents", "filecontent", "files" -> "fileContents";
			case "worktree", "local", "localworktree" -> "worktree";
			case "commit" -> "commit";
			case "pullrequest", "pullrequests", "pr" -> "pullRequest";
			case "directories", "directory", "dirs" -> "directories";
			case "url", "uri" -> "url";
			default -> sourceType.trim();
		};
	}

	private static McpAnalysisResult unsupportedSourceArgument(Map<String, Object> source, String toolName) {
		String unsupportedArgument = unsupportedArgument(source, SOURCE_ARGUMENTS);
		if (unsupportedArgument == null) {
			return null;
		}
		return McpAnalysisResult.error("Unsupported source argument for " + toolName + ": " + unsupportedArgument
				+ ".", unsupportedArgumentWarnings(unsupportedArgument));
	}

	private static McpValidationResult unsupportedValidationSourceArgument(Map<String, Object> source,
			McpRefactoringIntent intent) {
		String unsupportedArgument = unsupportedArgument(source, SOURCE_ARGUMENTS);
		if (unsupportedArgument == null) {
			return null;
		}
		return McpValidationResult.error("Unsupported source argument for " + VALIDATE + ": " + unsupportedArgument
				+ ".", intent, unsupportedArgumentWarnings(unsupportedArgument));
	}

	private static McpDiffBrowserResult unsupportedDiffSourceArgument(Map<String, Object> source, int port) {
		String unsupportedArgument = unsupportedArgument(source, SOURCE_ARGUMENTS);
		if (unsupportedArgument == null) {
			return null;
		}
		return McpDiffBrowserResult.error("Unsupported source argument for " + DIFF + ": " + unsupportedArgument
				+ ".", port, "RefactoringMiner diff", unsupportedArgumentWarnings(unsupportedArgument));
	}

	private static String unsupportedArgument(Map<String, Object> arguments, Set<String> supportedArguments) {
		for (String argument : arguments.keySet()) {
			if (!supportedArguments.contains(argument)) {
				return argument;
			}
		}
		return null;
	}

	private static String unsupportedSourceArgumentForType(Map<String, Object> source, String sourceType) {
		Set<String> supportedArguments = SOURCE_ARGUMENTS_BY_TYPE.get(sourceType);
		if (supportedArguments == null) {
			return null;
		}
		return unsupportedArgument(source, supportedArguments);
	}

	private static List<String> unsupportedArgumentWarnings(String unsupportedArgument) {
		if ("repositoryPath".equals(unsupportedArgument)) {
			return List.of("Start the MCP server in the target project directory instead of passing repositoryPath.");
		}
		return List.of("Use the source object to select fileContents, worktree, commit, pullRequest, directories, or url.");
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
		List<String> strings = new ArrayList<>();
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

	private static Path pathValue(Object value, String name) {
		return Path.of(stringValue(value, name));
	}

	private static Path workingDirectory(Map<String, Object> source) {
		Object value = source.get("workingDirectory");
		if (value == null) {
			return null;
		}
		String path = stringValue(value, "source.workingDirectory");
		Path relativePath = Path.of(path);
		if (relativePath.isAbsolute()) {
			throw new IllegalArgumentException(
					"source.workingDirectory must be relative to the MCP server working directory.");
		}
		Path normalizedPath = relativePath.normalize();
		if (normalizedPath.startsWith("..")) {
			throw new IllegalArgumentException(
					"source.workingDirectory must stay inside the MCP server working directory.");
		}
		return Path.of(System.getProperty("user.dir")).resolve(normalizedPath).normalize().toAbsolutePath();
	}

	private static int integerValue(Object value, int defaultValue, String name) {
		if (value == null) {
			return defaultValue;
		}
		if (value instanceof Number number) {
			return number.intValue();
		}
		if (value instanceof String stringValue) {
			try {
				return Integer.parseInt(stringValue);
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException(name + " must be an integer.", e);
			}
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

	private static int portValue(Object value) {
		int port = integerValue(value, DEFAULT_WEB_DIFF_PORT, "port");
		if (port < 1 || port > 65535) {
			throw new IllegalArgumentException("port must be between 1 and 65535.");
		}
		return port;
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
		throw new IllegalArgumentException("source.cloneUrl must be a non-empty string when provided.");
	}

	private static String optionalStringValue(Object value, String defaultValue) {
		if (value == null) {
			return defaultValue;
		}
		if (value instanceof String stringValue && !stringValue.isBlank()) {
			return stringValue;
		}
		throw new IllegalArgumentException("source.baseRef must be a non-empty string.");
	}

	private static JsonSchema analyzeInputSchema() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("source", sourceSchema(List.of("fileContents", "worktree", "commit", "pullRequest",
				"directories")));
		properties.put("maxRefactorings", Map.of("type", "integer", "minimum", 0,
				"default", DEFAULT_MAX_REFACTORINGS));
		return new JsonSchema("object", properties, List.of(), false, null, null);
	}

	private static JsonSchema validateInputSchema() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("source", sourceSchema(List.of("fileContents", "worktree", "commit", "pullRequest",
				"directories")));
		properties.put("intent", intentSchema());
		properties.put("maxCandidates", Map.of("type", "integer", "minimum", 0,
				"default", DEFAULT_MAX_CANDIDATES));
		return new JsonSchema("object", properties, List.of("intent"), false, null, null);
	}

	private static JsonSchema diffInputSchema() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("source", sourceSchema(List.of("fileContents", "worktree", "commit", "pullRequest", "url")));
		properties.put("port", Map.of("type", "integer", "minimum", 1, "maximum", 65535,
				"default", DEFAULT_WEB_DIFF_PORT,
				"description", "Local WebDiff port. Defaults to 6789."));
		properties.put("maxRefactorings", Map.of("type", "integer", "minimum", 0,
				"default", DEFAULT_MAX_REFACTORINGS));
		return new JsonSchema("object", properties, List.of(), false, null, null);
	}

	private static Map<String, Object> sourceSchema(List<String> sourceTypes) {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("type", Map.of("type", "string", "enum", sourceTypes, "default", "worktree",
				"description", "Optional when another source field makes the source clear, such as pullRequestId, url, commitId, beforeFiles, or beforePath."));
		properties.put("beforeFiles", fileMapSchema());
		properties.put("afterFiles", fileMapSchema());
		properties.put("baseRef", Map.of("type", "string", "default", "HEAD"));
		properties.put("workingDirectory", Map.of("type", "string",
				"description", "Relative subdirectory under the MCP server working directory for local worktree and commit sources."));
		properties.put("includeUntracked", Map.of("type", "boolean", "default", false));
		properties.put("maxFiles", Map.of("type", "integer", "minimum", 1, "default", DEFAULT_MAX_FILES));
		properties.put("maxBytesPerFile", Map.of("type", "integer", "minimum", 1,
				"default", DEFAULT_MAX_BYTES_PER_FILE));
		properties.put("commitId", Map.of("type", "string"));
		properties.put("parentIndex", Map.of("type", "integer", "minimum", 0));
		properties.put("cloneUrl", Map.of("type", "string",
				"description", "GitHub repository clone URL. Defaults to the MCP server working directory's origin remote."));
		properties.put("pullRequestId", Map.of("type", "integer", "minimum", 1));
		properties.put("timeoutSeconds", Map.of("type", "integer", "minimum", 1,
				"default", DEFAULT_TIMEOUT_SECONDS));
		properties.put("beforePath", Map.of("type", "string"));
		properties.put("afterPath", Map.of("type", "string"));
		properties.put("url", Map.of("type", "string", "description", "GitHub commit or pull request URL."));
		return Map.of(
				"type", "object",
				"properties", properties,
				"additionalProperties", false);
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
		properties.put("refactorings", Map.of("type", "array"));
		properties.put("affectedFiles", Map.of("type", "array", "items", Map.of("type", "string")));
		properties.put("warnings", Map.of("type", "array", "items", Map.of("type", "string")));
		return Map.of(
				"type", "object",
				"properties", properties,
				"required", List.of("status", "summary", "inputSummary", "refactoringCount", "astDiffCount",
						"moveAstDiffCount", "filesBefore", "filesAfter", "refactorings", "affectedFiles", "warnings"));
	}
}
