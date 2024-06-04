package gui.webdiff;

import com.github.gumtreediff.utils.Pair;
import org.refactoringminer.astDiff.models.ASTDiff;
import org.refactoringminer.astDiff.models.ProjectASTDiff;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;
import spark.Spark;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

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

    public void run() throws IOException {
        DirComparator comparator = new DirComparator(projectASTDiff);
        killProcessOnPort(this.port);
        configureSpark(comparator, this.port);
        Spark.awaitInitialization();
        System.out.println(String.format("Starting server: %s:%d.", "http://127.0.0.1", this.port));
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
//                response.redirect("/monaco-diff/0");
            return "";
        });
        get("/list", (request, response) -> {
            Renderable view = new DirectoryDiffView(comparator);
            return render(view);
        });
        get("/vanilla-diff/:id", (request, response) -> {
            int id = Integer.parseInt(request.params(":id"));
            ASTDiff astDiff = comparator.getASTDiff(id);
            Renderable view = new VanillaDiffView(
                    toolName, astDiff.getSrcPath(),  astDiff.getDstPath(),
                    astDiff, id, comparator.getNumOfDiffs(), request.pathInfo().split("/")[0],
                    projectASTDiff.getFileContentsBefore().get(astDiff.getSrcPath()),
                    projectASTDiff.getFileContentsAfter().get(astDiff.getDstPath()),
                    false);
            return render(view);
        });
        get("/monaco-diff/:id", (request, response) -> {
            int id = Integer.parseInt(request.params(":id"));
            ASTDiff astDiff = comparator.getASTDiff(id);
            Renderable view = new MonacoDiffView(
                    toolName, astDiff.getSrcPath(),  astDiff.getDstPath(),
                    astDiff, id, comparator.getNumOfDiffs(), request.pathInfo().split("/")[0]
            );
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
