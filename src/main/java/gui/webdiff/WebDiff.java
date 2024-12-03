package gui.webdiff;

import com.github.gumtreediff.utils.Pair;
import gui.webdiff.dir.DirComparator;
import gui.webdiff.dir.DirectoryDiffView;
import gui.webdiff.viewers.monaco.MonacoView;
import gui.webdiff.viewers.spv.SinglePageView;
import gui.webdiff.viewers.vanilla.VanillaDiffView;
import org.refactoringminer.astDiff.models.ASTDiff;
import org.refactoringminer.astDiff.models.ProjectASTDiff;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;
import spark.Spark;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;

import static spark.Spark.*;

public class WebDiff  {
    public static final String JQUERY_JS_URL = "https://code.jquery.com/jquery-3.4.1.min.js";
    public static final String BOOTSTRAP_CSS_URL = "https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css";
    public static final String BOOTSTRAP_JS_URL = "https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js";
    public final int port = 6789;

    private final String toolName = "RefactoringMiner";

    public ProjectASTDiff projectASTDiff;
    public WebDiff(ProjectASTDiff projectASTDiff) {
        this.projectASTDiff = projectASTDiff;
    }

    public void run() {
        DirComparator comparator = new DirComparator(projectASTDiff);
        killProcessOnPort(this.port);
        configureSpark(comparator, this.port);
        Spark.awaitInitialization();
        System.out.println(String.format("Starting server: %s:%d.", "http://127.0.0.1", this.port));
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
            if (pid != null && !pid.isEmpty()) {
                Process killProcess = Runtime.getRuntime().exec(String.format("kill -9 %s", pid));
                killProcess.waitFor();
            }
        } catch (IOException | InterruptedException ignored) { }
    }

    public void configureSpark(final DirComparator comparator, int port) {
        port(port);
        staticFiles.location("/web/");
        get("/", (request, response) -> {
//            if (comparator.isDirMode())
                response.redirect("/list");
//            else
//                response.redirect("/monaco-page/0");
            return "";
        });
        get("/list", (request, response) -> {
            Renderable view = new DirectoryDiffView(comparator, false);
            return render(view);
        });
        get("/vanilla-diff/:id", (request, response) -> {
            int id = Integer.parseInt(request.params(":id"));
            ASTDiff astDiff = comparator.getASTDiff(id);
            Renderable view = new VanillaDiffView(
                    toolName, astDiff.getSrcPath(),  astDiff.getDstPath(),
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
                    toolName, astDiff.getSrcPath(),  astDiff.getDstPath(),
                    astDiff, id, comparator.getNumOfDiffs(), request.pathInfo().split("/")[0],
                    comparator.isMoveDiff(id)
            );
            return render(view);
        });
        get("/monaco-diff/:id", (request, response) -> {
            int id = Integer.parseInt(request.params(":id"));
            ASTDiff astDiff = comparator.getASTDiff(id);
            MonacoView view = new MonacoView(
                    toolName, astDiff.getSrcPath(),  astDiff.getDstPath(),
                    astDiff, id, comparator.getNumOfDiffs(), request.pathInfo().split("/")[0],
                    comparator.isMoveDiff(id)
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
        get("/singleView", (request, response) -> render(new SinglePageView(comparator)));
        get("/quit", (request, response) -> {
            System.exit(0);
            return "";
        });
    }



    private static String render(Renderable r) throws IOException {
        HtmlCanvas c = new HtmlCanvas();
        r.renderOn(c);
        return c.toHtml();
    }
}
