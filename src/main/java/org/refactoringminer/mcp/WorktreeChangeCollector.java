package org.refactoringminer.mcp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.refactoringminer.util.PathFileUtils;

public class WorktreeChangeCollector {
	private static final String DEFAULT_BASE_REF = "HEAD";

	public WorktreeChanges collect(Path repositoryPath, String baseRef, boolean includeUntracked, int maxFiles,
			int maxBytesPerFile) throws Exception {
		validateLimits(maxFiles, maxBytesPerFile);
		Path requestedPath = validateRequestedPath(repositoryPath);
		try (Repository repository = openRepository(requestedPath); Git git = new Git(repository)) {
			Path workTree = repository.getWorkTree().toPath().toRealPath();
			if (!requestedPath.startsWith(workTree)) {
				throw new IllegalArgumentException("repositoryPath must point inside the Git working tree.");
			}
			Status status = git.status().call();
			Set<String> changedPaths = changedPaths(status, includeUntracked);
			List<String> warnings = new ArrayList<>();
			changedPaths.removeIf(path -> {
				boolean unsupported = !PathFileUtils.isSupportedFile(path);
				if (unsupported) {
					warnings.add("Skipping unsupported file: " + path);
				}
				return unsupported;
			});
			if (changedPaths.size() > maxFiles) {
				throw new IllegalArgumentException("Changed supported file count " + changedPaths.size()
						+ " exceeds maxFiles=" + maxFiles + ".");
			}

			String resolvedBaseRef = baseRef == null || baseRef.isBlank() ? DEFAULT_BASE_REF : baseRef;
			ObjectId treeId = repository.resolve(resolvedBaseRef + "^{tree}");
			if (treeId == null) {
				throw new IllegalArgumentException("baseRef does not resolve to a tree: " + resolvedBaseRef);
			}

			Map<String, String> beforeFiles = new LinkedHashMap<>();
			Map<String, String> afterFiles = new LinkedHashMap<>();
			for (String path : changedPaths) {
				if (hasBaseContent(status, path)) {
					String baseContent = readBaseContent(repository, treeId, path, maxBytesPerFile);
					if (baseContent != null) {
						beforeFiles.put(path, baseContent);
					}
				}
				if (hasWorkingTreeContent(status, path)) {
					afterFiles.put(path, readWorkingTreeContent(workTree, path, maxBytesPerFile));
				}
			}
			if (beforeFiles.isEmpty() && afterFiles.isEmpty()) {
				throw new IllegalArgumentException("No supported worktree changes found.");
			}
			return new WorktreeChanges(beforeFiles, afterFiles, warnings);
		}
	}

	private static void validateLimits(int maxFiles, int maxBytesPerFile) {
		if (maxFiles < 1) {
			throw new IllegalArgumentException("maxFiles must be greater than 0.");
		}
		if (maxBytesPerFile < 1) {
			throw new IllegalArgumentException("maxBytesPerFile must be greater than 0.");
		}
	}

	private static Path validateRequestedPath(Path repositoryPath) throws IOException {
		if (repositoryPath == null) {
			throw new IllegalArgumentException("repositoryPath is required.");
		}
		if (!repositoryPath.isAbsolute()) {
			throw new IllegalArgumentException("repositoryPath must be absolute.");
		}
		if (!Files.exists(repositoryPath)) {
			throw new IllegalArgumentException("repositoryPath does not exist: " + repositoryPath);
		}
		return repositoryPath.toRealPath();
	}

	private static Repository openRepository(Path requestedPath) throws IOException {
		RepositoryBuilder builder = new RepositoryBuilder()
				.findGitDir(requestedPath.toFile())
				.readEnvironment();
		if (builder.getGitDir() == null) {
			throw new IllegalArgumentException("repositoryPath does not point to a Git working tree: " + requestedPath);
		}
		return builder.build();
	}

	private static Set<String> changedPaths(Status status, boolean includeUntracked) {
		Set<String> paths = new LinkedHashSet<>();
		paths.addAll(status.getModified());
		paths.addAll(status.getChanged());
		paths.addAll(status.getAdded());
		paths.addAll(status.getRemoved());
		paths.addAll(status.getMissing());
		if (includeUntracked) {
			paths.addAll(status.getUntracked());
		}
		return paths;
	}

	private static boolean hasBaseContent(Status status, String path) {
		return !status.getAdded().contains(path) && !status.getUntracked().contains(path);
	}

	private static boolean hasWorkingTreeContent(Status status, String path) {
		return !status.getRemoved().contains(path) && !status.getMissing().contains(path);
	}

	private static String readBaseContent(Repository repository, ObjectId treeId, String path, int maxBytesPerFile)
			throws IOException {
		try (TreeWalk treeWalk = TreeWalk.forPath(repository, path, treeId)) {
			if (treeWalk == null) {
				return null;
			}
			ObjectLoader loader = repository.open(treeWalk.getObjectId(0));
			if (loader.getSize() > maxBytesPerFile) {
				throw new IllegalArgumentException(path + " in baseRef exceeds maxBytesPerFile=" + maxBytesPerFile + ".");
			}
			return new String(loader.getBytes(), StandardCharsets.UTF_8);
		}
	}

	private static String readWorkingTreeContent(Path workTree, String path, int maxBytesPerFile) throws IOException {
		Path file = workTree.resolve(path).normalize().toRealPath();
		if (!file.startsWith(workTree)) {
			throw new IllegalArgumentException("Changed file escapes repository root: " + path);
		}
		if (Files.size(file) > maxBytesPerFile) {
			throw new IllegalArgumentException(path + " exceeds maxBytesPerFile=" + maxBytesPerFile + ".");
		}
		return Files.readString(file);
	}

	public record WorktreeChanges(Map<String, String> beforeFiles, Map<String, String> afterFiles, List<String> warnings) {
	}
}
