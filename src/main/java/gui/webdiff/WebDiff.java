package gui.webdiff;

import com.github.gumtreediff.matchers.CompositeMatchers;
import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.utils.Pair;
import gui.webdiff.dir.DirComparator;
import gui.webdiff.dir.DirectoryDiffView;
import gui.webdiff.viewers.monaco.MonacoView;
import gui.webdiff.viewers.spv.SinglePageView;
import gui.webdiff.viewers.vanilla.VanillaDiffView;
import org.refactoringminer.astDiff.models.ASTDiff;
import org.refactoringminer.astDiff.models.ExtendedMultiMappingStore;
import org.refactoringminer.astDiff.models.ProjectASTDiff;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;

import static spark.Spark.*;

public class WebDiff  {
    public static final String JQUERY_JS_URL = "https://code.jquery.com/jquery-3.4.1.min.js";
    public static final String BOOTSTRAP_CSS_URL = "https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css";
    public static final String BOOTSTRAP_JS_URL = "https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js";
    public final int port = 6789;

    private final String toolName = "RefactoringMiner";

    private final ProjectASTDiff projectASTDiff;
    private final String resourcesPath = "/web/";
    private final DirComparator comparator;

    public String getResources() {
        return resourcesPath;
    }

    public DirComparator getComparator() {
        return comparator;
    }

    public ProjectASTDiff getProjectASTDiff() {
        return projectASTDiff;
    }

    public WebDiff(ProjectASTDiff projectASTDiff) {
        this.projectASTDiff = projectASTDiff;
        this.comparator = new DirComparator(projectASTDiff);
    }

    public void run() {
        killProcessOnPort(this.port);
        configureSpark(comparator, this.port);
        awaitInitialization();
        System.out.println(String.format("Starting server: %s:%d.", "http://127.0.0.1", this.port));
    }
    public void terminate(){
        stop();
    }

    public void openInBrowser() {
        run();
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
                desktop.browse(new URI("http://127.0.0.1" + ":" + this.port));
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

    public void configureSpark(final DirComparator comparator, int port) {
        port(port);
        staticFiles.location(getResources());
        get("/", (request, response) -> {
//            if (comparator.isDirMode())
                response.redirect("/list");
//            else
//                response.redirect("/monaco-page/0");
            return "";
        });
        get("/list", (request, response) -> {
            Renderable view = new DirectoryDiffView(comparator, false, projectASTDiff.getMetaInfo());
            return render(view);
        });
        get("/vanilla-diff/:id", (request, response) -> {
            int id = Integer.parseInt(request.params(":id"));
            ASTDiff astDiff = comparator.getASTDiff(id);
            Renderable view = new VanillaDiffView(
                    toolName, projectASTDiff.getMetaInfo(), astDiff.getSrcPath(),  astDiff.getDstPath(),
                    astDiff, id, comparator.getNumOfDiffs(), request.pathInfo().split("/")[0],
                    comparator.isMoveDiff(id),
                    projectASTDiff.getFileContentsBefore().get(astDiff.getSrcPath()),
                    projectASTDiff.getFileContentsAfter().get(astDiff.getDstPath()),
                    false);
            return render(view);
        });
        get("/monaco-page/:id", (request, response) -> {
            int id = Integer.parseInt(request.params(":id"));
            ASTDiff astDiff = comparator.getASTDiff(id);
            Renderable view = new MonacoView(
                    toolName, projectASTDiff.getMetaInfo(), astDiff.getSrcPath(),  astDiff.getDstPath(),
                    astDiff, id, comparator.getNumOfDiffs(), request.pathInfo().split("/")[0],
                    comparator.isMoveDiff(id),
                    projectASTDiff.getFileContentsBefore().get(astDiff.getSrcPath()),
                    projectASTDiff.getFileContentsAfter().get(astDiff.getDstPath()),
                    projectASTDiff.getRefactorings()
            );
            return render(view);
        });
        get("/monaco-diff/:id", (request, response) -> {
            int id = Integer.parseInt(request.params(":id"));
            ASTDiff astDiff = comparator.getASTDiff(id);
            MonacoView view = new MonacoView(
                    toolName, projectASTDiff.getMetaInfo(), astDiff.getSrcPath(),  astDiff.getDstPath(),
                    astDiff, id, comparator.getNumOfDiffs(), request.pathInfo().split("/")[0],
                    comparator.isMoveDiff(id),
                    projectASTDiff.getFileContentsBefore().get(astDiff.getSrcPath()),
                    projectASTDiff.getFileContentsAfter().get(astDiff.getDstPath()),
                    projectASTDiff.getRefactorings()
            );
            view.setDecorate(false);
            return render(view);
        });
        get("/left/:id", (request, response) -> {
            int id = Integer.parseInt(request.params(":id"));
//            String id = (request.params(":id"));
//            String _id = id.replace("*","/");
            Pair<String, String> pair = comparator.getFileContentsPair(id);
            return pair.first;
        });
        get("/right/:id", (request, response) -> {
            int id = Integer.parseInt(request.params(":id"));
//            String id = (request.params(":id"));
//            String _id = id.replace("*","/");
            Pair<String, String> pair = comparator.getFileContentsPair(id);
            return pair.second;
        });
        get("/singleView", (request, response) -> render(new SinglePageView(comparator, projectASTDiff.getMetaInfo())));
        get("/quit", (request, response) -> {
            System.exit(0);
            return "";
        });
        get("/onDemand", (request, response) -> {
            String rawFile1 = request.queryParams("file1");
            String rawFile2 = request.queryParams("file2");
            String srcPath = URLDecoder.decode(rawFile1, StandardCharsets.UTF_8);
            String dstPath = URLDecoder.decode(rawFile2, StandardCharsets.UTF_8);
            String srcContent = this.projectASTDiff.getFileContentsBefore().get(srcPath);
            String dstContent = this.projectASTDiff.getFileContentsAfter().get(dstPath);
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
                System.out.println(e.getMessage());
            }

            ASTDiff astDiff;
            if (customProjectASTDiff != null && customProjectASTDiff.getDiffSet().iterator().hasNext()){
                astDiff = customProjectASTDiff.getDiffSet().iterator().next();
            }
            else {
                TreeContext srcContext = projectASTDiff.getParentContextMap().get(srcPath);
                TreeContext dstContext = projectASTDiff.getChildContextMap().get(dstPath);
                ExtendedMultiMappingStore extendedMappingStore = new ExtendedMultiMappingStore(srcContext.getRoot(), dstContext.getRoot());
                MappingStore match = new CompositeMatchers.SimpleGumtree().match(srcContext.getRoot(), dstContext.getRoot());
                for (Mapping mapping : match) extendedMappingStore.addMapping(mapping.first, mapping.second);
                astDiff = new ASTDiff(srcPath, dstPath, srcContext, dstContext, extendedMappingStore);
                astDiff.computeVanillaEditScript();
            }
            MonacoView view = new MonacoView(
                    "GTS", projectASTDiff.getMetaInfo(), srcPath, dstPath,
                    astDiff, -1, comparator.getNumOfDiffs(), request.pathInfo().split("/")[0],
                    false,
                    projectASTDiff.getFileContentsBefore().get(astDiff.getSrcPath()),
                    projectASTDiff.getFileContentsAfter().get(astDiff.getDstPath()),
                    projectASTDiff.getRefactorings()
            );
            view.setDecorate(true);
            return render(view);
        });
    }



    private static String render(Renderable r) throws IOException {
        HtmlCanvas c = new HtmlCanvas();
        r.renderOn(c);
        return c.toHtml();
    }
}
