package gr.uom.java.xmi;

import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTreeEntry;
import org.refactoringminer.util.PathFileUtils;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * The files of a repository at a commit, read through GitHub. With a cache folder, what is read is kept there (the
 * paths of the commit in {@code <cacheName>-<stateSha>-paths.txt.gz}, the content of a file at
 * {@code <cacheName>-<stateSha>/<path>}) and read from there afterwards, so that a state read before needs no GitHub
 * access at all: the repository is only connected to when something is not cached.
 */
public class RepositoryStateFiles {
  private final Callable<GHRepository> repositorySupplier;
  private GHRepository repository = null;
  private final String stateSha;
  @Nullable
  private final File stateFolder;
  @Nullable
  private final File pathsFile;
  private Set<String> allPathsCache = null;
  private Set<String> pathsCache = null;
  private final Map<String, String> contentCache = new HashMap<>();

  public RepositoryStateFiles(GHRepository repository, String stateSha) {
    this(() -> repository, null, stateSha, null);
  }

  /**
   * @param cacheName  the name of the repository in the cache folder, unique among the repositories cached there
   * @param cacheFolder the folder caching the files read, or null to read them from GitHub every time
   */
  public RepositoryStateFiles(Callable<GHRepository> repositorySupplier, @Nullable String cacheName, String stateSha,
                              @Nullable File cacheFolder) {
    this.repositorySupplier = repositorySupplier;
    this.stateSha = stateSha;
    this.stateFolder = cacheFolder == null ? null : new File(cacheFolder, cacheName + "-" + stateSha);
    this.pathsFile = cacheFolder == null ? null : new File(cacheFolder, cacheName + "-" + stateSha + "-paths.txt.gz");
  }

  private GHRepository repository() throws Exception {
    if (repository == null) {
      repository = repositorySupplier.call();
    }
    return repository;
  }

  public Set<String> allPaths() throws Exception {
    if (allPathsCache == null) {
      if (pathsFile != null && pathsFile.exists()) {
        try (InputStream input = new GZIPInputStream(Files.newInputStream(pathsFile.toPath()))) {
          allPathsCache = new String(input.readAllBytes(), StandardCharsets.UTF_8).lines().collect(Collectors.toCollection(HashSet::new));
        }
      } else {
        allPathsCache = repository().getTreeRecursive(stateSha, 1).getTree().stream()
                .filter(entry -> entry.getType().equals("blob"))
                .map(GHTreeEntry::getPath).collect(Collectors.toSet());
        if (pathsFile != null) {
          ByteArrayOutputStream paths = new ByteArrayOutputStream();
          try (OutputStream output = new GZIPOutputStream(paths)) {
            output.write(String.join("\n", allPathsCache.stream().sorted().toList()).getBytes(StandardCharsets.UTF_8));
          }
          write(pathsFile.toPath(), paths.toByteArray());
        }
      }
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
      Path cachedFile = stateFolder == null ? null : new File(stateFolder, path).toPath();
      if (cachedFile != null && Files.exists(cachedFile)) {
        cached = Files.readString(cachedFile, StandardCharsets.UTF_8);
      } else {
        try (InputStream content = repository().getFileContent(path, stateSha).read()) {
          cached = new String(content.readAllBytes(), StandardCharsets.UTF_8);
        }
        if (cachedFile != null) {
          write(cachedFile, cached.getBytes(StandardCharsets.UTF_8));
        }
      }
      contentCache.put(path, cached);
    }
    return cached;
  }

  // Processes sharing the cache folder may read a file another one is writing, so a file appears there only complete
  private static void write(Path file, byte[] content) throws Exception {
    Files.createDirectories(file.getParent());
    Path written = Files.createTempFile(file.getParent(), file.getFileName().toString(), ".tmp");
    Files.write(written, content);
    Files.move(written, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
  }
}
