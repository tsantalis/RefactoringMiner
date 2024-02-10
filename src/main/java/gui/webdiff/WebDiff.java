package gui.webdiff;

import com.github.gumtreediff.utils.Pair;
import org.refactoringminer.astDiff.actions.ASTDiff;
import org.refactoringminer.astDiff.actions.ProjectASTDiff;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;
import spark.Spark;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import static spark.Spark.*;

;

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
        DirComparator comperator = new DirComparator(projectASTDiff);
        configureSpark(comperator, this.port);
        Spark.awaitInitialization();
        System.out.println(String.format("Starting server: %s:%d.", "http://127.0.0.1", this.port));
    }

    public void configureSpark(final DirComparator comperator, int port) {
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
            Renderable view = new DirectoryDiffView(comperator);
            return render(view);
        });
        get("/vanilla-diff/:id", (request, response) -> {
            int id = Integer.parseInt(request.params(":id"));
            ASTDiff astDiff = comperator.getASTDiff(id);
            Renderable view = new VanillaDiffView(toolName, astDiff.getSrcPath(),astDiff.getDstPath(),
                    projectASTDiff.getFileContentsBefore().get(astDiff.getSrcPath()),
                    projectASTDiff.getFileContentsAfter().get(astDiff.getDstPath()),
                    astDiff, false);
            return render(view);
        });
        get("/monaco-diff/:id", (request, response) -> {
            int id = Integer.parseInt(request.params(":id"));
            ASTDiff astDiff = comperator.getASTDiff(id);
            Renderable view = new MonacoDiffView(toolName,astDiff.getSrcPath(),astDiff.getDstPath(),
                    projectASTDiff.getFileContentsBefore().get(astDiff.getSrcPath()),
                    projectASTDiff.getFileContentsAfter().get(astDiff.getDstPath()),
                    astDiff, id,false);
            return render(view);
        });
        get("/left/:id", (request, response) -> {
            int id = Integer.parseInt(request.params(":id"));
//            String id = (request.params(":id"));
//            String _id = id.replace("*","/");
            Pair<String, String> pair = comperator.getFileContentsPair(id);
            return pair.first;
        });
        get("/right/:id", (request, response) -> {
            int id = Integer.parseInt(request.params(":id"));
//            String id = (request.params(":id"));
//            String _id = id.replace("*","/");
            Pair<String, String> pair = comperator.getFileContentsPair(id);
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

    private static String readFile(String path, Charset encoding)  throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }
}
