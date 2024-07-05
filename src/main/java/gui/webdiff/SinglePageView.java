package gui.webdiff;

import org.refactoringminer.astDiff.models.ASTDiff;
import org.rendersnake.DocType;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;
import spark.Response;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.rendersnake.HtmlAttributesFactory.*;

/* Created by pourya on 2024-07-04*/
public class SinglePageView extends DirectoryDiffView implements Renderable {

    public SinglePageView(DirComparator comparator) {
        super(comparator);
    }

    @Override
    public void renderOn(HtmlCanvas html) {
        int n = comparator.getNumOfDiffs();
        try {
            html
                    .render(DocType.HTML5)
                    .html(lang("en")).render(new Header())
                    .body()
                    .div(class_("container-fluid"))
                    .div(class_("row h-100"))
                    // DirDiffComperator 1/5 width
                    .div(class_("col-2 bg-light dir-diff"))
                    .h3().content("DirDiffComperator")
                    // Content for DirDiffComperator (add your content here)
                    ._div()
                    // Monaco editors 4/5 width
                    .div(class_("col-10 monaco-panel"))
                    .div(id("accordion"));

            // Generate panels for /monaco-0 to /monaco-n
            for (int i = 0; i < n; i++) {
//                int id = i;
//                ASTDiff astDiff = comparator.getASTDiff(id);
//                MonacoDiffView monacoDiffView = new MonacoDiffView(
//                        "", astDiff.getSrcPath(), astDiff.getDstPath(),
//                        astDiff, id, comparator.getNumOfDiffs(), "",
//                        comparator.isMoveDiff(id)
//                );

                html.div(class_("card"))
                        .div(class_("card-header").id("heading-" + i))
                        .h5(class_("mb-0"))
                        .button(class_("btn btn-link")
                                .data("toggle", "collapse")
                                .data("target", "#collapse-" + i)
//                                .aria("expanded", "false") // Initially false
//                                .aria("controls", "collapse-" + i)
                        )
                        .content("Monaco Editor " + i)
                        ._h5()
                        ._div()
                        .div(id("collapse-" + i)
//                                .class_("collapse")
//                                .aria("labelledby", "heading-" + i)
                                .data("parent", "#accordion"))
                        .div(class_("card-body"))
                        .iframe(src("/monaco-page/" + i)
                        .style("width: 100%; height: 500px; border: none;"))
                        ._iframe()
                        ._div()
                        ._div()
                        ._div();
            }

            html._div() // Close accordion div
                    ._div() // Close monaco-panel div
                    ._div() // Close row div
                    ._div(); // Close container-fluid div

            html._body()
                    ._html();

        } catch (Exception e) {
            System.out.println(e);
            // Handle exception
        }

    }

    // Method to fetch content from a URL using HttpURLConnection
    private String fetchContentFromUrl(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        // Read the response into a StringBuilder
        StringBuilder content = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = in.readLine()) != null) {
                content.append(line);
            }
        } finally {
            conn.disconnect();
        }

        return content.toString();
    }
}
