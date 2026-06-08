package gui.webdiff;

import com.github.gumtreediff.matchers.CompositeMatchers;
import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.utils.Pair;
import gui.webdiff.dir.DirComparator;
import gui.webdiff.dir.DirectoryDiffView;
import gui.webdiff.dir.filters.DiffFilterer;
import gui.webdiff.dir.filters.DiffFilterKind;
import gui.webdiff.viewers.monaco.MonacoView;
import gui.webdiff.viewers.monaco.SingleMonacoContent;
import gui.webdiff.viewers.spv.SinglePageView;
import gui.webdiff.viewers.vanilla.VanillaDiffView;
import org.refactoringminer.astDiff.models.ASTDiff;
import org.refactoringminer.astDiff.models.DiffMetaInfo;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;
import org.refactoringminer.astDiff.models.ProjectASTDiff;
import org.refactoringminer.astDiff.utils.Constants;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;
import spark.Request;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static spark.Spark.*;

public class WebDiff  {
    public static final String JQUERY_JS_URL = "https://code.jquery.com/jquery-3.4.1.min.js";
    public static final String BOOTSTRAP_CSS_URL = "https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css";
    public static final String BOOTSTRAP_JS_URL = "https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js";
    public static final String HIGHLIGHT_CSS_URL = "https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/styles/default.min.css";
    public static final String HIGHLIGHT_JS_URL = "https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/highlight.min.js";
    public static final String HIGHLIGHT_JAVA_URL = "https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/languages/java.min.js";
    public static final String LOCAL_HOST = "127.0.0.1";
    public static final String BIND_HOST_PROPERTY = "refactoringminer.webdiff.bindHost";
    public static final String PUBLIC_HOST_PROPERTY = "refactoringminer.webdiff.publicHost";
    public static final String BIND_HOST_ENV = "REFACTORINGMINER_WEBDIFF_BIND_HOST";
    public static final String PUBLIC_HOST_ENV = "REFACTORINGMINER_WEBDIFF_PUBLIC_HOST";
    public static final int DEFAULT_PORT = 6789;
    public int port = DEFAULT_PORT;
    private static final AtomicInteger DIFF_LOADER_THREAD_SEQUENCE = new AtomicInteger();

    private String toolName = "RefactoringMiner";
    private boolean staticExport;
    private boolean exitJvmOnQuit = true;
    private boolean quitEnabled = true;
    private String bindHost = configuredBindHost();
    private String publicHost = configuredPublicHost();

    public void setPort(int port) {
        this.port = port;
    }

    public void setBindHost(String bindHost) {
        this.bindHost = normalizeHost(bindHost, "bindHost");
    }

    public void setPublicHost(String publicHost) {
        this.publicHost = normalizeHost(publicHost, "publicHost");
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public void setStaticExport(boolean staticExport) {
        this.staticExport = staticExport;
    }

    public void setExitJvmOnQuit(boolean exitJvmOnQuit) {
        this.exitJvmOnQuit = exitJvmOnQuit;
    }

    public void setQuitEnabled(boolean quitEnabled) {
        this.quitEnabled = quitEnabled;
    }

    private final String resourcesPath = "/web/";
    private final DiffFilterer diffFilterer;
    private final Map<Integer, CompletableFuture<DiffViewState>> diffStates = new ConcurrentHashMap<>();
    private final ExecutorService diffLoader = Executors.newCachedThreadPool(createDiffLoaderThreadFactory());
    private volatile DiffViewState currentState;

    private static ThreadFactory createDiffLoaderThreadFactory() {
        return runnable -> {
            Thread thread = new Thread(runnable, "webdiff-merge-loader-" + DIFF_LOADER_THREAD_SEQUENCE.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }

    public String getResources() {
        return resourcesPath;
    }

    public DirComparator getComparator() {
        return currentState.comparator();
    }

    public ProjectASTDiff getProjectASTDiff() {
        return currentState.projectASTDiff();
    }

    public WebDiff(ProjectASTDiff projectASTDiff) {
        this(projectASTDiff, DiffFilterKind.NO_FILTER);
    }

    public WebDiff(ProjectASTDiff projectASTDiff, DiffFilterer diffFilterer) {
        this.diffFilterer = diffFilterer;
        DiffViewState initialState = new DiffViewState(projectASTDiff, new DirComparator(projectASTDiff, diffFilterer));
        this.currentState = initialState;
        this.diffStates.put(selectedParentIndex(projectASTDiff), CompletableFuture.completedFuture(initialState));
    }

    public synchronized ProjectASTDiff switchToParent(int parentIndex) throws Exception {
        DiffMetaInfo metaInfo = currentState.projectASTDiff().getMetaInfo();
        if (metaInfo == null || !metaInfo.supportsParentSelection()) {
            throw new IllegalArgumentException("This diff does not support merge parent selection");
        }
        if (parentIndex < 0 || parentIndex >= metaInfo.getParentCount()) {
            throw new IllegalArgumentException(String.format("Parent index %d is out of range", parentIndex));
        }
        DiffViewState state = awaitDiffState(metaInfo, parentIndex);
        currentState = state;
        return state.projectASTDiff();
    }

    private DiffViewState awaitDiffState(DiffMetaInfo metaInfo, int parentIndex) throws Exception {
        try {
            return diffStates.computeIfAbsent(parentIndex,
                    ignored -> CompletableFuture.supplyAsync(() -> {
                        try {
                            return loadDiffState(metaInfo, parentIndex);
                        } catch (Exception e) {
                            throw new CompletionException(e);
                        }
                    }, diffLoader)).join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof Exception exception) {
                throw exception;
            }
            throw e;
        }
    }

    private void warmUpMergeParents() {
        DiffMetaInfo metaInfo = currentState.projectASTDiff().getMetaInfo();
        if (staticExport || metaInfo == null || !metaInfo.supportsParentSelection()) {
            return;
        }
        for (Integer parentIndex : metaInfo.getAvailableParentIndices()) {
            if (!parentIndex.equals(metaInfo.getSelectedParentIndex())) {
                diffStates.computeIfAbsent(parentIndex,
                        ignored -> CompletableFuture.supplyAsync(() -> {
                            try {
                                return loadDiffState(metaInfo, parentIndex);
                            } catch (Exception e) {
                                throw new CompletionException(e);
                            }
                        }, diffLoader));
            }
        }
    }

    private DiffViewState loadDiffState(DiffMetaInfo metaInfo, int parentIndex) throws Exception {
        GitHistoryRefactoringMinerImpl miner = new GitHistoryRefactoringMinerImpl();
        ProjectASTDiff projectASTDiff;
        if (metaInfo.getRepositoryPath() != null && !metaInfo.getRepositoryPath().isEmpty()) {
            projectASTDiff = miner.diffAtMergeCommit(Path.of(metaInfo.getRepositoryPath()), metaInfo.getCommitId(), parentIndex);
        } else if (metaInfo.getCloneURL() != null && !metaInfo.getCloneURL().isEmpty()) {
            int timeout = metaInfo.getTimeout() != null ? metaInfo.getTimeout() : 1000;
            projectASTDiff = miner.diffAtMergeCommit(metaInfo.getCloneURL(), metaInfo.getCommitId(), parentIndex, timeout);
        } else {
            throw new IllegalArgumentException("Unable to reload the diff for a different merge parent");
        }
        return new DiffViewState(projectASTDiff, new DirComparator(projectASTDiff, diffFilterer));
    }

    private static int selectedParentIndex(ProjectASTDiff projectASTDiff) {
        DiffMetaInfo metaInfo = projectASTDiff.getMetaInfo();
        return metaInfo != null && metaInfo.getSelectedParentIndex() != null ? metaInfo.getSelectedParentIndex() : 0;
    }

    private record DiffViewState(ProjectASTDiff projectASTDiff, DirComparator comparator) {
    }

    public String localUrl() {
        return localUrl(this.publicHost, this.port);
    }

    public static String localUrl(int port) {
        return localUrl(LOCAL_HOST, port);
    }

    public static String localUrl(String publicHost, int port) {
        return "http://" + normalizeHost(publicHost, "publicHost") + ":" + port;
    }

    public static String startupMessage(int port) {
        return "Starting server: " + localUrl(port);
    }

    public static String startupMessage(String publicHost, int port) {
        return "Starting server: " + localUrl(publicHost, port);
    }

    public static String configuredBindHost() {
        return configuredHost(BIND_HOST_PROPERTY, BIND_HOST_ENV, LOCAL_HOST);
    }

    public static String configuredPublicHost() {
        return configuredHost(PUBLIC_HOST_PROPERTY, PUBLIC_HOST_ENV, LOCAL_HOST);
    }

    private static String configuredHost(String propertyName, String envName, String defaultValue) {
        String propertyValue = System.getProperty(propertyName);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return normalizeHost(propertyValue, propertyName);
        }
        String envValue = System.getenv(envName);
        if (envValue != null && !envValue.isBlank()) {
            return normalizeHost(envValue, envName);
        }
        return defaultValue;
    }

    private static String normalizeHost(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must be a non-empty host.");
        }
        return value.trim();
    }

    public String start() {
//        killProcessOnPort(this.port);
        configureSpark(this.port);
        warmUpMergeParents();
        awaitInitialization();
        return startupMessage(this.publicHost, this.port);
    }

    public void run() {
        System.out.println(start() + ".");
    }
    public void terminate(){
        diffLoader.shutdownNow();
        stop();
    }

    public void openInBrowser() {
        run();
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
                desktop.browse(new URI(localUrl()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void killProcessOnPort(int port) {
        try {
            Process findPidProcess = Runtime.getRuntime().exec(String.format("lsof -t -i:%d", port));
            BufferedReader pidReader = new BufferedReader(new InputStreamReader(findPidProcess.getInputStream()));
            String pid = pidReader.readLine();
            pidReader.close();
            try {
                Integer.parseInt(pid);
            }
            catch (NumberFormatException e){
                return;
            }
            if (!pid.isEmpty()) {
                Process killProcess = Runtime.getRuntime().exec(String.format("kill -9 %s", pid));
                killProcess.waitFor();
            }
        } catch (IOException | InterruptedException ignored) {
//            System.out.println(ignored.getMessage());
        }
    }

    public void configureSpark(int port) {
        ipAddress(bindHost);
        port(port);
        staticFiles.location(getResources());
        get("/", (request, response) -> {
//            if (comparator.isDirMode())
                response.redirect("/list");
//            else
//                response.redirect("/monaco-page/0");
            return "";
        });
        get("/switch-parent/:parentIndex", (request, response) -> {
            try {
                switchToParent(Integer.parseInt(request.params(":parentIndex")));
                response.redirect("/list");
                return "";
            } catch (RuntimeException e) {
                if (e.getCause() instanceof Exception cause) {
                    throw cause;
                }
                throw e;
            }
        });
        get("/list", (request, response) -> {
            DiffViewState state = currentState;
            Renderable view = new DirectoryDiffView(state.comparator(), false, state.projectASTDiff().getMetaInfo(),
                    !staticExport, quitEnabled);
            return render(view);
        });
        get("/vanilla-diff/:id", (request, response) -> {
            return getVanillaDiff(request);
        });
        get("/vanilla-diff/:id/", (request, response) -> {
            return getVanillaDiff(request);
        });


        get("/monaco-page/:id", (request, response) -> {
            return returnMonacoView(request);
        });
        get("/monaco-page/:id/", (request, response) -> {
            return returnMonacoView(request);
        });

        get("/monaco-minimal/:id", (request, response) -> {
            DiffViewState state = currentState;
            int id = Integer.parseInt(request.params(":id"));
            MonacoView view = new MonacoView(
                    toolName, state.comparator(), request.pathInfo().split("/")[0], id
            );
            view.setButtons(false);
            return render(view);
        });
        get("/monaco-diff/:id", (request, response) -> {
            DiffViewState state = currentState;
            int id = Integer.parseInt(request.params(":id"));
            MonacoView view = new MonacoView(
                    toolName, state.comparator(), request.pathInfo().split("/")[0], id
            );
            view.setDecorate(false);
            return render(view);
        });

        get("/left/:id", (request, response) -> {
            DiffViewState state = currentState;
            int id = Integer.parseInt(request.params(":id"));
//            String id = (request.params(":id"));
//            String _id = id.replace("*","/");
            Pair<String, String> pair = state.comparator().getFileContentsPair(id);
            return pair.first;
        });
        get("/right/:id", (request, response) -> {
            DiffViewState state = currentState;
            int id = Integer.parseInt(request.params(":id"));
//            String id = (request.params(":id"));
//            String _id = id.replace("*","/");
            Pair<String, String> pair = state.comparator().getFileContentsPair(id);
            return pair.second;
        });
        get("/singleView", (request, response) -> {
            DiffViewState state = currentState;
            return render(new SinglePageView(state.comparator(), state.projectASTDiff().getMetaInfo(), !staticExport, !staticExport));
        });
        get("/quit", (request, response) -> {
            if (!quitEnabled) {
                response.status(403);
                return "WebDiff quit is disabled.";
            }
            if (exitJvmOnQuit) {
                System.exit(0);
                return "";
            }
            Thread stopper = new Thread(() -> {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                terminate();
            }, "webdiff-quit");
            stopper.setDaemon(true);
            stopper.start();
            return "WebDiff server stopping.";
        });
        get("/content", (request, response) -> {
            DiffViewState state = currentState;
            String rawFilePath = request.queryParams("path");
            String side = request.queryParams("side");
            Map<String, String> contentsMap;

            boolean isAdded = "right".equals(side) || "added".equals(side);
            boolean isDeleted = "left".equals(side) || "deleted".equals(side);

            if (isDeleted) {
                contentsMap = state.projectASTDiff().getFileContentsBefore();
            } else if (isAdded) {
                contentsMap = state.projectASTDiff().getFileContentsAfter();
            } else {
                contentsMap = new LinkedHashMap<>();
            }

            String path = URLDecoder.decode(rawFilePath, StandardCharsets.UTF_8);
            String content = contentsMap.getOrDefault(path, "");
            return render(new SingleMonacoContent(toolName, request.pathInfo(), state.comparator().getNumOfDiffs(),
                    isAdded, path, content, state.projectASTDiff().getMetaInfo(),
                    state.comparator().getRemovedFilesName().stream().collect(Collectors.toList()),
                    state.comparator().getAddedFilesName().stream().collect(Collectors.toList())));
        });
        get("/onDemand", (request, response) -> {
            DiffViewState state = currentState;
            String rawFile1 = request.queryParams("file1");
            String rawFile2 = request.queryParams("file2");
            String srcPath = URLDecoder.decode(rawFile1, StandardCharsets.UTF_8);
            String dstPath = URLDecoder.decode(rawFile2, StandardCharsets.UTF_8);
            String srcContent = state.projectASTDiff().getFileContentsBefore().get(srcPath);
            String dstContent = state.projectASTDiff().getFileContentsAfter().get(dstPath);
            ProjectASTDiff customProjectASTDiff = null;
            try {
                customProjectASTDiff = new GitHistoryRefactoringMinerImpl().diffAtFileContents(
                        new LinkedHashMap<>() {{
                            put(srcPath, srcContent);
                        }},
                        new LinkedHashMap<>() {{
                            put(dstPath, dstContent);
                        }}
                );
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }

            ASTDiff astDiff;
            String toolName = this.toolName;
            if (customProjectASTDiff != null && customProjectASTDiff.getDiffSet().size() > 0){
                astDiff = customProjectASTDiff.getDiffSet().iterator().next();
            }
            else {
                toolName = "GTS";
                TreeContext srcContext = state.projectASTDiff().getParentContextMap().get(srcPath);
                TreeContext dstContext = state.projectASTDiff().getChildContextMap().get(dstPath);
                Constants LANG1 = new Constants(srcPath);
                Constants LANG2 = new Constants(dstPath);
                ExtendedMultiMappingStore extendedMappingStore = new ExtendedMultiMappingStore(srcContext.getRoot(), dstContext.getRoot(), LANG1, LANG2);
                MappingStore match = new CompositeMatchers.SimpleGumtree().match(srcContext.getRoot(), dstContext.getRoot());
                for (Mapping mapping : match) extendedMappingStore.addMapping(mapping.first, mapping.second);
                astDiff = new ASTDiff(srcPath, dstPath, srcContext, dstContext, extendedMappingStore);
                astDiff.computeVanillaEditScript();
            }
            MonacoView view = new MonacoView(
                    toolName, state.comparator(), request.pathInfo().split("/")[0], -1, astDiff
            );
            view.setDecorate(true);
            return render(view);
        });
    }

    private String getVanillaDiff(Request request) throws IOException {
        DiffViewState state = currentState;
        int id = Integer.parseInt(request.params(":id"));
        ASTDiff astDiff = state.comparator().getASTDiff(id);
        Renderable view = new VanillaDiffView(
                toolName, state.projectASTDiff().getMetaInfo(), astDiff.getSrcPath(),  astDiff.getDstPath(),
                astDiff, id, state.comparator().getNumOfDiffs(), routePrefix(request),
                state.comparator().isMoveDiff(id),
                state.projectASTDiff().getFileContentsBefore().get(astDiff.getSrcPath()),
                state.projectASTDiff().getFileContentsAfter().get(astDiff.getDstPath()),
                false);
        return render(view);
    }

    private String returnMonacoView(Request request) throws IOException {
        DiffViewState state = currentState;
        int id = Integer.parseInt(request.params(":id"));
        Renderable view = new MonacoView(
                toolName, state.comparator(), routePrefix(request), id
        );
        return render(view);
    }

    /**
     * Returns the folder portion of the current request path, with both slashes
     * intact, e.g. "/monaco-page/5" or "/monaco-page/5/" -> "/monaco-page/".
     * Used as the Next/Prev route prefix so those links resolve to a full path
     * (e.g. /monaco-page/6) instead of a bare index (6), which 404s in the
     * static export where there is no live server to fill in the route.
     */
    private static String routePrefix(Request request) {
        return routePrefix(request.pathInfo());
    }

    /** Package-private for testing; see {@link #routePrefix(Request)}. */
    static String routePrefix(String pathInfo) {
        String path = pathInfo;
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path.substring(0, path.lastIndexOf('/') + 1);
    }

    private static String render(Renderable r) throws IOException {
        HtmlCanvas c = new HtmlCanvas();
        r.renderOn(c);
        return c.toHtml();
    }
}
