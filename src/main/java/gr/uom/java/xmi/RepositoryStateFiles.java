 package gr.uom.java.xmi;

import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTreeEntry;
import org.refactoringminer.util.PathFileUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class RepositoryStateFiles {
  private final GHRepository repository;
  private final String stateSha;
  private Set<String> allPathsCache = null;
  private Set<String> pathsCache = null;
  private final Map<String, String> contentCache = new HashMap<>();

  public RepositoryStateFiles(GHRepository repository, String stateSha) {
    this.repository = repository;
    this.stateSha = stateSha;
  }

  public Set<String> allPaths() throws Exception {
    if (allPathsCache == null) {
      allPathsCache = repository.getTreeRecursive(stateSha, 1).getTree().stream()
              .filter(entry -> entry.getType().equals("blob"))
              .map(GHTreeEntry::getPath).collect(Collectors.toSet());
    }
    return allPathsCache;
  }

  public Set<String> paths() throws Exception {
    if (pathsCache == null) {
      pathsCache = allPaths().stream().filter(PathFileUtils::isSupportedFile).collect(Collectors.toSet());
    }
    return pathsCache;
  }

  public String content(String path) throws Exception {
    String cached = contentCache.get(path);
    if (cached == null) {
      try (InputStream content = repository.getFileContent(path, stateSha).read()) {
        cached = new String(content.readAllBytes(), StandardCharsets.UTF_8);
      }
      contentCache.put(path, cached);
    }
    return cached;
  }
}
