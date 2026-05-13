package org.refactoringminer.mcp;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.astDiff.models.ProjectASTDiff;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

public class RefactoringMinerMcpService {
	private final FileContentDiffer differ;
	private final CommitDiffer commitDiffer;
	private final PullRequestDiffer pullRequestDiffer;
	private final DirectoryDiffer directoryDiffer;

	public RefactoringMinerMcpService() {
		this(new GitHistoryRefactoringMinerImpl());
	}

	RefactoringMinerMcpService(GitHistoryRefactoringMiner miner) {
		this(miner::diffAtFileContents, miner::diffAtMergeCommit, miner::diffAtPullRequest, miner::diffAtDirectories);
	}

	RefactoringMinerMcpService(FileContentDiffer differ) {
		this.differ = differ;
		GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
		this.commitDiffer = miner::diffAtMergeCommit;
		this.pullRequestDiffer = miner::diffAtPullRequest;
		this.directoryDiffer = miner::diffAtDirectories;
	}

	RefactoringMinerMcpService(FileContentDiffer differ, CommitDiffer commitDiffer, PullRequestDiffer pullRequestDiffer,
			DirectoryDiffer directoryDiffer) {
		this.differ = differ;
		this.commitDiffer = commitDiffer;
		this.pullRequestDiffer = pullRequestDiffer;
		this.directoryDiffer = directoryDiffer;
	}

	public McpAnalysisResult analyzeFileContents(Map<String, String> beforeFiles, Map<String, String> afterFiles,
			int maxRefactorings) {
		if (maxRefactorings < 0) {
			return McpAnalysisResult.error("maxRefactorings must be greater than or equal to 0.",
					List.of("maxRefactorings=" + maxRefactorings));
		}
		if (beforeFiles == null || afterFiles == null) {
			return McpAnalysisResult.error("beforeFiles and afterFiles are required.",
					List.of("beforeFiles and afterFiles must be JSON objects mapping paths to file contents."));
		}
		if (beforeFiles.isEmpty() && afterFiles.isEmpty()) {
			return McpAnalysisResult.error("At least one before or after file is required.",
					List.of("beforeFiles and afterFiles cannot both be empty."));
		}

		try {
			ProjectASTDiff diff = differ.diffAtFileContents(beforeFiles, afterFiles);
			return toResult(diff, maxRefactorings, "File-content");
		} catch (Exception e) {
			return McpAnalysisResult.error("File-content analysis failed: " + e.getMessage(),
					List.of(e.getClass().getName()));
		}
	}

	public McpValidationResult validateFileContents(Map<String, String> beforeFiles, Map<String, String> afterFiles,
			McpRefactoringIntent intent, int maxCandidates) {
		if (maxCandidates < 0) {
			return McpValidationResult.error("maxCandidates must be greater than or equal to 0.", intent,
					List.of("maxCandidates=" + maxCandidates));
		}
		if (intent == null) {
			return McpValidationResult.error("intent is required.", null, List.of("intent=null"));
		}
		if (beforeFiles == null || afterFiles == null) {
			return McpValidationResult.error("beforeFiles and afterFiles are required.", intent,
					List.of("beforeFiles and afterFiles must be JSON objects mapping paths to file contents."));
		}
		if (beforeFiles.isEmpty() && afterFiles.isEmpty()) {
			return McpValidationResult.error("At least one before or after file is required.", intent,
					List.of("beforeFiles and afterFiles cannot both be empty."));
		}

		try {
			ProjectASTDiff diff = differ.diffAtFileContents(beforeFiles, afterFiles);
			return new McpIntentValidator().validate(diff, intent, maxCandidates);
		} catch (Exception e) {
			return McpValidationResult.error("File-content validation failed: " + e.getMessage(), intent,
					List.of(e.getClass().getName()));
		}
	}

	public McpValidationResult validateWorktree(Path repositoryPath, String baseRef, boolean includeUntracked,
			int maxFiles, int maxBytesPerFile, McpRefactoringIntent intent, int maxCandidates) {
		if (intent == null) {
			return McpValidationResult.error("intent is required.", null, List.of("intent=null"));
		}
		if (maxCandidates < 0) {
			return McpValidationResult.error("maxCandidates must be greater than or equal to 0.", intent,
					List.of("maxCandidates=" + maxCandidates));
		}
		try {
			WorktreeChangeCollector.WorktreeChanges changes = new WorktreeChangeCollector()
					.collect(repositoryPath, baseRef, includeUntracked, maxFiles, maxBytesPerFile);
			ProjectASTDiff diff = differ.diffAtFileContents(changes.beforeFiles(), changes.afterFiles());
			return new McpIntentValidator().validate(diff, intent, maxCandidates, changes.warnings());
		} catch (Exception e) {
			return McpValidationResult.error("Worktree validation failed: " + e.getMessage(), intent,
					List.of(e.getClass().getName()));
		}
	}

	public McpValidationResult validateCommit(Path repositoryPath, String commitId, Integer parentIndex,
			McpRefactoringIntent intent, int maxCandidates) {
		if (intent == null) {
			return McpValidationResult.error("intent is required.", null, List.of("intent=null"));
		}
		if (maxCandidates < 0) {
			return McpValidationResult.error("maxCandidates must be greater than or equal to 0.", intent,
					List.of("maxCandidates=" + maxCandidates));
		}
		if (commitId == null || commitId.isBlank()) {
			return McpValidationResult.error("commitId must be a non-empty string.", intent,
					List.of("commitId is required."));
		}
		int resolvedParentIndex = parentIndex == null ? 0 : parentIndex;
		if (resolvedParentIndex < 0) {
			return McpValidationResult.error("parentIndex must be greater than or equal to 0.", intent,
					List.of("parentIndex=" + resolvedParentIndex));
		}

		try {
			requireExistingAbsolutePath(repositoryPath, "repositoryPath");
			Path analysisRepositoryPath = resolveCommitRepositoryPath(repositoryPath);
			ProjectASTDiff diff = commitDiffer.diffAtMergeCommit(analysisRepositoryPath, commitId, resolvedParentIndex);
			return new McpIntentValidator().validate(diff, intent, maxCandidates);
		} catch (Exception e) {
			return McpValidationResult.error("Commit validation failed: " + e.getMessage(), intent,
					List.of(e.getClass().getName()));
		}
	}

	public McpValidationResult validatePullRequest(String cloneUrl, int pullRequestId, int timeoutSeconds,
			McpRefactoringIntent intent, int maxCandidates) {
		if (intent == null) {
			return McpValidationResult.error("intent is required.", null, List.of("intent=null"));
		}
		if (maxCandidates < 0) {
			return McpValidationResult.error("maxCandidates must be greater than or equal to 0.", intent,
					List.of("maxCandidates=" + maxCandidates));
		}
		if (cloneUrl == null || cloneUrl.isBlank()) {
			return McpValidationResult.error("cloneUrl must be a non-empty string.", intent,
					List.of("cloneUrl is required."));
		}
		if (pullRequestId <= 0) {
			return McpValidationResult.error("pullRequestId must be greater than 0.", intent,
					List.of("pullRequestId=" + pullRequestId));
		}
		if (timeoutSeconds <= 0) {
			return McpValidationResult.error("timeoutSeconds must be greater than 0.", intent,
					List.of("timeoutSeconds=" + timeoutSeconds));
		}

		try {
			ProjectASTDiff diff = pullRequestDiffer.diffAtPullRequest(cloneUrl, pullRequestId, timeoutSeconds);
			return new McpIntentValidator().validate(diff, intent, maxCandidates);
		} catch (Exception e) {
			return McpValidationResult.error("Pull-request validation failed: " + e.getMessage(), intent,
					List.of(e.getClass().getName()));
		}
	}

	public McpValidationResult validateDirectories(Path beforePath, Path afterPath, McpRefactoringIntent intent,
			int maxCandidates) {
		if (intent == null) {
			return McpValidationResult.error("intent is required.", null, List.of("intent=null"));
		}
		if (maxCandidates < 0) {
			return McpValidationResult.error("maxCandidates must be greater than or equal to 0.", intent,
					List.of("maxCandidates=" + maxCandidates));
		}

		try {
			requireExistingAbsolutePath(beforePath, "beforePath");
			requireExistingAbsolutePath(afterPath, "afterPath");
			ProjectASTDiff diff = directoryDiffer.diffAtDirectories(beforePath, afterPath);
			return new McpIntentValidator().validate(diff, intent, maxCandidates);
		} catch (Exception e) {
			return McpValidationResult.error("Directory validation failed: " + e.getMessage(), intent,
					List.of(e.getClass().getName()));
		}
	}

	public McpAnalysisResult analyzeWorktree(Path repositoryPath, String baseRef, boolean includeUntracked, int maxFiles,
			int maxBytesPerFile, int maxRefactorings) {
		try {
			WorktreeChangeCollector.WorktreeChanges changes = new WorktreeChangeCollector()
					.collect(repositoryPath, baseRef, includeUntracked, maxFiles, maxBytesPerFile);
			ProjectASTDiff diff = differ.diffAtFileContents(changes.beforeFiles(), changes.afterFiles());
			if (diff == null) {
				return McpAnalysisResult.error("Worktree analysis did not produce a diff.",
						List.of("RefactoringMiner returned null."));
			}
			return McpAnalysisResult.ok(diff, maxRefactorings, changes.warnings());
		} catch (Exception e) {
			return McpAnalysisResult.error("Worktree analysis failed: " + e.getMessage(), List.of(e.getClass().getName()));
		}
	}

	public McpAnalysisResult analyzeCommit(Path repositoryPath, String commitId, Integer parentIndex,
			int maxRefactorings) {
		if (maxRefactorings < 0) {
			return McpAnalysisResult.error("maxRefactorings must be greater than or equal to 0.",
					List.of("maxRefactorings=" + maxRefactorings));
		}
		if (commitId == null || commitId.isBlank()) {
			return McpAnalysisResult.error("commitId must be a non-empty string.", List.of("commitId is required."));
		}
		int resolvedParentIndex = parentIndex == null ? 0 : parentIndex;
		if (resolvedParentIndex < 0) {
			return McpAnalysisResult.error("parentIndex must be greater than or equal to 0.",
					List.of("parentIndex=" + resolvedParentIndex));
		}

		try {
			requireExistingAbsolutePath(repositoryPath, "repositoryPath");
			Path analysisRepositoryPath = resolveCommitRepositoryPath(repositoryPath);
			ProjectASTDiff diff = commitDiffer.diffAtMergeCommit(analysisRepositoryPath, commitId, resolvedParentIndex);
			return toResult(diff, maxRefactorings, "Commit");
		} catch (Exception e) {
			return McpAnalysisResult.error("Commit analysis failed: " + e.getMessage(), List.of(e.getClass().getName()));
		}
	}

	public McpAnalysisResult analyzePullRequest(String cloneUrl, int pullRequestId, int timeoutSeconds,
			int maxRefactorings) {
		if (maxRefactorings < 0) {
			return McpAnalysisResult.error("maxRefactorings must be greater than or equal to 0.",
					List.of("maxRefactorings=" + maxRefactorings));
		}
		if (cloneUrl == null || cloneUrl.isBlank()) {
			return McpAnalysisResult.error("cloneUrl must be a non-empty string.", List.of("cloneUrl is required."));
		}
		if (pullRequestId <= 0) {
			return McpAnalysisResult.error("pullRequestId must be greater than 0.",
					List.of("pullRequestId=" + pullRequestId));
		}
		if (timeoutSeconds <= 0) {
			return McpAnalysisResult.error("timeoutSeconds must be greater than 0.",
					List.of("timeoutSeconds=" + timeoutSeconds));
		}

		try {
			ProjectASTDiff diff = pullRequestDiffer.diffAtPullRequest(cloneUrl, pullRequestId, timeoutSeconds);
			return toResult(diff, maxRefactorings, "Pull-request");
		} catch (Exception e) {
			return McpAnalysisResult.error("Pull-request analysis failed: " + e.getMessage(),
					List.of(e.getClass().getName()));
		}
	}

	public McpAnalysisResult analyzeDirectories(Path beforePath, Path afterPath, int maxRefactorings) {
		if (maxRefactorings < 0) {
			return McpAnalysisResult.error("maxRefactorings must be greater than or equal to 0.",
					List.of("maxRefactorings=" + maxRefactorings));
		}

		try {
			requireExistingAbsolutePath(beforePath, "beforePath");
			requireExistingAbsolutePath(afterPath, "afterPath");
			ProjectASTDiff diff = directoryDiffer.diffAtDirectories(beforePath, afterPath);
			return toResult(diff, maxRefactorings, "Directory");
		} catch (Exception e) {
			return McpAnalysisResult.error("Directory analysis failed: " + e.getMessage(),
					List.of(e.getClass().getName()));
		}
	}

	private static void requireExistingAbsolutePath(Path path, String name) {
		if (path == null) {
			throw new IllegalArgumentException(name + " is required.");
		}
		if (!path.isAbsolute()) {
			throw new IllegalArgumentException(name + " must be absolute.");
		}
		if (!Files.exists(path)) {
			throw new IllegalArgumentException(name + " does not exist: " + path);
		}
	}

	static Path resolveCommitRepositoryPath(Path repositoryPath) throws Exception {
		Path realPath = repositoryPath.toRealPath();
		Path dotGit = realPath.resolve(".git");
		if (!Files.isRegularFile(dotGit)) {
			return realPath;
		}

		String gitFile = Files.readString(dotGit).trim();
		if (!gitFile.startsWith("gitdir:")) {
			return realPath;
		}

		Path gitDir = Path.of(gitFile.substring("gitdir:".length()).trim());
		if (!gitDir.isAbsolute()) {
			gitDir = realPath.resolve(gitDir).normalize();
		}

		Path commonDirFile = gitDir.resolve("commondir");
		if (!Files.isRegularFile(commonDirFile)) {
			return realPath;
		}

		Path commonGitDir = Path.of(Files.readString(commonDirFile).trim());
		if (!commonGitDir.isAbsolute()) {
			commonGitDir = gitDir.resolve(commonGitDir).normalize();
		}
		if (commonGitDir.getFileName() != null && ".git".equals(commonGitDir.getFileName().toString())
				&& commonGitDir.getParent() != null) {
			return commonGitDir.getParent().toRealPath();
		}
		return realPath;
	}

	private static McpAnalysisResult toResult(ProjectASTDiff diff, int maxRefactorings, String label) {
		if (diff == null) {
			return McpAnalysisResult.error(label + " analysis did not produce a diff.",
					List.of("RefactoringMiner returned null."));
		}
		return McpAnalysisResult.ok(diff, maxRefactorings);
	}

	@FunctionalInterface
	interface FileContentDiffer {
		ProjectASTDiff diffAtFileContents(Map<String, String> beforeFiles, Map<String, String> afterFiles) throws Exception;
	}

	@FunctionalInterface
	interface CommitDiffer {
		ProjectASTDiff diffAtMergeCommit(Path repositoryPath, String commitId, int parentIndex) throws Exception;
	}

	@FunctionalInterface
	interface PullRequestDiffer {
		ProjectASTDiff diffAtPullRequest(String cloneUrl, int pullRequestId, int timeoutSeconds) throws Exception;
	}

	@FunctionalInterface
	interface DirectoryDiffer {
		ProjectASTDiff diffAtDirectories(Path beforePath, Path afterPath) throws Exception;
	}
}
