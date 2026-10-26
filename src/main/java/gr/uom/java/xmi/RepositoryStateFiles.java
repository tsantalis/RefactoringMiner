package gr.uom.java.xmi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTreeEntry;

import javax.annotation.Nullable;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;

/**
 * The files of a repository at a commit, read through GitHub. With a cache folder, what is read is kept there (the
 * paths of the commit having each file name looked up in {@code <cacheName>-<stateSha>-paths-by-file-name.json}, the
 * content of a file at {@code <cacheName>-<stateSha>/<path>}) and read from there afterwards, so that a state read
 * before needs no GitHub access at all: the repository is only connected to when something is not cached.
 */
public class RepositoryStateFiles {
  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
  private static final TypeToken<TreeMap<String, List<String>>> PATHS_BY_FILE_NAME = new TypeToken<>() {};

  private final Callable<GHRepository> repositorySupplier;
  private GHRepository repository = null;
  private final String stateSha;
  @Nullable
  private final File stateFolder;
  @Nullable
  private final File pathsByFileNameFile;
  // The file names looked up so far, with the paths having them
  private Map<String, List<String>> pathsByFileName = null;
  // The paths of the whole tree of the commit by file name, read from GitHub only when a file name is not cached
  private Map<String, List<String>> treePathsByFileName = null;
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
    this.pathsByFileNameFile = cacheFolder == null ? null
            : new File(cacheFolder, cacheName + "-" + stateSha + "-paths-by-file-name.json");
  }

  private GHRepository repository() throws Exception {
    if (repository == null) {
      repository = repositorySupplier.call();
    }
    return repository;
  }

  /**
   * The paths of the commit whose last segment is {@code fileName}, sorted. Synchronized, as tests share an instance
   * across threads.
   */
  public synchronized List<String> pathsNamed(String fileName) throws Exception {
    if (pathsByFileName == null) {
      pathsByFileName = cachedPathsByFileName();
    }
    List<String> paths = pathsByFileName.get(fileName);
    if (paths == null) {
      paths = treePathsByFileName().getOrDefault(fileName, List.of());
      pathsByFileName.put(fileName, paths);
      if (pathsByFileNameFile != null) {
        // Other instances on the same commit may have cached other file names meanwhile
        Map<String, List<String>> cached = cachedPathsByFileName();
        cached.putAll(pathsByFileName);
        write(pathsByFileNameFile.toPath(), GSON.toJson(cached).getBytes(StandardCharsets.UTF_8));
      }
    }
    return paths;
  }

  public boolean exists(String path) throws Exception {
    return pathsNamed(path.substring(path.lastIndexOf('/') + 1)).contains(path);
  }

  private Map<String, List<String>> cachedPathsByFileName() throws Exception {
    if (pathsByFileNameFile == null || !pathsByFileNameFile.exists()) {
      return new TreeMap<>();
    }
    return GSON.fromJson(Files.readString(pathsByFileNameFile.toPath(), StandardCharsets.UTF_8), PATHS_BY_FILE_NAME);
  }

  private Map<String, List<String>> treePathsByFileName() throws Exception {
    if (treePathsByFileName == null) {
      treePathsByFileName = new HashMap<>();
      repository().getTreeRecursive(stateSha, 1).getTree().stream()
              .filter(entry -> entry.getType().equals("blob"))
              .map(GHTreeEntry::getPath).sorted()
              .forEach(path -> treePathsByFileName.computeIfAbsent(path.substring(path.lastIndexOf('/') + 1),
                      k -> new ArrayList<>()).add(path));
    }
    return treePathsByFileName;
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
