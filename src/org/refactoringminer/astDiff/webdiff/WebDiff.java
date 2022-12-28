package org.refactoringminer.astDiff.webdiff;
;
import com.github.gumtreediff.utils.Pair;
import org.refactoringminer.astDiff.models.DiffInfo;
import org.refactoringminer.astDiff.matchers.DirComparator;
import org.refactoringminer.astDiff.actions.ASTDiff;
import org.refactoringminer.astDiff.models.ProjectASTDiff;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;
import spark.Spark;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import static spark.Spark.*;

public class WebDiff  {
    public static final String JQUERY_JS_URL = "https://code.jquery.com/jquery-3.4.1.min.js";
    public static final String BOOTSTRAP_CSS_URL = "https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css";
    public static final String BOOTSTRAP_JS_URL = "https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js";
    public static final int port = 5678;

    public ProjectASTDiff projectASTDiff;


    public WebDiff(ProjectASTDiff projectASTDiff) {
        this.projectASTDiff = projectASTDiff;
    }

    public void run() {
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
            Pair<String, String> pair = comperator.getFileContentsPair(id);
            DiffInfo diffInfo = comperator.getDiffInfo(id);
            ASTDiff diff = projectASTDiff.getASTDiff(diffInfo);
            Renderable view = new VanillaDiffView(diffInfo.first,diffInfo.second,pair.first, pair.second, diff, false);
            return render(view);
        });
        get("/monaco-diff/:id", (request, response) -> {
            int id = Integer.parseInt(request.params(":id"));
            Pair<String, String> pair = comperator.getFileContentsPair(id);
            DiffInfo diffInfo = comperator.getDiffInfo(id);
            ASTDiff diff = projectASTDiff.getASTDiff(diffInfo);
            Renderable view = new MonacoDiffView(diffInfo.first,diffInfo.second,pair.first, pair.second, diff,  id,false);
            return render(view);
        });
//        get("/raw-diff/:id", (request, response) -> {
//            int id = Integer.parseInt(request.params(":id"));
//            Pair<File, File> pair = comparator.getModifiedFiles().get(id);
//            ASTDiff diff = projectASTDiff.astDiffByName(pair.first.getAbsolutePath());
//            Renderable view = new TextDiffView(pair.first, pair.second, diff,  id);
//            return render(view);
//        });
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
