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
	static final String ANALYZE_FILE_CONTENTS = "refactoringminer_analyze_file_contents";
	static final String ANALYZE_WORKTREE = "refactoringminer_analyze_worktree";
	static final String ANALYZE_COMMIT = "refactoringminer_analyze_commit";
	static final String ANALYZE_PULL_REQUEST = "refactoringminer_analyze_pull_request";
	static final String ANALYZE_DIRECTORIES = "refactoringminer_analyze_directories";
	static final String VALIDATE_FILE_CONTENTS = "refactoringminer_validate_file_contents";
	static final String VALIDATE_WORKTREE = "refactoringminer_validate_worktree";
	static final String VALIDATE_COMMIT = "refactoringminer_validate_commit";
	static final String VALIDATE_PULL_REQUEST = "refactoringminer_validate_pull_request";
	static final String VALIDATE_DIRECTORIES = "refactoringminer_validate_directories";
	static final String DIFF_FILE_CONTENTS = "refactoringminer_diff_file_contents";
	static final String DIFF_WORKTREE = "refactoringminer_diff_worktree";
	static final String DIFF_COMMIT = "refactoringminer_diff_commit";
	static final String DIFF_PULL_REQUEST = "refactoringminer_diff_pull_request";
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
		return new SyncToolSpecification[] { analyzeFileContentsTool(service), analyzeWorktreeTool(service),
				analyzeCommitTool(service), analyzePullRequestTool(service), analyzeDirectoriesTool(service),
				validateFileContentsTool(service), validateWorktreeTool(service), validateCommitTool(service),
				validatePullRequestTool(service), validateDirectoriesTool(service), diffFileContentsTool(service),
				diffWorktreeTool(service), diffCommitTool(service), diffPullRequestTool(service) };
	}

	static SyncToolSpecification analyzeFileContentsTool(RefactoringMinerMcpService service) {
		return SyncToolSpecification.builder()
				.tool(Tool.builder()
						.name(ANALYZE_FILE_CONTENTS)
						.title("Analyze explicit before/after file contents")
						.description("Find refactorings in explicit before/after file-content maps. Does not edit files.")
						.inputSchema(inputSchema())
						.outputSchema(outputSchema())
						.annotations(new ToolAnnotations("Analyze file contents", true, false, true, false, false))
						.build())
				.callHandler((exchange, request) -> handleAnalyzeFileContents(service, request))
				.build();
	}

	static SyncToolSpecification analyzeWorktreeTool(RefactoringMinerMcpService service) {
		return SyncToolSpecification.builder()
				.tool(Tool.builder()
						.name(ANALYZE_WORKTREE)
						.title("Analyze local worktree changes")
						.description("Find refactorings in local modified, added, deleted, and optional untracked files. Does not edit files.")
						.inputSchema(worktreeInputSchema())
						.outputSchema(outputSchema())
						.annotations(new ToolAnnotations("Analyze worktree", true, false, true, false, false))
						.build())
				.callHandler((exchange, request) -> handleAnalyzeWorktree(service, request))
				.build();
	}

	static SyncToolSpecification analyzeCommitTool(RefactoringMinerMcpService service) {
		return SyncToolSpecification.builder()
				.tool(Tool.builder()
						.name(ANALYZE_COMMIT)
						.title("Analyze a local commit")
						.description("Find refactorings introduced by a local repository commit. Supports merge parent selection.")
						.inputSchema(commitInputSchema())
						.outputSchema(outputSchema())
						.annotations(new ToolAnnotations("Analyze commit", true, false, true, false, false))
						.build())
				.callHandler((exchange, request) -> handleAnalyzeCommit(service, request))
				.build();
	}

	static SyncToolSpecification analyzePullRequestTool(RefactoringMinerMcpService service) {
		return SyncToolSpecification.builder()
				.tool(Tool.builder()
						.name(ANALYZE_PULL_REQUEST)
						.title("Analyze a GitHub pull request")
						.description("Find refactorings in a GitHub pull request using RefactoringMiner's GitHub reader.")
						.inputSchema(pullRequestInputSchema())
						.outputSchema(outputSchema())
						.annotations(new ToolAnnotations("Analyze pull request", true, false, true, true, false))
						.build())
				.callHandler((exchange, request) -> handleAnalyzePullRequest(service, request))
				.build();
	}

	static SyncToolSpecification analyzeDirectoriesTool(RefactoringMinerMcpService service) {
		return SyncToolSpecification.builder()
				.tool(Tool.builder()
						.name(ANALYZE_DIRECTORIES)
						.title("Analyze before and after directories")
						.description("Find refactorings between local before/after directories or files.")
						.inputSchema(directoriesInputSchema())
						.outputSchema(outputSchema())
						.annotations(new ToolAnnotations("Analyze directories", true, false, true, false, false))
						.build())
				.callHandler((exchange, request) -> handleAnalyzeDirectories(service, request))
				.build();
	}

	static SyncToolSpecification validateFileContentsTool(RefactoringMinerMcpService service) {
		return SyncToolSpecification.builder()
				.tool(Tool.builder()
						.name(VALIDATE_FILE_CONTENTS)
						.title("Validate intended refactoring against file contents")
						.description("Check a stated refactoring intent against explicit before/after file-content maps.")
						.inputSchema(validateFileContentsInputSchema())
						.outputSchema(validationOutputSchema())
						.annotations(new ToolAnnotations("Validate file contents", true, false, true, false, false))
						.build())
				.callHandler((exchange, request) -> handleValidateFileContents(service, request))
				.build();
	}

	static SyncToolSpecification validateWorktreeTool(RefactoringMinerMcpService service) {
		return SyncToolSpecification.builder()
				.tool(Tool.builder()
						.name(VALIDATE_WORKTREE)
						.title("Validate intended refactoring against worktree changes")
						.description("Check a stated refactoring intent against local worktree changes.")
						.inputSchema(validateWorktreeInputSchema())
						.outputSchema(validationOutputSchema())
						.annotations(new ToolAnnotations("Validate worktree", true, false, true, false, false))
						.build())
				.callHandler((exchange, request) -> handleValidateWorktree(service, request))
				.build();
	}

	static SyncToolSpecification validateCommitTool(RefactoringMinerMcpService service) {
		return SyncToolSpecification.builder()
				.tool(Tool.builder()
						.name(VALIDATE_COMMIT)
						.title("Validate intended refactoring against a commit")
						.description("Check a stated refactoring intent against a local repository commit.")
						.inputSchema(validateCommitInputSchema())
						.outputSchema(validationOutputSchema())
						.annotations(new ToolAnnotations("Validate commit", true, false, true, false, false))
						.build())
				.callHandler((exchange, request) -> handleValidateCommit(service, request))
				.build();
	}

	static SyncToolSpecification validatePullRequestTool(RefactoringMinerMcpService service) {
		return SyncToolSpecification.builder()
				.tool(Tool.builder()
						.name(VALIDATE_PULL_REQUEST)
						.title("Validate intended refactoring against a pull request")
						.description("Check a stated refactoring intent against a GitHub pull request.")
						.inputSchema(validatePullRequestInputSchema())
						.outputSchema(validationOutputSchema())
						.annotations(new ToolAnnotations("Validate pull request", true, false, true, true, false))
						.build())
				.callHandler((exchange, request) -> handleValidatePullRequest(service, request))
				.build();
	}

	static SyncToolSpecification validateDirectoriesTool(RefactoringMinerMcpService service) {
		return SyncToolSpecification.builder()
				.tool(Tool.builder()
						.name(VALIDATE_DIRECTORIES)
						.title("Validate intended refactoring against directories")
						.description("Check a stated refactoring intent against before/after directories.")
						.inputSchema(validateDirectoriesInputSchema())
						.outputSchema(validationOutputSchema())
						.annotations(new ToolAnnotations("Validate directories", true, false, true, false, false))
						.build())
				.callHandler((exchange, request) -> handleValidateDirectories(service, request))
				.build();
	}

	static SyncToolSpecification diffFileContentsTool(RefactoringMinerMcpService service) {
		return SyncToolSpecification.builder()
				.tool(Tool.builder()
						.name(DIFF_FILE_CONTENTS)
						.title("Open AST diff browser for file contents")
						.description("Start a local RefactoringMiner AST diff page for explicit before/after file-content maps. Returns a localhost URL and does not open a browser.")
						.inputSchema(diffFileContentsInputSchema())
						.outputSchema(diffBrowserOutputSchema())
						.annotations(new ToolAnnotations("Open file-content AST diff browser", true, false, true, false,
								false))
						.build())
				.callHandler((exchange, request) -> handleDiffFileContents(service, request))
				.build();
	}

	static SyncToolSpecification diffWorktreeTool(RefactoringMinerMcpService service) {
		return SyncToolSpecification.builder()
				.tool(Tool.builder()
						.name(DIFF_WORKTREE)
						.title("Open AST diff browser for worktree changes")
						.description("Start a local RefactoringMiner AST diff page for modified, added, deleted, and optional untracked files. Returns a localhost URL and does not open a browser.")
						.inputSchema(diffWorktreeInputSchema())
						.outputSchema(diffBrowserOutputSchema())
						.annotations(new ToolAnnotations("Open worktree AST diff browser", true, false, true, false,
								false))
						.build())
				.callHandler((exchange, request) -> handleDiffWorktree(service, request))
				.build();
	}

	static SyncToolSpecification diffCommitTool(RefactoringMinerMcpService service) {
		return SyncToolSpecification.builder()
				.tool(Tool.builder()
						.name(DIFF_COMMIT)
						.title("Open AST diff browser for a local commit")
						.description("Start a local RefactoringMiner AST diff page for a repository commit. Supports merge parent selection and returns a localhost URL.")
						.inputSchema(diffCommitInputSchema())
						.outputSchema(diffBrowserOutputSchema())
						.annotations(new ToolAnnotations("Open commit AST diff browser", true, false, true, false,
								false))
						.build())
				.callHandler((exchange, request) -> handleDiffCommit(service, request))
				.build();
	}

	static SyncToolSpecification diffPullRequestTool(RefactoringMinerMcpService service) {
		return SyncToolSpecification.builder()
				.tool(Tool.builder()
						.name(DIFF_PULL_REQUEST)
						.title("Open AST diff browser for a GitHub pull request")
						.description("Start a local RefactoringMiner AST diff page for a GitHub pull request. Returns a localhost URL and does not open a browser.")
						.inputSchema(diffPullRequestInputSchema())
						.outputSchema(diffBrowserOutputSchema())
						.annotations(new ToolAnnotations("Open pull request AST diff browser", true, false, true, true,
								false))
						.build())
				.callHandler((exchange, request) -> handleDiffPullRequest(service, request))
				.build();
	}

	private static CallToolResult handleAnalyzeFileContents(RefactoringMinerMcpService service, CallToolRequest request) {
		McpAnalysisResult result = analyze(service, request.arguments());
		return toCallToolResult(result);
	}

	static McpAnalysisResult analyze(RefactoringMinerMcpService service, Map<String, Object> arguments) {
		if (arguments == null) {
			return McpAnalysisResult.error("Tool arguments are required.", List.of("arguments=null"));
		}
		try {
			Map<String, String> beforeFiles = stringMap(arguments.get("beforeFiles"), "beforeFiles");
			Map<String, String> afterFiles = stringMap(arguments.get("afterFiles"), "afterFiles");
			int maxRefactorings = integerValue(arguments.get("maxRefactorings"), DEFAULT_MAX_REFACTORINGS,
					"maxRefactorings");
			int maxFiles = integerValue(arguments.get("maxFiles"), DEFAULT_MAX_FILES, "maxFiles");
			int maxBytesPerFile = integerValue(arguments.get("maxBytesPerFile"), DEFAULT_MAX_BYTES_PER_FILE,
					"maxBytesPerFile");
			return service.analyzeFileContents(beforeFiles, afterFiles, maxRefactorings, maxFiles,
					maxBytesPerFile);
		} catch (IllegalArgumentException e) {
			return McpAnalysisResult.error(e.getMessage(), List.of("Invalid tool arguments."));
		}
	}

	private static CallToolResult handleAnalyzeWorktree(RefactoringMinerMcpService service, CallToolRequest request) {
		McpAnalysisResult result = analyzeWorktree(service, request.arguments());
		return toCallToolResult(result);
	}

	static McpAnalysisResult analyzeWorktree(RefactoringMinerMcpService service, Map<String, Object> arguments) {
		if (arguments == null) {
			return McpAnalysisResult.error("Tool arguments are required.", List.of("arguments=null"));
		}
		try {
			Path repositoryPath = repositoryPath(arguments.get("repositoryPath"));
			String baseRef = optionalStringValue(arguments.get("baseRef"), "HEAD");
			boolean includeUntracked = booleanValue(arguments.get("includeUntracked"), false);
			int maxFiles = integerValue(arguments.get("maxFiles"), DEFAULT_MAX_FILES, "maxFiles");
			int maxBytesPerFile = integerValue(arguments.get("maxBytesPerFile"), DEFAULT_MAX_BYTES_PER_FILE,
					"maxBytesPerFile");
			int maxRefactorings = integerValue(arguments.get("maxRefactorings"), DEFAULT_MAX_REFACTORINGS,
					"maxRefactorings");
			return service.analyzeWorktree(repositoryPath, baseRef, includeUntracked, maxFiles,
					maxBytesPerFile, maxRefactorings);
		} catch (IllegalArgumentException e) {
			return McpAnalysisResult.error(e.getMessage(), List.of("Invalid tool arguments."));
		}
	}

	private static CallToolResult handleAnalyzeCommit(RefactoringMinerMcpService service, CallToolRequest request) {
		McpAnalysisResult result = analyzeCommit(service, request.arguments());
		return toCallToolResult(result);
	}

	static McpAnalysisResult analyzeCommit(RefactoringMinerMcpService service, Map<String, Object> arguments) {
		if (arguments == null) {
			return McpAnalysisResult.error("Tool arguments are required.", List.of("arguments=null"));
		}
		try {
			Path repositoryPath = repositoryPath(arguments.get("repositoryPath"));
			String commitId = stringValue(arguments.get("commitId"), "commitId");
			Integer parentIndex = optionalIntegerValue(arguments.get("parentIndex"), "parentIndex");
			int maxRefactorings = integerValue(arguments.get("maxRefactorings"), DEFAULT_MAX_REFACTORINGS,
					"maxRefactorings");
			return service.analyzeCommit(repositoryPath, commitId, parentIndex, maxRefactorings);
		} catch (IllegalArgumentException e) {
			return McpAnalysisResult.error(e.getMessage(), List.of("Invalid tool arguments."));
		}
	}

	private static CallToolResult handleAnalyzePullRequest(RefactoringMinerMcpService service, CallToolRequest request) {
		McpAnalysisResult result = analyzePullRequest(service, request.arguments());
		return toCallToolResult(result);
	}

	static McpAnalysisResult analyzePullRequest(RefactoringMinerMcpService service, Map<String, Object> arguments) {
		if (arguments == null) {
			return McpAnalysisResult.error("Tool arguments are required.", List.of("arguments=null"));
		}
		try {
			String cloneUrl = stringValue(arguments.get("cloneUrl"), "cloneUrl");
			int pullRequestId = integerValue(arguments.get("pullRequestId"), 0, "pullRequestId");
			int timeoutSeconds = integerValue(arguments.get("timeoutSeconds"), DEFAULT_TIMEOUT_SECONDS, "timeoutSeconds");
			int maxRefactorings = integerValue(arguments.get("maxRefactorings"), DEFAULT_MAX_REFACTORINGS,
					"maxRefactorings");
			return service.analyzePullRequest(cloneUrl, pullRequestId, timeoutSeconds, maxRefactorings);
		} catch (IllegalArgumentException e) {
			return McpAnalysisResult.error(e.getMessage(), List.of("Invalid tool arguments."));
		}
	}

	private static CallToolResult handleAnalyzeDirectories(RefactoringMinerMcpService service, CallToolRequest request) {
		McpAnalysisResult result = analyzeDirectories(service, request.arguments());
		return toCallToolResult(result);
	}

	static McpAnalysisResult analyzeDirectories(RefactoringMinerMcpService service, Map<String, Object> arguments) {
		if (arguments == null) {
			return McpAnalysisResult.error("Tool arguments are required.", List.of("arguments=null"));
		}
		try {
			String beforePath = stringValue(arguments.get("beforePath"), "beforePath");
			String afterPath = stringValue(arguments.get("afterPath"), "afterPath");
			int maxRefactorings = integerValue(arguments.get("maxRefactorings"), DEFAULT_MAX_REFACTORINGS,
					"maxRefactorings");
			return service.analyzeDirectories(Path.of(beforePath), Path.of(afterPath), maxRefactorings);
		} catch (IllegalArgumentException e) {
			return McpAnalysisResult.error(e.getMessage(), List.of("Invalid tool arguments."));
		}
	}

	private static CallToolResult handleValidateFileContents(RefactoringMinerMcpService service,
			CallToolRequest request) {
		McpValidationResult result = validateFileContents(service, request.arguments());
		return toCallToolResult(result);
	}

	static McpValidationResult validateFileContents(RefactoringMinerMcpService service, Map<String, Object> arguments) {
		if (arguments == null) {
			return McpValidationResult.error("Tool arguments are required.", null, List.of("arguments=null"));
		}
		try {
			Map<String, String> beforeFiles = stringMap(arguments.get("beforeFiles"), "beforeFiles");
			Map<String, String> afterFiles = stringMap(arguments.get("afterFiles"), "afterFiles");
			McpRefactoringIntent intent = intentValue(arguments.get("intent"));
			int maxCandidates = integerValue(arguments.get("maxCandidates"), DEFAULT_MAX_CANDIDATES, "maxCandidates");
			int maxFiles = integerValue(arguments.get("maxFiles"), DEFAULT_MAX_FILES, "maxFiles");
			int maxBytesPerFile = integerValue(arguments.get("maxBytesPerFile"), DEFAULT_MAX_BYTES_PER_FILE,
					"maxBytesPerFile");
			return service.validateFileContents(beforeFiles, afterFiles, intent, maxCandidates, maxFiles,
					maxBytesPerFile);
		} catch (IllegalArgumentException e) {
			return McpValidationResult.error(e.getMessage(), null, List.of("Invalid tool arguments."));
		}
	}

	private static CallToolResult handleValidateWorktree(RefactoringMinerMcpService service, CallToolRequest request) {
		McpValidationResult result = validateWorktree(service, request.arguments());
		return toCallToolResult(result);
	}

	static McpValidationResult validateWorktree(RefactoringMinerMcpService service, Map<String, Object> arguments) {
		if (arguments == null) {
			return McpValidationResult.error("Tool arguments are required.", null, List.of("arguments=null"));
		}
		try {
			Path repositoryPath = repositoryPath(arguments.get("repositoryPath"));
			String baseRef = optionalStringValue(arguments.get("baseRef"), "HEAD");
			boolean includeUntracked = booleanValue(arguments.get("includeUntracked"), false);
			int maxFiles = integerValue(arguments.get("maxFiles"), DEFAULT_MAX_FILES, "maxFiles");
			int maxBytesPerFile = integerValue(arguments.get("maxBytesPerFile"), DEFAULT_MAX_BYTES_PER_FILE,
					"maxBytesPerFile");
			McpRefactoringIntent intent = intentValue(arguments.get("intent"));
			int maxCandidates = integerValue(arguments.get("maxCandidates"), DEFAULT_MAX_CANDIDATES, "maxCandidates");
			return service.validateWorktree(repositoryPath, baseRef, includeUntracked, maxFiles,
					maxBytesPerFile, intent, maxCandidates);
		} catch (IllegalArgumentException e) {
			return McpValidationResult.error(e.getMessage(), null, List.of("Invalid tool arguments."));
		}
	}

	private static CallToolResult handleValidateCommit(RefactoringMinerMcpService service, CallToolRequest request) {
		McpValidationResult result = validateCommit(service, request.arguments());
		return toCallToolResult(result);
	}

	static McpValidationResult validateCommit(RefactoringMinerMcpService service, Map<String, Object> arguments) {
		if (arguments == null) {
			return McpValidationResult.error("Tool arguments are required.", null, List.of("arguments=null"));
		}
		try {
			Path repositoryPath = repositoryPath(arguments.get("repositoryPath"));
			String commitId = stringValue(arguments.get("commitId"), "commitId");
			Integer parentIndex = optionalIntegerValue(arguments.get("parentIndex"), "parentIndex");
			McpRefactoringIntent intent = intentValue(arguments.get("intent"));
			int maxCandidates = integerValue(arguments.get("maxCandidates"), DEFAULT_MAX_CANDIDATES, "maxCandidates");
			return service.validateCommit(repositoryPath, commitId, parentIndex, intent, maxCandidates);
		} catch (IllegalArgumentException e) {
			return McpValidationResult.error(e.getMessage(), null, List.of("Invalid tool arguments."));
		}
	}

	private static CallToolResult handleValidatePullRequest(RefactoringMinerMcpService service,
			CallToolRequest request) {
		McpValidationResult result = validatePullRequest(service, request.arguments());
		return toCallToolResult(result);
	}

	static McpValidationResult validatePullRequest(RefactoringMinerMcpService service, Map<String, Object> arguments) {
		if (arguments == null) {
			return McpValidationResult.error("Tool arguments are required.", null, List.of("arguments=null"));
		}
		try {
			String cloneUrl = stringValue(arguments.get("cloneUrl"), "cloneUrl");
			int pullRequestId = integerValue(arguments.get("pullRequestId"), 0, "pullRequestId");
			int timeoutSeconds = integerValue(arguments.get("timeoutSeconds"), DEFAULT_TIMEOUT_SECONDS, "timeoutSeconds");
			McpRefactoringIntent intent = intentValue(arguments.get("intent"));
			int maxCandidates = integerValue(arguments.get("maxCandidates"), DEFAULT_MAX_CANDIDATES, "maxCandidates");
			return service.validatePullRequest(cloneUrl, pullRequestId, timeoutSeconds, intent, maxCandidates);
		} catch (IllegalArgumentException e) {
			return McpValidationResult.error(e.getMessage(), null, List.of("Invalid tool arguments."));
		}
	}

	private static CallToolResult handleValidateDirectories(RefactoringMinerMcpService service,
			CallToolRequest request) {
		McpValidationResult result = validateDirectories(service, request.arguments());
		return toCallToolResult(result);
	}

	static McpValidationResult validateDirectories(RefactoringMinerMcpService service, Map<String, Object> arguments) {
		if (arguments == null) {
			return McpValidationResult.error("Tool arguments are required.", null, List.of("arguments=null"));
		}
		try {
			String beforePath = stringValue(arguments.get("beforePath"), "beforePath");
			String afterPath = stringValue(arguments.get("afterPath"), "afterPath");
			McpRefactoringIntent intent = intentValue(arguments.get("intent"));
			int maxCandidates = integerValue(arguments.get("maxCandidates"), DEFAULT_MAX_CANDIDATES, "maxCandidates");
			return service.validateDirectories(Path.of(beforePath), Path.of(afterPath), intent, maxCandidates);
		} catch (IllegalArgumentException e) {
			return McpValidationResult.error(e.getMessage(), null, List.of("Invalid tool arguments."));
		}
	}

	private static CallToolResult handleDiffFileContents(RefactoringMinerMcpService service, CallToolRequest request) {
		McpDiffBrowserResult result = diffFileContents(service, request.arguments());
		return toCallToolResult(result);
	}

	static McpDiffBrowserResult diffFileContents(RefactoringMinerMcpService service, Map<String, Object> arguments) {
		if (arguments == null) {
			return McpDiffBrowserResult.error("Tool arguments are required.", null, "Explicit file contents",
					List.of("arguments=null"));
		}
		try {
			Map<String, String> beforeFiles = stringMap(arguments.get("beforeFiles"), "beforeFiles");
			Map<String, String> afterFiles = stringMap(arguments.get("afterFiles"), "afterFiles");
			int port = integerValue(arguments.get("port"), DEFAULT_WEB_DIFF_PORT, "port");
			int maxFiles = integerValue(arguments.get("maxFiles"), DEFAULT_MAX_FILES, "maxFiles");
			int maxBytesPerFile = integerValue(arguments.get("maxBytesPerFile"), DEFAULT_MAX_BYTES_PER_FILE,
					"maxBytesPerFile");
			return service.diffFileContents(beforeFiles, afterFiles, port, maxFiles, maxBytesPerFile);
		} catch (IllegalArgumentException e) {
			return McpDiffBrowserResult.error(e.getMessage(), null, "Explicit file contents",
					List.of("Invalid tool arguments."));
		}
	}

	private static CallToolResult handleDiffWorktree(RefactoringMinerMcpService service, CallToolRequest request) {
		McpDiffBrowserResult result = diffWorktree(service, request.arguments());
		return toCallToolResult(result);
	}

	static McpDiffBrowserResult diffWorktree(RefactoringMinerMcpService service, Map<String, Object> arguments) {
		if (arguments == null) {
			return McpDiffBrowserResult.error("Tool arguments are required.", null, "Worktree changes",
					List.of("arguments=null"));
		}
		try {
			Path repositoryPath = repositoryPath(arguments.get("repositoryPath"));
			String baseRef = optionalStringValue(arguments.get("baseRef"), "HEAD");
			boolean includeUntracked = booleanValue(arguments.get("includeUntracked"), false);
			int maxFiles = integerValue(arguments.get("maxFiles"), DEFAULT_MAX_FILES, "maxFiles");
			int maxBytesPerFile = integerValue(arguments.get("maxBytesPerFile"), DEFAULT_MAX_BYTES_PER_FILE,
					"maxBytesPerFile");
			int port = integerValue(arguments.get("port"), DEFAULT_WEB_DIFF_PORT, "port");
			return service.diffWorktree(repositoryPath, baseRef, includeUntracked, maxFiles, maxBytesPerFile,
					port);
		} catch (IllegalArgumentException e) {
			return McpDiffBrowserResult.error(e.getMessage(), null, "Worktree changes",
					List.of("Invalid tool arguments."));
		}
	}

	private static CallToolResult handleDiffCommit(RefactoringMinerMcpService service, CallToolRequest request) {
		McpDiffBrowserResult result = diffCommit(service, request.arguments());
		return toCallToolResult(result);
	}

	static McpDiffBrowserResult diffCommit(RefactoringMinerMcpService service, Map<String, Object> arguments) {
		if (arguments == null) {
			return McpDiffBrowserResult.error("Tool arguments are required.", null, "Commit diff",
					List.of("arguments=null"));
		}
		try {
			Path repositoryPath = repositoryPath(arguments.get("repositoryPath"));
			String commitId = stringValue(arguments.get("commitId"), "commitId");
			Integer parentIndex = optionalIntegerValue(arguments.get("parentIndex"), "parentIndex");
			int port = integerValue(arguments.get("port"), DEFAULT_WEB_DIFF_PORT, "port");
			return service.diffCommit(repositoryPath, commitId, parentIndex, port);
		} catch (IllegalArgumentException e) {
			return McpDiffBrowserResult.error(e.getMessage(), null, "Commit diff", List.of("Invalid tool arguments."));
		}
	}

	private static CallToolResult handleDiffPullRequest(RefactoringMinerMcpService service, CallToolRequest request) {
		McpDiffBrowserResult result = diffPullRequest(service, request.arguments());
		return toCallToolResult(result);
	}

	static McpDiffBrowserResult diffPullRequest(RefactoringMinerMcpService service, Map<String, Object> arguments) {
		if (arguments == null) {
			return McpDiffBrowserResult.error("Tool arguments are required.", null, "Pull-request diff",
					List.of("arguments=null"));
		}
		try {
			String cloneUrl = stringValue(arguments.get("cloneUrl"), "cloneUrl");
			int pullRequestId = integerValue(arguments.get("pullRequestId"), 0, "pullRequestId");
			int timeoutSeconds = integerValue(arguments.get("timeoutSeconds"), DEFAULT_TIMEOUT_SECONDS, "timeoutSeconds");
			int port = integerValue(arguments.get("port"), DEFAULT_WEB_DIFF_PORT, "port");
			return service.diffPullRequest(cloneUrl, pullRequestId, timeoutSeconds, port);
		} catch (IllegalArgumentException e) {
			return McpDiffBrowserResult.error(e.getMessage(), null, "Pull-request diff",
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

	private static JsonSchema inputSchema() {
		Map<String, Object> fileMapSchema = Map.of(
				"type", "object",
				"additionalProperties", Map.of("type", "string"));
		Map<String, Object> maxRefactoringsSchema = Map.of(
				"type", "integer",
				"minimum", 0,
				"default", DEFAULT_MAX_REFACTORINGS);
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("beforeFiles", fileMapSchema);
		properties.put("afterFiles", fileMapSchema);
		properties.put("maxRefactorings", maxRefactoringsSchema);
		putFileContentBoundsProperties(properties);
		return new JsonSchema("object", properties, List.of("beforeFiles", "afterFiles"), false, null, null);
	}

	private static JsonSchema worktreeInputSchema() {
		Map<String, Object> properties = new LinkedHashMap<>();
		putRepositoryPathProperty(properties);
		properties.put("baseRef", Map.of("type", "string", "default", "HEAD"));
		properties.put("includeUntracked", Map.of("type", "boolean", "default", false));
		properties.put("maxFiles", Map.of("type", "integer", "minimum", 1, "default", DEFAULT_MAX_FILES));
		properties.put("maxBytesPerFile", Map.of("type", "integer", "minimum", 1, "default", DEFAULT_MAX_BYTES_PER_FILE));
		properties.put("maxRefactorings", Map.of("type", "integer", "minimum", 0, "default", DEFAULT_MAX_REFACTORINGS));
		return new JsonSchema("object", properties, List.of(), false, null, null);
	}

	private static JsonSchema commitInputSchema() {
		Map<String, Object> properties = new LinkedHashMap<>();
		putRepositoryPathProperty(properties);
		properties.put("commitId", Map.of("type", "string"));
		properties.put("parentIndex", Map.of("type", "integer", "minimum", 0));
		properties.put("maxRefactorings", Map.of("type", "integer", "minimum", 0, "default", DEFAULT_MAX_REFACTORINGS));
		return new JsonSchema("object", properties, List.of("commitId"), false, null, null);
	}

	private static JsonSchema pullRequestInputSchema() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("cloneUrl", Map.of("type", "string"));
		properties.put("pullRequestId", Map.of("type", "integer", "minimum", 1));
		properties.put("timeoutSeconds", Map.of("type", "integer", "minimum", 1, "default", DEFAULT_TIMEOUT_SECONDS));
		properties.put("maxRefactorings", Map.of("type", "integer", "minimum", 0, "default", DEFAULT_MAX_REFACTORINGS));
		return new JsonSchema("object", properties, List.of("cloneUrl", "pullRequestId"), false, null, null);
	}

	private static JsonSchema directoriesInputSchema() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("beforePath", Map.of("type", "string"));
		properties.put("afterPath", Map.of("type", "string"));
		properties.put("maxRefactorings", Map.of("type", "integer", "minimum", 0, "default", DEFAULT_MAX_REFACTORINGS));
		return new JsonSchema("object", properties, List.of("beforePath", "afterPath"), false, null, null);
	}

	private static JsonSchema validateFileContentsInputSchema() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("beforeFiles", fileMapSchema());
		properties.put("afterFiles", fileMapSchema());
		putFileContentBoundsProperties(properties);
		putValidationProperties(properties);
		return new JsonSchema("object", properties, List.of("beforeFiles", "afterFiles", "intent"), false, null, null);
	}

	private static JsonSchema validateWorktreeInputSchema() {
		Map<String, Object> properties = new LinkedHashMap<>();
		putRepositoryPathProperty(properties);
		properties.put("baseRef", Map.of("type", "string", "default", "HEAD"));
		properties.put("includeUntracked", Map.of("type", "boolean", "default", false));
		properties.put("maxFiles", Map.of("type", "integer", "minimum", 1, "default", DEFAULT_MAX_FILES));
		properties.put("maxBytesPerFile", Map.of("type", "integer", "minimum", 1, "default", DEFAULT_MAX_BYTES_PER_FILE));
		putValidationProperties(properties);
		return new JsonSchema("object", properties, List.of("intent"), false, null, null);
	}

	private static JsonSchema validateCommitInputSchema() {
		Map<String, Object> properties = new LinkedHashMap<>();
		putRepositoryPathProperty(properties);
		properties.put("commitId", Map.of("type", "string"));
		properties.put("parentIndex", Map.of("type", "integer", "minimum", 0));
		putValidationProperties(properties);
		return new JsonSchema("object", properties, List.of("commitId", "intent"), false, null, null);
	}

	private static JsonSchema validatePullRequestInputSchema() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("cloneUrl", Map.of("type", "string"));
		properties.put("pullRequestId", Map.of("type", "integer", "minimum", 1));
		properties.put("timeoutSeconds", Map.of("type", "integer", "minimum", 1, "default", DEFAULT_TIMEOUT_SECONDS));
		putValidationProperties(properties);
		return new JsonSchema("object", properties, List.of("cloneUrl", "pullRequestId", "intent"), false, null, null);
	}

	private static JsonSchema validateDirectoriesInputSchema() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("beforePath", Map.of("type", "string"));
		properties.put("afterPath", Map.of("type", "string"));
		putValidationProperties(properties);
		return new JsonSchema("object", properties, List.of("beforePath", "afterPath", "intent"), false, null, null);
	}

	private static JsonSchema diffFileContentsInputSchema() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("beforeFiles", fileMapSchema());
		properties.put("afterFiles", fileMapSchema());
		putFileContentBoundsProperties(properties);
		putDiffBrowserProperties(properties);
		return new JsonSchema("object", properties, List.of("beforeFiles", "afterFiles"), false, null, null);
	}

	private static JsonSchema diffWorktreeInputSchema() {
		Map<String, Object> properties = new LinkedHashMap<>();
		putRepositoryPathProperty(properties);
		properties.put("baseRef", Map.of("type", "string", "default", "HEAD"));
		properties.put("includeUntracked", Map.of("type", "boolean", "default", false));
		properties.put("maxFiles", Map.of("type", "integer", "minimum", 1, "default", DEFAULT_MAX_FILES));
		properties.put("maxBytesPerFile", Map.of("type", "integer", "minimum", 1, "default", DEFAULT_MAX_BYTES_PER_FILE));
		putDiffBrowserProperties(properties);
		return new JsonSchema("object", properties, List.of(), false, null, null);
	}

	private static JsonSchema diffCommitInputSchema() {
		Map<String, Object> properties = new LinkedHashMap<>();
		putRepositoryPathProperty(properties);
		properties.put("commitId", Map.of("type", "string"));
		properties.put("parentIndex", Map.of("type", "integer", "minimum", 0));
		putDiffBrowserProperties(properties);
		return new JsonSchema("object", properties, List.of("commitId"), false, null, null);
	}

	private static JsonSchema diffPullRequestInputSchema() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("cloneUrl", Map.of("type", "string"));
		properties.put("pullRequestId", Map.of("type", "integer", "minimum", 1));
		properties.put("timeoutSeconds", Map.of("type", "integer", "minimum", 1, "default", DEFAULT_TIMEOUT_SECONDS));
		putDiffBrowserProperties(properties);
		return new JsonSchema("object", properties, List.of("cloneUrl", "pullRequestId"), false, null, null);
	}

	private static void putDiffBrowserProperties(Map<String, Object> properties) {
		properties.put("port", Map.of("type", "integer", "minimum", 1, "maximum", 65535,
				"default", DEFAULT_WEB_DIFF_PORT,
				"description", "Local WebDiff port. Defaults to 6789."));
	}

	private static void putRepositoryPathProperty(Map<String, Object> properties) {
		properties.put("repositoryPath", Map.of(
				"type", "string",
				"description", "Absolute repository path. Defaults to the MCP server working directory."));
	}

	private static void putFileContentBoundsProperties(Map<String, Object> properties) {
		properties.put("maxFiles", Map.of("type", "integer", "minimum", 1, "default", DEFAULT_MAX_FILES));
		properties.put("maxBytesPerFile", Map.of("type", "integer", "minimum", 1,
				"default", DEFAULT_MAX_BYTES_PER_FILE));
	}

	private static void putValidationProperties(Map<String, Object> properties) {
		properties.put("intent", intentSchema());
		properties.put("maxCandidates", Map.of("type", "integer", "minimum", 0, "default", DEFAULT_MAX_CANDIDATES));
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
